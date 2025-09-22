package dev.jamiecraane.kjstools.fromschema.generation

import com.squareup.kotlinpoet.TypeSpec
import dev.jamiecraane.kjstools.fromschema.parsing.JsonSchemaParser
import dev.jamiecraane.kjstools.fromschema.mapping.KotlinTypeMapper

/**
 * Generates Kotlin enum classes from JSON Schema enum definitions.
 */
class EnumClassGenerator(
    private val typeMapper: KotlinTypeMapper
) {

    fun generateEnumClasses(parsedSchema: JsonSchemaParser.ParsedSchema): List<TypeSpec> {
        val enumClasses = mutableListOf<TypeSpec>()

        // Check root properties for enums
        parsedSchema.rootProperties.values.forEach { property ->
            typeMapper.mapEnum(property)?.let { enumInfo ->
                enumClasses.add(generateEnumClass(enumInfo))
            }
        }

        // Check definition properties for enums
        parsedSchema.definitions.values.forEach { definition ->
            definition.properties.values.forEach { property ->
                typeMapper.mapEnum(property)?.let { enumInfo ->
                    enumClasses.add(generateEnumClass(enumInfo))
                }
            }
        }

        return enumClasses
    }

    fun generateEnumClass(enumInfo: KotlinTypeMapper.KotlinEnumInfo): TypeSpec {
        val enumBuilder = TypeSpec.enumBuilder(enumInfo.name)

        // Add KDoc if available
        enumInfo.description?.let { description ->
            enumBuilder.addKdoc(description.replace("\"", "\\\""))
        }

        // Add enum constants
        enumInfo.values.forEach { value ->
            enumBuilder.addEnumConstant(sanitizeEnumConstant(value))
        }

        return enumBuilder.build()
    }

    private fun sanitizeEnumConstant(value: String): String {
        // Convert enum value to valid Kotlin enum constant name
        return value
            .replace(Regex("[^a-zA-Z0-9_]"), "_")
            .uppercase()
            .let { name ->
                when {
                    name.isEmpty() -> "UNKNOWN"
                    name.first().isDigit() -> "_$name"
                    else -> name
                }
            }
    }
}
