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
        val className = KotlinTypeMapperUtil.resolveClassNameConflict(baseClassName, mainClassName)

        return generateClassWithProperties(
            className = className,
            properties = definition.properties,
            definitions = definitions,
            parentContext = mainClassName,
            description = definition.description,
            includeNestedObjects = true
        )
    }

    fun generateNestedDataClass(
        definitionName: String,
        definition: JsonSchemaParser.DefinitionInfo,
        parentClassName: String,
        definitions: Map<String, JsonSchemaParser.DefinitionInfo> = emptyMap(),
    ): TypeSpec {
        val className = typeMapper.sanitizeClassName(definitionName)

        return generateClassWithProperties(
            className = className,
            properties = definition.properties,
            definitions = definitions,
            parentContext = "",
            description = definition.description,
            includeNestedObjects = false
        )
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

    /**
     * Common method to generate a class with properties, eliminating code duplication
     * across generateMainDataClass, generateDefinitionAsNestedClass, generateDefinitionClass, and generateNestedDataClass
     */
    internal fun generateClassWithProperties(
        className: String,
        properties: Map<String, JsonSchemaParser.PropertyInfo>,
        definitions: Map<String, JsonSchemaParser.DefinitionInfo> = emptyMap(),
        parentContext: String = "",
        description: String? = null,
        includeNestedObjects: Boolean = true
    ): TypeSpec {
        val classBuilder = TypeSpec.classBuilder(className)
            .addModifiers(KModifier.DATA)
            .addAnnotation(
                AnnotationSpec.builder(JsonIgnoreProperties::class)
                    .addMember("ignoreUnknown = false")
                    .build()
            )

        // Add class-level KDoc if available
        description?.let { desc ->
            classBuilder.addKdoc(desc.replace("\"", "\\\""))
        }

        // Generate primary constructor
        val constructorBuilder = FunSpec.constructorBuilder()

        properties.forEach { (_, property) ->
            val kotlinProperty = when (property.type) {
                is JsonSchemaParser.PropertyType.Array -> {
                    val mappedProperty = typeMapper.mapProperty(property, className, definitions, parentContext)
                    val arrayType = typeMapper.mapArrayType(property, definitions, parentContext)
                    // If the default value is null, make the array type nullable
                    val finalArrayType = if (mappedProperty.defaultValue == "null") {
                        arrayType.copy(nullable = true)
                    } else {
                        arrayType
                    }
                    mappedProperty.copy(type = finalArrayType)
                }

                else -> typeMapper.mapProperty(property, className, definitions, parentContext)
            }

            addPropertyToClass(classBuilder, constructorBuilder, kotlinProperty)
        }

        // Add nested classes for nested objects if requested
        if (includeNestedObjects) {
            properties.values.forEach { property ->
                if (property.type is JsonSchemaParser.PropertyType.NestedObject) {
                    val baseNestedClassName =
                        typeMapper.sanitizeClassName(property.name).replaceFirstChar { it.uppercase() }
                    val nestedClassName = KotlinTypeMapperUtil.resolveClassNameConflict(baseNestedClassName, className)
                    val nestedClass = generateNestedObjectClass(nestedClassName, property.type, definitions)
                    classBuilder.addType(nestedClass)
                }
            }
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
