package com.microsoft.did.sdk.crypto.plugins

import android.util.Base64
import com.microsoft.did.sdk.crypto.models.webCryptoApi.*
import com.microsoft.did.sdk.crypto.plugins.subtleCrypto.Provider
import com.microsoft.did.sdk.utilities.stringToByteArray
import org.bitcoin.NativeSecp256k1
import java.security.SecureRandom
import java.util.*

class Secp256k1Provider(): Provider() {
    companion object {
        init {
            System.loadLibrary("secp256k1")
        }
    }

    override val name: String = "ECDSA"
    override val privateKeyUsage: Set<KeyUsage> = setOf(KeyUsage.Sign)
    override val publicKeyUsage: Set<KeyUsage> = setOf(KeyUsage.Verify)
    override val symmetricKeyUsage: Set<KeyUsage>? = null

    override fun onGenerateKeyPair(
        algorithm: Algorithm,
        extractable: Boolean,
        keyUsages: Set<KeyUsage>
    ): CryptoKeyPair {
        val seed = ByteArray(32)
        val random = SecureRandom()
        random.nextBytes(seed)
        NativeSecp256k1.randomize(seed)

        val secret = ByteArray(32)
        random.nextBytes(secret)

        val publicKey = NativeSecp256k1.computePubkey(secret)

        val keyPair = CryptoKeyPair(CryptoKey(
            KeyType.Private,
            extractable,
            algorithm,
            keyUsages.toList(),
            secret
        ), CryptoKey(
            KeyType.Public,
            true,
            algorithm,
            publicKeyUsage.toList(),
            publicKey
        ))

        return return keyPair
    }

    override fun checkGenerateKeyParams(algorithm: Algorithm) {
        val keyGenParams = algorithm as? EcKeyGenParams ?: throw Error("EcKeyGenParams expected as algorithm")
        if (keyGenParams.namedCurve.toUpperCase(Locale.ROOT) != W3cCryptoApiConstants.Secp256k1.value.toUpperCase(Locale.ROOT) &&
            keyGenParams.namedCurve.toUpperCase(Locale.ROOT) != W3cCryptoApiConstants.Secp256k1.name.toUpperCase(Locale.ROOT)) {
            throw Error("The curve ${keyGenParams.namedCurve} is not supported by Secp256k1Provider")
        }
    }

    override fun onSign(algorithm: Algorithm, key: CryptoKey, data: ByteArray): ByteArray {
        val keyData = getKeyData(key)
        if (data.size !== 32) {
            throw Error("Data must be 32 bytes")
        }

        return NativeSecp256k1.sign(data, keyData)
    }

    override fun onVerify(algorithm: Algorithm, key: CryptoKey, signature: ByteArray, data: ByteArray): Boolean {
        val keyData = getKeyData(key)
        if (data.size !== 32) {
            throw Error("Data must be 32 bytes")
        }

        return NativeSecp256k1.verify(data, signature, keyData)
    }

    override fun onImportKey(
        format: KeyFormat,
        keyData: JsonWebKey,
        algorithm: Algorithm,
        extractable: Boolean,
        keyUsages: Set<KeyUsage>
    ): CryptoKey {
        if (keyData.d != null) { // import d as the private key handle
            return CryptoKey(
                type = KeyType.Private,
                extractable = extractable,
                algorithm = algorithm,
                usages = keyUsages.toList(),
                handle = Base64.decode(stringToByteArray(keyData.d!!), Base64.URL_SAFE)
            )
        } else {// public key
            val x = Base64.decode(stringToByteArray(keyData.x!!), Base64.URL_SAFE)
            val y = Base64.decode(stringToByteArray(keyData.y!!), Base64.URL_SAFE)
            val xyData = ByteArray(65)
            xyData[0] = secp256k1Tag.uncompressed.byte
            x.forEachIndexed { index, byte ->
                xyData[index + 1] = byte
            }
            y.forEachIndexed { index, byte ->
                xyData[index + 33] = byte
            }
            return CryptoKey(
                type = KeyType.Public,
                extractable = extractable,
                algorithm = algorithm,
                usages = keyUsages.toList(),
                handle = xyData
            )
        }
    }

    override fun onExportKeyJwk(key: CryptoKey): JsonWebKey {
        val keyOps = mutableListOf<String>()
        for (usage in key.usages) {
            keyOps.add(usage.value)
        }
        var publicKey = key.handle as? ByteArray
        val d: String? = if (key.type == KeyType.Private) {
            publicKey = NativeSecp256k1.computePubkey(key.handle as? ByteArray)
            Base64.encodeToString(key.handle as? ByteArray, Base64.URL_SAFE)
        } else {
            null
        }
        if (publicKey == null) {
            throw Error("No public key components could be found")
        }
        println("Getting XY")
        val xyData = publicToXY(publicKey)
        println("XY got")
        return JsonWebKey(
            kty = com.microsoft.did.sdk.crypto.keys.KeyType.EllipticCurve.value,
            crv = W3cCryptoApiConstants.Secp256k1.value,
            use = "sig",
            key_ops = keyOps,
            alg = this.name,
            ext = key.extractable,
            d = d,
            x = xyData.first,
            y = xyData.second
        )
    }

    override fun checkCryptoKey(key: CryptoKey, keyUsage: KeyUsage) {
        super.checkCryptoKey(key, keyUsage)
        if (key.type == KeyType.Private) {
            val keyData = getKeyData(key)
            if (!NativeSecp256k1.secKeyVerify(keyData)) {
                throw Error("Private key invalid")
            }
        }
    }

    private fun getKeyData(key: CryptoKey): ByteArray {
        checkAlgorithmName(key.algorithm)
        return key.handle as? ByteArray ?: throw Error("Invalid key")
    }

    // mapped from secp256k1_eckey_pubkey_parse
    private fun publicToXY(keyData: ByteArray): Pair<String, String> {
        if (keyData.size == 33 && (
                    keyData[0] == secp256k1Tag.even.byte ||
                    keyData[0] == secp256k1Tag.odd.byte)) {
            // compressed form
            println("Compressed form")
            return Pair(
                "",
                ""
            )
        } else if (keyData.size == 65 && (
                    keyData[0] == secp256k1Tag.uncompressed.byte ||
                    keyData[0] == secp256k1Tag.hybridEven.byte ||
                    keyData[0] == secp256k1Tag.hybridOdd.byte
                    )) {
            println("Uncompressed form")
            // uncompressed, bytes 1-32, and 33-end are x and y
            val x = keyData.sliceArray(1..32)
            val y = keyData.sliceArray(33..64)
            println("X: $x, Y: $y")
            return Pair(
                Base64.encodeToString(x, Base64.URL_SAFE),
                Base64.encodeToString(y, Base64.URL_SAFE)
                )
        } else {
            throw Error("Public key improperly formatted")
        }
    }

    enum class secp256k1Tag(val byte: Byte) {
        even(0x02),
        odd(0x03),
        uncompressed(0x04),
        hybridEven(0x06),
        hybridOdd(0x07)
    }
}