package com.arangodb.intellij.aql.util

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import org.slf4j.LoggerFactory

object JSON {
    private val logger = LoggerFactory.getLogger(JSON::class.java)

    @JvmField
    val EMPTY_BYTES = ByteArray(0)

    private val mapper: ObjectMapper = ObjectMapper().apply {
        setSerializationInclusion(JsonInclude.Include.NON_NULL)
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.NONE)
    }

    @JvmStatic
    fun <T> toJson(obj: T?): String {
        if (obj == null) return ""
        return try {
            mapper.writeValueAsString(obj)
        } catch (e: Exception) {
            logger.error("JSON error", e)
            ""
        }
    }

    @JvmStatic
    fun <T> toBytesJson(obj: T?): ByteArray {
        if (obj == null) return EMPTY_BYTES
        return try {
            mapper.writeValueAsBytes(obj)
        } catch (e: Exception) {
            logger.error("JSON error", e)
            EMPTY_BYTES
        }
    }

    @JvmStatic
    fun <T> fromJson(message: ByteArray, clazz: Class<T>): T? =
        try {
            mapper.readValue(message, clazz)
        } catch (e: Exception) {
            logger.error("JSON error", e)
            null
        }

    @JvmStatic
    fun <T> fromJson(message: String, clazz: Class<T>): T? =
        try {
            mapper.readValue(message, clazz)
        } catch (e: Exception) {
            logger.error("JSON error (see message below)", e)
            logger.error("{}", message)
            null
        }

    @JvmStatic
    fun <T> fromJson(bytes: ByteArray, typeReference: TypeReference<List<T>>): List<T> =
        try {
            mapper.readValue(bytes, typeReference)
        } catch (e: Exception) {
            logger.error("JSON error (see message below)", e)
            emptyList()
        }
}
