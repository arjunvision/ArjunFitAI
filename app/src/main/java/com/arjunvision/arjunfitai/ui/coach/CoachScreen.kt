package com.arjunvision.arjunfitai.ui.coach

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arjunvision.arjunfitai.R
import com.arjunvision.arjunfitai.data.coach.ChatKind
import com.arjunvision.arjunfitai.data.coach.ChatMessage
import com.arjunvision.arjunfitai.data.coach.ChatRole
import com.arjunvision.arjunfitai.data.coach.apiKeyProblem
import com.arjunvision.arjunfitai.data.coach.looksLikeApiKey
import com.arjunvision.arjunfitai.ui.components.AfCircleButton
import com.arjunvision.arjunfitai.ui.components.AfDialog
import com.arjunvision.arjunfitai.ui.components.AfIcons
import com.arjunvision.arjunfitai.ui.components.AfOutlineButton
import com.arjunvision.arjunfitai.ui.components.AfPrimaryButton
import com.arjunvision.arjunfitai.ui.components.AfTextField
import com.arjunvision.arjunfitai.ui.components.LocalReduceMotion
import com.arjunvision.arjunfitai.ui.components.riseIn
import com.arjunvision.arjunfitai.ui.home.greeting
import com.arjunvision.arjunfitai.ui.theme.AfColors
import com.arjunvision.arjunfitai.ui.theme.AfShapes
import java.time.LocalTime

private val CoachBubble = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 6.dp)
private val UserBubble = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 6.dp, bottomStart = 20.dp)

private val Suggestions = listOf("Swap today’s workout", "High-protein dinner idea", "Why is my weight flat?")

@Composable
fun CoachScreen(
    controller: CoachController,
    inputs: CoachInputs,
    name: String,
    consent: Boolean,
    onConsent: (Boolean) -> Unit,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
) {
    val hasKey by controller.hasKey.collectAsStateWithLifecycle()
    val summary = coachSummary(inputs)
    var showSummary by rememberSaveable { mutableStateOf(false) }
    var confirmClear by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(contentPadding).imePadding().padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Header(onBack, menu = hasKey && consent, onWhatItSees = { showSummary = true }, onClear = { confirmClear = true }, onRemoveKey = controller::removeKey)
        when {
            !hasKey -> KeySetup(onSave = controller::saveKey)
            !consent -> Consent(summary, onAgree = { onConsent(true) }, onNotNow = onBack)
            else -> Chat(controller, inputs, name, summary, Modifier.weight(1f))
        }
    }

    if (showSummary) {
        AfDialog("What the coach sees", { showSummary = false }, "Close" to { showSummary = false }, showCancel = false) {
            Text("This summary goes to Claude with each message you send — nothing else from your health data.", style = MaterialTheme.typography.bodySmall)
            SummaryBox(summary)
        }
    }
    if (confirmClear) {
        AfDialog("Clear the chat?", { confirmClear = false }, "Clear" to { controller.clearChat(); confirmClear = false }) {
            Text("Deletes the conversation from this phone.")
        }
    }
}

@Composable
private fun Header(onBack: () -> Unit, menu: Boolean, onWhatItSees: () -> Unit, onClear: () -> Unit, onRemoveKey: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        AfCircleButton(AfIcons.Back, "Back", AfColors.Text, onBack)
        Image(painterResource(R.drawable.arjunfit_logo), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.size(width = 40.dp, height = 36.dp))
        Column(Modifier.weight(1f)) {
            Text(
                buildAnnotatedString {
                    append("Arjun"); withStyle(SpanStyle(color = AfColors.Accent)) { append("Fit") }; append(" AI Coach")
                },
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp), color = AfColors.Text,
            )
            Text("Powered by Claude", style = MaterialTheme.typography.bodySmall, color = AfColors.TextMuted)
        }
        if (menu) {
            Box {
                Box(Modifier.size(48.dp).clip(AfShapes.Pill).clickable(role = Role.Button, onClickLabel = "Coach options") { open = true }, contentAlignment = Alignment.Center) {
                    Text("⋯", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AfColors.Text)
                }
                DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = AfColors.Card) {
                    DropdownMenuItem(text = { Text("What the coach sees") }, onClick = { open = false; onWhatItSees() })
                    DropdownMenuItem(text = { Text("Clear chat") }, onClick = { open = false; onClear() })
                    DropdownMenuItem(text = { Text("Remove API key") }, onClick = { open = false; onRemoveKey() })
                }
            }
        }
    }
}

