package dev.jamiecraane.kjstools.fromschema

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

/**
 * Generates Kotlin data classes using KotlinPoet from parsed JSON Schema information.
 * Focuses on primary data class generation and delegates specialized responsibilities
 * to NestedClassGenerator and EnumClassGenerator.
 */
class DataClassGenerator(
    formatMappers: Map<FormatEnum, String> = emptyMap(),
) {
    private val typeMapper: KotlinTypeMapper = KotlinTypeMapper(formatMappers)
    private val nestedClassGenerator: NestedClassGenerator = NestedClassGenerator(typeMapper)
    private val enumClassGenerator: EnumClassGenerator = EnumClassGenerator(typeMapper)

    fun generateDataClass(
        parsedSchema: JsonSchemaParser.ParsedSchema,
        packageName: String,
        schemaFileName: String? = null,
    ): FileSpec {
        val mainClassName = typeMapper.sanitizeClassName(
            name = extractMainClassName(parsedSchema)
        )

        val fileBuilder = FileSpec.builder(packageName, mainClassName)
            .addFileComment("Generated from JSON Schema file: ${schemaFileName ?: "unknown"}.json")
            .addFileComment("")
            .addFileComment("Schema ID: ${parsedSchema.id}")
            .addFileComment("Title: ${parsedSchema.title ?: "N/A"}")
            .addFileComment("Description: ${parsedSchema.description ?: "N/A"}")

        // Generate main data class
        val mainClass = generateMainDataClass(mainClassName, parsedSchema)
        fileBuilder.addType(mainClass)

        // Definitions will be generated as nested classes within the main class
        // No need to generate them as separate top-level classes

        // Generate enum classes if any
        val enumClasses = enumClassGenerator.generateEnumClasses(parsedSchema)
        enumClasses.forEach { fileBuilder.addType(it) }


        return fileBuilder.build()
    }

    private fun generateMainDataClass(
        className: String,
        parsedSchema: JsonSchemaParser.ParsedSchema,
    ): TypeSpec {
        val mainClass = nestedClassGenerator.generateClassWithProperties(
            className = className,
            properties = parsedSchema.rootProperties,
            definitions = parsedSchema.definitions,
            parentContext = className,
            description = parsedSchema.description,
            includeNestedObjects = true
        )

        // Add nested classes for definitions (like Subdoel, Activiteit)
        // Only generate classes for complex objects with properties, not simple type definitions
        val classBuilder = mainClass.toBuilder()
        parsedSchema.definitions.forEach { (defName, definition) ->
            if (definition.properties.isNotEmpty()) {
                val definitionClass =
                    nestedClassGenerator.generateDefinitionAsNestedClass(defName, definition, parsedSchema.definitions, className)
                classBuilder.addType(definitionClass)
            }
        }

        return classBuilder.build()
    }










    private fun extractMainClassName(parsedSchema: JsonSchemaParser.ParsedSchema): String {
        return (parsedSchema.title ?: extractClassNameFromSchemaId(parsedSchema.id))
            .replaceFirstChar(Char::uppercaseChar)
    }

    private fun extractClassNameFromSchemaId(schemaId: String): String = schemaId.substringAfterLast("/").ifEmpty {
        schemaId.substringAfterLast("#").ifEmpty { "GeneratedClass" }
    }
}

