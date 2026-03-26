/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.shourov.apps.pacedream.core.network.di

import com.google.gson.*
import timber.log.Timber
import java.lang.reflect.Type

sealed class RulesWrapper {
    data class RulesList(val rules: List<String>) : RulesWrapper()
    data class RulesObject(val rules: Rules) : RulesWrapper()
}

// Custom Gson TypeAdapter for RulesWrapper
class RulesWrapperAdapter : JsonDeserializer<RulesWrapper>, JsonSerializer<RulesWrapper> {

    override fun deserialize(json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext?): RulesWrapper {
        return if (json.isJsonArray) {
            // If it's a JSON array, treat it as a list of strings
            val rulesList = json.asJsonArray.map { it.asString }
            RulesWrapper.RulesList(rulesList)
        } else {
            // If it's a JSON object, treat it as a Rules object
            val rulesObject = context?.deserialize<Rules>(json, Rules::class.java)
            if (rulesObject == null) {
                Timber.w("RulesWrapper: deserialization returned null for Rules object")
                RulesWrapper.RulesList(emptyList())
            } else {
                RulesWrapper.RulesObject(rulesObject)
            }
        }
    }

    override fun serialize(src: RulesWrapper?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return when (src) {
            is RulesWrapper.RulesList -> {
                // Serialize the list of strings
                JsonArray().apply {
                    src.rules.forEach { add(it) }
                }
            }
            is RulesWrapper.RulesObject -> {
                // Serialize the Rules object
                context?.serialize(src.rules) ?: JsonNull.INSTANCE
            }
            else -> JsonNull.INSTANCE
        }
    }
}