@Composable
private fun KeySetup(onSave: (String) -> Unit) {
    var key by rememberSaveable { mutableStateOf("") }
    val valid = looksLikeApiKey(key)
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Bubble(AfColors.Card, CoachBubble) {
            Text("Connect your Claude API key", style = MaterialTheme.typography.titleMedium, color = AfColors.Text)
            Text(
                "1. Open console.anthropic.com › API keys and create a key just for ArjunFit.\n" +
                    "2. Set a monthly spend limit there — messages are billed to your Anthropic account.\n" +
                    "3. Paste the key below. It's encrypted with your phone's Keystore and never leaves this phone except to call Claude.",
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp), color = AfColors.TextMuted, modifier = Modifier.padding(top = 6.dp),
            )
        }
        AfTextField(
            key, { key = it.trim() }, "API key (sk-ant-…)",
            KeyboardOptions(keyboardType = KeyboardType.Password),
            apiKeyProblem(key),
            visualTransformation = PasswordVisualTransformation(),
        )
        AfPrimaryButton("Save key", { if (valid) { onSave(key); key = "" } }, enabled = valid)
    }
}

@Composable
private fun Consent(summary: String, onAgree: () -> Unit, onNotNow: () -> Unit) {
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Bubble(AfColors.Card, CoachBubble) {
            Text("Before your first message", style = MaterialTheme.typography.titleMedium, color = AfColors.Text)
            Text(
                "Each time you send a message, ArjunFit sends Claude your message and this short summary — no name, no raw records. " +
                    "Nothing is sent until you tap Send. Turn sharing off any time in Profile › AI coach.",
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp), color = AfColors.TextMuted, modifier = Modifier.padding(top = 6.dp),
            )
        }
        SummaryBox(summary)
        AfPrimaryButton("Agree & start", onAgree)
        AfOutlineButton("Not now", onNotNow)
        Text("Coach tips are general fitness guidance, not medical advice", style = MaterialTheme.typography.bodySmall, color = AfColors.TextMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun SummaryBox(summary: String) {
    Text(summary, style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp), color = AfColors.Text,
        modifier = Modifier.fillMaxWidth().clip(AfShapes.Inner).background(AfColors.Inner).padding(14.dp))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Chat(controller: CoachController, inputs: CoachInputs, name: String, summary: String, modifier: Modifier) {
    val messages by controller.messages.collectAsStateWithLifecycle(emptyList())
    val sending by controller.sending.collectAsStateWithLifecycle()
    val partial by controller.partial.collectAsStateWithLifecycle()
    var text by rememberSaveable { mutableStateOf("") }
    val list = rememberLazyListState()
    val itemCount = messages.size + 2 + (if (sending) 1 else 0)
    LaunchedEffect(itemCount, partial?.length) { if (itemCount > 0) list.animateScrollToItem(itemCount - 1) }
    val send: (String) -> Unit = { t -> if (t.isNotBlank() && !sending) { controller.send(t, summary); text = "" } }

    LazyColumn(modifier.fillMaxWidth(), state = list, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { PlanBubble(inputs, name) }
        item {
            Bubble(AfColors.Accent, CoachBubble, Modifier.riseIn(1)) {
                Text("“${motivation(inputs)}”", style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp), color = AfColors.OnAccent)
            }
        }
        items(messages, key = { it.id }) { MessageBubble(it) }
        if (sending) item {
            val p = partial
            if (p.isNullOrEmpty()) TypingDots() else Bubble(AfColors.Card, CoachBubble, Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
                Text(p, style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp), color = AfColors.Text)
            }
        }
    }

    if (!sending) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Suggestions.forEach { s ->
                Box(
                    Modifier.heightIn(min = 48.dp).clip(AfShapes.Pill).border(1.dp, AfColors.Border, AfShapes.Pill).clickable(role = Role.Button) { send(s) }.padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(s, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium), color = AfColors.Text) }
            }
        }
    }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(AfShapes.Pill).background(AfColors.Card).padding(start = 18.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.weight(1f).semantics { contentDescription = "Message your coach" }) {
            if (text.isEmpty()) Text("Ask your fitness coach…", style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp), color = Color.White.copy(alpha = 0.5f))
            BasicTextField(
                value = text, onValueChange = { text = it.take(2000) }, maxLines = 4,
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp, color = AfColors.Text),
                cursorBrush = SolidColor(AfColors.Accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { send(text) }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Box(
            Modifier.size(48.dp).clip(AfShapes.Pill).background(if (text.isNotBlank() && !sending) AfColors.Accent else AfColors.Inner)
                .clickable(enabled = text.isNotBlank() && !sending, role = Role.Button, onClickLabel = "Send") { send(text) },
            contentAlignment = Alignment.Center,
        ) { Icon(AfIcons.Send, contentDescription = "Send", tint = if (text.isNotBlank() && !sending) AfColors.OnAccent else AfColors.TextMuted, modifier = Modifier.size(18.dp)) }
    }
    Text("Coach tips are general fitness guidance, not medical advice", fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun PlanBubble(inputs: CoachInputs, name: String) {
    val tasks = todayChecklist(inputs)
    Bubble(AfColors.Card, CoachBubble, Modifier.riseIn(0)) {
        Text("${greeting(LocalTime.now(), name)}! Here's your plan for today:", style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold), color = AfColors.Text)
        tasks.forEach { t ->
            Row(
                Modifier.padding(top = 8.dp).semantics(mergeDescendants = true) { contentDescription = t.label + if (t.done) ", done" else ", not done yet" },
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    Modifier.size(22.dp).clip(RoundedCornerShape(7.dp))
                        .then(if (t.done) Modifier.background(AfColors.Success) else Modifier.border(2.dp, Color(0xFF3A4350), RoundedCornerShape(7.dp))),
                    contentAlignment = Alignment.Center,
                ) { if (t.done) Text("✓", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = AfColors.Background) }
                Text(
                    t.label, style = MaterialTheme.typography.bodyMedium,
                    color = if (t.done) AfColors.TextMuted else AfColors.Text,
                    textDecoration = if (t.done) TextDecoration.LineThrough else null,
                )
            }
        }
        Text("${tasks.count { it.done }} of ${tasks.size} done today · ticked from your data", style = MaterialTheme.typography.bodySmall, color = AfColors.TextMuted, modifier = Modifier.padding(top = 10.dp))
    }
}

