package studio.cortex.thunderstack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import studio.cortex.thunderstack.feedback.ThunderFeedbackController
import studio.cortex.thunderstack.ui.ThunderStackApp
import studio.cortex.thunderstack.ui.ThunderStackViewModel

class MainActivity : ComponentActivity() {
    private lateinit var feedback: ThunderFeedbackController
    private var model: ThunderStackViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        feedback = ThunderFeedbackController(this)
        setContent {
            MaterialTheme {
                val viewModel: ThunderStackViewModel = viewModel()
                val progress by viewModel.progress.collectAsStateWithLifecycle()
                model = viewModel
                LaunchedEffect(viewModel) {
                    viewModel.feedback.collect(feedback::handle)
                }
                LaunchedEffect(progress) { feedback.updateSettings(progress) }
                ThunderStackApp(viewModel)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        feedback.setForeground(true)
    }

    override fun onStop() {
        model?.onAppBackground()
        feedback.setForeground(false)
        super.onStop()
    }

    override fun onDestroy() {
        feedback.release()
        super.onDestroy()
    }
}
