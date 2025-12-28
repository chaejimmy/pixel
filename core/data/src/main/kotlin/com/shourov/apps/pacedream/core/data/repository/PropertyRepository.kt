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

package com.shourov.apps.pacedream.core.data.repository

import com.shourov.apps.pacedream.core.common.result.Result
import com.shourov.apps.pacedream.core.database.dao.PropertyDao
import com.shourov.apps.pacedream.core.database.entity.PropertyEntity
import com.shourov.apps.pacedream.core.database.entity.asExternalModel
import com.shourov.apps.pacedream.core.network.services.PaceDreamApiService
import com.shourov.apps.pacedream.model.response.home.rooms.Result as PropertyModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PropertyRepository @Inject constructor(
    private val apiService: PaceDreamApiService,
    private val propertyDao: PropertyDao
) {
    
    fun getAllProperties(): Flow<Result<List<PropertyModel>>> {
        return propertyDao.getAllProperties().map { entities ->
            Result.Success(entities.map { it.asExternalModel() })
        }
    }

    fun getPropertyById(propertyId: String): Flow<Result<PropertyModel?>> {
        return propertyDao.getPropertyById(propertyId).map { entity ->
            Result.Success(entity?.asExternalModel())
        }
    }

    fun getPropertiesByType(propertyType: String): Flow<Result<List<PropertyModel>>> {
        return propertyDao.getPropertiesByType(propertyType).map { entities ->
            Result.Success(entities.map { it.asExternalModel() })
        }
    }

    fun getAvailableProperties(): Flow<Result<List<PropertyModel>>> {
        return propertyDao.getAvailableProperties().map { entities ->
            Result.Success(entities.map { it.asExternalModel() })
        }
    }

    fun searchProperties(query: String): Flow<Result<List<PropertyModel>>> {
        return propertyDao.searchProperties(query).map { entities ->
            Result.Success(entities.map { it.asExternalModel() })
        }
    }

    fun getTopRatedProperties(limit: Int = 10): Flow<Result<List<PropertyModel>>> {
        return propertyDao.getTopRatedProperties(limit).map { entities ->
            Result.Success(entities.map { it.asExternalModel() })
        }
    }

    fun getRecentProperties(limit: Int = 10): Flow<Result<List<PropertyModel>>> {
        return propertyDao.getRecentProperties(limit).map { entities ->
            Result.Success(entities.map { it.asExternalModel() })
        }
    }

    suspend fun refreshProperties(): Result<Unit> {
        return try {
            val response = apiService.getAllProperties()
            // Handle the response and save to database
            // This would need to be implemented based on your API response structure
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun searchPropertiesRemote(
        query: String,
        propertyType: String? = null,
        minPrice: Double? = null,
        maxPrice: Double? = null,
        location: String? = null,
        page: Int = 1,
        limit: Int = 20
    ): Result<List<PropertyModel>> {
        return try {
            val response = apiService.searchProperties(
                query = query,
                propertyType = propertyType,
                minPrice = minPrice,
                maxPrice = maxPrice,
                location = location,
                page = page,
                limit = limit
            )
            // Handle response and convert to PropertyModel
            // This would need to be implemented based on your API response structure
            Result.Success(emptyList())
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getPropertyByIdRemote(propertyId: String): Result<PropertyModel?> {
        return try {
            val response = apiService.getPropertyById(propertyId)
            // Handle response and convert to PropertyModel
            // This would need to be implemented based on your API response structure
            Result.Success(null)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
