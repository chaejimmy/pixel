package com.shourov.apps.pacedream.model.adapter

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonQualifier
import com.squareup.moshi.ToJson

@Retention(AnnotationRetention.RUNTIME)
@JsonQualifier
annotation class StringDouble

class StringDoubleAdapter {
    @ToJson
    fun toJson(@StringDouble value: Double): String {
        return if (value==0.0) "" else value.toString()
    }

    @FromJson
    @StringDouble
    fun fromJson(value: String): Double {
        if (value.isEmpty())
            return 0.0
        return value.toDoubleOrNull() ?: 0.0
    }
}
