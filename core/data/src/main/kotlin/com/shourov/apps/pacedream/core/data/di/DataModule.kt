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

package com.shourov.apps.pacedream.core.data.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Data module for Hilt dependency injection.
 * 
 * Note: Repositories (BookingRepository, PropertyRepository, MessageRepository, 
 * PaceDreamRepository) are injectable via their @Inject constructors and don't
 * need explicit @Provides methods here.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    // Repositories are auto-provided via @Inject constructors
}
