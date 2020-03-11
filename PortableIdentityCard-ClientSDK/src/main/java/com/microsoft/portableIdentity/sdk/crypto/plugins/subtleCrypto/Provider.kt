package com.microsoft.portableIdentity.sdk.crypto.plugins.subtleCrypto

import com.microsoft.portableIdentity.sdk.crypto.models.webCryptoApi.*
import com.microsoft.portableIdentity.sdk.utilities.ILogger

abstract class Provider(
    internal val logger: ILogger
) {
    public abstract val name: String
    public abstract val privateKeyUsage: Set<KeyUsage>?
    public abstract val publicKeyUsage: Set<KeyUsage>?
    public abstract val symmetricKeyUsage: Set<KeyUsage>?

    protected open fun onDigest(algorithm: Algorithm, data: ByteArray): ByteArray {
        throw logger.error("Digest not supported.")
    }
    protected open fun onGenerateKey(algorithm: Algorithm, extractable: Boolean, keyUsages: Set<KeyUsage>): CryptoKey {
        throw logger.error("GenerateKey not supported.")
    }
    protected open fun onGenerateKeyPair(algorithm: Algorithm, extractable: Boolean, keyUsages: Set<KeyUsage>): CryptoKeyPair {
        throw logger.error("GenerateKeyPair not supported.")
    }
    protected open fun onSign(algorithm: Algorithm, key: CryptoKey, data: ByteArray): ByteArray {
        throw logger.error("Sign not supported.")
    }
    protected open fun onVerify(algorithm: Algorithm, key: CryptoKey, signature: ByteArray, data: ByteArray): Boolean {
        throw logger.error("Verify not supported.")
    }
    protected open fun onEncrypt(algorithm: Algorithm, key: CryptoKey, data: ByteArray): ByteArray {
        throw logger.error("Encrypt not supported.")
    }
    protected open fun onDecrypt(algorithm: Algorithm, key: CryptoKey, data: ByteArray): ByteArray {
        throw logger.error("Decrypt not supported.")
    }
    protected open fun onDeriveBits(algorithm: Algorithm, baseKey: CryptoKey, length: ULong): ByteArray {
        throw logger.error("DeriveBits not supported.")
    }
    protected open fun onExportKey(format: KeyFormat, key: CryptoKey): ByteArray {
        throw logger.error("ExportKey not supported.")
    }
    protected open fun onExportKeyJwk(key: CryptoKey): JsonWebKey {
        throw logger.error("ExportKeyJwk not supported.")
    }
    protected open fun onImportKey(format: KeyFormat, keyData: ByteArray, algorithm: Algorithm,
                              extractable: Boolean, keyUsages: Set<KeyUsage>): CryptoKey {
        throw logger.error("ImportKey not supported.")
    }
    protected open fun onImportKey(format: KeyFormat, keyData: JsonWebKey, algorithm: Algorithm,
                                   extractable: Boolean, keyUsages: Set<KeyUsage>): CryptoKey {
        throw logger.error("ImportKey not supported.")
    }
    protected open fun checkGenerateKeyParams(algorithm: Algorithm) {
        throw logger.error("GenerateKey params check not implemented")
    }
    public open fun checkDerivedKeyParams(algorithm: Algorithm) {
        throw logger.error("DerivedKey params check not implemented")
    }

    public fun digest(algorithm: Algorithm, data: ByteArray): ByteArray {
        checkDigest(algorithm)
        return this.onDigest(algorithm, data)
    }
    public fun generateKey(algorithm: Algorithm, extractable: Boolean, keyUsages: Set<KeyUsage>): CryptoKey {
        checkGenerateKey(algorithm, extractable, keyUsages)
        return onGenerateKey(algorithm, extractable, keyUsages)
    }
    public fun generateKeyPair(algorithm: Algorithm, extractable: Boolean, keyUsages: Set<KeyUsage>): CryptoKeyPair {
        checkGenerateKey(algorithm, extractable, keyUsages)
        return onGenerateKeyPair(algorithm, extractable, keyUsages)
    }
    public fun sign(algorithm: Algorithm, key: CryptoKey, data: ByteArray): ByteArray {
        checkSign(algorithm, key)
        return onSign(algorithm, key, data)
    }
    public fun verify(algorithm: Algorithm, key: CryptoKey, signature: ByteArray, data: ByteArray): Boolean {
        checkVerify(algorithm, key)
        return onVerify(algorithm, key, signature, data)
    }
    public fun encrypt(algorithm: Algorithm, key: CryptoKey, data: ByteArray): ByteArray {
        checkEncrypt(algorithm, key)
        return onEncrypt(algorithm, key, data)
    }
    public fun decrypt(algorithm: Algorithm, key: CryptoKey, data: ByteArray): ByteArray {
        checkDecrypt(algorithm, key)
        return onDecrypt(algorithm, key, data)
    }
    public fun deriveBits(algorithm: Algorithm, baseKey: CryptoKey, length: ULong): ByteArray {
        checkDeriveBits(algorithm, baseKey, length)
        return  onDeriveBits(algorithm, baseKey, length)
    }
    public fun exportKey(format: KeyFormat, key: CryptoKey): ByteArray {
        checkExportKey(format, key)
        return onExportKey(format, key)
    }
    public fun exportKeyJwk(key: CryptoKey): JsonWebKey {
        checkExportKey(KeyFormat.Jwk, key)
        return onExportKeyJwk(key)
    }
    public fun importKey(format: KeyFormat, keyData: ByteArray, algorithm: Algorithm, extractable: Boolean,
                         keyUsages: Set<KeyUsage>): CryptoKey {
        if (format == KeyFormat.Jwk) {
            throw logger.error("KeyData does not match format")
        }
        checkImportKey(format, algorithm, extractable, keyUsages)
        return onImportKey(format, keyData, algorithm, extractable, keyUsages)
    }
    public fun importKey(format: KeyFormat, keyData: JsonWebKey, algorithm: Algorithm, extractable: Boolean,
                         keyUsages: Set<KeyUsage>): CryptoKey {
        if (format != KeyFormat.Jwk) {
            throw logger.error("KeyData does not match format")
        }
        checkImportKey(format, algorithm, extractable, keyUsages)
        return onImportKey(format, keyData, algorithm, extractable, keyUsages)
    }

    private fun checkDigest(algorithm: Algorithm) {
        checkAlgorithmName(algorithm)
    }
    private fun checkGenerateKey(algorithm: Algorithm, extractable: Boolean, keyUsages: Set<KeyUsage>) {
        checkAlgorithmName(algorithm)
        checkGenerateKeyParams(algorithm)
        if (keyUsages.count() == 0) {
            throw logger.error("Usages cannot be empty when creating a key.")
        }
        var allowedUsages: Set<KeyUsage> = if (this.symmetricKeyUsage != null) {
            this.symmetricKeyUsage!!
        } else {
            this.privateKeyUsage!! union this.publicKeyUsage!!
        }
        this.checkKeyUsages(keyUsages, allowedUsages)
    }
    private fun checkSign(algorithm: Algorithm, key: CryptoKey) {
        checkAlgorithmName(algorithm)
        checkAlgorithmParams(algorithm)
        checkCryptoKey(key, KeyUsage.Sign)
    }
    private fun checkVerify(algorithm: Algorithm, key: CryptoKey) {
        checkAlgorithmName(algorithm)
        checkAlgorithmParams(algorithm)
        checkCryptoKey(key, KeyUsage.Verify)
    }
    private fun checkEncrypt(algorithm: Algorithm, key: CryptoKey) {
        checkAlgorithmName(algorithm)
        checkAlgorithmParams(algorithm)
        checkCryptoKey(key, KeyUsage.Encrypt)
    }
    private fun checkDecrypt(algorithm: Algorithm, key: CryptoKey) {
        this.checkAlgorithmName(algorithm)
        this.checkAlgorithmParams(algorithm)
        this.checkCryptoKey(key, KeyUsage.Decrypt)
    }
    private fun checkDeriveBits(algorithm: Algorithm, baseKey: CryptoKey, length: ULong) {
        checkAlgorithmName(algorithm)
        checkAlgorithmParams(algorithm)
        checkCryptoKey(baseKey, KeyUsage.DeriveBits)
        if (length.rem(8u).compareTo(0u) != 0) {
            throw logger.error("Length is not a multiple of 8")
        }
    }
    private fun checkExportKey(format: KeyFormat, key: CryptoKey) {
        if (!key.extractable) {
            throw logger.error("Key is not extractable")
        }
    }
    private fun checkImportKey(format: KeyFormat, algorithm: Algorithm, extractable: Boolean, keyUsages: Set<KeyUsage>) {
        checkAlgorithmName(algorithm)
        checkAlgorithmParams(algorithm)
        checkImportParams(algorithm)

        if (this.symmetricKeyUsage != null) {
            checkKeyUsages(keyUsages, this.symmetricKeyUsage!!)
        } else {
            // check to see if its a private (or fall back to public)
            try {
                checkKeyUsages(keyUsages, this.privateKeyUsage!!)
            } catch (error: Error) {
                checkKeyUsages(keyUsages, this.publicKeyUsage!!)
            }
        }
    }

    protected open fun checkAlgorithmName(algorithm: Algorithm) {
        if (algorithm.name.toLowerCase() != this.name.toLowerCase()) {
            throw logger.error("Unrecognized Algorithm ${algorithm.name}")
        }
    }

    protected open fun checkAlgorithmParams(algorithm: Algorithm) {
        // there are no generic checks to perform
    }

    protected fun checkKeyUsages(usages: Set<KeyUsage>, allowed: Set<KeyUsage>) {
        val forbiddenUsages = usages - allowed
        if (forbiddenUsages.isNotEmpty()) {
            throw logger.error("Key Usages contains forbidden Key Usage: ${forbiddenUsages.joinToString { use -> use.value }}")
        }
    }

    public open fun checkCryptoKey(key: CryptoKey, keyUsage: KeyUsage) {
        checkAlgorithmName(key.algorithm)
        if (!key.usages.contains(keyUsage)) {
            throw logger.error("Key does not allow ${keyUsage.name}")
        }
    }

    protected open fun checkImportParams(algorithm: Algorithm) {
        // there are no generic checks to perform
    }
}