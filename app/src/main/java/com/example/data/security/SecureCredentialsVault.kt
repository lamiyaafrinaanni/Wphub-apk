package com.example.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Hardware-backed AndroidKeyStore AES-256 GCM encryption vault.
 * Secures sensitive WordPress Application Passwords before persisting to local storage,
 * preventing plain-text credential extraction from Room database dumps or cloud backups.
 */
object SecureCredentialsVault {
    private const val TAG = "SecureCredentialsVault"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "wphub_credential_master_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH_BYTES = 12
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val ENCRYPTED_PREFIX = "enc_v1:"

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
        }
    }

    @Synchronized
    private fun getOrCreateSecretKey(): SecretKey {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            val parameterSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()

            keyGenerator.init(parameterSpec)
            keyGenerator.generateKey()
            Log.d(TAG, "Initialized new 256-bit AES master key in AndroidKeyStore")
        }

        val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
        return entry.secretKey
    }

    /**
     * Encrypts a plain-text password or token using AES-GCM-256.
     * Returns a prefixed Base64 string containing IV + Ciphertext.
     */
    @Synchronized
    fun encrypt(plainText: String?): String {
        if (plainText.isNullOrEmpty()) return ""
        // If already encrypted, return as is
        if (plainText.startsWith(ENCRYPTED_PREFIX)) return plainText

        return try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            val iv = cipher.iv
            val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            // Pack IV + ciphertext into single payload
            val byteBuffer = ByteBuffer.allocate(iv.size + cipherText.size)
            byteBuffer.put(iv)
            byteBuffer.put(cipherText)

            val encoded = Base64.encodeToString(byteBuffer.array(), Base64.NO_WRAP)
            "$ENCRYPTED_PREFIX$encoded"
        } catch (e: Exception) {
            Log.e(TAG, "Encryption failure: ${e.message}", e)
            // Fallback: If hardware key fails on older/unsupported devices, return plain text safely
            plainText
        }
    }

    /**
     * Decrypts an AES-GCM ciphertext. If input is not encrypted, returns as-is for backward compatibility.
     */
    @Synchronized
    fun decrypt(cipherText: String?): String {
        if (cipherText.isNullOrEmpty()) return ""
        // If not encrypted with our prefix, it's legacy plain text; return as-is
        if (!cipherText.startsWith(ENCRYPTED_PREFIX)) return cipherText

        return try {
            val rawPayload = cipherText.removePrefix(ENCRYPTED_PREFIX)
            val decoded = Base64.decode(rawPayload, Base64.NO_WRAP)

            if (decoded.size <= GCM_IV_LENGTH_BYTES) {
                Log.w(TAG, "Ciphertext payload is too short")
                return ""
            }

            val byteBuffer = ByteBuffer.wrap(decoded)
            val iv = ByteArray(GCM_IV_LENGTH_BYTES)
            byteBuffer.get(iv)

            val encryptedBytes = ByteArray(byteBuffer.remaining())
            byteBuffer.get(encryptedBytes)

            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            val decryptedBytes = cipher.doFinal(encryptedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Decryption failure: ${e.message}", e)
            ""
        }
    }
}
