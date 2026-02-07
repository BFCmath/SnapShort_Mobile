package com.example.snapshort_real.ui.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.snapshort_real.data.ScreenshotRepository
import com.example.snapshort_real.data.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val repository: ScreenshotRepository,
    private val taskRepository: com.example.snapshort_real.data.TaskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<GalleryUiState>(GalleryUiState.Loading)
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    init {
        observeImages()
    }

    private fun observeImages() {
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                repository.observeScreenshots(),
                taskRepository.getAllTasks()
            ) { images, tasks ->
                val taskImagePaths = tasks.map { it.imagePath }.toSet()
                images.filter { it.absolutePath !in taskImagePaths }
            }.collect { filteredImages ->
                _uiState.value = if (filteredImages.isEmpty()) {
                    GalleryUiState.Empty
                } else {
                    GalleryUiState.Success(filteredImages)
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
