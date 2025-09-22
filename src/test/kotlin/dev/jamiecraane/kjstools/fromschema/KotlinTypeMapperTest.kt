package dev.jamiecraane.kjstools.fromschema

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.junit.Test
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertIs

class KotlinTypeMapperTest {

    @Test
    fun `test basic type mapping for all primitive types`() {
        // Create mapper with empty format mappers as we're ignoring them for now
        val mapper = KotlinTypeMapper(emptyMap())

        // Create test property info instances for each basic type
        val stringProperty = JsonSchemaParser.PropertyInfo(
            name = "stringField",
            type = JsonSchemaParser.PropertyType.StringType,
            nullable = false,
            required = true,
            description = "A string field",
            items = null
        )

        val numberProperty = JsonSchemaParser.PropertyInfo(
            name = "numberField",
            type = JsonSchemaParser.PropertyType.Number,
            nullable = false,
            required = true,
            description = "A number field",
            items = null
        )

        val integerProperty = JsonSchemaParser.PropertyInfo(
            name = "integerField",
            type = JsonSchemaParser.PropertyType.Integer,
            nullable = false,
            required = true,
            description = "An integer field",
            items = null
        )

        val booleanProperty = JsonSchemaParser.PropertyInfo(
            name = "booleanField",
            type = JsonSchemaParser.PropertyType.Boolean,
            nullable = false,
            required = true,
            description = "A boolean field",
            items = null
        )

        val arrayProperty = JsonSchemaParser.PropertyInfo(
            name = "arrayField",
            type = JsonSchemaParser.PropertyType.Array,
            nullable = false,
            required = true,
            description = "An array field",
            items = null
        )

        val objectProperty = JsonSchemaParser.PropertyInfo(
            name = "objectField",
            type = JsonSchemaParser.PropertyType.Object,
            nullable = false,
            required = true,
            description = "An object field",
            items = null
        )

        val nullProperty = JsonSchemaParser.PropertyInfo(
            name = "nullField",
            type = JsonSchemaParser.PropertyType.Null,
            nullable = false,
            required = true,
            description = "A null field",
            items = null
        )

        // Test string type mapping
        val stringResult = mapper.mapProperty(stringProperty)
        assertEquals("stringfield", stringResult.name)  // sanitized to camelCase
        assertEquals(String::class.asTypeName(), stringResult.type)
        assertEquals(false, stringResult.nullable)
        assertEquals("A string field", stringResult.description)

        // Test number type mapping
        val numberResult = mapper.mapProperty(numberProperty)
        assertEquals("numberfield", numberResult.name)  // sanitized to camelCase
        assertEquals(BigDecimal::class.asTypeName(), numberResult.type)
        assertEquals(false, numberResult.nullable)
        assertEquals("A number field", numberResult.description)

        // Test integer type mapping
        val integerResult = mapper.mapProperty(integerProperty)
        assertEquals("integerfield", integerResult.name)  // sanitized to camelCase
        assertEquals(Long::class.asTypeName(), integerResult.type)
        assertEquals(false, integerResult.nullable)
        assertEquals("An integer field", integerResult.description)

        // Test boolean type mapping
        val booleanResult = mapper.mapProperty(booleanProperty)
        assertEquals("booleanfield", booleanResult.name)  // sanitized to camelCase
        assertEquals(Boolean::class.asTypeName(), booleanResult.type)
        assertEquals(false, booleanResult.nullable)
        assertEquals("A boolean field", booleanResult.description)

        // Test array type mapping
        val arrayResult = mapper.mapProperty(arrayProperty)
        assertEquals("arrayfield", arrayResult.name)  // sanitized to camelCase
        val expectedListType = ClassName("kotlin.collections", "List").parameterizedBy(ClassName("kotlin", "Any"))
        assertEquals(expectedListType, arrayResult.type)
        assertEquals(false, arrayResult.nullable)
        assertEquals("An array field", arrayResult.description)

        // Test object type mapping
        val objectResult = mapper.mapProperty(objectProperty)
        assertEquals("objectfield", objectResult.name)  // sanitized to camelCase
        assertEquals(ClassName("kotlin", "Any"), objectResult.type)
        assertEquals(false, objectResult.nullable)
        assertEquals("An object field", objectResult.description)

        // Test null type mapping
        val nullResult = mapper.mapProperty(nullProperty)
        assertEquals("nullfield", nullResult.name)  // sanitized to camelCase
        assertEquals(Unit::class.asTypeName(), nullResult.type)
        assertEquals(false, nullResult.nullable)
        assertEquals("A null field", nullResult.description)
    }

    @Test
    fun `test nullable type mapping`() {
        val mapper = KotlinTypeMapper(emptyMap())

        val nullableStringProperty = JsonSchemaParser.PropertyInfo(
            name = "nullableString",
            type = JsonSchemaParser.PropertyType.StringType,
            nullable = true,
            required = false,
            description = "A nullable string",
            items = null
        )

        val result = mapper.mapProperty(nullableStringProperty)
        assertEquals("nullablestring", result.name)  // sanitized to camelCase
        assertEquals(String::class.asTypeName().copy(nullable = true), result.type)
        assertEquals(true, result.nullable)
        assertEquals("null", result.defaultValue)
        assertEquals("A nullable string", result.description)
    }

    @Test
    fun `test enum type mapping`() {
        val mapper = KotlinTypeMapper(emptyMap())

        val enumProperty = JsonSchemaParser.PropertyInfo(
            name = "statusField",
            type = JsonSchemaParser.PropertyType.Enum(listOf("ACTIVE", "INACTIVE", "PENDING")),
            nullable = false,
            required = true,
            description = "A status enum",
            items = null
        )

        val result = mapper.mapProperty(enumProperty)
        assertEquals("statusfield", result.name)  // sanitized to camelCase
        assertEquals(String::class.asTypeName(), result.type)
        assertEquals(false, result.nullable)
        assertEquals("A status enum", result.description)

        // Also test enum info extraction
        val enumInfo = mapper.mapEnum(enumProperty)
        assertEquals("StatusfieldEnum", enumInfo?.name)
        assertEquals(listOf("ACTIVE", "INACTIVE", "PENDING"), enumInfo?.values)
        assertEquals("A status enum", enumInfo?.description)
    }
}
