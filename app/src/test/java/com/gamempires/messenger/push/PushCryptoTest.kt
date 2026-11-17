package com.gamempires.messenger.push

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.Signature
import java.util.Base64
import javax.crypto.Cipher

class PushCryptoTest {
    @Test
    fun publicKeyPemMatchesNextcloudValidation() {
        val pem = PushCrypto.publicKeyToPem(PushCrypto.generateDeviceKeyPair().public)
        assertTrue(pem.startsWith("-----BEGIN PUBLIC KEY-----\n"))
        assertEquals(450, pem.length)
        assertEquals(425, pem.indexOf("\n-----END PUBLIC KEY-----"))
    }

    @Test
    fun tokenHashIs128LowercaseHex() {
        assertTrue(Regex("^[a-f0-9]{128}$").matches(PushCrypto.sha512Hex("fcm-token")))
    }

    @Test
    fun verifiesAndDecryptsServerStylePush() {
        val device = PushCrypto.generateDeviceKeyPair()
        val user = PushCrypto.generateDeviceKeyPair()
        val json = """{"nid":7,"app":"spreed","subject":"Ali sent you a message","type":"chat","id":"tok12345"}"""
        // Same as Nextcloud: OAEP encrypt with the device key, SHA-512 sign the ciphertext with the user key.
        val encrypted = Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding")
            .apply { init(Cipher.ENCRYPT_MODE, device.public) }.doFinal(json.toByteArray())
        val signature = Signature.getInstance("SHA512withRSA").apply { initSign(user.private); update(encrypted) }.sign()
        val b64 = Base64.getEncoder()
        val userPem = PushCrypto.publicKeyToPem(user.public)

        assertEquals(json, PushCrypto.verifyAndDecrypt(b64.encodeToString(encrypted), b64.encodeToString(signature),
            PushCrypto.parsePublicKeyPem(userPem), device.private))

        val tampered = encrypted.copyOf().also { it[3] = (it[3].toInt() xor 1).toByte() }
        assertNull(PushCrypto.verifyAndDecrypt(b64.encodeToString(tampered), b64.encodeToString(signature),
            user.public, device.private))
    }
}
