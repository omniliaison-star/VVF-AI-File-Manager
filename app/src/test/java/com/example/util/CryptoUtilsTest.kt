package com.example.util

import android.util.Base64
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.util.Random

@RunWith(RobolectricTestRunner::class)
class CryptoUtilsTest {

    @Test
    fun testTextEncryptionDecryption() {
        val originalText = "SuperSecretMessage"
        val encrypted = CryptoUtils.encrypt(originalText, "1234")
        assertNotEquals(originalText, encrypted)

        val decrypted = CryptoUtils.decrypt(encrypted, "1234")
        assertEquals(originalText, decrypted)
    }

    @Test
    fun testTextEncryptionEmpty() {
        val encrypted = CryptoUtils.encrypt("", "1234")
        assertEquals("", encrypted)
        
        val decrypted = CryptoUtils.decrypt("", "1234")
        assertEquals("", decrypted)
    }

    @Test
    fun testTextDecryptionWrongCiphertext() {
        val originalText = "SuperSecretMessage"
        val wrongCiphertext = Base64.encodeToString("completelyWrongCiphertext123456789".toByteArray(), Base64.NO_WRAP)
        val decrypted = CryptoUtils.decrypt(wrongCiphertext, "1234")
        // Should not crash, and should not return original text. 
        // CryptoUtils catches Exception and returns the passed string back on failure.
        assertEquals(wrongCiphertext, decrypted)
    }

    @Test
    fun testTextDecryptionModifiedCiphertext() {
        val originalText = "SuperSecretMessage"
        val encrypted = CryptoUtils.encrypt(originalText, "1234")
        // Modify ciphertext
        val modified = encrypted.substring(0, encrypted.length - 2) + "a"
        val decrypted = CryptoUtils.decrypt(modified, "1234")
        // Decryption fails, returns the input string
        assertEquals(modified, decrypted)
    }

    @Test
    fun testFileEncryptionDecryption() {
        val originalText = "This is a secret file content."
        val inputFile = File.createTempFile("test_input", ".txt")
        inputFile.writeText(originalText)
        
        val encryptedFile = File.createTempFile("test_encrypted", ".enc")
        val decryptedFile = File.createTempFile("test_decrypted", ".txt")

        assertTrue(CryptoUtils.encryptFile(inputFile, encryptedFile, "1234"))
        assertTrue(encryptedFile.exists())
        assertTrue(encryptedFile.length() > 0)
        assertNotEquals(originalText, encryptedFile.readText())

        assertTrue(CryptoUtils.decryptFile(encryptedFile, decryptedFile, "1234"))
        assertTrue(decryptedFile.exists())
        assertEquals(originalText, decryptedFile.readText())

        inputFile.delete()
        encryptedFile.delete()
        decryptedFile.delete()
    }

    @Test
    fun testFileDecryptionModifiedCiphertext() {
        val originalText = "This is a secret file content."
        val inputFile = File.createTempFile("test_input", ".txt")
        inputFile.writeText(originalText)
        
        val encryptedFile = File.createTempFile("test_encrypted", ".enc")
        val decryptedFile = File.createTempFile("test_decrypted", ".txt")

        assertTrue(CryptoUtils.encryptFile(inputFile, encryptedFile, "1234"))
        
        // Modify encrypted file
        val bytes = encryptedFile.readBytes()
        bytes[bytes.size - 1] = bytes[bytes.size - 1].inc()
        encryptedFile.writeBytes(bytes)

        // Decrypting tampered file should fail with GCM or at least result in a false return.
        val result = CryptoUtils.decryptFile(encryptedFile, decryptedFile, "1234")
        assertFalse("Decryption of tampered file should fail", result)

        inputFile.delete()
        encryptedFile.delete()
        decryptedFile.delete()
    }

    @Test
    fun testFileDecryptionModifiedIV() {
        val originalText = "This is a secret file content."
        val inputFile = File.createTempFile("test_input", ".txt")
        inputFile.writeText(originalText)
        
        val encryptedFile = File.createTempFile("test_encrypted", ".enc")
        val decryptedFile = File.createTempFile("test_decrypted", ".txt")

        assertTrue(CryptoUtils.encryptFile(inputFile, encryptedFile, "1234"))
        
        // Modify IV (first 12 bytes)
        val bytes = encryptedFile.readBytes()
        bytes[0] = bytes[0].inc()
        encryptedFile.writeBytes(bytes)

        // Decrypting tampered file should fail
        val result = CryptoUtils.decryptFile(encryptedFile, decryptedFile, "1234")
        assertFalse("Decryption with tampered IV should fail", result)

        inputFile.delete()
        encryptedFile.delete()
        decryptedFile.delete()
    }

    @Test
    fun testLargeFileEncryptionDecryption() {
        val inputFile = File.createTempFile("large_test_input", ".txt")
        
        // Generate a 50MB file
        val random = Random()
        val chunk = ByteArray(1024 * 1024)
        inputFile.outputStream().use { out ->
            for (i in 0 until 50) {
                random.nextBytes(chunk)
                out.write(chunk)
            }
        }
        
        val originalHash = CryptoUtils.calculateSHA256(inputFile)
        
        val encryptedFile = File.createTempFile("large_test_encrypted", ".enc")
        val decryptedFile = File.createTempFile("large_test_decrypted", ".txt")

        assertTrue("Large file encryption failed", CryptoUtils.encryptFile(inputFile, encryptedFile, "1234"))
        assertTrue("Encrypted file should exist and have size", encryptedFile.length() > 50 * 1024 * 1024)

        assertTrue("Large file decryption failed", CryptoUtils.decryptFile(encryptedFile, decryptedFile, "1234"))
        
        val decryptedHash = CryptoUtils.calculateSHA256(decryptedFile)
        
        assertEquals("Decrypted file hash does not match original", originalHash, decryptedHash)

        inputFile.delete()
        encryptedFile.delete()
        decryptedFile.delete()
    }
}
