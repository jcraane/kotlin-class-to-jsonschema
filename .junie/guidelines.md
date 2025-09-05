# Kotlin JSON Schema Generator - Development Guidelines

## Build and Configuration

### Prerequisites
- **Kotlin**: 2.2.0 or higher (project uses 2.2.0)
- **JVM**: OpenJDK 21 or higher (configured via `jvmToolchain(21)`)
- **Gradle**: Uses Gradle wrapper (gradlew/gradlew.bat)

### Key Dependencies
- `kotlin-reflect` (2.1.10): Essential for runtime reflection-based schema generation
- `kotlinx-serialization-json` (1.9.0): JSON serialization support with custom configuration
- `kotlinx-datetime` (0.6.1): Date/time type support
- `logback-classic` (1.5.18): Logging framework

### Build Commands
```bash
# Build the project
./gradlew build

# Run all tests
./gradlew test

# Clean build artifacts
./gradlew clean
```

### Important Build Notes
- The project uses version catalog (`gradle/libs.versions.toml`) for dependency management
- kotlinx-serialization plugin is enabled for JSON schema serialization
- Group ID is configured as `dev.jamiecraane.imagecompression` (note: may need correction for this JSON schema project)

## Testing

### Test Framework
- **JUnit 4** with **kotlin-test** assertions
- Tests are located in `src/test/kotlin/dev/jamiecraane/kotlinjsonschema/`
- Uses `kotlin-test-junit` and `ktor-server-tests` from the testing bundle

### Running Tests
```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "dev.jamiecraane.kotlinjsonschema.SchemaGeneratorTest"

# Run tests with verbose output
./gradlew test --info
```

### Test Structure and Patterns

#### Basic Test Example
```kotlin
import dev.jamiecraane.kotlinjsonschema.schema
import dev.jamiecraane.kotlinjsonschema.jsonschema.Type
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

data class TestUser(
    val name: String,
    val age: Int,
    val isActive: Boolean = true
)

class UserSchemaTest {
    @Test
    fun `test user schema generation`() {
        val schema = TestUser::class.schema
        
        assertEquals("Generated schema for TestUser", schema.description)
        assertEquals("object", schema.type)
        assertEquals(3, schema.properties.size)
        
        // Test property types
        val nameProperty = schema.properties["name"]
        assertIs<Type.Primitive>(nameProperty)
        assertEquals("string", nameProperty.type)
    }
}
```

#### Testing Required Fields
**Important**: Required fields are returned in alphabetical order, not declaration order. Use containment checks instead of exact list comparison:

```kotlin
// ❌ Don't do this - will fail due to ordering
assertEquals(listOf("name", "email", "age"), schema.required)

// ✅ Do this instead
assertEquals(3, schema.required.size)
val expectedRequired = listOf("name", "email", "age")
expectedRequired.forEach { field ->
    assertTrue(schema.required.contains(field))
}
```

#### Testing Serialization
```kotlin
import dev.jamiecraane.kotlinjsonschema.jsonschema.JsonSchema
import dev.jamiecraane.kotlinjsonschema.jsonschema.toJsonSchemaString

@Test
fun `test schema serialization`() {
    val schema = MyClass::class.schema
    val jsonSchema = JsonSchema(
        name = "MyClass",
        description = "Schema for MyClass",
        schema = schema,
        strict = false
    )
    val jsonString = jsonSchema.toJsonSchemaString()
    
    assertTrue(jsonString.isNotEmpty())
    assertTrue(jsonString.contains("\"properties\""))
}
```

### Test Coverage Areas
The existing tests cover:
- Basic schema generation (`SchemaGeneratorTest`)
- Primitive type mappings (string, int, long, float, double, boolean)
- Collection handling (List<T>)
- Nested object schemas
- Nullable vs required field identification
- Empty class handling

## Development Information

### Project Architecture

#### Core Components
1. **SchemaGenerator.kt**: Main extension property `KClass<T>.schema` that generates Type.Object from Kotlin classes
2. **JsonSchemaWrapper.kt**: Defines the type system (`Type` sealed class) and JSON serialization support

#### Type System Hierarchy
```
Type (sealed class)
├── Primitive(type: String, description: String)
├── Enum(description: String, type: String, enum: List<String>)
├── Array(description: String, items: Type)
└── Object(description: String, properties: Map<String, Type>, required: List<String>)
```

### Key Implementation Details

#### Extension Property Pattern
The library uses Kotlin extension properties for clean API:
```kotlin
val schema = MyClass::class.schema  // Returns Type.Object
```

#### Reflection-Based Generation
- Uses `KClass.memberProperties` to analyze class structure
- Recursively generates schemas for nested data classes
- Determines required fields by checking `!returnType.isMarkedNullable`

#### Type Mappings
- `String` → `"string"`
- `Int`, `Long` → `"int"`
- `Float`, `Double` → `"number"`
- `Boolean` → `"boolean"`
- `List<T>` → Array type with item schema
- Data classes → Recursive object schema

#### JSON Serialization Configuration
The library uses custom JSON configuration:
```kotlin
Json {
    encodeDefaults = true
    coerceInputValues = true
    ignoreUnknownKeys = true
    isLenient = true
    prettyPrint = true
    classDiscriminatorMode = ClassDiscriminatorMode.NONE
}
```

### Code Style Guidelines

#### Data Class Requirements
- Target classes must be `data class` for automatic schema generation
- Use clear, descriptive property names
- Leverage Kotlin's type system (nullable vs non-nullable) for required field detection

#### Naming Conventions
- Test classes: `[FeatureName]Test`
- Test methods: Descriptive names with backticks for readability
- Data classes for testing: `Test[Purpose]` (e.g., `TestClass`, `NestedTestClass`)

#### Error Handling
- The library uses fallback to `Type.Primitive("string", description)` for unsupported types
- Complex non-data-class types default to string type

### Common Pitfalls

1. **Required Fields Ordering**: Don't assume declaration order in required fields list
2. **Serialization Method**: Use `JsonSchema.toJsonSchemaString()`, not `Type.Object.toJsonSchemaString()`
3. **Missing Imports**: Remember to import `kotlin.test.assertTrue` for test assertions
4. **Data Class Requirement**: Only `data class` instances generate proper object schemas

### Future Development Considerations

- Consider adding support for `kotlinx-datetime` types (LocalDate, LocalDateTime) - imports exist but mapping not implemented
- Enum support is defined in type system but not implemented in generator
- Additional validation constraints could be added to the Type system
- Consider supporting custom descriptions via annotations
