package com.sistema.distribuido.network.prefecto

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import com.sistema.distribuido.network.TestModeManager
import kotlinx.coroutines.launch

@Composable
fun rememberTestModeEnabled(context: android.content.Context): State<Boolean> {
    val testMode = remember { TestModeManager.getInstance(context) }
    return testMode.isEnabledFlow.collectAsState(initial = false)
}

@Composable
fun Modifier.testModeSecretGesture(
    context: android.content.Context,
    onToggled: (Boolean) -> Unit = {}
): Modifier {
    val scope = rememberCoroutineScope()
    val testMode = remember { TestModeManager.getInstance(context) }
    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableLongStateOf(0L) }

    return this.pointerInput(Unit) {
        detectTapGestures {
            val now = System.currentTimeMillis()
            if (now - lastTapTime > 2000) tapCount = 0
            lastTapTime = now
            tapCount++
            if (tapCount >= 5) {
                tapCount = 0
                scope.launch {
                    testMode.toggle()
                    onToggled(testMode.isEnabled())
                }
            }
        }
    }
}
