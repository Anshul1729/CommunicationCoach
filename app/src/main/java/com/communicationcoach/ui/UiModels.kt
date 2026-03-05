package com.communicationcoach.ui

import org.json.JSONArray
import org.json.JSONObject

// ── Screen-level data classes ─────────────────────────────────────────────────

data class TodayStats(
    val conversationCount: Int = 0,
    val topIssue: String? = null
)

data class ConversationUiItem(
    val id: Long,
    val startTime: Long,
    val durationSeconds: Int,
    val status: String,
    val summary: String?,
    val firstTip: String?
)

data class IssueDetail(
    val key: String,
    val label: String,
    val detected: Boolean,
    val note: String,
    val examples: List<String> = emptyList(),
    val score: Int? = null             // only used for "clarity"
)

// ── Parsing helpers ───────────────────────────────────────────────────────────

fun parseTips(tipsJson: String): List<String> = try {
    val arr = JSONArray(tipsJson)
    (0 until arr.length()).map { arr.getString(it) }
} catch (e: Exception) {
    emptyList()
}

fun parseIssues(issuesJson: String): List<IssueDetail> = try {
    val obj = JSONObject(issuesJson)
    listOf(
        parseIssue(obj, "defensive",    "Defensive Language"),
        parseIssue(obj, "emotionalTone","Emotional Tone"),
        parseIssue(obj, "grammar",      "Grammar"),
        parseIssue(obj, "clarity",      "Clarity"),
        parseIssue(obj, "confidence",   "Confidence")
    )
} catch (e: Exception) {
    emptyList()
}

private fun parseIssue(root: JSONObject, key: String, label: String): IssueDetail {
    val obj = root.optJSONObject(key) ?: return IssueDetail(key, label, false, "")
    val detected = obj.optBoolean("detected", false)
    val note     = obj.optString("note", "")
    val score    = if (obj.has("score")) obj.optInt("score") else null
    val examples = obj.optJSONArray("examples")?.let { arr ->
        (0 until arr.length()).map { arr.getString(it) }
    } ?: emptyList()
    return IssueDetail(key, label, detected, note, examples, score)
}

/** Human-readable key → label map for the top issue chip on Home screen. */
fun issueKeyToLabel(key: String): String = when (key) {
    "defensive"    -> "Defensive language"
    "emotionalTone"-> "Emotional tone"
    "grammar"      -> "Grammar"
    "clarity"      -> "Clarity"
    "confidence"   -> "Confidence"
    else           -> key
}
