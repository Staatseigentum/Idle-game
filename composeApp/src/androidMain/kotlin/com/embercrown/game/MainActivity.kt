package com.embercrown.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.embercrown.game.game.appContext
import com.embercrown.game.reboot.RebootApp
import com.embercrown.game.reboot.RebootGraph
import com.embercrown.game.update.autoUpdateIfNeeded
import com.embercrown.game.update.resumePendingUpdateInstall
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appContext = applicationContext
        enableEdgeToEdge()
        setContent {
            RebootApp()
        }
        // RebootApp is the shipped UI. The older App() update hook is never reached here.
        // Check once per Android launch without blocking the first frame or gameplay.
        lifecycleScope.launch { autoUpdateIfNeeded() }
    }

    override fun onPause() {
        RebootGraph.pauseAudioIfInitialized()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        RebootGraph.resumeAudioIfInitialized()
        resumePendingUpdateInstall()
    }
}
