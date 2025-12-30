package com.pacedream.app.core.auth

/**
 * Auth0 Connection names (match iOS behavior).
 */
enum class Auth0Connection(val connection: String) {
    Google("google-oauth2"),
    Apple("apple")
}

