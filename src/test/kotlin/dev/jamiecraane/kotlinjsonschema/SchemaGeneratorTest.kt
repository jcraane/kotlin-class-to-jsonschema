package dev.jamiecraane.kotlinjsonschema

import dev.jamiecraane.kotlinjsonschema.jsonschema.Type
import org.junit.Test
import kotlin.collections.forEach
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Test data class with various property types for testing schema generation
 */
data class TestClass(
    val stringProperty: String,
    val intProperty: Int,
    val longProperty: Long,
    val floatProperty: Float,
    val doubleProperty: Double,
    val booleanProperty: Boolean,
    val stringCollection: List<String>,
    val nullableString: String?,
    val nullableInt: Int?,
    val nullableCollection: List<String>?
)

/**
 * Nested test data class for testing object schema generation
 */
data class NestedTestClass(
    val name: String,
    val nested: TestClass,
    val nestedList: List<TestClass>
)

class SchemaGeneratorTest {

    @Test
    fun `test basic schema generation for test class`() {
        val schema = TestClass::class.schema

        assertEquals("Generated schema for TestClass", schema.description)
        assertEquals("object", schema.type)
        assertEquals(10, schema.properties.size)
        assertEquals(false, schema.additionalProperties)
    }

    @Test
    fun `test string property type mapping`() {
        val schema = TestClass::class.schema
        val stringProp = schema.properties["stringProperty"]

        assertIs<Type.Primitive>(stringProp)
        assertEquals("string", stringProp.type)
        assertEquals("Property stringProperty", stringProp.description)
    }

    @Test
    fun `test numeric property type mappings`() {
        val schema = TestClass::class.schema

        val intProp = schema.properties["intProperty"] as Type.Primitive
        assertEquals("int", intProp.type)
        assertEquals("Property intProperty", intProp.description)

        val longProp = schema.properties["longProperty"] as Type.Primitive
        assertEquals("int", longProp.type)
        assertEquals("Property longProperty", longProp.description)

        val floatProp = schema.properties["floatProperty"] as Type.Primitive
        assertEquals("number", floatProp.type)
        assertEquals("Property floatProperty", floatProp.description)

        val doubleProp = schema.properties["doubleProperty"] as Type.Primitive
        assertEquals("number", doubleProp.type)
        assertEquals("Property doubleProperty", doubleProp.description)
    }

    @Test
    fun `test boolean property type mapping`() {
        val schema = TestClass::class.schema
        val booleanProp = schema.properties["booleanProperty"]

        assertIs<Type.Primitive>(booleanProp)
        assertEquals("boolean", booleanProp.type)
        assertEquals("Property booleanProperty", booleanProp.description)
    }

    @Test
    fun `test string collection property type mapping`() {
        val schema = TestClass::class.schema
        val collectionProp = schema.properties["stringCollection"]

        assertIs<Type.Array>(collectionProp)
        assertEquals("array", collectionProp.type)
        assertEquals("Property stringCollection", collectionProp.description)

        val itemType = collectionProp.items
        assertIs<Type.Primitive>(itemType)
        assertEquals("string", itemType.type)
        assertEquals("Array item", itemType.description)
    }

    @Test
    fun `test required fields identification`() {
        val schema = TestClass::class.schema
        val expectedRequired = listOf(
            "stringProperty",
            "intProperty",
            "longProperty",
            "floatProperty",
            "doubleProperty",
            "booleanProperty",
            "stringCollection"
        )

        assertEquals(expectedRequired.size, schema.required.size)
        expectedRequired.forEach { field ->
            assertTrue(schema.required.contains(field), "Required field $field should be present")
        }
    }

    @Test
    fun `test nullable fields are not required`() {
        val schema = TestClass::class.schema
        val nullableFields = listOf("nullableString", "nullableInt", "nullableCollection")

        nullableFields.forEach { field ->
            assertTrue(schema.properties.containsKey(field), "Nullable field $field should be present in properties")
            assertTrue(!schema.required.contains(field), "Nullable field $field should not be required")
        }
    }

    @Test
    fun `test nested object schema generation`() {
        val schema = NestedTestClass::class.schema

        assertEquals("Generated schema for NestedTestClass", schema.description)
        assertEquals(3, schema.properties.size)

        // Test nested object property
        val nestedProp = schema.properties["nested"]
        assertIs<Type.Object>(nestedProp)
        assertEquals("Generated schema for TestClass", nestedProp.description)
        assertEquals(10, nestedProp.properties.size)

        // Test nested list property
        val nestedListProp = schema.properties["nestedList"]
        assertIs<Type.Array>(nestedListProp)
        assertEquals("array", nestedListProp.type)

        val nestedListItemType = nestedListProp.items
        assertIs<Type.Object>(nestedListItemType)
        assertEquals("Generated schema for TestClass", nestedListItemType.description)
    }

    @Test
    fun `test required fields for nested class`() {
        val schema = NestedTestClass::class.schema
        val expectedRequired = listOf("name", "nested", "nestedList")

        assertEquals(expectedRequired.size, schema.required.size)
        expectedRequired.forEach { field ->
            assertTrue(schema.required.contains(field), "Required field $field should be present")
        }
    }

    @Test
    fun `test empty class schema generation`() {
        data class EmptyClass(val dummy: Unit = Unit)

        val schema = EmptyClass::class.schema
        assertEquals("Generated schema for EmptyClass", schema.description)
        assertEquals("object", schema.type)
        assertEquals(1, schema.properties.size) // Unit property
        assertEquals(false, schema.additionalProperties)
    }
}


@Description("Simple class description")
data class SimpleAnnotated(val id: Int)

@Description("Custom class description")
data class ClassWithDescription(val value: String)

data class PropertyAnnotated(
    @Description("User name") val name: String,
    @Description("Nested override") val nested: SimpleAnnotated,
    @Description("Tags list") val tags: List<String>
)

class DescriptionAnnotationTest {
    @Test
    fun `class-level description overrides default`() {
        val schema = ClassWithDescription::class.schema
        assertEquals("Custom class description", schema.description)
    }

    @Test
    fun `property-level description for primitive and array and object`() {
        val schema = PropertyAnnotated::class.schema

        val nameProp = schema.properties["name"]
        assertIs<Type.Primitive>(nameProp)
        assertEquals("User name", nameProp.description)

        val nestedProp = schema.properties["nested"]
        assertIs<Type.Object>(nestedProp)
        // Property-level description should override the object's class-level description
        assertEquals("Nested override", nestedProp.description)

        val tagsProp = schema.properties["tags"]
        assertIs<Type.Array>(tagsProp)
        assertEquals("Tags list", tagsProp.description)
        val itemType = tagsProp.items
        assertIs<Type.Primitive>(itemType)
        assertEquals("Array item", itemType.description)
    }
}
