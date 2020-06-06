/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

package com.microsoft.did.sdk.auth.protectors

import com.microsoft.did.sdk.auth.models.oidc.AttestationClaimModel
import com.microsoft.did.sdk.auth.models.oidc.OidcResponseContent
import com.microsoft.did.sdk.cards.verifiableCredential.VerifiableCredential
import com.microsoft.did.sdk.crypto.CryptoOperations
import com.microsoft.did.sdk.crypto.models.Sha
import com.microsoft.did.sdk.identifier.Identifier
import com.microsoft.did.sdk.utilities.Serializer
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Class that forms Response Contents Properly.
 */
@Singleton
class OidcResponseFormatter @Inject constructor(
    private val cryptoOperations: CryptoOperations,
    private val serializer: Serializer,
    private val verifiablePresentationFormatter: VerifiablePresentationFormatter,
    private val signer: TokenSigner
) {

    fun format(
        responder: Identifier,
        audience: String,
        expiresIn: Int,
        requestedVcs: Map<String, VerifiableCredential>? = null,
        requestedIdTokens: Map<String, String>? = null,
        requestedSelfIssuedClaims: Map<String, String>? = null,
        contract: String? = null,
        nonce: String? = null,
        state: String? = null,
        transformingVerifiableCredential: VerifiableCredential? = null,
        recipientIdentifier: String? = null
    ): String {
        val (iat, exp) = createIatAndExp(expiresIn)
        val key = cryptoOperations.keyStore.getPublicKey(responder.signatureKeyReference).getKey()
        val jti = UUID.randomUUID().toString()
        val did = responder.id
        val attestationResponse = this.createAttestationClaimModel(requestedVcs, requestedIdTokens, requestedSelfIssuedClaims, audience, responder, expiresIn)

        val contents = OidcResponseContent(
            sub = key.getThumbprint(cryptoOperations, Sha.SHA256.algorithm),
            aud = audience,
            nonce = nonce,
            did = did,
            subJwk = key.toJWK(),
            iat = iat,
            exp = exp,
            state = state,
            jti = jti,
            contract = contract,
            attestations = attestationResponse,
            vc = transformingVerifiableCredential?.raw,
            recipient = recipientIdentifier
        )
        return signContents(contents, responder)
    }

    private fun signContents(contents: OidcResponseContent, responder: Identifier): String {
        val serializedResponseContent = serializer.stringify(OidcResponseContent.serializer(), contents)
        return signer.signWithIdentifier(serializedResponseContent, responder)
    }

    private fun createAttestationClaimModel(requestedVcs: Map<String, VerifiableCredential>?, requestedIdTokens: Map<String, String>?, requestedSelfIssuedClaims: Map<String, String>?, audience: String, responder: Identifier, expiresIn: Int): AttestationClaimModel? {
        if (areNoCollectedClaims(requestedVcs, requestedIdTokens, requestedSelfIssuedClaims)) {
            return null
        }
        val presentationAttestations = createPresentations(requestedVcs, audience, responder, expiresIn)
        return AttestationClaimModel(requestedSelfIssuedClaims, requestedIdTokens, presentationAttestations)
    }

    private fun createPresentations(requestedVcs: Map<String, VerifiableCredential>?, audience: String, responder: Identifier, expiresIn: Int): Map<String, String>? {
        val presentations = requestedVcs?.mapValues { verifiablePresentationFormatter.createPresentation(listOf(it.value), audience, responder, expiresIn) }
        if (presentations.isNullOrEmpty()) {
            return null
        }
        return presentations
    }

    private fun areNoCollectedClaims(requestedVcs: Map<String, VerifiableCredential>?, requestedIdTokens: Map<String, String>?, requestedSelfIssuedClaims: Map<String, String>?): Boolean {
        return (requestedVcs.isNullOrEmpty() && requestedIdTokens.isNullOrEmpty() && requestedSelfIssuedClaims.isNullOrEmpty())
    }
}