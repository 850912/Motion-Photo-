package com.motionphoto.converter.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.motionphoto.converter.core.MotionPhotoBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val motionPhotoBuilder = MotionPhotoBuilder(application)
    
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    sealed class UiState {
        object Idle : UiState()
        data class VideoSelected(val uri: Uri, val thumbnailPath: String? = null) : UiState()
        data class Converting(
            val progress: Float,
            val stage: MotionPhotoBuilder.ConversionProgress.Stage
        ) : UiState()
        data class Success(val outputFile: File) : UiState()
        data class Error(val message: String) : UiState()
    }
    
    fun selectVideo(uri: Uri) {
        _uiState.value = UiState.VideoSelected(uri)
    }
    
    fun startConversion(
        videoUri: Uri, 
        frameTimeUs: Long = 0L,
        format: MotionPhotoBuilder.OutputFormat = MotionPhotoBuilder.OutputFormat.UNIVERSAL
    ) {
        viewModelScope.launch {
            try {
                val outputDir = File(getApplication<Application>().getExternalFilesDir(null), "MotionPhotos")
                if (!outputDir.exists()) {
                    outputDir.mkdirs()
                }
                
                val outputFile = File(outputDir, "motion_photo_${System.currentTimeMillis()}.jpg")
                
                motionPhotoBuilder.convertVideoToMotionPhoto(
                    videoUri = videoUri,
                    outputFile = outputFile,
                    frameTimeUs = frameTimeUs,
                    format = format,
                    onProgress = { progress ->
                        _uiState.value = UiState.Converting(
                            progress = progress.progress,
                            stage = progress.stage
                        )
                    }
                ).fold(
                    onSuccess = { file ->
                        _uiState.value = UiState.Success(file)
                    },
                    onFailure = { error ->
                        _uiState.value = UiState.Error(
                            error.message ?: "Unknown error occurred"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = UiState.Error(
                    e.message ?: "Conversion failed"
                )
            }
        }
    }
    
    fun reset() {
        _uiState.value = UiState.Idle
    }
}
