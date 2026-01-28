package ca.etrak.wifiautoconnect.ui

import androidx.lifecycle.*
import ca.etrak.wifiautoconnect.data.ConnectionLog
import ca.etrak.wifiautoconnect.data.WiFiRepository
import kotlinx.coroutines.launch

class LogViewModel(private val repository: WiFiRepository) : ViewModel() {

    private val _filter = MutableLiveData<String?>(null)

    val allLogs: LiveData<List<ConnectionLog>> = _filter.switchMap { filter ->
        when (filter) {
            "CONNECTION" -> repository.allLogs.map { logs ->
                logs.filter {
                    it.eventType == ConnectionLog.EVENT_CONNECT_SUCCESS ||
                            it.eventType == ConnectionLog.EVENT_CONNECT_FAILED ||
                            it.eventType == ConnectionLog.EVENT_CONNECT_ATTEMPT
                }
            }
            "SCAN" -> repository.allLogs.map { logs ->
                logs.filter { it.eventType == ConnectionLog.EVENT_SCAN }
            }
            else -> repository.allLogs
        }
    }

    fun setFilter(filter: String?) {
        _filter.value = filter
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            repository.deleteAllData()
        }
    }
}

class LogViewModelFactory(private val repository: WiFiRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LogViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LogViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
