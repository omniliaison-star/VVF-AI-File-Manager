package com.example.util

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {
    private const val TAG = "CryptoUtils"
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_ALIAS = "VaultMasterKey"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128
    private const val SALT = "FileManagerVaultSalt"

    private var testKey: SecretKey? = null

    private fun isTestEnvironment(): Boolean {
        return Build.FINGERPRINT.lowercase().contains("robolectric") || 
               Build.MODEL.lowercase().contains("robolectric")
    }

    private fun getSecretKey(pin: String): SecretKey {
        if (pin.isNotEmpty()) {
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val spec = PBEKeySpec(pin.toCharArray(), SALT.toByteArray(Charsets.UTF_8), 10000, 256)
            val tmp = factory.generateSecret(spec)
            return SecretKeySpec(tmp.encoded, "AES")
        }

        if (isTestEnvironment()) {
            if (testKey == null) {
                val keyGen = KeyGenerator.getInstance("AES")
                keyGen.init(256)
                testKey = keyGen.generateKey()
            }
            return testKey!!
        }

        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
        }
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }

    fun encryptFile(inputFile: File, outputFile: File, pin: String): Boolean {
        return try {
            val secretKey = getSecretKey(pin)
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv

            inputFile.inputStream().use { input ->
                outputFile.outputStream().use { output ->
                    output.write(iv)
                    val buffer = ByteArray(4096)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        val outputBytes = cipher.update(buffer, 0, bytesRead)
                        if (outputBytes != null) {
                            output.write(outputBytes)
                        }
                    }
                    val finalBytes = cipher.doFinal()
                    if (finalBytes != null) {
                        output.write(finalBytes)
                    }
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "File encryption failed: ", e)
            outputFile.delete()
            false
        }
    }

    fun decryptFile(inputFile: File, outputFile: File, pin: String): Boolean {
        val tempOutputFile = File(outputFile.absolutePath + ".tmp")
        return try {
            val secretKey = getSecretKey(pin)
            var success = false
            inputFile.inputStream().use { input ->
                val iv = ByteArray(GCM_IV_LENGTH)
                if (input.read(iv) != GCM_IV_LENGTH) return false
                val cipher = Cipher.getInstance(ALGORITHM)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))

                tempOutputFile.outputStream().use { output ->
                    val buffer = ByteArray(4096)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        val outputBytes = cipher.update(buffer, 0, bytesRead)
                        if (outputBytes != null) {
                            output.write(outputBytes)
                        }
                    }
                    val finalBytes = cipher.doFinal()
                    if (finalBytes != null) {
                        output.write(finalBytes)
                    }
                    success = true
                }
            }
            if (success) {
                tempOutputFile.renameTo(outputFile)
                true
            } else {
                tempOutputFile.delete()
                outputFile.delete()
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "File decryption failed: ", e)
            tempOutputFile.delete()
            outputFile.delete()
            false
        }
    }

    fun securelyDeleteFile(file: File): Boolean {
        return try {
            if (file.exists()) {
                val length = file.length()
                if (length > 0) {
                    file.outputStream().use { output ->
                        val buffer = ByteArray(4096)
                        var remaining = length
                        while (remaining > 0) {
                            val toWrite = minOf(remaining, buffer.size.toLong()).toInt()
                            output.write(buffer, 0, toWrite)
                            remaining -= toWrite
                        }
                    }
                }
                file.delete()
            } else {
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Secure deletion failed: ", e)
            file.delete()
        }
    }

    fun calculateSHA256(file: File): String {
        return try {
            if (!file.exists()) return ""
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(4096)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            val hashBytes = digest.digest()
            hashBytes.joinToString("") { String.format("%02x", it) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to calculate SHA-256: ", e)
            ""
        }
    }

    fun encrypt(plainText: String, pin: String): String {
        if (plainText.isEmpty()) return plainText
        return try {
            val secretKey = getSecretKey(pin)
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(GCM_IV_LENGTH + encryptedBytes.size)
            System.arraycopy(iv, 0, combined, 0, GCM_IV_LENGTH)
            System.arraycopy(encryptedBytes, 0, combined, GCM_IV_LENGTH, encryptedBytes.size)
            Base64.encodeToString(combined, Base64.NO_WRAP).trim()
        } catch (e: Exception) {
            Log.e(TAG, "Encryption failed: ", e)
            plainText
        }
    }

    fun decrypt(encryptedText: String, pin: String): String {
        if (encryptedText.isEmpty()) return encryptedText
        return try {
            val combined = Base64.decode(encryptedText, Base64.DEFAULT)
            if (combined.size < GCM_IV_LENGTH) return encryptedText
            val iv = ByteArray(GCM_IV_LENGTH)
            val ciphertext = ByteArray(combined.size - GCM_IV_LENGTH)
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH)
            System.arraycopy(combined, GCM_IV_LENGTH, ciphertext, 0, ciphertext.size)
            val secretKey = getSecretKey(pin)
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
            val decryptedBytes = cipher.doFinal(ciphertext)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Decryption failed: ", e)
            encryptedText
        }
    }
}
