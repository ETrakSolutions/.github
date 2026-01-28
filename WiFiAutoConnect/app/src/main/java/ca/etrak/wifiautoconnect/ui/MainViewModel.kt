package ca.etrak.wifiautoconnect.ui

import androidx.lifecycle.*
import ca.etrak.wifiautoconnect.data.WiFiNetwork
import ca.etrak.wifiautoconnect.data.WiFiRepository
import ca.etrak.wifiautoconnect.util.WiFiHelper
import kotlinx.coroutines.launch

class MainViewModel(private val repository: WiFiRepository) : ViewModel() {

    val allNetworks: LiveData<List<WiFiNetwork>> = repository.allNetworks
    val openNetworks: LiveData<List<WiFiNetwork>> = repository.openNetworks

    private val _connectionStatus = MutableLiveData<String>()
    val connectionStatus: LiveData<String> = _connectionStatus

    private val _isScanning = MutableLiveData(false)
    val isScanning: LiveData<Boolean> = _isScanning

    fun connectToNetwork(ssid: String, bssid: String, wifiHelper: WiFiHelper) {
        viewModelScope.launch {
            _connectionStatus.value = "Connexion à $ssid..."
            val success = wifiHelper.connectToOpenNetwork(ssid, bssid)
            _connectionStatus.value = if (success) {
                "Connecté à $ssid"
            } else {
                "Échec de connexion à $ssid"
            }
        }
    }

    suspend fun getStats(): NetworkStats {
        return NetworkStats(
            totalNetworks = repository.getNetworkCount(),
            openNetworks = repository.getOpenNetworkCount(),
            successfulConnections = repository.getSuccessfulConnectionCount(),
            totalLogs = repository.getLogCount()
        )
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.deleteAllData()
        }
    }

    data class NetworkStats(
        val totalNetworks: Int,
        val openNetworks: Int,
        val successfulConnections: Int,
        val totalLogs: Int
    )
}

class MainViewModelFactory(private val repository: WiFiRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
