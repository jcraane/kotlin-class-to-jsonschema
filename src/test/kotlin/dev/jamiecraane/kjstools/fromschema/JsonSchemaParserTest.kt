package dev.jamiecraane.kjstools.fromschema

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class JsonSchemaParserTest {

    @Test
    fun `test parsing simple Person schema`() {
        val testResourcesDir = File("src/test/resources")
        val objectMapper = ObjectMapper()
        val parser = JsonSchemaParser(testResourcesDir, objectMapper)

        val parsedSchema = parser.parseSchema("person")

        // Verify basic schema properties
        assertEquals("Person", parsedSchema.title)
        assertEquals("person", parsedSchema.id)
        assertEquals(2, parsedSchema.rootProperties.size)

        // Verify firstName property
        val firstName = parsedSchema.rootProperties["firstName"]
        assertIs<JsonSchemaParser.PropertyInfo>(firstName)
        assertEquals("firstName", firstName.name)
        assertIs<JsonSchemaParser.PropertyType.StringType>(firstName.type)
        assertEquals("The person's first name", firstName.description)
        assertTrue(firstName.required)

        // Verify lastName property
        val lastName = parsedSchema.rootProperties["lastName"]
        assertIs<JsonSchemaParser.PropertyInfo>(lastName)
        assertEquals("lastName", lastName.name)
        assertIs<JsonSchemaParser.PropertyType.StringType>(lastName.type)
        assertEquals("The person's last name", lastName.description)
        assertTrue(lastName.required)

        // Verify no definitions in this simple schema
        assertTrue(parsedSchema.definitions.isEmpty())
    }
}
