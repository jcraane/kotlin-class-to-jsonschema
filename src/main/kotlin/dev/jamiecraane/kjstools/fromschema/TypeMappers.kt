package dev.jamiecraane.kjstools.fromschema

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import java.math.BigDecimal

/**
 * Strategy pattern for handling different type mappings.
 * Each implementation handles a specific PropertyType mapping logic.
 */
sealed class TypeMapper {
    abstract fun canHandle(type: JsonSchemaParser.PropertyType): Boolean
    abstract fun mapType(
        type: JsonSchemaParser.PropertyType,
        nullable: Boolean,
        propertyName: String = "",
        parentClassName: String = "",
        definitions: Map<String, JsonSchemaParser.DefinitionInfo> = emptyMap(),
        mainClassName: String = "",
        format: String? = null,
        formatMappers: Map<FormatEnum, String> = emptyMap()
    ): TypeName
}

/**
 * Handles string type mapping with format support
 */
object StringTypeMapper : TypeMapper() {
    override fun canHandle(type: JsonSchemaParser.PropertyType): Boolean =
        type is JsonSchemaParser.PropertyType.StringType

    override fun mapType(
        type: JsonSchemaParser.PropertyType,
        nullable: Boolean,
        propertyName: String,
        parentClassName: String,
        definitions: Map<String, JsonSchemaParser.DefinitionInfo>,
        mainClassName: String,
        format: String?,
        formatMappers: Map<FormatEnum, String>
    ): TypeName {
        val baseType = if (format != null) {
            val formatEnum = FormatEnum.values().find { it.code == format }
            if (formatEnum != null && formatMappers.containsKey(formatEnum)) {
                ClassName.bestGuess(formatMappers[formatEnum]!!)
            } else {
                String::class.asTypeName()
            }
        } else {
            String::class.asTypeName()
        }
        return if (nullable) baseType.copy(nullable = true) else baseType
    }
}

/**
 * Handles array type mapping
 */
object ArrayTypeMapper : TypeMapper() {
    override fun canHandle(type: JsonSchemaParser.PropertyType): Boolean =
        type is JsonSchemaParser.PropertyType.Array

    override fun mapType(
        type: JsonSchemaParser.PropertyType,
        nullable: Boolean,
        propertyName: String,
        parentClassName: String,
        definitions: Map<String, JsonSchemaParser.DefinitionInfo>,
        mainClassName: String,
        format: String?,
        formatMappers: Map<FormatEnum, String>
    ): TypeName {
        val baseType = KotlinTypeMapper.LIST.parameterizedBy(ClassName("kotlin", "Any"))
        return if (nullable) baseType.copy(nullable = true) else baseType
    }
}

/**
 * Handles reference type mapping with definition resolution
 */
object ReferenceTypeMapper : TypeMapper() {
    override fun canHandle(type: JsonSchemaParser.PropertyType): Boolean =
        type is JsonSchemaParser.PropertyType.Reference

    override fun mapType(
        type: JsonSchemaParser.PropertyType,
        nullable: Boolean,
        propertyName: String,
        parentClassName: String,
        definitions: Map<String, JsonSchemaParser.DefinitionInfo>,
        mainClassName: String,
        format: String?,
        formatMappers: Map<FormatEnum, String>
    ): TypeName {
        val baseType = when (type) {
            is JsonSchemaParser.PropertyType.Reference -> {
                val referencedDefinition = definitions[type.ref]
                if (referencedDefinition != null) {
                    val targetClassName = if (mainClassName.isNotEmpty()) mainClassName else parentClassName
                    val baseNestedClassName = KotlinTypeMapperUtil.sanitizeClassName(type.ref).replaceFirstChar { it.uppercase() }
                    val nestedClassName = KotlinTypeMapperUtil.resolveClassNameConflict(baseNestedClassName, targetClassName)
                    ClassName(targetClassName, nestedClassName)
                } else {
                    ClassName("kotlin", "Any")
                }
            }
            else -> ClassName("kotlin", "Any")
        }
        return if (nullable) baseType.copy(nullable = true) else baseType
    }
}

/**
 * Handles nested object type mapping
 */
object NestedObjectTypeMapper : TypeMapper() {
    override fun canHandle(type: JsonSchemaParser.PropertyType): Boolean =
        type is JsonSchemaParser.PropertyType.NestedObject

    override fun mapType(
        type: JsonSchemaParser.PropertyType,
        nullable: Boolean,
        propertyName: String,
        parentClassName: String,
        definitions: Map<String, JsonSchemaParser.DefinitionInfo>,
        mainClassName: String,
        format: String?,
        formatMappers: Map<FormatEnum, String>
    ): TypeName {
        val baseNestedClassName = KotlinTypeMapperUtil.toPascalCase(propertyName).replaceFirstChar { it.uppercase() }
        val nestedClassName = KotlinTypeMapperUtil.resolveClassNameConflict(baseNestedClassName, parentClassName)
        val baseType = ClassName(parentClassName, nestedClassName)
        return if (nullable) baseType.copy(nullable = true) else baseType
    }
}

