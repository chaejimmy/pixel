package com.shourov.apps.pacedream.model.adapter

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonQualifier
import com.squareup.moshi.ToJson

@Retention(AnnotationRetention.RUNTIME)
@JsonQualifier
annotation class StringToStringList

/**
 * Converts comma separated integer string to list and vise-versa
 * e.g: "A,B,C,D" -> listOf("A","B","C","D")
 */
class StringToStringListAdapter {

    @ToJson
    fun toJson(@StringToStringList value: List<String>): String {
        return value.joinToString(separator = ",")
    }

    @FromJson
    @StringToStringList
    fun fromJson(value: String): List<String> {
        return value.split(",").mapNotNull { s -> s.trim().takeIf { it.isNotBlank() } }
    }
}
