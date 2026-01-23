package com.example.worthit

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.worthit.ui.theme.WorthItTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Calendar

class CoolDownActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val price = intent.getDoubleExtra("EXTRA_PRICE", 0.0)

        setContent {
            WorthItTheme {
                val settingsManager = remember { SettingsManager(this) }
                val dao = remember { (application as WorthItApplication).database.recordDao() }
                CoolDownScreen(
                    price = price,
                    settingsManager = settingsManager,
                    recordDao = dao,
                    onFinish = {
                        saveRecordAndFinish(price, settingsManager)
                    }
                )
            }
        }
    }

    private fun saveRecordAndFinish(price: Double, settingsManager: SettingsManager) {
        val dao = (application as WorthItApplication).database.recordDao()
        lifecycleScope.launch {
            dao.insert(Record(amount = price, timestamp = System.currentTimeMillis()))
            if (settingsManager.navToHome.first()) {
                val intent = Intent(this@CoolDownActivity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
            }
            finish()
        }
    }
}

@Composable
fun CoolDownScreen(
    price: Double,
    settingsManager: SettingsManager,
    recordDao: RecordDao,
    onFinish: () -> Unit
) {
    var timeLeft by remember { mutableStateOf(10) }
    val circleScale = remember { Animatable(1f) }

    val refItemName by settingsManager.refItemName.collectAsState(initial = "")
    val refItemPrice by settingsManager.refItemPrice.collectAsState(initial = 0f)
    val monthlyGoal by settingsManager.monthlyGoal.collectAsState(initial = 1000f)

    val monthlySpent by recordDao.getAllRecords().map { recordList ->
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        recordList.filter { record ->
            val recordCalendar = Calendar.getInstance()
            recordCalendar.timeInMillis = record.timestamp
            record.isSpent && recordCalendar.get(Calendar.MONTH) == currentMonth
        }.sumOf { it.amount }
    }.collectAsState(initial = 0.0)

    val foodEquivalent = if (refItemPrice > 0) price / refItemPrice else 0.0

    LaunchedEffect(Unit) {
        while (timeLeft > 0) {
            delay(1000L)
            timeLeft--
        }
        onFinish()
    }

    LaunchedEffect(Unit) {
        while (true) {
            circleScale.animateTo(1.2f, animationSpec = tween(4000, easing = LinearEasing))
            delay(1000)
            circleScale.animateTo(1f, animationSpec = tween(5000, easing = LinearEasing))
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "深呼吸",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (foodEquivalent > 0) {
                Text(
                    text = "本次消费 ≈ ${String.format("%.1f", foodEquivalent)} $refItemName",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "(${String.format("%.2f", price)} 元)",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(40.dp))

            val totalSpentAfterThis = monthlySpent + price
            val isOverBudget = totalSpentAfterThis > monthlyGoal

            val initialProgress = if (monthlyGoal > 0f) (monthlySpent / monthlyGoal).toFloat() else 0f
            val finalProgress = if (monthlyGoal > 0f) (totalSpentAfterThis / monthlyGoal).toFloat() else 0f
            val animatedProgress = remember { Animatable(initialProgress) }

            LaunchedEffect(finalProgress) {
                animatedProgress.animateTo(
                    targetValue = finalProgress,
                    animationSpec = tween(durationMillis = 1000, easing = LinearEasing)
                )
            }

            val progressColor by animateColorAsState(
                targetValue = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                animationSpec = tween(durationMillis = 1000)
            )

            Column(
                modifier = Modifier.fillMaxWidth(0.9f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator(
                    progress = { animatedProgress.value },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(MaterialTheme.shapes.medium),
                    color = progressColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "本月消费(含本次): ${String.format("%.2f", totalSpentAfterThis)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isOverBudget) progressColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "目标: ${String.format("%.2f", monthlyGoal)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.scale(circleScale.value)
            ) {
                CircularProgressIndicator(
                    progress = { timeLeft / 10f },
                    modifier = Modifier.size(120.dp),
                    strokeWidth = 10.dp,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Text(
                    text = "$timeLeft",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
