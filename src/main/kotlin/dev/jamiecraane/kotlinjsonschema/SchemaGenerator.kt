package dev.jamiecraane.kotlinjsonschema

import dev.jamiecraane.kotlinjsonschema.jsonschema.JsonSchema
import dev.jamiecraane.kotlinjsonschema.jsonschema.Type
import dev.jamiecraane.kotlinjsonschema.jsonschema.toJsonSchemaString
import kotlinx.datetime.LocalDate
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.jvmErasure

/**
 * Extension property that generates a JsonSchema from a Kotlin data class using reflection
 */
val <T : Any> KClass<T>.schema: JsonSchema
    get() {
        val typeUsageCount = mutableMapOf<String, Int>()
        val definitions = mutableMapOf<String, Type.Object>()

        // First pass: count type usage
        this.countTypeUsage(typeUsageCount)

        // Second pass: generate schema with definitions only for reused types
        val rootSchema = this.toObjectSchemaWithReuse(definitions, typeUsageCount)

        return JsonSchema(
            description = "Generated schema for ${rootSchema.description}",
            definitions = definitions,
            type = "object",
            properties = rootSchema.properties,
            required = rootSchema.required,
            additionalProperties = rootSchema.additionalProperties,
        )
    }

/**
 * Counts usage of complex types to determine which should become definitions
 */
private fun <T : Any> KClass<T>.countTypeUsage(typeUsageCount: MutableMap<String, Int>) {
    this.memberProperties.forEach { property ->
        property.countPropertyTypeUsage(typeUsageCount)
    }
}

/**
 * Counts type usage for a property, including nested types
 */
private fun KProperty1<*, *>.countPropertyTypeUsage(typeUsageCount: MutableMap<String, Int>) {
    val returnType = this.returnType.jvmErasure
    when (returnType) {
        List::class -> {
            val itemType = this.returnType.arguments.firstOrNull()?.type?.jvmErasure
            if (itemType?.isData == true) {
                val typeName = itemType.simpleName!!
                typeUsageCount[typeName] = (typeUsageCount[typeName] ?: 0) + 1
                itemType.countTypeUsage(typeUsageCount)
            }
        }
        else -> {
            if (returnType.isData) {
                val typeName = returnType.simpleName!!
                typeUsageCount[typeName] = (typeUsageCount[typeName] ?: 0) + 1
                returnType.countTypeUsage(typeUsageCount)
            }
        }
    }
}

/**
 * Converts a KClass to a Type.Object schema with reuse-based definitions
 */
private fun <T : Any> KClass<T>.toObjectSchemaWithReuse(
    definitions: MutableMap<String, Type.Object>,
    typeUsageCount: Map<String, Int>
): Type.Object {
    val properties = this.memberProperties.associate { property ->
        property.name to property.toTypeWithReuse(definitions, typeUsageCount)
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
 * Converts a KProperty1 to a Type with reuse-based definitions
 */
private fun KProperty1<*, *>.toTypeWithReuse(
    definitions: MutableMap<String, Type.Object>,
    typeUsageCount: Map<String, Int>
): Type {
    val returnType = this.returnType.jvmErasure
    val defaultDescription = "Property ${this.name}"
    val annotatedDescription = this.findAnnotation<Description>()?.value
        ?: this.javaField?.getAnnotation(Description::class.java)?.value
    val format = this.findAnnotation<Format>()?.value
        ?: this.javaField?.getAnnotation(Format::class.java)?.value

    val base: Type = when (returnType) {
        String::class -> Type.Primitive("string", defaultDescription, format)
        Int::class, Long::class -> Type.Primitive("integer", defaultDescription, format)
        Float::class, Double::class -> Type.Primitive("number", defaultDescription, format)
        Boolean::class -> Type.Primitive("boolean", defaultDescription, format)
        List::class -> {
            val itemType = this.returnType.arguments.firstOrNull()?.type?.jvmErasure
            val itemTypeSchema = when (itemType) {
                String::class -> Type.Primitive("string", "Array item")
                Int::class, Long::class -> Type.Primitive("integer", "Array item")
                Float::class, Double::class -> Type.Primitive("number", "Array item")
                Boolean::class -> Type.Primitive("boolean", "Array item")
                else -> {
                    if (itemType?.isData == true) {
                        val typeName = itemType.simpleName!!
                        val usageCount = typeUsageCount[typeName] ?: 0
                        if (usageCount > 1) {
                            // Create reference for reused types
                            if (!definitions.containsKey(typeName)) {
                                definitions[typeName] = itemType.toObjectSchemaWithReuse(definitions, typeUsageCount)
                            }
                            Type.Reference("Array item", "#/definitions/$typeName")
                        } else {
                            // Inline for single-use types
                            itemType.toObjectSchemaWithReuse(definitions, typeUsageCount)
                        }
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
            if (returnType.isData) {
                val typeName = returnType.simpleName!!
                val usageCount = typeUsageCount[typeName] ?: 0
                if (usageCount > 1) {
                    // Create reference for reused types
                    if (!definitions.containsKey(typeName)) {
                        definitions[typeName] = returnType.toObjectSchemaWithReuse(definitions, typeUsageCount)
                    }
                    Type.Reference(defaultDescription, "#/definitions/$typeName")
                } else {
                    // Inline for single-use types
                    returnType.toObjectSchemaWithReuse(definitions, typeUsageCount)
                }
            } else {
                Type.Primitive("string", defaultDescription, format) // fallback
            }
        }
    }

    return if (annotatedDescription != null) {
        when (base) {
            is Type.Primitive -> base.copy(description = annotatedDescription)
            is Type.Array -> base.copy(description = annotatedDescription)
            is Type.Object -> base.copy(description = annotatedDescription)
            is Type.Enum -> base.copy(description = annotatedDescription)
            is Type.Reference -> base.copy(description = annotatedDescription)
        }
    } else base
}


data class Person(
    @Description("The fullname of the user")
    val name: String,
    val age: Int,
    val addresses: List<Address> = emptyList(),
    @Format(FormatConstants.DATE)
    val birthData: LocalDate,
    val postAddress: Address,
)

data class Address(
    val street: String,
    val city: String,
    val zip: String,
)

fun main() {
    val personSchema = Person::class.schema
    println(personSchema.toJsonSchemaString())
}

