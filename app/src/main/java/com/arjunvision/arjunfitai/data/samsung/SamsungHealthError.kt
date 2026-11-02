package com.arjunvision.arjunfitai.data.samsung

import android.app.Activity
import com.samsung.android.sdk.health.data.error.AuthorizationException
import com.samsung.android.sdk.health.data.error.ErrorCode
import com.samsung.android.sdk.health.data.error.HealthDataException
import com.samsung.android.sdk.health.data.error.ResolvablePlatformException

/** A Samsung Health failure translated into something the UI can explain and, sometimes, fix. */
class SamsungHealthError(
    val reason: Reason,
    message: String,
    val errorCode: Int? = null,
    private val resolvable: ResolvablePlatformException? = null,
    cause: Throwable? = null,
) : Exception(message, cause) {

    enum class Reason {
        NOT_INSTALLED,
        UPDATE_REQUIRED,
        DISABLED,
        /** Samsung Health installed but its first-run setup / terms not completed. */
        NOT_INITIALIZED,
        PERMISSION_DENIED,
        /** Caller not allowed: usually Developer Mode for Data Read is off for an unregistered app. */
        ACCESS_DENIED,
        CONNECTION,
        UNKNOWN,
    }

    /** True when Samsung Health can fix this itself (install, update, enable, finish setup). */
    val canResolve: Boolean get() = resolvable?.hasResolution == true

    /** Launches Samsung Health's own fix-up flow. Call [SamsungHealthRepository.checkPermissions] again on return. */
    fun resolve(activity: Activity) {
        resolvable?.takeIf { it.hasResolution }?.resolve(activity)
    }

    companion object {
        fun from(throwable: Throwable): SamsungHealthError {
            if (throwable is SamsungHealthError) return throwable
            if (throwable !is HealthDataException) {
                return SamsungHealthError(Reason.UNKNOWN, throwable.message ?: "Unexpected error", cause = throwable)
            }
            val code = throwable.errorCode
            val resolvable = throwable as? ResolvablePlatformException
            val reason = when {
                code == ErrorCode.ERR_PLATFORM_NOT_INSTALLED -> Reason.NOT_INSTALLED
                code == ErrorCode.ERR_OLD_VERSION_PLATFORM -> Reason.UPDATE_REQUIRED
                code == ErrorCode.ERR_PLATFORM_DISABLED -> Reason.DISABLED
                code == ErrorCode.ERR_PLATFORM_NOT_INITIALIZED -> Reason.NOT_INITIALIZED
                code == ErrorCode.ERR_NO_USER_PERMISSION || throwable is AuthorizationException -> Reason.PERMISSION_DENIED
                code == ErrorCode.ERR_ACCESS_CONTROL ||
                    code == ErrorCode.ERR_INVALID_CALLER ||
                    code == ErrorCode.ERR_INVALID_PLATFORM_SIGNATURE -> Reason.ACCESS_DENIED
                code == ErrorCode.ERR_CONNECTION_FAIL ||
                    code == ErrorCode.ERR_CONNECTION_TIMEOUT ||
                    code == ErrorCode.ERR_PLATFORM_DISCONNECTED -> Reason.CONNECTION
                else -> Reason.UNKNOWN
            }
            return SamsungHealthError(reason, describe(reason, throwable.errorMessage), code, resolvable, throwable)
        }

        private fun describe(reason: Reason, sdkMessage: String): String = when (reason) {
            Reason.NOT_INSTALLED -> "Samsung Health is not installed."
            Reason.UPDATE_REQUIRED -> "Samsung Health needs to be updated (6.30.2 or later)."
            Reason.DISABLED -> "Samsung Health is disabled. Enable it in system settings."
            Reason.NOT_INITIALIZED -> "Open Samsung Health once and finish its setup and terms."
            Reason.PERMISSION_DENIED -> "ArjunFit does not have permission to read this data."
            Reason.ACCESS_DENIED ->
                "Samsung Health refused access. Turn on Samsung Health › Settings › About › " +
                    "Developer mode › Developer Mode for Data Read."
            Reason.CONNECTION -> "Could not connect to Samsung Health. Try again."
            Reason.UNKNOWN -> sdkMessage.ifBlank { "Samsung Health returned an error." }
        }
    }
}
