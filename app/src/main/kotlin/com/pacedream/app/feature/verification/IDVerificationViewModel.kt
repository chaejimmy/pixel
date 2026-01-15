package com.pacedream.app.feature.verification

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.core.network.model.verification.UploadFileRequest
import com.shourov.apps.pacedream.core.network.repository.VerificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.InputStream
import javax.inject.Inject

@HiltViewModel
class IDVerificationViewModel @Inject constructor(
    private val repository: VerificationRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(IDVerificationUiState())
    val uiState: StateFlow<IDVerificationUiState> = _uiState.asStateFlow()
    
    fun updateIdType(type: String) {
        _uiState.value = _uiState.value.copy(idType = type)
    }
    
    fun uploadImage(uri: Uri, isFront: Boolean) {
        viewModelScope.launch {
            if (isFront) {
                _uiState.value = _uiState.value.copy(
                    isUploadingFront = true,
                    frontImageUri = uri,
                    error = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isUploadingBack = true,
                    backImageUri = uri,
                    error = null
                )
            }
            
            try {
                // Step 1: Get pre-signed upload URL (same as web)
                val uploadRequest = UploadFileRequest(
                    side = if (isFront) "front" else "back",
                    contentType = "image/jpeg"
                )
                
                val uploadURLsResult = repository.getUploadURLs(listOf(uploadRequest))
                
                uploadURLsResult.fold(
                    onSuccess = { response ->
                        response.data?.uploads?.firstOrNull()?.let { uploadInfo ->
                            // Step 2: Read image data from URI
                            val imageData = readImageData(uri)
                            
                            // Step 3: Upload directly to S3 (same as web)
                            val s3UploadResult = repository.uploadToS3(
                                uploadUrl = uploadInfo.uploadUrl,
                                imageData = imageData,
                                contentType = "image/jpeg"
                            )
                            
                            s3UploadResult.fold(
                                onSuccess = {
                                    // Step 4: Store storage key
                                    if (isFront) {
                                        _uiState.value = _uiState.value.copy(
                                            isUploadingFront = false,
                                            frontStorageKey = uploadInfo.storageKey
                                        )
                                    } else {
                                        _uiState.value = _uiState.value.copy(
                                            isUploadingBack = false,
                                            backStorageKey = uploadInfo.storageKey
                                        )
                                    }
                                },
                                onFailure = { error ->
                                    _uiState.value = _uiState.value.copy(
                                        isUploadingFront = false,
                                        isUploadingBack = false,
                                        error = error.message ?: "Failed to upload image"
                                    )
                                }
                            )
                        } ?: run {
                            _uiState.value = _uiState.value.copy(
                                isUploadingFront = false,
                                isUploadingBack = false,
                                error = "Failed to get upload URL"
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isUploadingFront = false,
                            isUploadingBack = false,
                            error = error.message ?: "Failed to get upload URL"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isUploadingFront = false,
                    isUploadingBack = false,
                    error = e.message ?: "Upload failed"
                )
            }
        }
    }
    
    fun submitVerification() {
        val frontKey = _uiState.value.frontStorageKey
        val backKey = _uiState.value.backStorageKey
        val idType = _uiState.value.idType
        
        if (frontKey == null || backKey == null) {
            _uiState.value = _uiState.value.copy(
                error = "Please upload both front and back images"
            )
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, error = null)
            
            repository.submitVerification(idType, frontKey, backKey).fold(
                onSuccess = { response ->
                    if (response.success) {
                        _uiState.value = _uiState.value.copy(
                            isSubmitting = false,
                            isSubmitted = true,
                            isPending = true
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isSubmitting = false,
                            error = response.message ?: "Failed to submit verification"
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        error = error.message ?: "Network error"
                    )
                }
            )
        }
    }
    
    private suspend fun readImageData(uri: Uri): ByteArray {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.readBytes()
            } ?: byteArrayOf()
        } catch (e: Exception) {
            byteArrayOf()
        }
    }
}

data class IDVerificationUiState(
    val idType: String = "DRIVER_LICENSE",
    val frontImageUri: Uri? = null,
    val backImageUri: Uri? = null,
    val frontStorageKey: String? = null,
    val backStorageKey: String? = null,
    val isUploadingFront: Boolean = false,
    val isUploadingBack: Boolean = false,
    val isSubmitting: Boolean = false,
    val isSubmitted: Boolean = false,
    val isPending: Boolean = false,
    val error: String? = null
) {
    val canSubmit: Boolean
        get() = frontStorageKey != null && backStorageKey != null && !isPending
}
