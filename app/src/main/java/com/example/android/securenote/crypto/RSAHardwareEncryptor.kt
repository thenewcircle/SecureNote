package com.example.android.securenote.crypto

import android.content.Context
import android.content.SharedPreferences
import android.security.KeyPairGeneratorSpec
import android.util.Base64
import android.util.Base64InputStream
import android.util.Base64OutputStream
import android.util.Log

import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.math.BigInteger
import java.security.GeneralSecurityException
import java.security.Key
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.NoSuchAlgorithmException
import java.security.PublicKey
import java.security.spec.InvalidKeySpecException
import java.security.spec.X509EncodedKeySpec
import java.util.Calendar
import java.util.Date

import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.security.auth.x500.X500Principal

class RSAHardwareEncryptor(context: Context) {

    private val publicKeyStore: SharedPreferences

    init {
        publicKeyStore = context.getSharedPreferences(
                "publickey.store", Context.MODE_PRIVATE)
        try {
            if (!publicKeyStore.contains(KEY_PUBLIC)) {
                generatePrivateKey(context)
                Log.d(TAG, "Generated hardware-bound key")
            } else {
                Log.d(TAG, "Hardware key pair already exists")
            }
        } catch (e: Exception) {
            throw RuntimeException("Unable to generate key material.")
        }

    }

    /**
     * Return a cipher text blob of encrypted data, Base64 encoded.
     *
     * @throws GeneralSecurityException
     * @throws IOException
     */
    @Throws(GeneralSecurityException::class, IOException::class)
    fun encryptData(data: ByteArray, out: OutputStream) {
        var out = out
        val key = retrievePublicKey()
        val cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key)

        //Encode output to file
        out = Base64OutputStream(out, Base64.NO_WRAP)
        //Encrypt output to encoder
        out = CipherOutputStream(out, cipher)

        try {
            out.write(data)
            out.flush()
        } finally {
            out.close()
        }
    }

    /**
     * Return decrypted data from the received cipher text blob.
     *
     * @throws GeneralSecurityException
     * @throws IOException
     */
    @Throws(GeneralSecurityException::class, IOException::class)
    fun decryptData(`in`: InputStream): ByteArray {
        var `in` = `in`
        val key = retrievePrivateKey()
        val cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, key)

        //Decode input from file
        `in` = Base64InputStream(`in`, Base64.NO_WRAP)
        //Decrypt input from decoder
        `in` = CipherInputStream(`in`, cipher)

        return readFile(`in`).toByteArray()
    }

    @Throws(NoSuchAlgorithmException::class, InvalidKeySpecException::class)
    fun retrievePublicKey(): Key {
        val encodedKey = publicKeyStore.getString(KEY_PUBLIC, null)
                ?: throw RuntimeException("Expected valid public key!")

        val publicKey = Base64.decode(encodedKey, Base64.NO_WRAP)
        return KeyFactory.getInstance(KEY_ALGORITHM)
                .generatePublic(X509EncodedKeySpec(publicKey))
    }

    @Throws(GeneralSecurityException::class, IOException::class)
    fun retrievePrivateKey(): Key? {
        val ks = KeyStore.getInstance(PROVIDER_NAME)
        ks.load(null)
        val entry = ks.getEntry(KEY_ALIAS, null)
        if (entry !is KeyStore.PrivateKeyEntry) {
            Log.w(TAG, "Not an instance of a PrivateKeyEntry")
            return null
        }

        return entry.privateKey
    }

    @Throws(GeneralSecurityException::class)
    private fun generatePrivateKey(context: Context) {
        val cal = Calendar.getInstance()
        val now = cal.time
        cal.add(Calendar.YEAR, 1)
        val end = cal.time

        val kpg = KeyPairGenerator.getInstance(KEY_ALGORITHM, PROVIDER_NAME)
        kpg.initialize(KeyPairGeneratorSpec.Builder(context)
                .setAlias(KEY_ALIAS)
                .setStartDate(now)
                .setEndDate(end)
                .setSerialNumber(BigInteger.valueOf(1))
                .setSubject(X500Principal("CN=$KEY_ALIAS"))
                .build())

        //Generate and bind the private key to hardware
        val kp = kpg.generateKeyPair()

        //Persist the public key
        val publicKey = kp.public
        val encodedKey = Base64.encodeToString(publicKey.encoded, Base64.NO_WRAP)
        publicKeyStore.edit().putString(KEY_PUBLIC, encodedKey).apply()
    }

    @Throws(IOException::class)
    private fun readFile(`in`: InputStream): String {
        val reader = InputStreamReader(`in`)
        val sb = StringBuilder()

        val inputBuffer = CharArray(2048)
        var read: Int
        while ((read = reader.read(inputBuffer)) != -1) {
            sb.append(inputBuffer, 0, read)
        }

        return sb.toString()
    }

    companion object {
        private val TAG = RSAHardwareEncryptor::class.java.simpleName
        private val PROVIDER_NAME = "AndroidKeyStore"
        private val KEY_ALGORITHM = "RSA"
        private val ENCRYPTION_ALGORITHM = "RSA/ECB/PKCS1Padding"

        private val KEY_PUBLIC = "publickey"
        private val KEY_ALIAS = "secureKeyAlias"
    }
}
