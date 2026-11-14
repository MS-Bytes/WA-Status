package com.msbytes.statussaver.domain.model

/** Thrown when the status folders cannot be read. */
sealed class StatusAccessException(message: String) : Exception(message) {
    /** No folder permission, or the user revoked it in system settings. */
    class NoAccess : StatusAccessException("No access to the status folder")

    /** Access exists but no .Statuses folder was found (WhatsApp never opened / reinstalled). */
    class FolderMissing : StatusAccessException("Status folder not found")
}
