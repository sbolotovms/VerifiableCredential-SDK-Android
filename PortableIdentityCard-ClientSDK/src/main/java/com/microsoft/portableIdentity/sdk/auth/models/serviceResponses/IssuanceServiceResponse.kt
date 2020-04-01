/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

package com.microsoft.portableIdentity.sdk.auth.models.serviceResponses

import kotlinx.serialization.Serializable

@Serializable
data class IssuanceServiceResponse (
    val vc: String
): ServiceResponse