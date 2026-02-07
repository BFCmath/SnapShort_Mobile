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

    // Selection Mode State
    private val _selectedImages = MutableStateFlow<Set<File>>(emptySet())
    val selectedImages: StateFlow<Set<File>> = _selectedImages.asStateFlow()

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
                
                // Cleanup selection if images are removed externally or by this app
                _selectedImages.value = _selectedImages.value.filter { it in filteredImages }.toSet()
            }
        }
    }

    fun toggleSelection(file: File) {
        val current = _selectedImages.value
        if (current.contains(file)) {
            _selectedImages.value = current - file
        } else {
            _selectedImages.value = current + file
        }
    }

    fun clearSelection() {
        _selectedImages.value = emptySet()
    }

    fun deleteSelected() {
        viewModelScope.launch {
            val toDelete = _selectedImages.value
            toDelete.forEach { file ->
                repository.deleteScreenshot(file)
            }
            clearSelection()
        }
    }
}

sealed interface GalleryUiState {
    data object Loading : GalleryUiState
    data class Success(val images: List<File>) : GalleryUiState
    data object Empty : GalleryUiState
}
