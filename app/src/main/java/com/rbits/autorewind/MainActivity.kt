package com.rbits.autorewind

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.datastore.dataStore
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rbits.autorewind.data.SettingsRepository
import com.rbits.autorewind.data.SettingsSerializer
import com.rbits.autorewind.ui.MainScreen
import com.rbits.autorewind.ui.SettingsViewModel
import com.rbits.autorewind.ui.theme.AutoRewindTheme

const val TAG: String = "com.rbits.autorewind"
const val SETTINGS_STORE_FILE_NAME = "settings.json"

val Context.settingsStore by dataStore(
    fileName = SETTINGS_STORE_FILE_NAME,
    serializer = SettingsSerializer,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel {
                SettingsViewModel(SettingsRepository(settingsStore))
            }

            AutoRewindTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        settingsViewModel = settingsViewModel,
                        modifier = Modifier
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}