package com.example.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Android Keystore-backed AES/GCM/NoPadding Cryptographic Utility.
 * Securely encrypts and decrypts Application Passwords and REST tokens.
 * All keys are generated and stored inside the Android Keystore system.
 */
object CryptKeeper {
    private const val PROVIDER = "AndroidKeyStore"
    private const val ALIAS = "WPMobileHubSecureKey"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    init {
        initKey()
    }

    @Synchronized
    private fun initKey() {
        try {
            val keyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }
            if (!keyStore.containsAlias(ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
                val spec = KeyGenParameterSpec.Builder(
                    ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setUserAuthenticationRequired(false) // Allows background synchronization
                    .build()
                keyGenerator.init(spec)
                keyGenerator.generateKey()
            }
        } catch (e: Exception) {
            // Log/Handle fallback gracefully
        }
    }

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }
        return (keyStore.getEntry(ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    }

    /**
     * Encrypts plain text into a base64 encoded string containing "ciphertext:iv".
     */
    fun encrypt(plainText: String): String {
        if (plainText.isBlank()) return ""
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            
            val encryptedBase64 = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
            val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
            
            "$encryptedBase64:$ivBase64"
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Decrypts "ciphertext:iv" back into its original plain text.
     */
    fun decrypt(encryptedData: String): String {
        if (encryptedData.isBlank()) return ""
        if (!encryptedData.contains(":")) return encryptedData // Safety migration fallback
        
        return try {
            val parts = encryptedData.split(":")
            if (parts.size < 2) return encryptedData
            
            val encryptedBytes = Base64.decode(parts[0], Base64.NO_WRAP)
            val ivBytes = Base64.decode(parts[1], Base64.NO_WRAP)
            
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, ivBytes)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
            
            String(cipher.doFinal(encryptedBytes), Charsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }
}
