package dev.jamiecraane.kjstools.fromschema.parsing

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File

/**
 * Parses JSON Schema files and extracts normalized schema information.
 */
class JsonSchemaParser(
    private val schemaBaseDir: File,
    private val objectMapper: ObjectMapper,
) {
    private val propertyParser: PropertyParser = PropertyParser()

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
            rootProperties = propertyParser.parseProperties(schemaNode.get("properties"), schemaNode.get("required")),
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


    private fun parseDefinitions(definitionsNode: JsonNode?): Map<String, DefinitionInfo> {
        if (definitionsNode == null || !definitionsNode.isObject) {
            return emptyMap()
        }

        return definitionsNode.fields().asSequence().map { (name, defNode) ->
            name to DefinitionInfo(
                name = name,
                properties = propertyParser.parseProperties(defNode.get("properties"), defNode.get("required")),
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
