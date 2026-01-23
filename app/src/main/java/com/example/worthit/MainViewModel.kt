package com.example.worthit

import android.app.Application
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainViewModel(
    private val recordDao: RecordDao,
    private val settingsManager: SettingsManager,
    private val application: Application
) : ViewModel() {

    val allRecords: StateFlow<List<Record>> = recordDao.getAllRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isAccessibilityServiceEnabled = MutableStateFlow(false)
    val isAccessibilityServiceEnabled: StateFlow<Boolean> = _isAccessibilityServiceEnabled.asStateFlow()

    val monthlyGoal: StateFlow<Float> = settingsManager.monthlyGoal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1000f)

    val monthlySpent: StateFlow<Double> = allRecords.map { recordList ->
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        recordList.filter { record ->
            val recordCalendar = Calendar.getInstance().apply { timeInMillis = record.timestamp }
            record.isSpent && recordCalendar.get(Calendar.MONTH) == currentMonth
        }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val searchDateQuery = MutableStateFlow("")
    val searchAmountQuery = MutableStateFlow("")

    val filteredRecords: StateFlow<List<Record>> = combine(
        allRecords,
        searchDateQuery,
        searchAmountQuery
    ) { records, dateQuery, amountQuery ->
        records.filter { record ->
            val dateFormat = SimpleDateFormat("MM-dd", Locale.getDefault())
            val dateString = dateFormat.format(Date(record.timestamp))
            val dateMatches = dateQuery.isBlank() || dateString.contains(dateQuery)

            val amountMatches = amountQuery.isBlank() || record.amount.toString().contains(amountQuery)

            dateMatches && amountMatches
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clearAllRecords() {
        viewModelScope.launch {
            recordDao.deleteAll()
        }
    }

    init {
        checkAccessibilityServiceStatus()
    }

    fun checkAccessibilityServiceStatus() {
        val serviceId = "${application.packageName}/${MonitorService::class.java.name}"
        val enabledServices = Settings.Secure.getString(
            application.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        _isAccessibilityServiceEnabled.value = enabledServices?.contains(serviceId) == true
    }

    fun updateRecord(record: Record) {
        viewModelScope.launch {
            recordDao.update(record)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as WorthItApplication)
                val settingsManager = SettingsManager(app.applicationContext)
                MainViewModel(app.database.recordDao(), settingsManager, app)
            }
        }
    }
}
