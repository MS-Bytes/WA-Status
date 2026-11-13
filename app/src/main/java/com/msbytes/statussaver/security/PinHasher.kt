package com.msbytes.statussaver.security

import java.security.MessageDigest

/** Salted SHA-256 for the App Lock PIN (the PIN itself is never stored). */
object PinHasher {
    fun hash(pin: String, salt: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest("$salt:$pin".toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