/**
 * Handles union type mapping
 */
object UnionTypeMapper : TypeMapper() {
    override fun canHandle(type: JsonSchemaParser.PropertyType): Boolean =
        type is JsonSchemaParser.PropertyType.Union

    override fun mapType(
        type: JsonSchemaParser.PropertyType,
        nullable: Boolean,
        propertyName: String,
        parentClassName: String,
        definitions: Map<String, JsonSchemaParser.DefinitionInfo>,
        mainClassName: String,
        format: String?,
        formatMappers: Map<FormatEnum, String>
    ): TypeName {
        val baseType = when (type) {
            is JsonSchemaParser.PropertyType.Union -> {
                val nonNullTypes = type.types.filter { it !is JsonSchemaParser.PropertyType.Null }
                val itemType = if (nonNullTypes.size == 1) {
                    TypeMapperRegistry.mapType(
                        nonNullTypes.first(),
                        false,
                        propertyName,
                        mainClassName,
                        definitions,
                        mainClassName,
                        format,
                        formatMappers
                    )
                } else {
                    ClassName("kotlin", "Any")
                }
                KotlinTypeMapper.LIST.parameterizedBy(itemType)
            }
            else -> ClassName("kotlin", "Any")
        }
        return if (nullable) baseType.copy(nullable = true) else baseType
    }
}

/**
 * Handles primitive types (Number, Integer, Boolean, Object, Null, Enum)
 */
object PrimitiveTypeMapper : TypeMapper() {
    override fun canHandle(type: JsonSchemaParser.PropertyType): Boolean =
        type is JsonSchemaParser.PropertyType.Number ||
        type is JsonSchemaParser.PropertyType.Integer ||
        type is JsonSchemaParser.PropertyType.Boolean ||
        type is JsonSchemaParser.PropertyType.Object ||
        type is JsonSchemaParser.PropertyType.Null ||
        type is JsonSchemaParser.PropertyType.Enum

    override fun mapType(
        type: JsonSchemaParser.PropertyType,
        nullable: Boolean,
        propertyName: String,
        parentClassName: String,
        definitions: Map<String, JsonSchemaParser.DefinitionInfo>,
        mainClassName: String,
        format: String?,
        formatMappers: Map<FormatEnum, String>
    ): TypeName {
        val baseType = when (type) {
            is JsonSchemaParser.PropertyType.Number -> BigDecimal::class.asTypeName()
            is JsonSchemaParser.PropertyType.Integer -> Long::class.asTypeName()
            is JsonSchemaParser.PropertyType.Boolean -> Boolean::class.asTypeName()
            is JsonSchemaParser.PropertyType.Object -> ClassName("kotlin", "Any")
            is JsonSchemaParser.PropertyType.Null -> Unit::class.asTypeName()
            is JsonSchemaParser.PropertyType.Enum -> String::class.asTypeName()
            else -> ClassName("kotlin", "Any")
        }
        return if (nullable) baseType.copy(nullable = true) else baseType
    }
}

/**
 * Registry that manages all type mappers and routes type mapping requests
 */
object TypeMapperRegistry {
    private val mappers = listOf(
        StringTypeMapper,
        ArrayTypeMapper,
        ReferenceTypeMapper,
        NestedObjectTypeMapper,
        UnionTypeMapper,
        PrimitiveTypeMapper // Keep as fallback
    )

    fun mapType(
        type: JsonSchemaParser.PropertyType,
        nullable: Boolean,
        propertyName: String = "",
        parentClassName: String = "",
        definitions: Map<String, JsonSchemaParser.DefinitionInfo> = emptyMap(),
        mainClassName: String = "",
        format: String? = null,
        formatMappers: Map<FormatEnum, String> = emptyMap()
    ): TypeName {
        val mapper = mappers.first { it.canHandle(type) }
        return mapper.mapType(type, nullable, propertyName, parentClassName, definitions, mainClassName, format, formatMappers)
    }
}

/**
 * Utility object to hold shared utility methods
 */
object KotlinTypeMapperUtil {
    fun toPascalCase(input: String): String {
        if (input.isEmpty()) return input

        val words = input.split(Regex("[^a-zA-Z0-9]+"))
            .filter { it.isNotEmpty() }

        if (words.isEmpty()) return "GeneratedClass"

        return words.joinToString("") { word ->
            word.lowercase().replaceFirstChar { it.titlecase() }
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

    /**
     * Resolves class name conflicts by appending "Child" when necessary
     * @param baseClassName The base class name to check
     * @param parentClassName The parent class name to compare against
     * @return The resolved class name
     */
    fun resolveClassNameConflict(baseClassName: String, parentClassName: String): String {
        val parentSimpleName = parentClassName.substringAfterLast('.')
        return if (baseClassName == parentSimpleName) {
            "${baseClassName}Child"
        } else {
            baseClassName
        }
    }

    private fun isKotlinKeyword(name: String): Boolean {
        return KOTLIN_KEYWORDS.contains(name)
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
