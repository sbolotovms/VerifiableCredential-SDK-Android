/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

package com.microsoft.did.sdk.credential.service.models.attestations

import kotlinx.serialization.Serializable

/**
 * Data Container to represent a Verifiable Presentation Request.
 * Verifiable Presentation is a wrapper around a Verifiable Credential.
 * Must have either issuers property or contracts property
 */
@Serializable
data class PresentationAttestation(
    // True, if presentation is required.
    val required: Boolean = false,

    // The type of the verifiable credential that is being requested.
    val credentialType: String,

    // A list of issuers that requester will accept.
    val issuers: List<AcceptedIssuer> = emptyList(),

    // A list of contracts if user does not have requested credential.
    val contracts: List<String> = emptyList()
)