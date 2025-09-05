# Kotlin to JSON Schema

A Kotlin library that generates JSON schemas from Kotlin data classes using reflection. This library provides a simple and intuitive way to automatically create JSON schema definitions from your Kotlin data models.

## Features

- 🚀 **Simple API**: Generate schemas with a single extension property
- 🔍 **Reflection-based**: Automatically analyzes Kotlin data classes
- 🎯 **Type-safe**: Supports all common Kotlin types including nullables
- 📦 **Collections support**: Handles List, Array and other collection types
- 🌳 **Nested objects**: Recursive schema generation for complex data structures
- ⚡ **Lightweight**: Minimal dependencies with efficient processing

## Supported Types

- **Primitives**: String, Int, Long, Float, Double, Boolean
- **Collections**: List<T> with proper item type detection
- **Nullable types**: Automatic required field detection
- **Nested objects**: Recursive schema generation for data classes
- **Custom data classes**: Full support for complex nested structures

## Installation

Add the following to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("dev.jamiecraane:kotlin-to-jsonschema:1.0-SNAPSHOT")
}
```

## Quick Start

### Basic Usage

```kotlin
import dev.jamiecraane.kotlinjsonschema.schema

data class User(
    val name: String,
    val age: Int,
    val email: String?
)

// Generate schema
val schema = User::class.schema

println("Schema type: ${schema.type}")
println("Properties: ${schema.properties.keys}")
println("Required fields: ${schema.required}")
```

### Complex Example

```kotlin
data class Address(
    val street: String,
    val city: String,
    val zipCode: String
)

data class User(
    val name: String,
    val age: Int,
    val email: String?,
    val addresses: List<Address>,
    val isActive: Boolean
)

val userSchema = User::class.schema
```

### Generated Schema Structure

The library generates schemas with the following structure:

```kotlin
Type.Object(
    description = "Generated schema for User",
    type = "object",
    properties = mapOf(
        "name" to Type.Primitive("string", "Property name"),
        "age" to Type.Primitive("int", "Property age"),
        "email" to Type.Primitive("string", "Property email"),
        // ... more properties
    ),
    required = listOf("name", "age", "isActive", "addresses"), // non-nullable fields
    additionalProperties = false
)
```

## API Reference

### Extension Properties

#### `KClass<T>.schema: Type.Object`

Generates a JSON schema for the given Kotlin data class.

**Returns**: A `Type.Object` containing the complete schema definition

**Example**:
```kotlin
val schema = MyDataClass::class.schema
```

### Type System

The library uses a sealed class hierarchy to represent different JSON schema types:

#### `Type.Primitive`
Represents primitive types (string, int, number, boolean)

```kotlin
Type.Primitive(
    type: String,        // "string", "int", "number", "boolean"
    description: String  // Human-readable description
)
```

#### `Type.Array`
Represents array/list types

```kotlin
Type.Array(
    description: String, // Human-readable description
    type: String = "array",
    items: Type         // Schema for array items
)
```

#### `Type.Object`
Represents object types (data classes)

```kotlin
Type.Object(
    description: String,                    // Human-readable description
    type: String = "object",
    properties: Map<String, Type>,          // Property name to schema mapping
    required: List<String> = emptyList(),   // Required property names
    additionalProperties: Boolean = false   // Whether additional properties are allowed
)
```

## Type Mapping

| Kotlin Type | JSON Schema Type |
|-------------|------------------|
| `String` | `"string"` |
| `Int`, `Long` | `"int"` |
| `Float`, `Double` | `"number"` |
| `Boolean` | `"boolean"` |
| `List<T>` | `"array"` with items of type T |
| `Data Class` | `"object"` with recursive schema |
| `T?` (nullable) | Same as T, but not required |

## Examples

### Simple Data Class

```kotlin
data class Person(
    val firstName: String,
    val lastName: String,
    val age: Int
)

val schema = Person::class.schema
// Results in an object schema with 3 string/int properties, all required
```

### With Nullable Fields

```kotlin
data class User(
    val username: String,      // Required
    val email: String?,        // Optional
    val age: Int?,            // Optional
    val isActive: Boolean     // Required
)

val schema = User::class.schema
// Required fields: ["username", "isActive"]
// Optional fields: ["email", "age"]
```

### With Collections

```kotlin
data class BlogPost(
    val title: String,
    val content: String,
    val tags: List<String>,
    val comments: List<Comment>
)

data class Comment(
    val author: String,
    val text: String
)

val schema = BlogPost::class.schema
// tags: array of strings
// comments: array of Comment objects
```

### Nested Objects

```kotlin
data class Company(
    val name: String,
    val address: Address,
    val employees: List<Employee>
)

data class Address(
    val street: String,
    val city: String,
    val country: String
)

data class Employee(
    val name: String,
    val position: String,
    val salary: Double?
)

val schema = Company::class.schema
// Generates nested schemas for Address and Employee
```

## Advanced Usage

### Serialization

The library includes built-in serialization support:

```kotlin
import dev.jamiecraane.kotlinjsonschema.jsonschema.toJsonSchemaString

val schema = MyClass::class.schema
val jsonString = schema.toJsonSchemaString()
```

### Custom Descriptions

Property descriptions are automatically generated as "Property {propertyName}", but you can access and modify the schema as needed:

```kotlin
val schema = MyClass::class.schema
val customSchema = schema.copy(
    description = "Custom description for MyClass"
)
```

## Requirements

- Kotlin 2.2.0 or higher
- JVM target 21 or higher

## Dependencies

- `kotlin-reflect`: For reflection-based schema generation
- `kotlinx-serialization-json`: For JSON serialization support
- `kotlinx-datetime`: For date/time type support

## Contributing

Contributions are welcome! Please feel free to submit issues, feature requests, or pull requests.

## License

This project is licensed under the MIT License - see the LICENSE file for details.
