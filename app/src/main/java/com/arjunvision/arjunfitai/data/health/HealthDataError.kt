package com.arjunvision.arjunfitai.data.health

import android.app.Activity

/** A health-source failure translated into something the UI can explain and, sometimes, fix. */
class HealthDataError(
    val source: SourceKind?,
    val reason: Reason,
    message: String,
    cause: Throwable? = null,
    /** The source's own fix-up flow (install, update, enable, finish setup), when it has one. */
    private val fix: ((Activity) -> Unit)? = null,
) : Exception(message, cause) {

    enum class Reason {
        NOT_INSTALLED,
        UPDATE_REQUIRED,
        DISABLED,
        /** Installed but its first-run setup / terms not completed. */
        NOT_INITIALIZED,
        PERMISSION_DENIED,
        /** Caller not allowed: for Samsung, usually Developer Mode for Data Read is off for an unregistered app. */
        ACCESS_DENIED,
        CONNECTION,
        /** This build or this phone can't use the source at all. */
        UNSUPPORTED,
        UNKNOWN,
    }

    /** True when the source can fix this itself. */
    val canResolve: Boolean get() = fix != null

    /** Launches the source's fix-up flow. Check permissions again when the user comes back. */
    fun resolve(activity: Activity) {
        fix?.invoke(activity)
    }

    companion object {
        fun from(throwable: Throwable, source: SourceKind? = null): HealthDataError =
            throwable as? HealthDataError
                ?: HealthDataError(source, Reason.UNKNOWN, throwable.message ?: "Unexpected error", cause = throwable)
    }
}
