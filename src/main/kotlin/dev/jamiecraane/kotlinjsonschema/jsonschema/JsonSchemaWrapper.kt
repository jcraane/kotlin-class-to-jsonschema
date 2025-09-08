package dev.jamiecraane.kotlinjsonschema.jsonschema

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.ClassDiscriminatorMode
import kotlinx.serialization.json.Json

private val jsonSchemaJson = Json {
    encodeDefaults = true
    coerceInputValues = true
    ignoreUnknownKeys = true
    isLenient = true
    prettyPrint = true
    explicitNulls = false
    classDiscriminatorMode = ClassDiscriminatorMode.NONE
}

@Serializable
data class JsonSchema(
    @SerialName("\$schema")
    val schema: String = "http://json-schema.org/draft-07/schema#",
    val description: String? = null,
    val type: String = "object",
    val properties: Map<String, Type>,
    val required: List<String> = emptyList(),
    val additionalProperties: Boolean = false,
    val definitions: Map<String, Type.Object>? = null,
)

fun JsonSchema.toJsonSchemaString(): String = jsonSchemaJson.encodeToString(JsonSchema.serializer(), this)

@Serializable
sealed class JsonType {
    @Serializable
    @SerialName("string")
    data object StringType : JsonType()

    @Serializable
    @SerialName("number")
    data object NumberType : JsonType()

    @Serializable
    @SerialName("boolean")
    data object BooleanType : JsonType()

    @Serializable
    @SerialName("object")
    data object ObjectTypeValue : JsonType()

    @Serializable
    @SerialName("array")
    data object ArrayType : JsonType()
}

@Serializable
sealed class Type {
    abstract val description: String

    @Serializable
    @SerialName("primitive")
    data class Primitive(
        val type: String,
        override val description: String,
        val format: String? = null,
    ) : Type()

    @Serializable
    @SerialName("enum")
    data class Enum(
        override val description: String,
        val type: String = "string",
        val enum: List<String>,
    ) : Type()

    @Serializable
    @SerialName("array")
    data class Array(
        override val description: String,
        val type: String = "array",
        val items: Type,
    ) : Type()

    @Serializable
    @SerialName("reference")
    data class Reference(
        override val description: String,
        @SerialName("\$ref")
        val ref: String,
    ) : Type()

    @Serializable
    @SerialName("object")
    data class Object(
        override val description: String,
        val type: String = "object",
        val properties: Map<String, Type>,
        val required: List<String> = emptyList(),
        val additionalProperties: Boolean = false,
    ) : Type()
}
