package com.tapread.nfc.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tapread.nfc.model.ScanResult
import com.tapread.nfc.util.CardStorage

class CardsViewModel(application: Application) : AndroidViewModel(application) {

    private val storage = CardStorage(application)

    private val _scans = MutableLiveData<List<ScanResult>>()
    val scans: LiveData<List<ScanResult>> = _scans

    private val _selectedScan = MutableLiveData<ScanResult?>()
    val selectedScan: LiveData<ScanResult?> = _selectedScan

    val maskPan: Boolean get() = storage.maskPan

    init {
        // Load persisted scans on startup
        _scans.value = storage.loadScans()
    }

    fun addScan(result: ScanResult) {
        val current = _scans.value.orEmpty().toMutableList()
        current.add(0, result)
        _scans.value = current
        storage.saveScans(current)
    }

    fun selectScan(result: ScanResult) {
        _selectedScan.value = result
    }

    fun clearScans() {
        _scans.value = emptyList()
        _selectedScan.value = null
        storage.clearScans()
    }

    fun setMaskPan(mask: Boolean) {
        storage.maskPan = mask
    }

    fun exportJson(): String {
        return storage.exportJson(_scans.value.orEmpty())
    }

    fun getStorage(): CardStorage = storage
}
