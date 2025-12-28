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

package com.shourov.apps.pacedream.core.database.dao

import androidx.room.*
import com.shourov.apps.pacedream.core.database.entity.PropertyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PropertyDao {
    @Query("SELECT * FROM properties WHERE id = :propertyId")
    fun getPropertyById(propertyId: String): Flow<PropertyEntity?>

    @Query("SELECT * FROM properties WHERE propertyType = :propertyType")
    fun getPropertiesByType(propertyType: String): Flow<List<PropertyEntity>>

    @Query("SELECT * FROM properties WHERE status = 1")
    fun getAvailableProperties(): Flow<List<PropertyEntity>>

    @Query("SELECT * FROM properties WHERE hostId = :hostId")
    fun getPropertiesByHost(hostId: String): Flow<List<PropertyEntity>>

    @Query("SELECT * FROM properties WHERE name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    fun searchProperties(query: String): Flow<List<PropertyEntity>>

    @Query("SELECT * FROM properties ORDER BY rating DESC LIMIT :limit")
    fun getTopRatedProperties(limit: Int = 10): Flow<List<PropertyEntity>>

    @Query("SELECT * FROM properties ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentProperties(limit: Int = 10): Flow<List<PropertyEntity>>

    @Query("SELECT * FROM properties")
    fun getAllProperties(): Flow<List<PropertyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProperty(property: PropertyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProperties(properties: List<PropertyEntity>)

    @Update
    suspend fun updateProperty(property: PropertyEntity)

    @Delete
    suspend fun deleteProperty(property: PropertyEntity)

    @Query("DELETE FROM properties WHERE id = :propertyId")
    suspend fun deletePropertyById(propertyId: String)

    @Query("DELETE FROM properties")
    suspend fun deleteAllProperties()
}
