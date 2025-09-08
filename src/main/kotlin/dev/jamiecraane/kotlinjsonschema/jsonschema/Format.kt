package dev.jamiecraane.kotlinjsonschema.jsonschema

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Format(val value: String)

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
}
