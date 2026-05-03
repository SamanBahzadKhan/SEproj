/**
 * Purpose: Handles AI assistant requests and response orchestration.
 * Depends on: Android ViewModel/repository layers and Supabase Edge endpoint.
 * Notes: Manages chat responses and safe fallback behavior.
 */

package com.fridge.caps.ai

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.fridge.caps.R
import java.util.Locale

class AiViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AiRepository(application)

    sealed class ChatMessage {
        data class UserMessage(val text: String) : ChatMessage()
        data class AssistantMessage(
            val text: String,
            val counsellors: List<AiRepository.Counsellor> = emptyList(),
            val officeInfo: AiRepository.OfficeInfo? = null,
            val isCrisis: Boolean = false,
        ) : ChatMessage()

        data object LoadingMessage : ChatMessage()
    }

    private val _messages = MutableLiveData<List<ChatMessage>>(
        listOf<ChatMessage>(
            ChatMessage.AssistantMessage(
                text = "Hi! I'm the CAPs AI Assistant. I can help you find the right counsellor, " +
                    "guide you to the right LUMS office, or just point you in the right direction. " +
                    "What's on your mind?",
            ),
        ),
    )
    val messages: LiveData<List<ChatMessage>> = _messages

    private val _showSuggestedPrompts = MutableLiveData(true)
    val showSuggestedPrompts: LiveData<Boolean> = _showSuggestedPrompts

    fun sendMessage(query: String) {
        Log.d("AI_DEBUG", "ViewModel sendMessage called: $query")
        val q = query.trim()
        if (q.isEmpty()) return

        val current = _messages.value?.toMutableList() ?: mutableListOf<ChatMessage>()
        current.add(ChatMessage.UserMessage(q))
        current.add(ChatMessage.LoadingMessage)
        _messages.value = current
        _showSuggestedPrompts.value = false

        val localHelpReply = helpReplyIfRequested(q)
        if (localHelpReply != null) {
            val updated = _messages.value?.toMutableList() ?: mutableListOf<ChatMessage>()
            updated.removeAll { it is ChatMessage.LoadingMessage }
            updated.add(ChatMessage.AssistantMessage(text = localHelpReply))
            _messages.value = updated
            return
        }

        try {
            Log.d("AI_DEBUG", "ViewModel: repository.getRecommendation callback for \"$q\"")
            repository.getRecommendation(q) { result ->
                try {
                    val updated = _messages.value?.toMutableList() ?: mutableListOf<ChatMessage>()
                    updated.removeAll { it is ChatMessage.LoadingMessage }

                    result.fold(
                        onSuccess = { response ->
                            updated.add(
                                ChatMessage.AssistantMessage(
                                    text = response.answer,
                                    counsellors = response.counsellors,
                                    officeInfo = response.officeInfo,
                                    isCrisis = response.isCrisis,
                                ),
                            )
                        },
                        onFailure = {
                            updated.add(
                                ChatMessage.AssistantMessage(
                                    text = "Something went wrong. Please try again.",
                                ),
                            )
                        },
                    )

                    _messages.value = updated
                } catch (e: Exception) {
                    Log.e("AI_DEBUG", "Error applying AI result", e)
                    val updated = _messages.value?.toMutableList() ?: mutableListOf<ChatMessage>()
                    updated.removeAll { it is ChatMessage.LoadingMessage }
                    updated.add(
                        ChatMessage.AssistantMessage(
                            text = "Something went wrong. Please try again.",
                        ),
                    )
                    _messages.value = updated
                }
            }
        } catch (e: Exception) {
            Log.e("AI_DEBUG", "Crash starting getRecommendation", e)
            val updated = _messages.value?.toMutableList() ?: mutableListOf<ChatMessage>()
            updated.removeAll { it is ChatMessage.LoadingMessage }
            updated.add(
                ChatMessage.AssistantMessage(
                    text = "Something went wrong. Please try again.",
                ),
            )
            _messages.value = updated
        }
    }

    private fun helpReplyIfRequested(query: String): String? {
        val q = query.lowercase(Locale.US)
        val asksForSupport = q.contains("help") ||
            q.contains("support") ||
            q.contains("emergency") ||
            q.contains("contact") ||
            q.contains("website")
        if (!asksForSupport) {
            return null
        }
        return getApplication<Application>().getString(R.string.help_support_message)
    }
}
