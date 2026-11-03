package com.arjunvision.arjunfitai.ui.coach

import com.arjunvision.arjunfitai.data.coach.ApiKeyStore
import com.arjunvision.arjunfitai.data.coach.ChatKind
import com.arjunvision.arjunfitai.data.coach.ChatMessage
import com.arjunvision.arjunfitai.data.coach.ChatRole
import com.arjunvision.arjunfitai.data.coach.CoachClient
import com.arjunvision.arjunfitai.data.coach.CoachDatabase
import com.arjunvision.arjunfitai.data.coach.CoachReply
import com.arjunvision.arjunfitai.data.coach.CutOff
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Sends coach messages from an app-wide scope, so a reply keeps streaming (and gets saved) even if
 * you leave the Coach screen. Nothing is sent unless you have saved a key and agreed to share.
 */
class CoachController(private val scope: CoroutineScope, private val db: CoachDatabase, val keys: ApiKeyStore) {
    private val _sending = MutableStateFlow(false)
    val sending: StateFlow<Boolean> = _sending.asStateFlow()

    /** The reply as it streams in; null when nothing is streaming. */
    private val _partial = MutableStateFlow<String?>(null)
    val partial: StateFlow<String?> = _partial.asStateFlow()

    private val _hasKey = MutableStateFlow(keys.hasKey())
    val hasKey: StateFlow<Boolean> = _hasKey.asStateFlow()

    val messages = db.chat().all()

    fun saveKey(key: String) {
        keys.save(key)
        _hasKey.value = true
    }

    fun removeKey() {
        keys.clear()
        _hasKey.value = false
    }

    fun clearChat() = scope.launch { db.chat().clear() }

    fun send(text: String, summary: String) {
        if (_sending.value || text.isBlank()) return
        val key = keys.load() ?: return notice("Your saved API key couldn't be read. Add it again in Profile › AI coach.")
        _sending.value = true
        scope.launch {
            try {
                db.chat().insert(ChatMessage(time = System.currentTimeMillis(), role = ChatRole.USER, text = text.trim()))
                val history = historyForRequest(db.chat().snapshot()).toMutableList()
                // Only the newest message carries today's summary.
                val last = history.lastIndex
                if (last >= 0 && history[last].first == ChatRole.USER) history[last] = ChatRole.USER to withSummary(summary, history[last].second)
                _partial.value = ""
                val reply = CoachClient(key).reply(COACH_SYSTEM_PROMPT, history) { _partial.value = it }
                val saved = when (reply) {
                    is CoachReply.Text -> {
                        reply.cutOff?.let { cut ->
                            // Keep what arrived, then say clearly that it stopped early.
                            db.chat().insert(ChatMessage(time = System.currentTimeMillis(), role = ChatRole.COACH, text = reply.text))
                            ChatMessage(
                                time = System.currentTimeMillis() + 1, role = ChatRole.COACH, kind = ChatKind.NOTICE,
                                text = when (cut) {
                                    CutOff.LENGTH -> "That reply hit the length limit and was cut off. Ask \"continue\" for the rest."
                                    CutOff.CONNECTION -> "The connection dropped before the reply finished. Ask \"continue\" or send your message again."
                                },
                            )
                        } ?: ChatMessage(time = System.currentTimeMillis(), role = ChatRole.COACH, text = reply.text)
                    }
                    CoachReply.Refused -> ChatMessage(
                        time = System.currentTimeMillis(), role = ChatRole.COACH, kind = ChatKind.NOTICE,
                        text = "I can't help with that one. Try asking another way — and for anything medical, please talk to a doctor.",
                    )
                    is CoachReply.Failed -> ChatMessage(time = System.currentTimeMillis(), role = ChatRole.COACH, kind = ChatKind.NOTICE, text = reply.message)
                }
                db.chat().insert(saved)
            } finally {
                _partial.value = null
                _sending.value = false
            }
        }
    }

    private fun notice(text: String) {
        scope.launch { db.chat().insert(ChatMessage(time = System.currentTimeMillis(), role = ChatRole.COACH, kind = ChatKind.NOTICE, text = text)) }
    }
}
