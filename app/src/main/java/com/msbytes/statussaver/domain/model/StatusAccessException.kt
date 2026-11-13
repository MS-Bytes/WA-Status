package com.msbytes.statussaver.domain.model

/** Thrown when a status folder cannot be read. */
sealed class StatusAccessException(message: String) : Exception(message) {
    /** No folder permission, or the user revoked it in system settings. */
    class NoAccess : StatusAccessException("No access to the status folder")

    /** Permission exists but the folder is gone (e.g. WhatsApp reinstalled / cleared). */
    class FolderMissing : StatusAccessException("Status folder not found")
}
