package com.communicationcoach.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.communicationcoach.ui.ConversationDetailViewModel
import com.communicationcoach.ui.IssueDetail
import com.communicationcoach.ui.parseIssues
import com.communicationcoach.ui.parseTips
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationDetailScreen(
    conversationId: Long,
    onBack: () -> Unit,
    viewModel: ConversationDetailViewModel = viewModel()
) {
    val conversation by viewModel.conversation.collectAsState()
    val fullTranscript by viewModel.fullTranscript.collectAsState()
    val insight by viewModel.insight.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(conversationId) { viewModel.load(conversationId) }

    val dateStr = conversation?.startTime?.let {
        SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault()).format(Date(it))
    } ?: "Conversation"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(dateStr, fontWeight = FontWeight.SemiBold, fontSize = 15.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Meta header ───────────────────────────────────────────────
            val duration = formatDuration(conversation?.durationSeconds ?: 0)
            val status   = conversation?.status ?: ""
            Text(
                text = "$duration  ·  $status",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // ── Coaching tips ─────────────────────────────────────────────
            if (insight != null) {
                val tips = parseTips(insight!!.tipsJson)
                if (tips.isNotEmpty()) {
                    SectionCard(title = "Coaching Tips") {
                        tips.forEach { tip ->
                            Row(
                                modifier = Modifier.padding(vertical = 5.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "•",
                                    modifier = Modifier.padding(end = 8.dp, top = 1.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = tip,
                                    fontSize = 14.sp,
                                    lineHeight = 21.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // ── Summary ───────────────────────────────────────────────
                if (insight!!.summary.isNotBlank()) {
                    SectionCard(title = "Summary") {
                        Text(
                            text = insight!!.summary,
                            fontSize = 14.sp,
                            lineHeight = 21.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // ── Issues breakdown ──────────────────────────────────────
                val issues = parseIssues(insight!!.issuesJson)
                if (issues.isNotEmpty()) {
                    IssuesCard(issues = issues)
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = if (status == "ANALYZED") "No insights found for this conversation."
                        else "Analysis not complete yet. Check back shortly.",
                        modifier = Modifier.padding(20.dp),
                        fontSize = 14.sp,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── Full transcript ───────────────────────────────────────────
            TranscriptCard(transcript = fullTranscript)
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IssuesCard(issues: List<IssueDetail>) {
    SectionCard(title = "Issues Detected") {
        val detected = issues.filter { it.detected || it.score != null }
        if (detected.isEmpty()) {
            Text(
                text = "No major issues detected — great work!",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            return@SectionCard
        }

        detected.forEachIndexed { index, issue ->
            if (index > 0) {
                Divider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                )
            }
            IssueRow(issue = issue)
        }
    }
}

@Composable
private fun IssueRow(issue: IssueDetail) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(50),
                color = if (issue.detected)
                    MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                else
                    MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = issue.label,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (issue.detected)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                )
            }
            issue.score?.let { score ->
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "$score/10",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        if (issue.note.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = issue.note,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 19.sp
            )
        }

        issue.examples.forEach { example ->
            Spacer(Modifier.height(4.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = example,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TranscriptCard(transcript: String) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Full Transcript",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = { expanded = !expanded }) {
                    Text(text = if (expanded) "Collapse" else "Expand")
                }
            }

            if (transcript.isBlank()) {
                Text(
                    text = "No transcript available.",
                    fontSize = 13.sp,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = transcript,
                    fontSize = 13.sp,
                    lineHeight = 21.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = if (expanded) Int.MAX_VALUE else 6,
                )
            }
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return if (m > 0) "${m}m ${s}s" else "${s}s"
}
