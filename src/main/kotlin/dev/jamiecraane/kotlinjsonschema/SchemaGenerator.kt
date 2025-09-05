package dev.jamiecraane.kotlinjsonschema

import dev.jamiecraane.kotlinjsonschema.jsonschema.JsonSchema
import dev.jamiecraane.kotlinjsonschema.jsonschema.Type
import dev.jamiecraane.kotlinjsonschema.jsonschema.toJsonSchemaString
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.Any
import kotlin.Boolean
import kotlin.Double
import kotlin.Float
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.collections.associate
import kotlin.collections.filter
import kotlin.collections.map
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.jvmErasure
import kotlinx.serialization.Serializable

/**
 * Extension property that generates a JsonSchema from a Kotlin data class using reflection
 */
val <T : Any> KClass<T>.schema: Type.Object
    get() {
        val properties = this.memberProperties.associate { property ->
            property.name to property.toType()
        }

        val requiredFields = this.memberProperties
            .filter { !it.returnType.isMarkedNullable }
            .map { it.name }

        val classDescription = this.findAnnotation<Description>()?.value
            ?: "Generated schema for ${this.simpleName}"

        return Type.Object(
            description = classDescription,
            properties = properties,
            required = requiredFields,
            additionalProperties = false
        )
    }

/**
 * Converts a KProperty1 to a Type based on its return type
 */
private fun KProperty1<*, *>.toType(): Type {
    val returnType = this.returnType.jvmErasure
    val defaultDescription = "Property ${this.name}"
    val annotatedDescription = this.findAnnotation<Description>()?.value
        ?: this.javaField?.getAnnotation(Description::class.java)?.value

    val base: Type = when (returnType) {
        String::class -> Type.Primitive("string", defaultDescription)
        Int::class, Long::class -> Type.Primitive("int", defaultDescription)
        Float::class, Double::class -> Type.Primitive("number", defaultDescription)
        Boolean::class -> Type.Primitive("boolean", defaultDescription)
        List::class -> {
            // Analyze generic type parameters to determine the actual item type
            val itemType = this.returnType.arguments.firstOrNull()?.type?.jvmErasure
            val itemTypeSchema = when (itemType) {
                String::class -> Type.Primitive("string", "Array item")
                Int::class, Long::class -> Type.Primitive("int", "Array item")
                Float::class, Double::class -> Type.Primitive("number", "Array item")
                Boolean::class -> Type.Primitive("boolean", "Array item")
                else -> {
                    // For complex types like data classes, recursively generate their schema
                    if (itemType?.isData == true) {
                        itemType.schema
                    } else {
                        Type.Primitive("string", "Array item") // fallback
                    }
                }
            }

            Type.Array(
                description = defaultDescription,
                items = itemTypeSchema
            )
        }

        else -> {
            // For complex types, recursively generate object schema
            if (returnType.isData) {
                returnType.schema
            } else {
                Type.Primitive("string", defaultDescription) // fallback
            }
        }
    }

    return if (annotatedDescription != null) {
        when (base) {
            is Type.Primitive -> base.copy(description = annotatedDescription)
            is Type.Array -> base.copy(description = annotatedDescription)
            is Type.Object -> base.copy(description = annotatedDescription)
            is Type.Enum -> base.copy(description = annotatedDescription)
        }
    } else base
}
