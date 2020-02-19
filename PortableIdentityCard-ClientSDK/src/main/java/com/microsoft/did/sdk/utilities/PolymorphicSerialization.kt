@file:Suppress("OVERRIDE_BY_INLINE")

package com.microsoft.did.sdk.utilities

import com.microsoft.did.sdk.credentials.ClaimDetail
import com.microsoft.did.sdk.credentials.SignedClaimDetail
import com.microsoft.did.sdk.credentials.UnsignedClaimDetail
import com.microsoft.did.sdk.identifier.IdentifierDocumentService
import com.microsoft.did.sdk.identifier.document.service.Endpoint
import com.microsoft.did.sdk.identifier.document.service.IdentityHubService
import com.microsoft.did.sdk.identifier.document.service.ServiceHubEndpoint
import com.microsoft.did.sdk.identifier.document.service.UserHubEndpoint
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.getContextualOrDefault
import kotlinx.serialization.modules.plus
import kotlin.reflect.KClass
import kotlin.collections.Map

object PolymorphicSerialization : IPolymorphicSerialization {
    private val identifierDocumentServiceSerializer = SerializersModule {
        polymorphic(IdentifierDocumentService::class) {
            IdentityHubService::class with IdentityHubService.serializer()
        }
    }

    private val serviceEndpointSerializer = SerializersModule {
        polymorphic(Endpoint::class) {
            ServiceHubEndpoint::class with ServiceHubEndpoint.serializer()
            UserHubEndpoint::class with UserHubEndpoint.serializer()
        }
    }

    private val claimDetailSerializer = SerializersModule {
        polymorphic(ClaimDetail::class) {
            UnsignedClaimDetail::class with UnsignedClaimDetail.serializer()
            SignedClaimDetail::class with SignedClaimDetail.serializer()
        }
    }
    val json : Json = Json(
        context = identifierDocumentServiceSerializer + serviceEndpointSerializer + claimDetailSerializer,
        configuration = JsonConfiguration(
            encodeDefaults = false,
            strictMode = false
        ))
    override fun <T> parse(deserializer: DeserializationStrategy<T>, string: String): T =
        json.parse(deserializer, string)

    override fun <T> stringify(serializer: SerializationStrategy<T>, obj: T): String =
        json.stringify(serializer, obj)
    
    @ImplicitReflectionSerializer
    override inline fun <K : Any, V: Any> stringify(obj: Map<K, V>, keyclass: KClass<K>, valclass: KClass<V>): String = 
        json.stringify((keyclass.serializer() to valclass.serializer()).map, obj)

    @ImplicitReflectionSerializer
    override inline fun <K : Any, V: Any> parseMap(map: String, keyclass: KClass<K>, valclass: KClass<V>)
            = parse((keyclass.serializer() to valclass.serializer()).map, map)

    @ImplicitReflectionSerializer
    override inline fun <T : Any> stringify(objects: List<T>, kclass: KClass<T>): String = stringify((kclass.serializer()).list, objects)

}