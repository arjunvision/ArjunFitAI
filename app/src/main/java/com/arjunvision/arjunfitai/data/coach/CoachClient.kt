package com.arjunvision.arjunfitai.data.coach

import android.util.Log
import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.core.JsonValue
import com.anthropic.errors.AnthropicIoException
import com.anthropic.errors.AnthropicServiceException
import com.anthropic.errors.BadRequestException
import com.anthropic.errors.PermissionDeniedException
import com.anthropic.errors.RateLimitException
import com.anthropic.errors.UnauthorizedException
import com.anthropic.models.messages.CacheControlEphemeral
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.OutputConfig
import com.anthropic.models.messages.StopReason
import com.anthropic.models.messages.TextBlockParam
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.time.Duration

/** Why a reply stopped before Claude finished it. */
enum class CutOff { LENGTH, CONNECTION }

sealed interface CoachReply {
    /** [cutOff] is set when the reply ended early; [text] is what arrived. */
    data class Text(val text: String, val cutOff: CutOff? = null) : CoachReply
    /** Claude declined (stop_reason "refusal"); any partial text is discarded. */
    data object Refused : CoachReply
    data class Failed(val message: String) : CoachReply
}

/**
 * Calls the Claude Messages API with your own key, straight from the phone, streaming the reply.
 * - Model: Claude Opus 5.5 at low effort (chat replies, kept quick and inexpensive).
 * - The fixed coaching instructions are prompt-cached; the daily summary comes after them.
 * - Server-side fallback ("fallbacks": "default") retries a declined request on another model.
 */
class CoachClient(private val apiKey: String) {
    private val client: AnthropicClient = AnthropicOkHttpClient.builder()
        .apiKey(apiKey)
        // Generous: a streamed reply (plus a server-side fallback retry) must not be cut by the client.
        .timeout(Duration.ofMinutes(3))
        .maxRetries(2)
        .build()

    suspend fun reply(
        system: String,
        history: List<Pair<ChatRole, String>>,
        onText: (String) -> Unit,
    ): CoachReply = withContext(Dispatchers.IO) {
        val params = MessageCreateParams.builder()
            .model(MODEL)
            .maxTokens(16_000L)
            .outputConfig(OutputConfig.builder().effort(OutputConfig.Effort.LOW).build())
            .systemOfTextBlockParams(listOf(TextBlockParam.builder().text(system).cacheControl(CacheControlEphemeral.builder().build()).build()))
            .apply {
                history.forEach { (role, text) -> if (role == ChatRole.USER) addUserMessage(text) else addAssistantMessage(text) }
            }
            .putAdditionalHeader("anthropic-beta", "server-side-fallback-2026-07-01")
            .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"))
            .build()

        val text = StringBuilder()
        var stopReason: StopReason? = null
        var finished = false
        try {
            client.messages().createStreaming(params).use { stream ->
                stream.stream().forEach { event ->
                    event.contentBlockDelta().flatMap { it.delta().text() }.ifPresent { delta ->
                        text.append(delta.text())
                        onText(text.toString())
                    }
                    event.messageDelta().flatMap { it.delta().stopReason() }.ifPresent { stopReason = it }
                    if (event.messageStop().isPresent) finished = true
                }
            }
            currentCoroutineContext().ensureActive()
            Log.d(TAG, "reply done: stop=$stopReason finished=$finished chars=${text.length}")
            when {
                stopReason == StopReason.REFUSAL -> CoachReply.Refused
                text.isBlank() -> CoachReply.Failed("The coach sent an empty reply. Try again.")
                stopReason == StopReason.MAX_TOKENS -> CoachReply.Text(text.toString().trim(), CutOff.LENGTH)
                // The stream ended without Claude's "message_stop": the connection closed early.
                !finished || stopReason == null -> CoachReply.Text(text.toString().trim(), CutOff.CONNECTION)
                else -> CoachReply.Text(text.toString().trim())
            }
        } catch (e: AnthropicIoException) {
            Log.w(TAG, "stream broke after ${text.length} chars", e)
            if (text.isNotBlank()) CoachReply.Text(text.toString().trim(), CutOff.CONNECTION)
            else CoachReply.Failed("No connection to Claude. Check your internet and try again.")
        } catch (e: UnauthorizedException) {
            CoachReply.Failed("Your API key was rejected. Check it in Profile › AI coach, or create a new key at console.anthropic.com.")
        } catch (e: PermissionDeniedException) {
            CoachReply.Failed("This API key isn't allowed to use Claude Opus 5.5.")
        } catch (e: RateLimitException) {
            CoachReply.Failed("Too many requests right now. Wait a minute and try again.")
        } catch (e: BadRequestException) {
            CoachReply.Failed("Claude couldn't take that request: ${e.message ?: "bad request"}")
        } catch (e: AnthropicServiceException) {
            CoachReply.Failed("Claude is unavailable right now (${e.statusCode()}). Try again shortly.")
        }
    }

    companion object {
        const val MODEL = "claude-opus-5-5"
        private const val TAG = "ArjunFitCoach"
    }
}
