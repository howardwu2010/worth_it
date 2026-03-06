package com.example.worthit

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MonitorService : AccessibilityService() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private var lastTriggerTime: Long = 0
    private val COOLDOWN_MS = 20 * 1000

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val rootNode = rootInActiveWindow ?: return

        val detectedPrice = recursiveSearchPrice(rootNode)

        if (detectedPrice > 0.0) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastTriggerTime > COOLDOWN_MS) {
                Log.d("MonitorService", "检测到消费 ，金额 $detectedPrice")
                triggerCoolDown(detectedPrice)
            }
        }

        rootNode.recycle()
    }

    private fun recursiveSearchPrice(node: AccessibilityNodeInfo?): Double {
        if (node == null) return 0.0

        val text = node.text?.toString() ?: ""
        val desc = node.contentDescription?.toString() ?: ""

        if (text.contains("仅需 1 元，保住当前金币不流失！") || desc.contains("花费 1 元")) {
            return 1.0
        }

        if (text.contains("仅需 0.5 元，开启 2.0x 暴走模式！") || desc.contains("花费 0.5 元")) {
            return 0.5
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val result = recursiveSearchPrice(child)
                child.recycle()
                if (result > 0.0) {
                    return result
                }
            }
        }

        return 0.0
    }

    private fun triggerCoolDown(price: Double) {
        lastTriggerTime = System.currentTimeMillis()

        serviceScope.launch {
            delay(1000L)
            val intent = Intent(this@MonitorService, CoolDownActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra("EXTRA_PRICE", price)
            }
            startActivity(intent)
        }
    }

    override fun onInterrupt() {
        Log.w("MonitorService", "服务已中断")
        serviceJob.cancel()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i("MonitorService", "无障碍服务已连接")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}
