package com.communicationcoach.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.communicationcoach.data.db.AppDatabase
import com.communicationcoach.data.db.entity.ConversationEntity
import com.communicationcoach.data.db.entity.InsightEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ConversationDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)

    private val _conversation = MutableStateFlow<ConversationEntity?>(null)
    val conversation: StateFlow<ConversationEntity?> = _conversation.asStateFlow()

    private val _fullTranscript = MutableStateFlow("")
    val fullTranscript: StateFlow<String> = _fullTranscript.asStateFlow()

    private val _insight = MutableStateFlow<InsightEntity?>(null)
    val insight: StateFlow<InsightEntity?> = _insight.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun load(conversationId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _conversation.value = db.conversationDao().getById(conversationId)
            _fullTranscript.value =
                db.transcriptChunkDao().getFullTranscriptForConversation(conversationId) ?: ""
            _insight.value = db.insightDao().getForConversation(conversationId)
            _isLoading.value = false
        }
    }
}
