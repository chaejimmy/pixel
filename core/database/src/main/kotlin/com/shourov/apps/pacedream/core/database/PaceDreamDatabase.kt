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

package com.shourov.apps.pacedream.core.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.shourov.apps.pacedream.core.database.converter.Converters
import com.shourov.apps.pacedream.core.database.dao.BookingDao
import com.shourov.apps.pacedream.core.database.dao.CategoryDao
import com.shourov.apps.pacedream.core.database.dao.ChatDao
import com.shourov.apps.pacedream.core.database.dao.MessageDao
import com.shourov.apps.pacedream.core.database.dao.PropertyDao
import com.shourov.apps.pacedream.core.database.dao.UserDao
import com.shourov.apps.pacedream.core.database.entity.BookingEntity
import com.shourov.apps.pacedream.core.database.entity.CategoryEntity
import com.shourov.apps.pacedream.core.database.entity.ChatEntity
import com.shourov.apps.pacedream.core.database.entity.MessageEntity
import com.shourov.apps.pacedream.core.database.entity.PropertyEntity
import com.shourov.apps.pacedream.core.database.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        PropertyEntity::class,
        BookingEntity::class,
        MessageEntity::class,
        CategoryEntity::class,
        ChatEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PaceDreamDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun propertyDao(): PropertyDao
    abstract fun bookingDao(): BookingDao
    abstract fun messageDao(): MessageDao
    abstract fun categoryDao(): CategoryDao
    abstract fun chatDao(): ChatDao

    companion object {
        const val DATABASE_NAME = "pacedream_database"

        fun create(context: Context): PaceDreamDatabase {
            return Room.databaseBuilder(
                context,
                PaceDreamDatabase::class.java,
                DATABASE_NAME
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
