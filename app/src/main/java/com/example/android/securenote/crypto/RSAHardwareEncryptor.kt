package com.example.android.securenote.crypto

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.security.KeyPairGeneratorSpec
import android.util.Base64
import android.util.Log

import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.math.BigInteger
import java.security.GeneralSecurityException
import java.security.Key
import java.security.KeyPairGenerator
import java.security.NoSuchAlgorithmException
import java.security.spec.InvalidKeySpecException
import java.util.Calendar
import javax.security.auth.x500.X500Principal
import android.security.keystore.KeyProperties
import android.security.keystore.KeyGenParameterSpec


class RSAHardwareEncryptor(context: Context) {

    //Persistent location where we will save the public key
    private val publicKeyStore: SharedPreferences = context.getSharedPreferences(
            "publickey.store", Context.MODE_PRIVATE)

    init {
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
     * Create a self-signed certificate and private key in hardware storage.
     * Persist the (non-secret) public key into SharedPreferences.
     *
     * @throws GeneralSecurityException
     */
    @Throws(GeneralSecurityException::class)
    private fun generatePrivateKey(context: Context) {
        val cal = Calendar.getInstance()
        val now = cal.time
        cal.add(Calendar.YEAR, 1)
        val end = cal.time

        val kpg = KeyPairGenerator.getInstance(KEY_ALGORITHM, PROVIDER_NAME)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            kpg.initialize(KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_SIGN)
                    .setKeyValidityStart(now)
                    .setKeyValidityEnd(end)
                    .setCertificateSerialNumber(BigInteger.valueOf(1))
                    .setCertificateSubject(X500Principal("CN=$KEY_ALIAS"))
                    .build())
        } else {
            @Suppress("DEPRECATION")
            kpg.initialize(KeyPairGeneratorSpec.Builder(context)
                    .setAlias(KEY_ALIAS)
                    .setStartDate(now)
                    .setEndDate(end)
                    .setSerialNumber(BigInteger.valueOf(1))
                    .setSubject(X500Principal("CN=$KEY_ALIAS"))
                    .build())
        }

        //Generate and bind the private key to hardware
        val kp = kpg.generateKeyPair()

        //Persist the public key
        val publicKey = kp.public
        val encodedKey = Base64.encodeToString(publicKey.encoded, Base64.NO_WRAP)
        publicKeyStore.edit().putString(KEY_PUBLIC, encodedKey).apply()
    }

    /**
     * Return a cipher text blob of encrypted data, Base64 encoded.
     *
     * @throws GeneralSecurityException
     * @throws IOException
     */
    @Throws(GeneralSecurityException::class, IOException::class)
    fun encryptData(data: ByteArray, out: OutputStream) {
        /*
         * TODO: Encryption Lab:
         * Obtain the public key for encryption
         * Create and init a Cipher (with ENCRYPTION_ALGORITHM)
         * Wrap the supplied stream in another provides Base64 encoding
         * Wrap the encoding stream in another that encrypts with cipher
         * Write the supplied data to the streams.
         */
    }

    /**
     * Return decrypted data from the received cipher text blob.
     *
     * @throws GeneralSecurityException
     * @throws IOException
     */
    @Throws(GeneralSecurityException::class, IOException::class)
    fun decryptData(input: InputStream): ByteArray? {
        /*
         * TODO: Encryption Lab:
         * Obtain the private key for decryption
         * Create and init a Cipher (with ENCRYPTION_ALGORITHM)
         * Wrap the supplied stream in another parses Base64 encoding
         * Wrap the encoding stream in another that decrypts with cipher
         * Read the stream fully and return the decrypted bytes
         */
        return null
    }

    @Throws(NoSuchAlgorithmException::class, InvalidKeySpecException::class)
    fun retrievePublicKey(): Key? {
        /*
         * TODO: Encryption Lab:
         * Get the encoded key from SharedPreferences
         * Decode the key (from Base64) to raw bytes
         * Return a public key instance from the bytes using KeyFactory
         */
        return null
    }

    @Throws(GeneralSecurityException::class, IOException::class)
    fun retrievePrivateKey(): Key? {
        /*
         * TODO: Encryption Lab:
         * Obtain an instance of AndroidKeyStore and load it
         * Get the private key alias and return the key
         */
        return null
    }


    /* Helper method to parse a file stream into memory */
    @Throws(IOException::class)
    private fun readFile(input: InputStream): String {
        val reader = InputStreamReader(input)
        val sb = StringBuilder()

        val inputBuffer = CharArray(2048)
        var read: Int = reader.read(inputBuffer)
        while (read != -1) {
            sb.append(inputBuffer, 0, read)
            read = reader.read(inputBuffer)
        }

        return sb.toString()
    }

    companion object {
        private val TAG = RSAHardwareEncryptor::class.java.simpleName
        private const val PROVIDER_NAME = "AndroidKeyStore"
        private const val KEY_ALGORITHM = "RSA"
        private const val ENCRYPTION_ALGORITHM = "RSA/ECB/PKCS1Padding"

        //Preferences alias for the public key
        private const val KEY_PUBLIC = "publickey"
        //KeyStore alias for the private key
        private const val KEY_ALIAS = "secureKeyAlias"
    }
}
