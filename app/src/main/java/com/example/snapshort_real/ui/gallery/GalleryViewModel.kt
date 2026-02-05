package com.example.snapshort_real.ui.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.snapshort_real.data.ScreenshotRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val repository: ScreenshotRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<GalleryUiState>(GalleryUiState.Loading)
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    init {
        observeImages()
    }

    private fun observeImages() {
        viewModelScope.launch {
            repository.observeScreenshots().collect { images ->
                _uiState.value = if (images.isEmpty()) {
                    GalleryUiState.Empty
                } else {
                    GalleryUiState.Success(images)
                }
            }
        }
    }

    fun deleteImage(file: File) {
        viewModelScope.launch {
            repository.deleteScreenshot(file)
        }
    }
}

sealed interface GalleryUiState {
    data object Loading : GalleryUiState
    data class Success(val images: List<File>) : GalleryUiState
    data object Empty : GalleryUiState
}
