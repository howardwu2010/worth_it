package com.example.worthit

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.worthit.ui.theme.WorthItTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: MainViewModel

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            WorthItTheme {
                viewModel = viewModel(factory = MainViewModel.Factory)
                RequestNotificationPermission()
                Surface(modifier = Modifier.fillMaxSize()) {
                    DashboardScreen(viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::viewModel.isInitialized) {
            viewModel.checkAccessibilityServiceStatus()
        }
    }

    @Composable
    private fun RequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val context = LocalContext.current
            LaunchedEffect(Unit) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val records by viewModel.filteredRecords.collectAsState()
    val isServiceEnabled by viewModel.isAccessibilityServiceEnabled.collectAsState()
    val monthlyGoal by viewModel.monthlyGoal.collectAsState()
    val monthlySpent by viewModel.monthlySpent.collectAsState()
    val searchDateQuery by viewModel.searchDateQuery.collectAsState()
    val searchAmountQuery by viewModel.searchAmountQuery.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Worth It • 值乎", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                actions = {
                    IconButton(onClick = { context.startActivity(Intent(context, SettingsActivity::class.java)) }) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            AccessibilityStatusCard(isServiceEnabled)
            Spacer(modifier = Modifier.height(16.dp))
            StatsSection(records, monthlySpent, monthlyGoal)
            Spacer(modifier = Modifier.height(16.dp))
            SearchSection(searchDateQuery, searchAmountQuery, viewModel)
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "近期拦截记录",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(records, key = { it.id }) { record ->
                    RecordItemCard(
                        record = record,
                        onUpdate = { updatedRecord -> viewModel.updateRecord(updatedRecord) }
                    )
                }
            }
        }
    }
}

@Composable
fun SearchSection(dateQuery: String, amountQuery: String, viewModel: MainViewModel) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = dateQuery,
            onValueChange = { viewModel.searchDateQuery.value = it },
            label = { Text("日期 (MM-dd)") },
            modifier = Modifier.weight(1f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        OutlinedTextField(
            value = amountQuery,
            onValueChange = { viewModel.searchAmountQuery.value = it },
            label = { Text("金额") },
            modifier = Modifier.weight(1f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
    }
}


@Composable
fun AccessibilityStatusCard(isEnabled: Boolean) {
    val context = LocalContext.current
    val containerColor = if (isEnabled) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Settings, "服务状态", tint = contentColor)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Accessibility Service API", fontWeight = FontWeight.Bold, color = contentColor)
                    Text(if (isEnabled) "运行中" else "未开启", style = MaterialTheme.typography.bodyMedium, color = contentColor)
                }
            }
            if (!isEnabled) {
                Button(
                    onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = contentColor
                    )
                ) {
                    Text("去开启")
                }
            }
        }
    }
}

@Composable
fun StatsSection(records: List<Record>, monthlySpent: Double, monthlyGoal: Float) {
    val jumpCount = records.size
    val totalImpulse = records.sumOf { it.amount }
    val savedAmount = records.filter { !it.isSpent }.sumOf { it.amount }
    val regretAmount = records.filter { it.isSpent && it.isRegret }.sumOf { it.amount }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatItem("拦截次数", "$jumpCount 次", MaterialTheme.colorScheme.onSurfaceVariant)
                StatItem("已拦截金额", "${String.format("%.2f", totalImpulse)} 元", MaterialTheme.colorScheme.onSurfaceVariant)
                StatItem("节省金额", "${String.format("%.2f", savedAmount)} 元", MaterialTheme.colorScheme.primary)
                StatItem("后悔金额", "${String.format("%.2f", regretAmount)} 元", MaterialTheme.colorScheme.error)
            }
            Spacer(modifier = Modifier.height(12.dp))
            StatItem("本月消费", "${String.format("%.2f", monthlySpent)} / ${String.format("%.2f", monthlyGoal)} 元", MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun RecordItemCard(record: Record, onUpdate: (Record) -> Unit) {
    val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    val timeStr = dateFormat.format(Date(record.timestamp))
    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.primary,
        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("${String.format("%.2f", record.amount)} 元", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.History, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(timeStr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilterChip(
                    selected = record.isSpent,
                    onClick = {
                        val newIsSpent = !record.isSpent
                        val newIsRegret = if (!newIsSpent) false else record.isRegret
                        onUpdate(record.copy(isSpent = newIsSpent, isRegret = newIsRegret))
                    },
                    label = { Text("已消费") },
                    colors = chipColors
                )
                AnimatedVisibility(
                    visible = record.isSpent,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = record.isRegret,
                            onClick = { onUpdate(record.copy(isRegret = !record.isRegret)) },
                            label = { Text("后悔") },
                            colors = chipColors
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardPreview() {
    WorthItTheme {}
}
