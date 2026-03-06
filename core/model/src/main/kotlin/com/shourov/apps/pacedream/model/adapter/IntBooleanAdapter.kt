package com.shourov.apps.pacedream.model.adapter

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonQualifier
import com.squareup.moshi.ToJson

@Retention(AnnotationRetention.RUNTIME)
@JsonQualifier
annotation class IntBoolean

class IntBooleanAdapter {
    @ToJson
    fun toJson(@IntBoolean value: Boolean): Int {
        return if (value) 1 else 0
    }

    @FromJson
    @IntBoolean
    fun fromJson(value: Int): Boolean {
        return value != 0
    }
}
