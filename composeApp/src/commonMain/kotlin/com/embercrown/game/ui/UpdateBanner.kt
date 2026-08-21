package com.embercrown.game.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.embercrown.game.BuildInfo
import com.embercrown.game.resources.Res
import com.embercrown.game.resources.update_available
import com.embercrown.game.resources.update_current_version
import com.embercrown.game.resources.update_dismiss
import com.embercrown.game.resources.update_download
import com.embercrown.game.ui.pixelart.EmberPalette
import com.embercrown.game.ui.pixelart.PixelButton
import com.embercrown.game.ui.pixelart.pixelFrame
import com.embercrown.game.update.AvailableUpdate
import com.embercrown.game.update.checkForUpdate
import com.embercrown.game.update.openUrl
import org.jetbrains.compose.resources.stringResource

/**
 * Polls GitHub once per app launch and, if a newer release exists, offers it. The check is
 * fire-and-forget: any failure leaves [update] null and the banner simply never appears.
 */
@Composable
fun UpdateBanner(modifier: Modifier = Modifier) {
    var update by remember { mutableStateOf<AvailableUpdate?>(null) }
    var dismissed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        update = checkForUpdate()
    }

    val available = update
    if (available == null || dismissed) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .pixelFrame(fill = EmberPalette.Accent.copy(alpha = 0.28f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text = stringResource(Res.string.update_available, available.version.toString()),
            color = EmberPalette.GoldBright,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = pixelFontFamily(),
        )
        Text(
            text = stringResource(Res.string.update_current_version, BuildInfo.VERSION),
            color = EmberPalette.Gold.copy(alpha = 0.7f),
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 3.dp, bottom = 8.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            PixelButton(onClick = { openUrl(available.releaseUrl) }) {
                Text(
                    stringResource(Res.string.update_download),
                    fontFamily = pixelFontFamily(),
                    fontSize = 12.sp,
                    color = Color.White,
                )
            }
            PixelButton(
                onClick = { dismissed = true },
                containerColor = EmberPalette.PanelLight,
                contentColor = EmberPalette.Gold,
            ) {
                Text(
                    stringResource(Res.string.update_dismiss),
                    fontFamily = pixelFontFamily(),
                    fontSize = 12.sp,
                    color = EmberPalette.Gold,
                )
            }
        }
    }
}
