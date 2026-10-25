package github.xzynine.checkupdata.internal

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal class JsonParseException(
    message: String,
) : Exception(message)

internal val readJson: Json =
    Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
internal fun prettyJson(): Json =
    Json {
        prettyPrint = true
        prettyPrintIndent = "  "
    }

internal fun JsonObject.requireArray(key: String): JsonArray = this[key] as? JsonArray ?: throw JsonParseException("Missing array: " + key)

internal fun JsonObject.requireString(key: String): String = this[key]?.let { readString(it) } ?: throw JsonParseException("Missing key: " + key)

internal fun JsonObject.requireLong(key: String): Long = this[key]?.let { readLong(it) } ?: throw JsonParseException("Missing key: " + key)

internal fun JsonObject.optionalArrayOrNull(key: String): JsonArray? = this[key] as? JsonArray

internal fun JsonObject.optionalString(
    key: String,
    fallback: String = "",
): String = this[key]?.let { element -> if (element is JsonNull) "null" else readString(element) } ?: fallback

internal fun JsonObject.optionalStringOrNull(key: String): String? =
    this[key]?.let { element ->
        if (element is JsonNull) null else readString(element)
    }

internal fun JsonObject.optionalLongOrNull(key: String): Long? =
    this[key]?.let { element ->
        if (element is JsonNull) null else readLong(element)
    }

internal fun JsonObject.optionalBoolean(
    key: String,
    fallback: Boolean = false,
): Boolean =
    this[key]?.let { element ->
        if (element is JsonNull) fallback else readBoolean(element)
    } ?: fallback

internal fun parseJsonObject(text: String): JsonObject {
    val element =
        try {
            readJson.parseToJsonElement(text)
        } catch (e: IllegalArgumentException) {
            throw JsonParseException(e.message ?: "Malformed JSON")
        }
    return element as? JsonObject ?: throw JsonParseException("Not a JSON object")
}

internal fun parseJsonArray(text: String): JsonArray {
    val element =
        try {
            readJson.parseToJsonElement(text)
        } catch (e: IllegalArgumentException) {
            throw JsonParseException(e.message ?: "Malformed JSON")
        }
    return element as? JsonArray ?: throw JsonParseException("Not a JSON array")
}

private fun readString(element: JsonElement): String = (element as? JsonPrimitive)?.content ?: throw JsonParseException("Not a primitive")

private fun readLong(element: JsonElement): Long {
    val primitive = element as? JsonPrimitive ?: throw JsonParseException("Not a primitive")
    if (!primitive.isString) {
        primitive.content.toLongOrNull()?.let { return it }
    }
    return primitive.content.toLongOrNull() ?: throw JsonParseException("Not a number: " + primitive.content)
}

private fun readBoolean(element: JsonElement): Boolean {
    val primitive = element as? JsonPrimitive ?: throw JsonParseException("Not a primitive")
    val text = primitive.content
    return when {
        text.equals("true", ignoreCase = true) -> true
        text.equals("false", ignoreCase = true) -> false
        else -> throw JsonParseException("Not a boolean: " + text)
    }
}
