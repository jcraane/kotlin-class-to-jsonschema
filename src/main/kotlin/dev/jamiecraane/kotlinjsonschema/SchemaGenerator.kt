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

        return Type.Object(
            description = "Generated schema for ${this.simpleName}",
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
    val description = "Property ${this.name}"

    return when (returnType) {
        String::class -> Type.Primitive("string", description)
        Int::class, Long::class -> Type.Primitive("int", description)
        Float::class, Double::class -> Type.Primitive("number", description)
        Boolean::class -> Type.Primitive("boolean", description)
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
                description = description,
                items = itemTypeSchema
            )
        }

        else -> {
            // For complex types, recursively generate object schema
            if (returnType.isData) {
                returnType.schema
            } else {
                Type.Primitive("string", description) // fallback
            }
        }
    }
}
