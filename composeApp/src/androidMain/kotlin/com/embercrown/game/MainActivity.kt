package com.embercrown.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.embercrown.game.game.appContext
import com.embercrown.game.reboot.RebootApp
import com.embercrown.game.reboot.RebootGraph

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appContext = applicationContext
        enableEdgeToEdge()
        setContent {
            RebootApp()
        }
    }

    override fun onPause() {
        RebootGraph.pauseAudioIfInitialized()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        RebootGraph.resumeAudioIfInitialized()
    }
}
