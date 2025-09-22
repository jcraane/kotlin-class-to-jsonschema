package dev.jamiecraane.kjstools.fromschema.generation

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import dev.jamiecraane.kjstools.fromschema.parsing.JsonSchemaParser

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

    @Test
    fun `test generate data class from complex Person schema`() {
        // Setup parser and generator
        val testResourcesDir = File("src/test/resources")
        val objectMapper = ObjectMapper()
        val parser = JsonSchemaParser(testResourcesDir, objectMapper)
        val generator = DataClassGenerator()

        // Parse the complex-person schema
        val parsedSchema = parser.parseSchema("complex-person")

        // Generate the data class
        val fileSpec = generator.generateDataClass(
            parsedSchema = parsedSchema,
            packageName = "com.example.test",
            schemaFileName = "complex-person"
        )

        // Verify the generated file spec
        assertEquals("com.example.test", fileSpec.packageName)
        assertEquals("Complexperson", fileSpec.name)

        // Convert to string to examine the generated code
        val generatedCode = fileSpec.toString()

        // Verify file comments include schema information
        assertTrue(generatedCode.contains("Generated from JSON Schema file: complex-person.json"))
        assertTrue(generatedCode.contains("Schema ID: complex-person"))
        assertTrue(generatedCode.contains("Title: ComplexPerson"))

        // Verify the main ComplexPerson data class structure
        assertTrue(generatedCode.contains("data class Complexperson"))
        assertTrue(generatedCode.contains("@JsonIgnoreProperties(ignoreUnknown = false)"))

        // Verify required fields (firstname, lastname, birthdate)
        assertTrue(generatedCode.contains("firstname: String"))
        assertTrue(generatedCode.contains("@JsonProperty(\"firstname\")"))
        assertTrue(generatedCode.contains("lastname: String"))
        assertTrue(generatedCode.contains("@JsonProperty(\"lastname\")"))
        assertTrue(generatedCode.contains("birthdate: String"))
        assertTrue(generatedCode.contains("@JsonProperty(\"birthdate\")"))

        // Verify optional fields (livingAddress, postalAddress, courses)
        assertTrue(generatedCode.contains("livingaddress: Complexperson.Address?"))
        assertTrue(generatedCode.contains("@JsonProperty(\"livingAddress\")"))
        assertTrue(generatedCode.contains("postaladdress: Complexperson.Address?"))
        assertTrue(generatedCode.contains("@JsonProperty(\"postalAddress\")"))
        assertTrue(generatedCode.contains("courses: List<Complexperson.Course>"))
        assertTrue(generatedCode.contains("@JsonProperty(\"courses\")"))

        // Verify nested Address class is generated
        assertTrue(generatedCode.contains("data class Address"))
        assertTrue(generatedCode.contains("street: String"))
        assertTrue(generatedCode.contains("city: String"))
        assertTrue(generatedCode.contains("housenumber: String"))
        assertTrue(generatedCode.contains("zipcode: String"))

        // Verify nested Course class is generated
        assertTrue(generatedCode.contains("data class Course"))
        assertTrue(generatedCode.contains("name: String"))
        assertTrue(generatedCode.contains("datetime: String"))

        // Verify that Address properties are all required (no nullable types)
        assertTrue(generatedCode.contains("street: String,"))
        assertTrue(generatedCode.contains("city: String,"))
        assertTrue(generatedCode.contains("housenumber: String,"))
        assertTrue(generatedCode.contains("zipcode: String,"))

        // Verify that Course properties are all required (no nullable types)
        assertTrue(generatedCode.contains("name: String,"))
        assertTrue(generatedCode.contains("datetime: String,"))

        // Verify the generated code has proper structure
        val lines = generatedCode.lines()
        val complexPersonLine = lines.find { it.contains("data class Complexperson") }
        val addressLine = lines.find { it.contains("data class Address") }
        val courseLine = lines.find { it.contains("data class Course") }

        assertTrue(complexPersonLine != null, "Should contain Complexperson data class declaration")
        assertTrue(addressLine != null, "Should contain Address data class declaration")
        assertTrue(courseLine != null, "Should contain Course data class declaration")
    }
}
