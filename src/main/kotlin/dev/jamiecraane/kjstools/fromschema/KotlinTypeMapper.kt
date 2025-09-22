package dev.jamiecraane.kjstools.fromschema

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import java.math.BigDecimal

/**
 * Maps JSON Schema types to Kotlin types for code generation.
 */
class KotlinTypeMapper(
    private val formatMappers: Map<FormatEnum, String>,
) {

    data class KotlinPropertyInfo(
        val name: String,
        val type: TypeName,
        val nullable: Boolean,
        val defaultValue: String?,
        val description: String?,
        val jsonPropertyName: String
    )

    data class KotlinEnumInfo(
        val name: String,
        val values: List<String>,
        val description: String?
    )

    fun mapProperty(property: JsonSchemaParser.PropertyInfo, parentClassName: String = "", definitions: Map<String, JsonSchemaParser.DefinitionInfo> = emptyMap(), mainClassName: String = ""): KotlinPropertyInfo {
        val kotlinType = mapType(property.type, property.nullable, property.name, parentClassName, definitions, mainClassName, property.format)

        // Only provide defaults for non-required fields or nullable fields
        val defaultValue = if (property.required && !property.nullable) {
            null // No default for required non-nullable fields
        } else {
            generateDefaultValue(kotlinType, property.nullable, property.required)
        }

        // If we're using null as default, make sure the type is nullable
        val finalType = if (defaultValue == "null" && !property.nullable) {
            kotlinType.copy(nullable = true)
        } else {
            kotlinType
        }
        val finalNullable = defaultValue == "null" || property.nullable

        return KotlinPropertyInfo(
            name = sanitizePropertyName(property.name),
            type = finalType,
            nullable = finalNullable,
            defaultValue = defaultValue,
            description = property.description,
            jsonPropertyName = property.name
        )
    }

    fun mapEnum(property: JsonSchemaParser.PropertyInfo): KotlinEnumInfo? {
        return when (property.type) {
            is JsonSchemaParser.PropertyType.Enum -> KotlinEnumInfo(
                name = toPascalCase(property.name) + "Enum", // Add suffix to avoid name conflicts
                values = property.type.values,
                description = property.description
            )
            else -> null
        }
    }

    private fun mapType(type: JsonSchemaParser.PropertyType, nullable: Boolean, propertyName: String = "", parentClassName: String = "", definitions: Map<String, JsonSchemaParser.DefinitionInfo> = emptyMap(), mainClassName: String = "", format: String? = null): TypeName {
        return TypeMapperRegistry.mapType(
            type = type,
            nullable = nullable,
            propertyName = propertyName,
            parentClassName = parentClassName,
            definitions = definitions,
            mainClassName = mainClassName,
            format = format,
            formatMappers = formatMappers
        )
    }

    fun mapArrayType(property: JsonSchemaParser.PropertyInfo, definitions: Map<String, JsonSchemaParser.DefinitionInfo> = emptyMap(), mainClassName: String = ""): TypeName {
        val itemType = when (val items = property.items) {
            null -> ClassName("kotlin", "Any")
            else -> mapType(items.type, items.nullable, items.name, mainClassName, definitions, mainClassName, null)
        }
        return LIST.parameterizedBy(itemType)
    }


    private fun generateDefaultValue(type: TypeName, nullable: Boolean, required: Boolean = true): String? {
        return when {
            nullable -> "null"
            type.toString().startsWith("kotlin.collections.List") -> "emptyList()" // Lists always default to emptyList()
            type.toString().startsWith("kotlin.collections.Map") -> "emptyMap()" // Maps always default to emptyMap()
            !required -> "null" // Other non-required fields default to null (type will be made nullable)
            type == String::class.asTypeName() -> "\"\"" // Default empty string for required strings
            type == Long::class.asTypeName() -> "0L" // Default 0L for required longs
            type == BigDecimal::class.asTypeName() -> "BigDecimal.ZERO" // Default BigDecimal.ZERO for required decimals
            type == Boolean::class.asTypeName() -> "false" // Default false for required booleans
            type.toString().contains("kotlin.Any") && !type.toString().startsWith("kotlin.collections.") -> "null" // Any type (not collections) will be made nullable
            else -> "null"
        }
    }

    fun sanitizePropertyName(name: String): String {
        // Convert to camelCase and ensure it's a valid Kotlin identifier
        val camelCase = toCamelCase(name)

        return when {
            isKotlinKeyword(camelCase) -> "`$camelCase`"
            camelCase.isEmpty() -> "property"
            !camelCase.first().isLetter() && camelCase.first() != '_' -> "_$camelCase"
            else -> camelCase
        }
    }

    fun sanitizeClassName(name: String): String {
        val pascalCase = toPascalCase(name)

        return when {
            isKotlinKeyword(pascalCase) -> "${pascalCase}Class"
            pascalCase.isEmpty() -> "GeneratedClass"
            !pascalCase.first().isLetter() -> "Class$pascalCase"
            else -> pascalCase
        }
    }

    private fun toCamelCase(input: String): String {
        if (input.isEmpty()) return input

        val words = input.split(Regex("[^a-zA-Z0-9]+"))
            .filter { it.isNotEmpty() }

        if (words.isEmpty()) return "property"

        return words.first().lowercase() + words.drop(1).joinToString("") { word ->
            word.lowercase().replaceFirstChar { it.titlecase() }
        }
    }

    private fun toPascalCase(input: String): String {
        if (input.isEmpty()) return input

        val words = input.split(Regex("[^a-zA-Z0-9]+"))
            .filter { it.isNotEmpty() }

        if (words.isEmpty()) return "GeneratedClass"

        return words.joinToString("") { word ->
            word.lowercase().replaceFirstChar { it.titlecase() }
        }
    }

    private fun isKotlinKeyword(name: String): Boolean {
        return KOTLIN_KEYWORDS.contains(name)
    }

    companion object {
        val LIST = ClassName("kotlin.collections", "List")
        val MAP = ClassName("kotlin.collections", "Map")

        /**
         * Default format mappers for common date/time formats
         */
        val JAVA_TIME_FORMAT_MAPPERS = mapOf(
            FormatEnum.DATE to "java.time.LocalDate",
            FormatEnum.TIME to "java.time.LocalTime",
            FormatEnum.DATE_TIME to "java.time.LocalDateTime"
        )

        /**
         * Creates a KotlinTypeMapper with default format mappers.
         * Custom mappers can be provided to override defaults.
         */
        fun withJavaTimeMapping(customMappers: Map<FormatEnum, String> = emptyMap()): KotlinTypeMapper {
            val combinedMappers = JAVA_TIME_FORMAT_MAPPERS + customMappers
            return KotlinTypeMapper(combinedMappers)
        }

        private val KOTLIN_KEYWORDS = setOf(
            "as", "break", "class", "continue", "do", "else", "false", "for", "fun", "if",
            "in", "interface", "is", "null", "object", "package", "return", "super", "this",
            "throw", "true", "try", "typealias", "typeof", "val", "var", "when", "while",
            "by", "catch", "constructor", "delegate", "dynamic", "field", "file", "finally",
            "get", "import", "init", "param", "property", "receiver", "set", "setparam",
            "where", "actual", "abstract", "annotation", "companion", "const", "crossinline",
            "data", "enum", "expect", "external", "final", "infix", "inline", "inner",
            "internal", "lateinit", "noinline", "open", "operator", "out", "override",
            "private", "protected", "public", "reified", "sealed", "suspend", "tailrec", "vararg"
        )
    }
}

