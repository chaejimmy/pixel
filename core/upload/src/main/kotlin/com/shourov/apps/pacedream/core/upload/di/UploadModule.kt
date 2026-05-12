package com.shourov.apps.pacedream.core.upload.di

import com.shourov.apps.pacedream.core.upload.ImageUploader
import com.shourov.apps.pacedream.core.upload.PresignedUrlImageUploader
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class UploadModule {

    @Binds
    @Singleton
    abstract fun bindImageUploader(impl: PresignedUrlImageUploader): ImageUploader
}
