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
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.worthit.ui.theme.WorthItTheme
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: MainViewModel

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

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

private val TRAP_QUOTES = listOf(
    "“首充双倍”不是福利，而是为了诱导你打破从不氪金的心理防线。",
    "所谓的“保底机制”，是策划通过精准计算，为你量身定制的消费终点。",
    "“限时UP池”利用了你的失去焦虑，逼你在理智回归前匆忙下单。",
    "战令锁定的不仅是奖励，更是利用沉没成本强行占用你的业余时间。",
    "游戏内复杂的虚拟货币转化率，是为了模糊你对现实金钱支出的感知。",
    "“每日签到”和“每日任务”，本质上是把游戏变成工作，用习惯性行为锁定你。",
    "抽卡前的“免费单抽”，是为了诱发赌徒心理，让你产生“下一把就出”的幻觉。",
    "装备强化中的“防止破碎”道具，是利用损失厌恶来收割你的避险心理。",
    "“皮肤特效”带来的新鲜感，往往会在下一次版本更新或新皮肤推出时归零。",
    "排行榜上的虚荣心，是策划利用社交压力和攀比欲为你挖掘的无底洞。",
    "“联动限定”的本质，是跨圈层收割你对特定IP的情怀，而非提升游戏性。",
    "礼包上标注的“超值600%”，是利用锚定效应让你产生“不买就亏”的错觉。",
    "“首充礼包”赠送的强力武器，本质上是让你提前透支本该属于探索的乐趣。",
    "游戏仓库或背包的扩容费用，是商人在利用你对虚拟资产的“囤积癖”获利。",
    "所谓的“回归礼包”，是为了利用补偿心理，重新开启你已经断掉的消费惯性。",
    "Steam大促时的“喜加一”，你买到的往往是一份永不打开的“占有欲”。",
    "预购游戏的奖励通常只是噱头，本质是诱导你为未经检验的半成品买单。",
    "角色试用关卡利用了“禀赋效应”，让你产生这名角色已经属于你的心理错觉。",
    "全收集成就的诱惑，是针对强迫症和完美主义者精准投放的消费诱饵。",
    "体力购买机制，是商人在对你的耐心和多巴胺渴求进行明码标价。"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val records by viewModel.filteredRecords.collectAsState()
    val allRecords by viewModel.allRecords.collectAsState()
    val dailyStats by viewModel.dailyStats.collectAsState()
    val isServiceEnabled by viewModel.isAccessibilityServiceEnabled.collectAsState()
    val monthlyGoal by viewModel.monthlyGoal.collectAsState()
    val monthlySpent by viewModel.monthlySpent.collectAsState()
    val userXP by viewModel.userXP.collectAsState()
    val userLevel by viewModel.userLevel.collectAsState()
    val xpToNext by viewModel.xpToNextLevel.collectAsState()
    val hasCheckedIn by viewModel.hasCheckedInThisSession.collectAsState()
    val interceptLevel by viewModel.interceptGoalLevel.collectAsState()
    val savingsLevel by viewModel.savingsGoalLevel.collectAsState()
    
    val searchDateQuery by viewModel.searchDateQuery.collectAsState()
    val searchAmountQuery by viewModel.searchAmountQuery.collectAsState()

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(top = 32.dp) // 给状态栏留空间
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(userLevel, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
                            Text(
                                text = if (xpToNext > 0) "距下一级还差 ${xpToNext} 经验值" else "MAX LVL",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        val progress = if (xpToNext > 0) userXP.toFloat() / (userXP + xpToNext) else 1f
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            gapSize = 0.dp,
                            drawStopIndicator = {}
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    IconButton(onClick = { context.startActivity(Intent(context, SettingsActivity::class.java)) }) {
                        Icon(Icons.Default.Settings, contentDescription = "设置", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // 只有在未开启服务时才显示卡片
            if (!isServiceEnabled) {
                item { Box(Modifier.padding(horizontal = 16.dp)) { AccessibilityStatusCard(false) } }
            }
            
            item { Box(Modifier.padding(horizontal = 16.dp)) { CheckInModule(hasCheckedIn) { viewModel.performCheckIn() } } }
            item { GoalChallengeSection(allRecords, interceptLevel, savingsLevel, viewModel) }
            item { Box(Modifier.padding(horizontal = 16.dp)) { StatsSection(dailyStats, monthlySpent, monthlyGoal) } }
            item { Box(Modifier.padding(horizontal = 16.dp)) { SearchSection(searchDateQuery, searchAmountQuery, viewModel) } }
            item {
                Text(
                    text = "近期拦截记录",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            items(records, key = { it.id }) { record ->
                Box(Modifier.padding(horizontal = 16.dp)) {
                    RecordItemCard(
                        record = record,
                        onUpdate = { viewModel.updateRecord(it) }
                    )
                }
            }
        }
    }
}

@Composable
fun CheckInModule(hasCheckedIn: Boolean, onCheckIn: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (!hasCheckedIn) {
                Text("开始新的一天 • 签到领经验", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val days = remember {
                        val list = mutableListOf<Int>()
                        val cal = Calendar.getInstance()
                        cal.add(Calendar.DAY_OF_YEAR, -3)
                        repeat(7) {
                            list.add(cal.get(Calendar.DAY_OF_MONTH))
                            cal.add(Calendar.DAY_OF_YEAR, 1)
                        }
                        list
                    }
                    val today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
                    
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        days.forEach { day ->
                            val isCurrent = day == today
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (isCurrent) MaterialTheme.colorScheme.primary else Color.Transparent),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    day.toString(),
                                    fontSize = 12.sp,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = onCheckIn,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("签到")
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = remember { TRAP_QUOTES.random() },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun GoalChallengeSection(records: List<Record>, interceptLevel: Int, savingsLevel: Int, viewModel: MainViewModel) {
    val interceptCount = records.size
    val totalSaved = records.filter { !it.isSpent }.sumOf { it.amount }
    
    val interceptGoal = (interceptLevel + 1) * 3
    val savingsGoal = (savingsLevel + 1) * 2.0

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            GoalCard(
                title = "拦截达人 Lvl.${interceptLevel + 1}",
                desc = "累计拦截 $interceptCount/$interceptGoal 次\n完成后 +20 经验值",
                progress = if (interceptGoal > 0) interceptCount.toFloat() / interceptGoal else 1f,
                isClaimable = interceptCount >= interceptGoal,
                onClaim = { viewModel.claimInterceptGoal() }
            )
        }
        item {
            GoalCard(
                title = "省钱能手 Lvl.${savingsLevel + 1}",
                desc = "累计节省 ${String.format("%.0f", totalSaved)}/${String.format("%.0f", savingsGoal)} 元\n完成后 +20 经验值",
                progress = if (savingsGoal > 0) (totalSaved / savingsGoal).toFloat() else 1f,
                isClaimable = totalSaved >= savingsGoal,
                onClaim = { viewModel.claimSavingsGoal() }
            )
        }
    }
}

@Composable
fun GoalCard(title: String, desc: String, progress: Float, isClaimable: Boolean, onClaim: () -> Unit) {
    Card(
        modifier = Modifier.width(182.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isClaimable) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress.coerceAtMost(1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                gapSize = 0.dp,
                drawStopIndicator = {}
            )
            if (isClaimable) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "点击领取 +20XP",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { onClaim() }.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun StatsSection(dailyStats: List<DailyStats>, monthlySpent: Double, monthlyGoal: Float) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("近 7 天消费情况", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            
            DailyLineChart(dailyStats)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                ChartLegend("节省", MaterialTheme.colorScheme.primary)
                ChartLegend("消费", MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
            
            Text(
                text = "本月消费情况: ${String.format("%.2f", monthlySpent)} / ${String.format("%.0f", monthlyGoal)} 元",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun DailyLineChart(stats: List<DailyStats>) {
    val maxVal = remember(stats) {
        val maxAmount = stats.maxOfOrNull { maxOf(it.savedAmount, it.spentAmount, it.regretAmount) } ?: 0.0
        maxAmount.coerceAtLeast(5.0).toFloat()
    }
    
    val savedColor = MaterialTheme.colorScheme.primary
    val spentColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column {
        Box(modifier = Modifier.fillMaxWidth().height(80.dp).padding(start = 30.dp, end = 8.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val spacing = if (stats.size > 1) width / (stats.size - 1) else width

                // 绘制参考线
                val gridLines = 4
                repeat(gridLines + 1) { i ->
                    val y = height - (i * height / gridLines)
                    val labelValue = (i * maxVal / gridLines).toInt()
                    
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.2f),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                    
                    drawContext.canvas.nativeCanvas.drawText(
                        labelValue.toString(),
                        -25.dp.toPx(),
                        y + 4.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.GRAY
                            textSize = 10.sp.toPx()
                        }
                    )
                }

                fun drawTrendLine(color: Color, getData: (DailyStats) -> Double, isDashed: Boolean = false) {
                    val path = Path()
                    stats.forEachIndexed { index, daily ->
                        val x = index * spacing
                        val y = height - (getData(daily).toFloat() / maxVal * height)
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        drawCircle(color = color, radius = 3.dp.toPx(), center = Offset(x, y))
                    }
                    drawPath(
                        path = path,
                        color = color,
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = if (isDashed) PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f) else null
                        )
                    )
                }

                drawTrendLine(savedColor, { it.savedAmount })
                drawTrendLine(spentColor, { it.spentAmount })
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(start = 30.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            stats.forEach { 
                Text(it.date, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun ChartLegend(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    val contentColor = if (isEnabled) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
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
                Button(onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }) {
                    Text("去开启")
                }
            }
        }
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
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("${String.format("%.2f", record.amount)} 元", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(timeStr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
