1. Extract Property Parser from JsonSchemaParser

The JsonSchemaParser class is doing too much - both parsing the schema structure and parsing individual properties. Consider extracting a PropertyParser class:
- JsonSchemaParser.parseProperty(), parseAnyOfProperty(), parseUnionType() methods would move to PropertyParser
- Reduces complexity in main parser class

2. Split DataClassGenerator Responsibilities

The DataClassGenerator has multiple generation methods that could be separated:
- Extract a NestedClassGenerator for handling nested object classes
- Extract an EnumClassGenerator for enum-specific generation
- Main class focuses only on the primary data class generation

3. Eliminate Code Duplication in DataClassGenerator

Multiple methods contain similar property handling logic:
- generateMainDataClass(), generateDefinitionAsNestedClass(), generateDefinitionClass(), and generateNestedDataClass() all have nearly identical patterns
- Extract common method: generateClassWithProperties(className, properties, definitions, parentContext)

4. Simplify Type Mapping Logic

The KotlinTypeMapper.mapType() method is quite complex with nested conditionals:
- Extract specific type mappers: StringTypeMapper, ArrayTypeMapper, ReferenceTypeMapper
- Use strategy pattern or sealed classes for cleaner type mapping

5. Remove Dead Code

Several methods appear unused or always return empty:
- generateNestedObjectClasses() always returns emptyList()
- generateDefinitionClass() seems unused compared to generateDefinitionAsNestedClass()

6. Improve Error Handling

- Add more specific exception types instead of generic exceptions
- Better error messages with context about which schema/property failed
- Consider using Result/Either types for better error propagation

7. Configuration Management

The SchemaToKotlinGenerator hardcodes some behavior:
- Extract configuration class for output settings, naming conventions, etc.
- Make format mappers configurable at the generator level

8. Reduce String Concatenation

Multiple places use string building for class names:
- "${baseNestedClassName}Child" pattern appears frequently
- Extract utility methods for consistent naming conventions

9. Schema Validation

Add input validation layer:
- Validate schema structure before parsing
- Ensure required fields are present
- Validate that references can be resolved

10. Immutable Data Structures

Consider making the data classes more immutable:
- Use @JvmRecord for simple data containers
- Make collections immutable by default
