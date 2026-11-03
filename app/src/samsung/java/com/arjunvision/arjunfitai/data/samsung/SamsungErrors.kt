package com.arjunvision.arjunfitai.data.samsung

import com.arjunvision.arjunfitai.data.health.HealthDataError
import com.arjunvision.arjunfitai.data.health.HealthDataError.Reason
import com.arjunvision.arjunfitai.data.health.SourceKind
import com.samsung.android.sdk.health.data.error.AuthorizationException
import com.samsung.android.sdk.health.data.error.ErrorCode
import com.samsung.android.sdk.health.data.error.HealthDataException
import com.samsung.android.sdk.health.data.error.ResolvablePlatformException

/** Translates a Samsung Health Data SDK failure into a [HealthDataError], keeping Samsung's own fix-up flow. */
internal fun samsungError(throwable: Throwable): HealthDataError {
    if (throwable is HealthDataError) return throwable
    if (throwable !is HealthDataException) {
        return HealthDataError(SourceKind.SAMSUNG, Reason.UNKNOWN, throwable.message ?: "Unexpected error", throwable)
    }
    val code = throwable.errorCode
    val resolvable = (throwable as? ResolvablePlatformException)?.takeIf { it.hasResolution }
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
    return HealthDataError(
        SourceKind.SAMSUNG, reason, describe(reason, throwable.errorMessage), throwable,
        fix = resolvable?.let { r -> { activity -> r.resolve(activity) } },
    )
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
    Reason.UNSUPPORTED -> "Samsung Health can't be used on this phone."
    Reason.UNKNOWN -> sdkMessage.ifBlank { "Samsung Health returned an error." }
}
