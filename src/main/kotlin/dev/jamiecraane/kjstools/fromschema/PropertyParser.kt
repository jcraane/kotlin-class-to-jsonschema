package dev.jamiecraane.kjstools.fromschema

import com.fasterxml.jackson.databind.JsonNode

/**
 * Handles parsing of individual properties from JSON Schema nodes.
 * Extracted from JsonSchemaParser to improve separation of concerns.
 */
class PropertyParser {

    fun parseProperties(propertiesNode: JsonNode?, requiredNode: JsonNode?): Map<String, JsonSchemaParser.PropertyInfo> {
        if (propertiesNode == null || !propertiesNode.isObject) {
            return emptyMap()
        }

        val requiredFields = requiredNode?.map { it.asText() }?.toSet() ?: emptySet()

        return propertiesNode.fields().asSequence().map { (name, propertyNode) ->
            name to parseProperty(name, propertyNode, name in requiredFields)
        }.toMap()
    }

    fun parseProperty(name: String, propertyNode: JsonNode, required: Boolean): JsonSchemaParser.PropertyInfo {
        val ref = propertyNode.get("\$ref")?.asText()
        if (ref != null) {
            return JsonSchemaParser.PropertyInfo(
                name = name,
                type = JsonSchemaParser.PropertyType.Reference(extractRefName(ref)),
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
                        JsonSchemaParser.PropertyType.NestedObject(nestedProperties, nestedRequired)
                    } else {
                        JsonSchemaParser.PropertyType.Object
                    }
                } else {
                    parseSimpleType(typeText)
                }
            }
            else -> JsonSchemaParser.PropertyType.Object // fallback
        }

        val enumNode = propertyNode.get("enum")
        val enumValues = if (enumNode?.isArray == true) {
            enumNode.map { it.asText() }
        } else null

        return JsonSchemaParser.PropertyInfo(
            name = name,
            type = if (enumValues != null) JsonSchemaParser.PropertyType.Enum(enumValues) else type,
            description = propertyNode.get("description")?.asText(),
            required = required,
            nullable = isNullable(propertyNode, typeNode),
            format = propertyNode.get("format")?.asText(),
            enumValues = enumValues,
            items = parseItemsProperty(propertyNode.get("items"))
        )
    }

    private fun parseAnyOfProperty(name: String, anyOfNode: JsonNode, required: Boolean): JsonSchemaParser.PropertyInfo {
        val types = anyOfNode.map { node ->
            val typeText = node.get("type")?.asText()
            val ref = node.get("\$ref")?.asText()
            when {
                ref != null -> JsonSchemaParser.PropertyType.Reference(extractRefName(ref))
                typeText != null -> parseSimpleType(typeText)
                else -> JsonSchemaParser.PropertyType.Object
            }
        }

        // Check if it's a simple nullable pattern (type + null)
        val nonNullTypes = types.filter { it !is JsonSchemaParser.PropertyType.Null }
        val hasNull = types.any { it is JsonSchemaParser.PropertyType.Null }

        return when {
            nonNullTypes.size == 1 && hasNull -> {
                // Simple nullable pattern: anyOf: [type, null]
                val baseType = nonNullTypes.first()
                val firstNode = anyOfNode.first { it.get("type")?.asText() != "null" }
                JsonSchemaParser.PropertyInfo(
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
                JsonSchemaParser.PropertyInfo(
                    name = name,
                    type = JsonSchemaParser.PropertyType.Union(types),
                    description = null,
                    required = required,
                    nullable = hasNull
                )
            }
        }
    }

    private fun parseUnionType(typeNode: JsonNode): JsonSchemaParser.PropertyType {
        val types = typeNode.map { parseSimpleType(it.asText()) }
        val nonNullTypes = types.filter { it !is JsonSchemaParser.PropertyType.Null }

        return when {
            nonNullTypes.size == 1 -> nonNullTypes.first()
            else -> JsonSchemaParser.PropertyType.Union(types)
        }
    }

    private fun parseSimpleType(typeText: String): JsonSchemaParser.PropertyType = when (typeText) {
        "string" -> JsonSchemaParser.PropertyType.StringType
        "number" -> JsonSchemaParser.PropertyType.Number
        "integer" -> JsonSchemaParser.PropertyType.Integer
        "boolean" -> JsonSchemaParser.PropertyType.Boolean
        "array" -> JsonSchemaParser.PropertyType.Array
        "object" -> JsonSchemaParser.PropertyType.Object
        "null" -> JsonSchemaParser.PropertyType.Null
        else -> JsonSchemaParser.PropertyType.StringType // fallback
    }

    private fun parseItemsProperty(itemsNode: JsonNode?): JsonSchemaParser.PropertyInfo? {
        if (itemsNode == null) return null

        val ref = itemsNode.get("\$ref")?.asText()
        if (ref != null) {
            return JsonSchemaParser.PropertyInfo(
                name = "item",
                type = JsonSchemaParser.PropertyType.Reference(extractRefName(ref)),
                description = null,
                ref = ref
            )
        }

        val typeText = itemsNode.get("type")?.asText() ?: "object"
        return JsonSchemaParser.PropertyInfo(
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

    private fun extractRefName(ref: String): String {
        // Extract name from "#/definitions/name" format
        return ref.substringAfterLast("/")
    }
}