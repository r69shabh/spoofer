package com.spoofer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spoofer.data.db.SpoofHistoryEntity
import com.spoofer.data.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel
    @Inject
    constructor(
        private val historyRepo: HistoryRepository,
    ) : ViewModel() {
        private val selectedFilter = MutableStateFlow<String?>(null)

        @OptIn(ExperimentalCoroutinesApi::class)
        val historyEntries: StateFlow<List<SpoofHistoryEntity>> =
            selectedFilter
                .flatMapLatest { filter ->
                    if (filter == null) {
                        historyRepo.getAll()
                    } else {
                        historyRepo.getByMode(filter)
                    }
                }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        val currentFilter: StateFlow<String?> = selectedFilter.asStateFlow()

        fun setFilter(mode: String?) {
            selectedFilter.value = mode
        }

        fun clearAll() {
            viewModelScope.launch {
                historyRepo.clearAll()
            }
        }
    }
