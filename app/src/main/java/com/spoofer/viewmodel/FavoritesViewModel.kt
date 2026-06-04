package com.spoofer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.spoofer.data.db.SavedLocationEntity
import com.spoofer.data.repository.FavoritesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoritesRepo: FavoritesRepository,
) : ViewModel() {
    val favorites: StateFlow<List<SavedLocationEntity>> = favoritesRepo.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun save(name: String, lat: Double, lng: Double) {
        viewModelScope.launch {
            favoritesRepo.save(name, lat, lng)
        }
    }

    fun delete(location: SavedLocationEntity) {
        viewModelScope.launch {
            favoritesRepo.delete(location)
        }
    }

    fun select(location: SavedLocationEntity): LatLng =
        LatLng(location.latitude, location.longitude)
}
