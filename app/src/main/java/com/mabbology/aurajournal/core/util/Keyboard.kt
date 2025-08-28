package com.mabbology.aurajournal.core.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat

@Composable
fun ExecuteOnKeyboardOpen(callback: () -> Unit) {
    val view = LocalView.current
    DisposableEffect(view) {
        ViewCompat.setWindowInsetsAnimationCallback(
            view,
            object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_CONTINUE_ON_SUBTREE) {
                override fun onProgress(
                    insets: WindowInsetsCompat,
                    runningAnimations: MutableList<WindowInsetsAnimationCompat>
                ): WindowInsetsCompat = insets // mandatory override

                override fun onEnd(animation: WindowInsetsAnimationCompat) {
                    if (animation.typeMask and WindowInsetsCompat.Type.ime()
                        == WindowInsetsCompat.Type.ime()
                    ) {
                        val isKeyboardOpen = ViewCompat.getRootWindowInsets(view)
                            ?.isVisible(WindowInsetsCompat.Type.ime()) != false
                        if (isKeyboardOpen) {
                            callback()
                        }
                    }
                    super.onEnd(animation)
                }
            }
        )

        onDispose {
            ViewCompat.setWindowInsetsAnimationCallback(view, null)
        }
    }
}
