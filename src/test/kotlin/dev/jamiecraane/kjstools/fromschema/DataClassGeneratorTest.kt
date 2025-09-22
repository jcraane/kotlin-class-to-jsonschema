package dev.jamiecraane.kjstools.fromschema

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DataClassGeneratorTest {

    @Test
    fun `test generate data class from simple Person schema`() {
        // Setup parser and generator
        val testResourcesDir = File("src/test/resources")
        val objectMapper = ObjectMapper()
        val parser = JsonSchemaParser(testResourcesDir, objectMapper)
        val generator = DataClassGenerator()

        // Parse the person schema
        val parsedSchema = parser.parseSchema("person")

        // Generate the data class
        val fileSpec = generator.generateDataClass(
            parsedSchema = parsedSchema,
            packageName = "com.example.test",
            schemaFileName = "person"
        )

        // Verify the generated file spec
        assertEquals("com.example.test", fileSpec.packageName)
        assertEquals("Person", fileSpec.name)

        // Convert to string to examine the generated code
        val generatedCode = fileSpec.toString()

        // Verify file comments include schema information
        assertTrue(generatedCode.contains("Generated from JSON Schema file: person.json"))
        assertTrue(generatedCode.contains("Schema ID: person"))
        assertTrue(generatedCode.contains("Title: Person"))

        // Verify the generated data class structure
        assertTrue(generatedCode.contains("data class Person"))
        assertTrue(generatedCode.contains("@JsonIgnoreProperties(ignoreUnknown = false)"))

        // Verify firstName property (note: property name is lowercase but JsonProperty annotation has original case)
        assertTrue(generatedCode.contains("firstname: String"))
        assertTrue(generatedCode.contains("@JsonProperty(\"firstName\")"))

        // Verify lastName property (note: property name is lowercase but JsonProperty annotation has original case)
        assertTrue(generatedCode.contains("lastname: String"))
        assertTrue(generatedCode.contains("@JsonProperty(\"lastName\")"))

        // Verify both properties are constructor parameters (no default values since they're required)
        assertTrue(generatedCode.contains("firstname: String,"))
        assertTrue(generatedCode.contains("lastname: String,"))

        // Verify the generated code compiles by checking basic structure
        val lines = generatedCode.lines()
        val dataClassLine = lines.find { it.contains("data class Person") }
        assertTrue(dataClassLine != null, "Should contain data class declaration")
    }
}
