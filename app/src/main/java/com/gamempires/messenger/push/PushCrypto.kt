package com.gamempires.messenger.push

import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import javax.crypto.Cipher

/**
 * Crypto used by the Nextcloud Notifications push-v2 protocol.
 *
 * Server side (nextcloud/notifications lib/Push.php):
 *  - subject = base64(openssl_public_encrypt(json, devicePublicKey, OPENSSL_PKCS1_OAEP_PADDING))  -> RSA-OAEP, SHA-1/MGF1-SHA-1
 *  - signature = base64(openssl_sign(encryptedSubjectBytes, userPrivateKey, OPENSSL_ALGO_SHA512)) -> SHA512withRSA (PKCS#1 v1.5)
 */
object PushCrypto {
    private const val OAEP = "RSA/ECB/OAEPWithSHA-1AndMGF1Padding"
    private const val SIGNATURE = "SHA512withRSA"

    fun generateDeviceKeyPair(): KeyPair =
        KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.genKeyPair()

    /**
     * PEM exactly as Nextcloud validates it: "-----BEGIN PUBLIC KEY-----\n", 64-character lines,
     * "\n-----END PUBLIC KEY-----", 450 characters in total for an RSA-2048 key.
     */
    fun publicKeyToPem(key: PublicKey): String {
        val body = Base64.getEncoder().encodeToString(key.encoded).chunked(64).joinToString("\n")
        return "-----BEGIN PUBLIC KEY-----\n$body\n-----END PUBLIC KEY-----"
    }

    fun parsePublicKeyPem(pem: String): PublicKey {
        val body = pem.replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace(Regex("\\s"), "")
        return KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(body)))
    }

    fun privateKeyToBase64(key: PrivateKey): String = Base64.getEncoder().encodeToString(key.encoded)

    fun privateKeyFromBase64(value: String): PrivateKey =
        KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(Base64.getDecoder().decode(value)))

    fun sha512Hex(value: String): String =
        MessageDigest.getInstance("SHA-512").digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    /** Returns the decrypted JSON, or null when the signature does not match (push not from our server). */
    fun verifyAndDecrypt(subjectBase64: String, signatureBase64: String, userPublicKey: PublicKey, devicePrivateKey: PrivateKey): String? {
        val decoder = Base64.getMimeDecoder()
        val encrypted = decoder.decode(subjectBase64)
        val signatureBytes = decoder.decode(signatureBase64)
        val verifier = Signature.getInstance(SIGNATURE)
        verifier.initVerify(userPublicKey)
        verifier.update(encrypted)
        if (!verifier.verify(signatureBytes)) return null
        val cipher = Cipher.getInstance(OAEP)
        cipher.init(Cipher.DECRYPT_MODE, devicePrivateKey)
        return String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }
}
