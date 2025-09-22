package dev.jamiecraane.kjstools.fromschema

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Format(val value: String)

/**
 * Enum representing all formats supported by Json Schema (as per opis.io).
 * Each enum value has a [code] property for the corresponding string identifier.
 * Reference: https://opis.io/json-schema/2.x/formats.html
 */
enum class FormatEnum(val code: String) {
    // Date and Time
    DATE_TIME("date-time"),
    TIME("time"),
    DATE("date"),
    DURATION("duration"),

    // Email
    EMAIL("email"),
    IDN_EMAIL("idn-email"),

    // Hostname
    HOSTNAME("hostname"),
    IDN_HOSTNAME("idn-hostname"),

    // IP Address
    IPV4("ipv4"),
    IPV6("ipv6"),

    // Resource identifiers
    UUID("uuid"),
    URI("uri"),
    URI_REFERENCE("uri-reference"),
    IRI("iri"),
    IRI_REFERENCE("iri-reference"),
    URI_TEMPLATE("uri-template"),

    // Regular expressions (ECMA 262 dialect)
    REGEX("regex"),

    // JSON Pointer, JSON Reference
    JSON_POINTER("json-pointer"),
    RELATIVE_JSON_POINTER("relative-json-pointer"),

    // Other
    BASE64("base64"),
    BINARY("binary"),

    // Internet-related
    URL("url"),

    // Phone & Messaging
    PHONE("phone"),

    // Credit card
    CREDIT_CARD("credit-card"),

    // Color
    COLOR("color")
}

/**
 * Existing constants for format strings (for reference/compatibility).
 * Prefer using [FormatEnum] where possible.
 */
object FormatConstants {
    // Date and Time formats
    const val DATE_TIME = "date-time"
    const val TIME = "time"
    const val DATE = "date"
    const val DURATION = "duration"

    // Email formats
    const val EMAIL = "email"
    const val IDN_EMAIL = "idn-email"

    // Hostname formats
    const val HOSTNAME = "hostname"
    const val IDN_HOSTNAME = "idn-hostname"

    // IP Address formats
    const val IPV4 = "ipv4"
    const val IPV6 = "ipv6"

    // Resource Identifier formats
    const val UUID = "uuid"
    const val URI = "uri"
    const val URI_REFERENCE = "uri-reference"
    const val IRI = "iri"
    const val IRI_REFERENCE = "iri-reference"
    const val URI_TEMPLATE = "uri-template"
    const val REGEX = "regex"
    const val JSON_POINTER = "json-pointer"
    const val RELATIVE_JSON_POINTER = "relative-json-pointer"
    const val BASE64 = "base64"
    const val BINARY = "binary"
    const val URL = "url"
    const val PHONE = "phone"
    const val CREDIT_CARD = "credit-card"
    const val COLOR = "color"
}
