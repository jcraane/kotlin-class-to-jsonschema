package dev.jamiecraane.kjstools.fromschema.parsing

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

    @Test
    fun `test parsing complex Person schema with definitions`() {
        val testResourcesDir = File("src/test/resources")
        val objectMapper = ObjectMapper()
        val parser = JsonSchemaParser(testResourcesDir, objectMapper)

        val parsedSchema = parser.parseSchema("complex-person")

        // Verify basic schema properties
        assertEquals("ComplexPerson", parsedSchema.title)
        assertEquals("complex-person", parsedSchema.id)
        assertEquals(6, parsedSchema.rootProperties.size)

        // Verify firstname property
        val firstname = parsedSchema.rootProperties["firstname"]
        assertIs<JsonSchemaParser.PropertyInfo>(firstname)
        assertEquals("firstname", firstname.name)
        assertIs<JsonSchemaParser.PropertyType.StringType>(firstname.type)
        assertEquals("The person's first name", firstname.description)
        assertTrue(firstname.required)

        // Verify lastname property
        val lastname = parsedSchema.rootProperties["lastname"]
        assertIs<JsonSchemaParser.PropertyInfo>(lastname)
        assertEquals("lastname", lastname.name)
        assertIs<JsonSchemaParser.PropertyType.StringType>(lastname.type)
        assertEquals("The person's last name", lastname.description)
        assertTrue(lastname.required)

        // Verify birthdate property with date format
        val birthdate = parsedSchema.rootProperties["birthdate"]
        assertIs<JsonSchemaParser.PropertyInfo>(birthdate)
        assertEquals("birthdate", birthdate.name)
        assertIs<JsonSchemaParser.PropertyType.StringType>(birthdate.type)
        assertEquals("The person's birth date", birthdate.description)
        assertEquals("date", birthdate.format)
        assertTrue(birthdate.required)

        // Verify livingAddress property with reference
        val livingAddress = parsedSchema.rootProperties["livingAddress"]
        assertIs<JsonSchemaParser.PropertyInfo>(livingAddress)
        assertEquals("livingAddress", livingAddress.name)
        assertIs<JsonSchemaParser.PropertyType.Reference>(livingAddress.type)
        assertEquals("Address", livingAddress.type.ref)
        assertEquals("The person's living address", livingAddress.description)

        // Verify postalAddress property with reference
        val postalAddress = parsedSchema.rootProperties["postalAddress"]
        assertIs<JsonSchemaParser.PropertyInfo>(postalAddress)
        assertEquals("postalAddress", postalAddress.name)
        assertIs<JsonSchemaParser.PropertyType.Reference>(postalAddress.type)
        assertEquals("Address", postalAddress.type.ref)
        assertEquals("The person's postal address", postalAddress.description)

        // Verify courses array property
        val courses = parsedSchema.rootProperties["courses"]
        assertIs<JsonSchemaParser.PropertyInfo>(courses)
        assertEquals("courses", courses.name)
        assertIs<JsonSchemaParser.PropertyType.Array>(courses.type)
        assertEquals("List of courses the person attended", courses.description)

        // Verify courses array items reference
        val courseItems = courses.items
        assertIs<JsonSchemaParser.PropertyInfo>(courseItems)
        assertIs<JsonSchemaParser.PropertyType.Reference>(courseItems.type)
        assertEquals("Course", courseItems.type.ref)

        // Verify definitions exist
        assertEquals(2, parsedSchema.definitions.size)
        assertTrue(parsedSchema.definitions.containsKey("Address"))
        assertTrue(parsedSchema.definitions.containsKey("Course"))

        // Verify Address definition
        val addressDef = parsedSchema.definitions["Address"]
        assertIs<JsonSchemaParser.DefinitionInfo>(addressDef)
        assertEquals("Address", addressDef.name)
        assertEquals(4, addressDef.properties.size)

        // Verify Address properties
        val street = addressDef.properties["street"]
        assertIs<JsonSchemaParser.PropertyInfo>(street)
        assertEquals("street", street.name)
        assertIs<JsonSchemaParser.PropertyType.StringType>(street.type)
        assertEquals("Street name", street.description)
        assertTrue(street.required)

        val city = addressDef.properties["city"]
        assertIs<JsonSchemaParser.PropertyInfo>(city)
        assertEquals("city", city.name)
        assertIs<JsonSchemaParser.PropertyType.StringType>(city.type)
        assertEquals("City name", city.description)
        assertTrue(city.required)

        val housenumber = addressDef.properties["housenumber"]
        assertIs<JsonSchemaParser.PropertyInfo>(housenumber)
        assertEquals("housenumber", housenumber.name)
        assertIs<JsonSchemaParser.PropertyType.StringType>(housenumber.type)
        assertEquals("House number", housenumber.description)
        assertTrue(housenumber.required)

        val zipcode = addressDef.properties["zipcode"]
        assertIs<JsonSchemaParser.PropertyInfo>(zipcode)
        assertEquals("zipcode", zipcode.name)
        assertIs<JsonSchemaParser.PropertyType.StringType>(zipcode.type)
        assertEquals("Zip code", zipcode.description)
        assertTrue(zipcode.required)

        // Verify Course definition
        val courseDef = parsedSchema.definitions["Course"]
        assertIs<JsonSchemaParser.DefinitionInfo>(courseDef)
        assertEquals("Course", courseDef.name)
        assertEquals(2, courseDef.properties.size)

        // Verify Course properties
        val courseName = courseDef.properties["name"]
        assertIs<JsonSchemaParser.PropertyInfo>(courseName)
        assertEquals("name", courseName.name)
        assertIs<JsonSchemaParser.PropertyType.StringType>(courseName.type)
        assertEquals("Course name", courseName.description)
        assertTrue(courseName.required)

        val datetime = courseDef.properties["datetime"]
        assertIs<JsonSchemaParser.PropertyInfo>(datetime)
        assertEquals("datetime", datetime.name)
        assertIs<JsonSchemaParser.PropertyType.StringType>(datetime.type)
        assertEquals("Course date and time", datetime.description)
        assertEquals("date-time", datetime.format)
        assertTrue(datetime.required)
    }

    @Test
    fun `test parsing invalid JSON schema throws exception`() {
        val testResourcesDir = File("src/test/resources")
        val objectMapper = ObjectMapper()
        val parser = JsonSchemaParser(testResourcesDir, objectMapper)

        try {
            parser.parseSchema("invalid-schema")
            assertTrue(false, "Expected exception when parsing invalid JSON schema")
        } catch (e: Exception) {
            // Document what type of exception is thrown
            println("[DEBUG_LOG] Exception type: ${e.javaClass.simpleName}")
            println("[DEBUG_LOG] Exception message: ${e.message}")

            // The test passes if any exception is thrown when parsing invalid JSON
            assertTrue(true, "Exception was correctly thrown for invalid JSON schema: ${e.message}")
        }
    }
}
