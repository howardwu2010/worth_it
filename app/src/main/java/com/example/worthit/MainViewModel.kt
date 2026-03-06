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
import java.util.*

data class DailyStats(
    val date: String, // MM-dd
    val savedAmount: Double,
    val spentAmount: Double,
    val regretAmount: Double
)

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

    val userXP: StateFlow<Int> = settingsManager.userXP
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val lastCheckIn: StateFlow<Long> = settingsManager.lastCheckIn
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    private val _hasCheckedInThisSession = MutableStateFlow(false)
    val hasCheckedInThisSession: StateFlow<Boolean> = _hasCheckedInThisSession.asStateFlow()

    val interceptGoalLevel: StateFlow<Int> = settingsManager.interceptGoalLevel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val savingsGoalLevel: StateFlow<Int> = settingsManager.savingsGoalLevel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val userLevel: StateFlow<String> = userXP.map { xp ->
        when {
            xp >= 45 -> "理财达人"
            xp >= 20 -> "克制之星"
            else -> "理性萌新"
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "冲动学徒")

    val xpToNextLevel: StateFlow<Int> = userXP.map { xp ->
        when {
            xp >= 45 -> 0
            xp >= 20 -> 45 - xp
            else -> 20 - xp
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 500)

    val monthlySpent: StateFlow<Double> = allRecords.map { recordList ->
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        recordList.filter { record ->
            val recordCalendar = Calendar.getInstance().apply { timeInMillis = record.timestamp }
            record.isSpent && recordCalendar.get(Calendar.MONTH) == currentMonth
        }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val dailyStats: StateFlow<List<DailyStats>> = allRecords.map { records ->
        val sdf = SimpleDateFormat("MM-dd", Locale.getDefault())
        val last7DaysMap = LinkedHashMap<String, DailyStats>()
        
        // 生成过去 7 天的空数据 (顺序从旧到新)
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -6)
        repeat(7) {
            val dateStr = sdf.format(cal.time)
            last7DaysMap[dateStr] = DailyStats(dateStr, 0.0, 0.0, 0.0)
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        records.forEach { record ->
            val dateStr = sdf.format(Date(record.timestamp))
            if (last7DaysMap.containsKey(dateStr)) {
                val current = last7DaysMap[dateStr]!!
                last7DaysMap[dateStr] = current.copy(
                    savedAmount = current.savedAmount + if (!record.isSpent) record.amount else 0.0,
                    spentAmount = current.spentAmount + if (record.isSpent) record.amount else 0.0,
                    regretAmount = current.regretAmount + if (record.isSpent && record.isRegret) record.amount else 0.0
                )
            }
        }
        last7DaysMap.values.toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
            settingsManager.resetXP()
        }
    }

    fun performCheckIn() {
        _hasCheckedInThisSession.value = true
        viewModelScope.launch {
            settingsManager.updateCheckIn(System.currentTimeMillis())
            settingsManager.addXP(5)
        }
    }

    fun addXP(amount: Int) {
        viewModelScope.launch {
            settingsManager.addXP(amount)
        }
    }

    fun claimInterceptGoal() {
        viewModelScope.launch {
            settingsManager.incrementInterceptGoal()
            settingsManager.addXP(20)
        }
    }

    fun claimSavingsGoal() {
        viewModelScope.launch {
            settingsManager.incrementSavingsGoal()
            settingsManager.addXP(20)
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
