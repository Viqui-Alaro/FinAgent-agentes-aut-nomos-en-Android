package dev.devfest.agent

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp
import dev.devfest.agent.presentation.chat.AgentChatScreen
import dev.devfest.agent.presentation.theme.FinAgentTheme

@HiltAndroidApp
class FinAgentApp : Application()

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FinAgentTheme {
                AgentChatScreen()
            }
        }
    }
}
