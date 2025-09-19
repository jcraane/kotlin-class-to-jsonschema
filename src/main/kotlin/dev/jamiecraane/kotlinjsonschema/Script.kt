package dev.jamiecraane.kotlinjsonschema

import com.ritense.valtimo.implementation.inburgering.domain.Inburgeringsprofiel
import dev.jamiecraane.kjstools.generator.schema
import dev.jamiecraane.kjstools.jsonschema.toJsonSchemaString

fun main() {
    val schema = Inburgeringsprofiel::class.schema
    println(schema)

    val jsonSchema = schema.toJsonSchemaString()
    println(jsonSchema)
}
