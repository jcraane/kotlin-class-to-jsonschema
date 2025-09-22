package dev.jamiecraane.kjstools.fromschema

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

/**
 * Generates nested Kotlin data classes for object properties and definitions.
 */
class NestedClassGenerator(
    private val typeMapper: KotlinTypeMapper
) {

    fun generateDefinitionAsNestedClass(
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

    fun generateDefinitionClass(
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

    fun generateNestedDataClass(
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

    fun generateNestedObjectClass(
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
}