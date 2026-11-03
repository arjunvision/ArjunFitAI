package com.arjunvision.arjunfitai.data.coach

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Your own Anthropic API key, entered in the app (never built into the APK). It is encrypted with
 * AES-GCM using a key held in the Android Keystore, which never leaves the device's secure hardware.
 * Only the ciphertext is saved, in app-private storage that is excluded from backups.
 */
class ApiKeyStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("coach_secure", Context.MODE_PRIVATE)

    fun hasKey(): Boolean = prefs.contains(CIPHERTEXT)

    fun save(apiKey: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, secretKey()) }
        val encrypted = cipher.doFinal(apiKey.trim().toByteArray(Charsets.UTF_8))
        prefs.edit()
            .putString(IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putString(CIPHERTEXT, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .apply()
    }

    /** The key, or null if none is saved or it can no longer be decrypted (e.g. after a device reset). */
    fun load(): String? {
        val iv = prefs.getString(IV, null) ?: return null
        val data = prefs.getString(CIPHERTEXT, null) ?: return null
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)))
            }
            String(cipher.doFinal(Base64.decode(data, Base64.NO_WRAP)), Charsets.UTF_8)
        }.getOrNull()
    }

    fun clear() {
        prefs.edit().clear().apply()
        runCatching { KeyStore.getInstance(KEYSTORE).apply { load(null) }.deleteEntry(ALIAS) }
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE).apply {
            init(
                KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build(),
            )
        }.generateKey()
    }

    private companion object {
        const val KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "arjunfit_claude_api_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV = "iv"
        const val CIPHERTEXT = "ciphertext"
    }
}

/**
 * A quick shape check before saving: an Anthropic key ("sk-ant-…", workspace or account-linked) with
 * no spaces. Admin keys and login tokens can't call Claude, so they're refused here; whether the key
 * actually works is decided by the API on the first message.
 */
fun looksLikeApiKey(text: String): Boolean {
    val t = text.trim()
    return t.startsWith("sk-ant-") && !t.startsWith("sk-ant-admin") && !t.startsWith("sk-ant-oat") &&
        t.length >= 40 && t.none { it.isWhitespace() }
}

/** Why [looksLikeApiKey] refused [text] — showing at most its first 10 characters. */
fun apiKeyProblem(text: String): String? {
    val t = text.trim()
    return when {
        t.isEmpty() || looksLikeApiKey(t) -> null
        t.any { it.isWhitespace() } -> "The key has a space or line break in it. Copy it again from the Console."
        t.startsWith("sk-ant-admin") -> "That's an admin key — it can't chat. Create a regular API key."
        t.startsWith("sk-ant-oat") -> "That's a login token, not an API key. Create an API key in the Console."
        !t.startsWith("sk-ant-") -> "Anthropic API keys start with sk-ant- (yours starts with \"${t.take(10)}\")."
        else -> "That key looks too short. Copy the whole key from the Console."
    }
}
