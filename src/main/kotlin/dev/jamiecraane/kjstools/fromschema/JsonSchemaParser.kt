package dev.jamiecraane.kjstools.fromschema

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File

/**
 * Parses JSON Schema files and extracts normalized schema information.
 */
class JsonSchemaParser(
    private val schemaBaseDir: File,
    private val objectMapper: ObjectMapper
) {

    data class ParsedSchema(
        val id: String,
        val title: String?,
        val description: String?,
        val rootProperties: Map<String, PropertyInfo>,
        val definitions: Map<String, DefinitionInfo>
    )

    data class PropertyInfo(
        val name: String,
        val type: PropertyType,
        val description: String?,
        val required: Boolean = false,
        val nullable: Boolean = false,
        val format: String? = null,
        val enumValues: List<String>? = null,
        val items: PropertyInfo? = null,
        val ref: String? = null
    )

    data class DefinitionInfo(
        val name: String,
        val properties: Map<String, PropertyInfo>,
        val description: String?,
        val additionalProperties: Boolean = true
    )

    sealed class PropertyType {
        object StringType : PropertyType()
        object Number : PropertyType()
        object Integer : PropertyType()
        object Boolean : PropertyType()
        object Array : PropertyType()
        object Object : PropertyType()
        data class NestedObject(val properties: Map<String, PropertyInfo>, val required: List<String> = emptyList()) : PropertyType()
        object Null : PropertyType()
        data class Reference(val ref: String) : PropertyType()
        data class Union(val types: List<PropertyType>) : PropertyType()
        data class Enum(val values: List<String>) : PropertyType()
    }

    fun parseSchema(schemaName: String): ParsedSchema {
        val file = loadSchemaFile(schemaName)
        val schemaNode = objectMapper.readTree(file)

        return ParsedSchema(
            id = schemaNode.get("\$id")?.asText() ?: schemaName,
            title = schemaNode.get("title")?.asText(),
            description = schemaNode.get("description")?.asText(),
            rootProperties = parseProperties(schemaNode.get("properties"), schemaNode.get("required")),
            definitions = parseDefinitions(schemaNode.get("definitions"))
        )
    }

    private fun loadSchemaFile(schemaName: String): File {
        val candidates = listOf(
            File(schemaBaseDir, "$schemaName.json"),
            File(schemaBaseDir, "$schemaName.schema.json")
        )
        return candidates.firstOrNull { it.exists() && it.isFile }
            ?: throw IllegalArgumentException("Schema not found in ${schemaBaseDir.absolutePath}: $schemaName")
    }

    private fun parseProperties(propertiesNode: JsonNode?, requiredNode: JsonNode?): Map<String, PropertyInfo> {
        if (propertiesNode == null || !propertiesNode.isObject) {
            return emptyMap()
        }

        val requiredFields = requiredNode?.map { it.asText() }?.toSet() ?: emptySet()

        return propertiesNode.fields().asSequence().map { (name, propertyNode) ->
            name to parseProperty(name, propertyNode, name in requiredFields)
        }.toMap()
    }

    private fun parseProperty(name: String, propertyNode: JsonNode, required: Boolean): PropertyInfo {
        val ref = propertyNode.get("\$ref")?.asText()
        if (ref != null) {
            return PropertyInfo(
                name = name,
                type = PropertyType.Reference(extractRefName(ref)),
                description = propertyNode.get("description")?.asText(),
                required = required,
                ref = ref
            )
        }

        val anyOfNode = propertyNode.get("anyOf")
        if (anyOfNode != null && anyOfNode.isArray) {
            return parseAnyOfProperty(name, anyOfNode, required)
        }

        val typeNode = propertyNode.get("type")
        val type = when {
            typeNode?.isArray == true -> parseUnionType(typeNode)
            typeNode?.isTextual == true -> {
                val typeText = typeNode.asText()
                if (typeText == "object") {
                    // Check if this object has nested properties
                    val nestedPropertiesNode = propertyNode.get("properties")
                    if (nestedPropertiesNode != null && nestedPropertiesNode.isObject) {
                        val nestedRequiredNode = propertyNode.get("required")
                        val nestedRequired = nestedRequiredNode?.map { it.asText() } ?: emptyList()
                        val nestedProperties = parseProperties(nestedPropertiesNode, nestedRequiredNode)
                        PropertyType.NestedObject(nestedProperties, nestedRequired)
                    } else {
                        PropertyType.Object
                    }
                } else {
                    parseSimpleType(typeText)
                }
            }
            else -> PropertyType.Object // fallback
        }

        val enumNode = propertyNode.get("enum")
        val enumValues = if (enumNode?.isArray == true) {
            enumNode.map { it.asText() }
        } else null

        return PropertyInfo(
            name = name,
            type = if (enumValues != null) PropertyType.Enum(enumValues) else type,
            description = propertyNode.get("description")?.asText(),
            required = required,
            nullable = isNullable(propertyNode, typeNode),
            format = propertyNode.get("format")?.asText(),
            enumValues = enumValues,
            items = parseItemsProperty(propertyNode.get("items"))
        )
    }

    private fun parseAnyOfProperty(name: String, anyOfNode: JsonNode, required: Boolean): PropertyInfo {
        val types = anyOfNode.map { node ->
            val typeText = node.get("type")?.asText()
            val ref = node.get("\$ref")?.asText()
            when {
                ref != null -> PropertyType.Reference(extractRefName(ref))
                typeText != null -> parseSimpleType(typeText)
                else -> PropertyType.Object
            }
        }

        // Check if it's a simple nullable pattern (type + null)
        val nonNullTypes = types.filter { it !is PropertyType.Null }
        val hasNull = types.any { it is PropertyType.Null }

        return when {
            nonNullTypes.size == 1 && hasNull -> {
                // Simple nullable pattern: anyOf: [type, null]
                val baseType = nonNullTypes.first()
                val firstNode = anyOfNode.first { it.get("type")?.asText() != "null" }
                PropertyInfo(
                    name = name,
                    type = baseType,
                    description = firstNode.get("description")?.asText(),
                    required = required,
                    nullable = true,
                    format = firstNode.get("format")?.asText()
                )
            }
            else -> {
                // Complex union type
                PropertyInfo(
                    name = name,
                    type = PropertyType.Union(types),
                    description = null,
                    required = required,
                    nullable = hasNull
                )
            }
        }
    }

    private fun parseUnionType(typeNode: JsonNode): PropertyType {
        val types = typeNode.map { parseSimpleType(it.asText()) }
        val nonNullTypes = types.filter { it !is PropertyType.Null }

        return when {
            nonNullTypes.size == 1 -> nonNullTypes.first()
            else -> PropertyType.Union(types)
        }
    }

    private fun parseSimpleType(typeText: String): PropertyType = when (typeText) {
        "string" -> PropertyType.StringType
        "number" -> PropertyType.Number
        "integer" -> PropertyType.Integer
        "boolean" -> PropertyType.Boolean
        "array" -> PropertyType.Array
        "object" -> PropertyType.Object
        "null" -> PropertyType.Null
        else -> PropertyType.StringType // fallback
    }

    private fun parseItemsProperty(itemsNode: JsonNode?): PropertyInfo? {
        if (itemsNode == null) return null

        val ref = itemsNode.get("\$ref")?.asText()
        if (ref != null) {
            return PropertyInfo(
                name = "item",
                type = PropertyType.Reference(extractRefName(ref)),
                description = null,
                ref = ref
            )
        }

        val typeText = itemsNode.get("type")?.asText() ?: "object"
        return PropertyInfo(
            name = "item",
            type = parseSimpleType(typeText),
            description = itemsNode.get("description")?.asText()
        )
    }

    private fun isNullable(propertyNode: JsonNode, typeNode: JsonNode?): Boolean {
        return when {
            typeNode?.isArray == true -> typeNode.any { it.asText() == "null" }
            propertyNode.get("anyOf") != null -> propertyNode.get("anyOf").any {
                it.get("type")?.asText() == "null"
            }
            else -> false
        }
    }

    private fun parseDefinitions(definitionsNode: JsonNode?): Map<String, DefinitionInfo> {
        if (definitionsNode == null || !definitionsNode.isObject) {
            return emptyMap()
        }

        return definitionsNode.fields().asSequence().map { (name, defNode) ->
            name to DefinitionInfo(
                name = name,
                properties = parseProperties(defNode.get("properties"), defNode.get("required")),
                description = defNode.get("description")?.asText(),
                additionalProperties = defNode.get("additionalProperties")?.asBoolean() ?: true
            )
        }.toMap()
    }

    private fun extractRefName(ref: String): String {
        // Extract name from "#/definitions/name" format
        return ref.substringAfterLast("/")
    }
}