@Composable
private fun MessageBubble(m: ChatMessage) {
    if (m.role == ChatRole.USER) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text(m.text, style = MaterialTheme.typography.bodyMedium, color = AfColors.Text,
                modifier = Modifier.widthIn(max = 300.dp).clip(UserBubble).background(AfColors.Inner).padding(horizontal = 14.dp, vertical = 10.dp))
        }
    } else {
        Bubble(if (m.kind == ChatKind.NOTICE) AfColors.Inner else AfColors.Card, CoachBubble) {
            Text(m.text, style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp), color = if (m.kind == ChatKind.NOTICE) AfColors.AccentSoft else AfColors.Text)
        }
    }
}

@Composable
private fun Bubble(color: Color, shape: RoundedCornerShape, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier.fillMaxWidth(0.92f).clip(shape).background(color).padding(16.dp)) { content() }
}

/** Three bouncing dots while Claude starts replying (still when motion is reduced). */
@Composable
private fun TypingDots() {
    val reduce = LocalReduceMotion.current
    Row(
        Modifier.clip(CoachBubble).background(AfColors.Card).padding(horizontal = 16.dp, vertical = 14.dp).semantics { contentDescription = "Coach is typing" },
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        repeat(3) { i ->
            val t = if (reduce) null else rememberInfiniteTransition(label = "dot$i").animateFloat(
                0f, 0f,
                infiniteRepeatable(keyframes { durationMillis = 1200; 0f at 0; 1f at 480; 0f at 960 }, RepeatMode.Restart, initialStartOffset = androidx.compose.animation.core.StartOffset(i * 150)),
                label = "dotY$i",
            )
            val p = t?.value ?: 1f
            Box(Modifier.offset(y = (-3 * p).dp).size(7.dp).clip(AfShapes.Pill).background(Color.White.copy(alpha = 0.25f + 0.75f * p)))
        }
    }
}
