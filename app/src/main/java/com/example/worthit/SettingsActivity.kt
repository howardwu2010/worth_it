package com.example.worthit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.worthit.ui.theme.WorthItTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WorthItTheme {
                val mainViewModel: MainViewModel = viewModel(factory = MainViewModel.Factory)
                val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory(SettingsManager(this)))
                SettingsScreen(mainViewModel = mainViewModel, viewModel = settingsViewModel, onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(mainViewModel: MainViewModel, viewModel: SettingsViewModel, onBack: () -> Unit) {
    val monthlyGoal by viewModel.monthlyGoal.collectAsState()
    val refItemName by viewModel.refItemName.collectAsState()
    val refItemPrice by viewModel.refItemPrice.collectAsState()
    val navToHome by viewModel.navToHome.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("确认操作") },
            text = { Text("您确定要清除所有记录吗？此操作无法撤销。") },
            confirmButton = {
                Button(
                    onClick = {
                        mainViewModel.clearAllRecords()
                        showDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("全部清除")
                }
            },
            dismissButton = {
                Button(onClick = { showDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = monthlyGoal,
                onValueChange = { viewModel.updateMonthlyGoal(it) },
                label = { Text("目标每月消费金额 (元)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = refItemName,
                onValueChange = { if (it.length <= 10) viewModel.updateRefItemName(it) },
                label = { Text("参考物品名称") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = refItemPrice,
                onValueChange = { viewModel.updateRefItemPrice(it) },
                label = { Text("参考物品价格 (元)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text("倒计时结束后...", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = !navToHome,
                    onClick = { viewModel.updateNavToHome(false) }
                )
                Text("返回到之前的应用")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = navToHome,
                    onClick = { viewModel.updateNavToHome(true) }
                )
                Text("返回到 App 主页")
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { showDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("清除所有记录")
            }
        }
    }
}

class SettingsViewModel(private val settingsManager: SettingsManager) : ViewModel() {

    private val _monthlyGoal = MutableStateFlow("")
    val monthlyGoal: StateFlow<String> = _monthlyGoal.asStateFlow()

    private val _refItemName = MutableStateFlow("")
    val refItemName: StateFlow<String> = _refItemName.asStateFlow()

    private val _refItemPrice = MutableStateFlow("")
    val refItemPrice: StateFlow<String> = _refItemPrice.asStateFlow()

    private val _navToHome = MutableStateFlow(false)
    val navToHome: StateFlow<Boolean> = _navToHome.asStateFlow()

    init {
        viewModelScope.launch {
            _monthlyGoal.value = String.format("%.2f", settingsManager.monthlyGoal.first())
            _refItemName.value = settingsManager.refItemName.first()
            _refItemPrice.value = String.format("%.2f", settingsManager.refItemPrice.first())
            _navToHome.value = settingsManager.navToHome.first()
        }
    }

    fun updateMonthlyGoal(value: String) {
        _monthlyGoal.value = value
        saveSettings()
    }

    fun updateRefItemName(value: String) {
        _refItemName.value = value
        saveSettings()
    }

    fun updateRefItemPrice(value: String) {
        _refItemPrice.value = value
        saveSettings()
    }

    fun updateNavToHome(value: Boolean) {
        _navToHome.value = value
        saveSettings()
    }

    private fun saveSettings() {
        viewModelScope.launch {
            settingsManager.saveSettings(
                monthlyGoal = _monthlyGoal.value.toFloatOrNull() ?: 0f,
                refItemName = _refItemName.value,
                refItemPrice = _refItemPrice.value.toFloatOrNull() ?: 0f,
                navToHome = _navToHome.value
            )
        }
    }

    companion object {
        fun Factory(settingsManager: SettingsManager): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                        @Suppress("UNCHECKED_CAST")
                        return SettingsViewModel(settingsManager) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }
    }
}