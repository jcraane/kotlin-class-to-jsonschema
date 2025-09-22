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
 */
class DataClassGenerator() {
    private val typeMapper: KotlinTypeMapper = KotlinTypeMapper()

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
        val enumClasses = generateEnumClasses(parsedSchema)
        enumClasses.forEach { fileBuilder.addType(it) }

        // Generate nested object classes
        val nestedClasses = generateNestedObjectClasses(parsedSchema, mainClassName)
        nestedClasses.forEach { fileBuilder.addType(it) }

        return fileBuilder.build()
    }

    private fun generateMainDataClass(
        className: String,
        parsedSchema: JsonSchemaParser.ParsedSchema,
    ): TypeSpec {
        val classBuilder = TypeSpec.classBuilder(className)
            .addModifiers(KModifier.DATA)
            .addAnnotation(
                AnnotationSpec.builder(JsonIgnoreProperties::class)
                    .addMember("ignoreUnknown = false")
                    .build()
            )

        // Add class-level KDoc if available
        parsedSchema.description?.let { description ->
            classBuilder.addKdoc(description.replace("\"", "\\\""))
        }

        // Generate primary constructor
        val constructorBuilder = FunSpec.constructorBuilder()

        parsedSchema.rootProperties.forEach { (_, property) ->
            val kotlinProperty = when (property.type) {
                is JsonSchemaParser.PropertyType.Array -> {
                    val mappedProperty =
                        typeMapper.mapProperty(property, className, parsedSchema.definitions, className)
                    val arrayType = typeMapper.mapArrayType(property, parsedSchema.definitions, className)
                    // If the default value is null, make the array type nullable
                    val finalArrayType = if (mappedProperty.defaultValue == "null") {
                        arrayType.copy(nullable = true)
                    } else {
                        arrayType
                    }
                    mappedProperty.copy(type = finalArrayType)
                }

                else -> typeMapper.mapProperty(property, className, parsedSchema.definitions, className)
            }

            addPropertyToClass(classBuilder, constructorBuilder, kotlinProperty)
        }

        // Add nested classes for nested objects
        parsedSchema.rootProperties.values.forEach { property ->
            if (property.type is JsonSchemaParser.PropertyType.NestedObject) {
                val baseNestedClassName =
                    typeMapper.sanitizeClassName(property.name).replaceFirstChar { it.uppercase() }
                val nestedClassName = if (baseNestedClassName == className) {
                    "${baseNestedClassName}Child"
                } else {
                    baseNestedClassName
                }
                val nestedClass = generateNestedObjectClass(nestedClassName, property.type, parsedSchema.definitions)
                classBuilder.addType(nestedClass)
            }
        }

        // Add nested classes for definitions (like Subdoel, Activiteit)
        // Only generate classes for complex objects with properties, not simple type definitions
        parsedSchema.definitions.forEach { (defName, definition) ->
            if (definition.properties.isNotEmpty()) {
                val definitionClass =
                    generateDefinitionAsNestedClass(defName, definition, parsedSchema.definitions, className)
                classBuilder.addType(definitionClass)
            }
        }

        return classBuilder.primaryConstructor(constructorBuilder.build()).build()
    }

    private fun generateDefinitionAsNestedClass(
        definitionName: String,
        definition: JsonSchemaParser.DefinitionInfo,
        definitions: Map<String, JsonSchemaParser.DefinitionInfo> = emptyMap(),
        mainClassName: String = "",
    ): TypeSpec {
        val baseClassName = typeMapper.sanitizeClassName(definitionName).replaceFirstChar { it.uppercase() }
        val className = if (baseClassName == mainClassName) {
            "${baseClassName}Child"
        } else {
            baseClassName
        }
        val classBuilder = TypeSpec.classBuilder(className)
            .addModifiers(KModifier.DATA)
            .addAnnotation(
                AnnotationSpec.builder(JsonIgnoreProperties::class)
                    .addMember("ignoreUnknown = false")
                    .build()
            )

        // Add class-level KDoc if available
        definition.description?.let { description ->
            classBuilder.addKdoc(description.replace("\"", "\\\""))
        }

        // Generate primary constructor
        val constructorBuilder = FunSpec.constructorBuilder()

        definition.properties.forEach { (_, property) ->
            val kotlinProperty = when (property.type) {
                is JsonSchemaParser.PropertyType.Array -> {
                    val mappedProperty = typeMapper.mapProperty(property, className, definitions, mainClassName)
                    val arrayType = typeMapper.mapArrayType(property, definitions, mainClassName)
                    // If the default value is null, make the array type nullable
                    val finalArrayType = if (mappedProperty.defaultValue == "null") {
                        arrayType.copy(nullable = true)
                    } else {
                        arrayType
                    }
                    mappedProperty.copy(type = finalArrayType)
                }

                else -> typeMapper.mapProperty(property, className, definitions, mainClassName)
            }

            addPropertyToClass(classBuilder, constructorBuilder, kotlinProperty)
        }

        // Add nested classes for nested objects within this definition
        definition.properties.values.forEach { property ->
            if (property.type is JsonSchemaParser.PropertyType.NestedObject) {
                val baseNestedClassName =
                    typeMapper.sanitizeClassName(property.name).replaceFirstChar { it.uppercase() }
                val nestedClassName = if (baseNestedClassName == className) {
                    "${baseNestedClassName}Child"
                } else {
                    baseNestedClassName
                }
                val nestedClass = generateNestedObjectClass(nestedClassName, property.type, definitions)
                classBuilder.addType(nestedClass)
            }
        }

        return classBuilder.primaryConstructor(constructorBuilder.build()).build()
    }

    private fun generateDefinitionClass(
        definitionName: String,
        definition: JsonSchemaParser.DefinitionInfo,
        definitions: Map<String, JsonSchemaParser.DefinitionInfo> = emptyMap(),
    ): TypeSpec {
        val className = typeMapper.sanitizeClassName(definitionName).replaceFirstChar { it.uppercase() }
        val classBuilder = TypeSpec.classBuilder(className)
            .addModifiers(KModifier.DATA)
            .addAnnotation(
                AnnotationSpec.builder(JsonIgnoreProperties::class)
                    .addMember("ignoreUnknown = false")
                    .build()
            )

        // Add class-level KDoc if available
        definition.description?.let { description ->
            classBuilder.addKdoc(description.replace("\"", "\\\""))
        }

        // Generate primary constructor
        val constructorBuilder = FunSpec.constructorBuilder()

        definition.properties.forEach { (_, property) ->
            val kotlinProperty = when (property.type) {
                is JsonSchemaParser.PropertyType.Array -> {
                    val mappedProperty = typeMapper.mapProperty(property, className, definitions, "")
                    val arrayType = typeMapper.mapArrayType(property, definitions, "")
                    // If the default value is null, make the array type nullable
                    val finalArrayType = if (mappedProperty.defaultValue == "null") {
                        arrayType.copy(nullable = true)
                    } else {
                        arrayType
                    }
                    mappedProperty.copy(type = finalArrayType)
                }

                else -> typeMapper.mapProperty(property, className, definitions, "")
            }

            addPropertyToClass(classBuilder, constructorBuilder, kotlinProperty)
        }

        return classBuilder.primaryConstructor(constructorBuilder.build()).build()
    }

    private fun generateNestedDataClass(
        definitionName: String,
        definition: JsonSchemaParser.DefinitionInfo,
        parentClassName: String,
        definitions: Map<String, JsonSchemaParser.DefinitionInfo> = emptyMap(),
    ): TypeSpec {
        val className = typeMapper.sanitizeClassName(definitionName)
        val classBuilder = TypeSpec.classBuilder(className)
            .addModifiers(KModifier.DATA)
            .addAnnotation(
                AnnotationSpec.builder(JsonIgnoreProperties::class)
                    .addMember("ignoreUnknown = false")
                    .build()
            )

        // Add class-level KDoc if available
        definition.description?.let { description ->
            classBuilder.addKdoc(description.replace("\"", "\\\""))
        }

        // Generate primary constructor
        val constructorBuilder = FunSpec.constructorBuilder()

        definition.properties.forEach { (_, property) ->
            val kotlinProperty = when (property.type) {
                is JsonSchemaParser.PropertyType.Array -> {
                    val mappedProperty = typeMapper.mapProperty(property, parentClassName, definitions, "")
                    val arrayType = typeMapper.mapArrayType(property, definitions, "")
                    // If the default value is null, make the array type nullable
                    val finalArrayType = if (mappedProperty.defaultValue == "null") {
                        arrayType.copy(nullable = true)
                    } else {
                        arrayType
                    }
                    mappedProperty.copy(type = finalArrayType)
                }

                else -> typeMapper.mapProperty(property, parentClassName, definitions, "")
            }

            addPropertyToClass(classBuilder, constructorBuilder, kotlinProperty)
        }

        return classBuilder.primaryConstructor(constructorBuilder.build()).build()
    }

    private fun addPropertyToClass(
        classBuilder: TypeSpec.Builder,
        constructorBuilder: FunSpec.Builder,
        kotlinProperty: KotlinTypeMapper.KotlinPropertyInfo,
    ) {
        // Add constructor parameter
        val parameterBuilder = ParameterSpec.builder(kotlinProperty.name, kotlinProperty.type)
            .addAnnotation(
                AnnotationSpec.builder(JsonProperty::class)
                    .addMember("%S", kotlinProperty.jsonPropertyName)
                    .build()
            )

        // Add default value if available
        kotlinProperty.defaultValue?.let { defaultValue ->
            parameterBuilder.defaultValue(defaultValue)
        }

        constructorBuilder.addParameter(parameterBuilder.build())

        // Add property to class
        val propertyBuilder = PropertySpec.builder(kotlinProperty.name, kotlinProperty.type)
            .initializer(kotlinProperty.name)

        // Add property KDoc if available
        kotlinProperty.description?.let { description ->
            propertyBuilder.addKdoc(description.replace("\"", "\\\""))
        }

        classBuilder.addProperty(propertyBuilder.build())
    }

    private fun generateNestedObjectClasses(
        parsedSchema: JsonSchemaParser.ParsedSchema,
        mainClassName: String,
    ): List<TypeSpec> {
        // We'll now add nested classes directly to the main class instead of as separate top-level classes
        return emptyList()
    }

    private fun generateNestedObjectClass(
        className: String,
        nestedObject: JsonSchemaParser.PropertyType.NestedObject,
        definitions: Map<String, JsonSchemaParser.DefinitionInfo> = emptyMap(),
    ): TypeSpec {
        val classBuilder = TypeSpec.classBuilder(className)
            .addModifiers(KModifier.DATA)
            .addAnnotation(
                AnnotationSpec.builder(JsonIgnoreProperties::class)
                    .addMember("ignoreUnknown = false")
                    .build()
            )

        // Generate primary constructor
        val constructorBuilder = FunSpec.constructorBuilder()

        nestedObject.properties.forEach { (_, property) ->
            val kotlinProperty = typeMapper.mapProperty(property, className, definitions, "")
            addPropertyToClass(classBuilder, constructorBuilder, kotlinProperty)
        }

        return classBuilder.primaryConstructor(constructorBuilder.build()).build()
    }

    private fun generateEnumClasses(parsedSchema: JsonSchemaParser.ParsedSchema): List<TypeSpec> {
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

    private fun generateEnumClass(enumInfo: KotlinTypeMapper.KotlinEnumInfo): TypeSpec {
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

    private fun extractMainClassName(parsedSchema: JsonSchemaParser.ParsedSchema): String {
        return (parsedSchema.title ?: extractClassNameFromSchemaId(parsedSchema.id))
            .replaceFirstChar(Char::uppercaseChar)
    }

    private fun extractClassNameFromSchemaId(schemaId: String): String = schemaId.substringAfterLast("/").ifEmpty {
        schemaId.substringAfterLast("#").ifEmpty { "GeneratedClass" }
    }
}

