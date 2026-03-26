package com.shourov.apps.pacedream.model.adapter

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonQualifier
import com.squareup.moshi.ToJson

@Retention(AnnotationRetention.RUNTIME)
@JsonQualifier
annotation class ZeroOneBoolean

class ZeroOneBooleanAdapter {
    @ToJson
    fun toJson(@ZeroOneBoolean value: Boolean): String {
        return if (value) "1" else "0"
    }

    @FromJson
    @ZeroOneBoolean
    fun fromJson(value: String): Boolean {
        return value == "1"
    }
}
