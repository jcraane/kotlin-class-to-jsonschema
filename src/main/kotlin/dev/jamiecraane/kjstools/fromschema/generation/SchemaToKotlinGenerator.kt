package dev.jamiecraane.kjstools.fromschema.generation

// No Spring dependencies; run as a plain CLI tool
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.two.KotlinLogging
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import dev.jamiecraane.kjstools.fromschema.parsing.JsonSchemaParser

/**
 * Main orchestrator for generating Kotlin data classes from JSON Schema files.
 */
class SchemaToKotlinGenerator(
    private val schemaDirectory: String,
    private val packageName: String,
    private val outputDirectory: String,
) {
    init {
        require(packageName.isNotBlank()) {
            "Package name must be provided"
        }
    }

    private val logger = KotlinLogging.logger {}
    private val parser = JsonSchemaParser(File(schemaDirectory), jacksonObjectMapper())
    private val dataClassGenerator = DataClassGenerator()

    data class GenerationResult(
        val schemaName: String,
        val className: String,
        val filePath: String,
        val success: Boolean,
        val error: String? = null,
    )

    data class GenerationSummary(
        val totalSchemas: Int,
        val successfulGenerations: Int,
        val failedGenerations: Int,
        val results: List<GenerationResult>,
    ) {
        val success: Boolean get() = failedGenerations == 0
    }

    /**
     * Generate Kotlin data classes for all JSON schemas in the schema directory.
     */
    fun generateAll(): GenerationSummary {
        logger.info { "Starting generation of Kotlin data classes from JSON schemas" }

        val schemaNames = discoverSchemaNames()
        logger.info { "Discovered ${schemaNames.size} schemas: ${schemaNames.joinToString(", ")}" }

        // Ensure output directory exists
        val outputPath = Paths.get(outputDirectory)
        Files.createDirectories(outputPath)

        // Clean existing generated files
        cleanOutputDirectory(outputPath)

        val results = schemaNames.map { schemaName ->
            try {
                generateForSchema(schemaName)
            } catch (e: Exception) {
                logger.error(e) { "Failed to generate classes for schema: $schemaName" }
                GenerationResult(
                    schemaName = schemaName,
                    className = "Unknown",
                    filePath = "Unknown",
                    success = false,
                    error = e.message
                )
            }
        }

        val summary = GenerationSummary(
            totalSchemas = schemaNames.size,
            successfulGenerations = results.count { it.success },
            failedGenerations = results.count { !it.success },
            results = results
        )

        logSummary(summary)
        return summary
    }

    /**
     * Generate Kotlin data class for a specific schema.
     */
    fun generateForSchema(schemaName: String): GenerationResult {
        logger.debug { "Generating classes for schema: $schemaName" }

        try {
            // Parse the schema
            val parsedSchema = parser.parseSchema(schemaName)
            logger.debug { "Parsed schema: ${parsedSchema.id}, title: ${parsedSchema.title}" }

            // Generate the file spec
            val fileSpec = dataClassGenerator.generateDataClass(parsedSchema, packageName, schemaName)

            // Determine output file path
            val outputPath = Paths.get(outputDirectory)
            val filePath = outputPath.resolve("${fileSpec.name}.kt")

            // Write the file - KotlinPoet expects the root directory where package structure will be created
            val sourceRoot = Paths.get(outputDirectory)
            fileSpec.writeTo(sourceRoot)

            logger.info { "Successfully generated: ${fileSpec.name}.kt for schema: $schemaName" }

            return GenerationResult(
                schemaName = schemaName,
                className = fileSpec.name,
                filePath = filePath.toString(),
                success = true
            )

        } catch (e: Exception) {
            logger.error(e) { "Failed to generate classes for schema: $schemaName" }
            throw e
        }
    }

    /**
     * Discover all available schema names from the classpath.
     */
    fun discoverSchemaNames(): List<String> {
        val schemaNames = mutableSetOf<String>()

        try {
            // Get all .json files from the provided schema directory
            val schemaDir = File(schemaDirectory)
            if (schemaDir.exists() && schemaDir.isDirectory) {
                schemaDir.listFiles { file -> file.extension == "json" }?.forEach { file ->
                    val name = file.nameWithoutExtension
                    // Filter out .schema.json files as they might be duplicates
                    if (!name.endsWith(".schema")) {
                        schemaNames.add(name)
                    }
                }
            }

            // Also check for .schema.json files and add them with their base name
            schemaDir.listFiles { file -> file.name.endsWith(".schema.json") }?.forEach { file ->
                val baseName = file.name.removeSuffix(".schema.json")
                schemaNames.add(baseName)
            }

        } catch (e: Exception) {
            logger.warn(e) { "Could not discover schemas from file system, trying known schemas in $schemaDirectory" }
        }

        return schemaNames.sorted()
    }

    private fun cleanOutputDirectory(outputPath: Path) {
        logger.info { "Cleaning output directory: $outputPath" }

        if (Files.exists(outputPath)) {
            Files.walk(outputPath)
                .filter { it != outputPath }
                .filter { Files.isRegularFile(it) }
                .filter { it.toString().endsWith(".kt") }
                .forEach { file ->
                    try {
                        Files.delete(file)
                        logger.debug { "Deleted existing file: $file" }
                    } catch (e: Exception) {
                        logger.warn(e) { "Could not delete file: $file" }
                    }
                }
        }
    }

    private fun logSummary(summary: GenerationSummary) {
        logger.info { "Generation Summary:" }
        logger.info { "  Total schemas: ${summary.totalSchemas}" }
        logger.info { "  Successful: ${summary.successfulGenerations}" }
        logger.info { "  Failed: ${summary.failedGenerations}" }

        if (summary.failedGenerations > 0) {
            logger.error { "Failed schemas:" }
            summary.results.filter { !it.success }.forEach { result ->
                logger.error { "  - ${result.schemaName}: ${result.error}" }
            }
        }

        if (summary.successfulGenerations > 0) {
            logger.info { "Generated classes:" }
            summary.results.filter { it.success }.forEach { result ->
                logger.info { "  - ${result.className} (from ${result.schemaName})" }
            }
        }
    }

    companion object {
        /**
         * Main method for Gradle task execution.
         */
        @JvmStatic
        fun main(args: Array<String>) {
            require(args.size == 3) { "Usage: SchemaToKotlinGenerator <schemaDirectory> <packageName> <outputDirectory>" }
            val schemaDirectory = args[0]
            val packageName = args[1]
            val outputDirectory = args[2]

            val generator = SchemaToKotlinGenerator(
                schemaDirectory = schemaDirectory,
                packageName = packageName,
                outputDirectory = outputDirectory
            )
            val summary = generator.generateAll()

            if (!summary.success) {
                System.exit(1)
            }
        }
    }
}
