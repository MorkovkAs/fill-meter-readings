package ru.morkovka.fill.meter.readings.service.impl

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import ru.morkovka.fill.meter.readings.service.EncryptorService
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.security.spec.AlgorithmParameterSpec
import java.security.spec.InvalidKeySpecException
import java.security.spec.KeySpec
import javax.crypto.Cipher
import javax.crypto.NoSuchPaddingException
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.PBEParameterSpec
import javax.xml.bind.DatatypeConverter

@Service
class EncryptorServiceImpl(
    @Value("\${auth.encoder.password}")
    private val KEY: String,

    @Value("\${auth.encoder.iterations}")
    private val iterations: Int
) : EncryptorService {

    private val PBE_WITH_MD_5_AND_DES_MODE = "PBEWithMD5AndDES"

    private var ecipher: Cipher? = null

    private var dcipher: Cipher? = null

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    private fun encodeHasField(hashField: String): String {
        return String.format("##%s##", hashField)
    }

    fun encrypt(str: String?, hashField: String): String? {
        return encrypt(String.format("%s%s%s", encodeHasField(hashField), str, KEY))
    }

    fun decrypt(encodeStr: String?, hashField: String): String? {
        // Decode using utf-8
        var decrypt = decrypt(encodeStr) ?: return null
        decrypt = decrypt.replace(KEY, "")
        val result = decrypt.replace(encodeHasField(hashField), "")
        return if (result == decrypt) {
            null
        } else result
    }

    /**
     * Takes a single String as an argument and returns an Encrypted version
     * of that String.
     *
     * @param str String to be encrypted
     * @return `String` Encrypted version of the provided String
     */
    override fun encrypt(str: String): String? {
        try {
            // Encode the string into bytes using utf-8
            val utf8 = str.toByteArray(charset("UTF8"))
            // Encrypt
            val enc = ecipher!!.doFinal(utf8)
            // Encode bytes to base64 to get a string
            return DatatypeConverter.printBase64Binary(enc)
        } catch (e: Exception) {
            logger.error(e.message)
        }
        return null
    }

    /**
     * Takes a encrypted String as an argument, decrypts and returns the
     * decrypted String.
     *
     * @param str Encrypted String to be decrypted
     * @return `String` Decrypted version of the provided String
     */
    override fun decrypt(str: String?): String? {
        try {
            // Decode base64 to get bytes
            val dec = DatatypeConverter.parseBase64Binary(str)
            // Decrypt
            val utf8 = dcipher!!.doFinal(dec)
            // Decode using utf-8
            return String(utf8, charset("UTF8"))
        } catch (e: Exception) {
            logger.error(e.message)
        }
        return null
    }

    /**
     * Constructor used to create this object. Responsible for setting
     * and initializing this object's encrypter and decrypter Chipher instances
     * given a Pass Phrase and algorithm.
     */
    init {
        // 8-bytes Salt
        val salt = byteArrayOf(
            0xA9.toByte(),
            0x9B.toByte(),
            0xC8.toByte(),
            0x32.toByte(),
            0x56.toByte(),
            0x34.toByte(),
            0xE3.toByte(),
            0x03.toByte()
        )
        try {
            val keySpec: KeySpec = PBEKeySpec(KEY.toCharArray(), salt, iterations)
            val key = SecretKeyFactory.getInstance(PBE_WITH_MD_5_AND_DES_MODE).generateSecret(keySpec)
            ecipher = Cipher.getInstance(PBE_WITH_MD_5_AND_DES_MODE)
            dcipher = Cipher.getInstance(PBE_WITH_MD_5_AND_DES_MODE)
            // Prepare the parameters to the cipthers
            val paramSpec: AlgorithmParameterSpec = PBEParameterSpec(salt, iterations)
            with(ecipher) {
                this?.init(Cipher.ENCRYPT_MODE, key, paramSpec)
            }
            with(dcipher) {
                with(ecipher) {
                    this?.init(Cipher.ENCRYPT_MODE, key, paramSpec)
                }
                this?.init(Cipher.DECRYPT_MODE, key, paramSpec)
            }
        } catch (e: InvalidAlgorithmParameterException) {
            logger.error("EXCEPTION: InvalidAlgorithmParameterException")
        } catch (e: InvalidKeySpecException) {
            logger.error("EXCEPTION: InvalidKeySpecException")
        } catch (e: NoSuchPaddingException) {
            logger.error("EXCEPTION: NoSuchPaddingException")
        } catch (e: NoSuchAlgorithmException) {
            logger.error("EXCEPTION: NoSuchAlgorithmException")
        } catch (e: InvalidKeyException) {
            logger.error("EXCEPTION: InvalidKeyException")
        }
    }
}