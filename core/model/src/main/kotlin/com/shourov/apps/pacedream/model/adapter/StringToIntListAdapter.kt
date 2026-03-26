package com.shourov.apps.pacedream.model.adapter

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonQualifier
import com.squareup.moshi.ToJson

@Retention(AnnotationRetention.RUNTIME)
@JsonQualifier
annotation class StringToIntList

/**
 * Converts comma separated integer string to list and vise-versa
 * e.g: "1,2,3,4" -> listOf(1,2,3,4)
 */
class StringToIntListAdapter {

    @ToJson
    fun toJson(@StringToIntList value: List<Int>): String {
        return value.joinToString(separator = ",")
    }

    @FromJson
    @StringToIntList
    fun fromJson(value: String): List<Int> {
        return value.split(",").mapNotNull { it.trim().toIntOrNull() }
    }
}
