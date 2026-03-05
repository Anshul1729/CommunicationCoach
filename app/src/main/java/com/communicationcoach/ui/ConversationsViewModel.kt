package com.communicationcoach.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.communicationcoach.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ConversationsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)

    private val _conversations = MutableStateFlow<List<ConversationUiItem>>(emptyList())
    val conversations: StateFlow<List<ConversationUiItem>> = _conversations.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            val convs = db.conversationDao().getAll()
            val items = convs.map { conv ->
                val insight = db.insightDao().getForConversation(conv.id)
                val firstTip = insight?.tipsJson?.let { json ->
                    try { parseTips(json).firstOrNull() } catch (e: Exception) { null }
                }
                ConversationUiItem(
                    id = conv.id,
                    startTime = conv.startTime,
                    durationSeconds = conv.durationSeconds,
                    status = conv.status,
                    summary = insight?.summary,
                    firstTip = firstTip
                )
            }
            _conversations.value = items
            _isLoading.value = false
        }
    }
}
