package com.microsoft.did.sdk.registrars

import com.microsoft.did.sdk.crypto.CryptoOperations
import com.microsoft.did.sdk.identifier.IdentifierDocument
import com.microsoft.did.sdk.utilities.getHttpClientEngine
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.url
import kotlinx.coroutines.*


/**
 * Registrar implementation for the Sidetree network
 * @class
 * @implements IRegistrar
 * @param registrarUrl to the registration endpoint
 * @param cryptoOperations
 */
class SidetreeRegistrar(registrarUrl: String, cryptoOperations: CryptoOperations): IRegistrar {


    /**
     * Registers the identifier document on the ledger
     * returning the identifier generated by the registrar.
     * @param identifierDocument to register.
     * @param signingKeyReference reference to the key to be used for signing request.
     */
    override fun register(identifierDocument: IdentifierDocument, signingKeyReference: String?) {

        val jws = "jws"
        GlobalScope.launch {
            sendRequest(jws)
        }
    }

    /**
     * Send request to the registration service
     * returning the fully discoverable Identifier Document.
     * @param request request sent to the registration service.
     */
    private suspend fun sendRequest(request: String) {
        val httpClientEngine = getHttpClientEngine()
        val client = HttpClient()
        val response = client.post<IdentifierDocument> {
            url("")
            body = request
        }
    }

    /**
     * Sign the Registration Payload
     * @param bodyString original payload to sign.
     * @param signingKeyReference reference to signature key if not default key.
     */
    private fun signPayload(bodyString: String, signingKeyReference: String?) {
        TODO("Not implemented")
    }


}