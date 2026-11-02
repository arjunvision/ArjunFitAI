package com.arjunvision.arjunfitai.ui.placeholder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.arjunvision.arjunfitai.ui.components.AfCircleButton
import com.arjunvision.arjunfitai.ui.components.AfIcons
import com.arjunvision.arjunfitai.ui.theme.AfColors
import com.arjunvision.arjunfitai.ui.theme.AfShapes

/**
 * Stand-in for a screen that arrives in a later phase. [onAllData] (health screens only) opens the
 * interim list of everything read from Samsung Health, so real data stays reachable until then.
 */
@Composable
fun ComingSoonScreen(
    title: String,
    phase: Int,
    contentPadding: PaddingValues,
    onBack: (() -> Unit)?,
    onAllData: (() -> Unit)? = null,
) {
    Column(
        Modifier.fillMaxSize().padding(contentPadding).padding(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            onBack?.let { AfCircleButton(AfIcons.Back, "Back", AfColors.Text, it) }
            Text(title.uppercase(), style = MaterialTheme.typography.headlineLarge, color = AfColors.Text)
        }
        Box(
            Modifier.fillMaxWidth().heightIn(min = 160.dp).clip(AfShapes.Card)
                .border(1.dp, AfColors.Border, AfShapes.Card).padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "This screen arrives in Phase $phase.",
                style = MaterialTheme.typography.bodyMedium,
                color = AfColors.TextMuted,
            )
        }
        onAllData?.let {
            Box(
                Modifier.heightIn(min = 48.dp).clip(AfShapes.Pill).background(AfColors.Inner)
                    .clickable(role = Role.Button, onClick = it).padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("See all Samsung Health data", style = MaterialTheme.typography.labelLarge, color = AfColors.Text)
            }
        }
    }
}
