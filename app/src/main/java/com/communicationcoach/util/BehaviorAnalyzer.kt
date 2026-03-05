package com.communicationcoach.util

import android.util.Log
import com.communicationcoach.data.model.BehaviorAnalysis
import org.json.JSONObject

class BehaviorAnalyzer {
    companion object {
        private const val TAG = "BehaviorAnalyzer"
    }
    
    /**
     * Parse Claude's JSON response into BehaviorAnalysis
     */
    fun parseAnalysisResponse(jsonString: String): BehaviorAnalysis {
        return try {
            val json = JSONObject(jsonString)
            
            BehaviorAnalysis(
                isHarshTone = json.optBoolean("harshTone", false),
                isTalkingFast = json.optBoolean("talkingFast", false),
                wordsPerMinute = json.optInt("estimatedWPM", 0),
                isMonologuing = json.optBoolean("monologuing", false),
                continuousSpeechSeconds = json.optInt("continuousSpeechSeconds", 0),
                summary = json.optString("summary", ""),
                rawAnalysis = json.optString("reasoning", "")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse analysis response", e)
            Log.d(TAG, "Raw response: $jsonString")
            
            // Fallback: try to extract basic info from malformed response
            BehaviorAnalysis(
                rawAnalysis = jsonString,
                summary = "Analysis failed to parse"
            )
        }
    }
    
    /**
     * Get detected behaviors as a list
     */
    fun getDetectedBehaviors(analysis: BehaviorAnalysis): List<BehaviorType> {
        val behaviors = mutableListOf<BehaviorType>()
        
        if (analysis.isHarshTone) {
            behaviors.add(BehaviorType.HARSH_TONE)
        }
        
        if (analysis.isTalkingFast) {
            behaviors.add(BehaviorType.TALKING_FAST)
        }
        
        if (analysis.isMonologuing) {
            behaviors.add(BehaviorType.MONOLOGUING)
        }
        
        return behaviors
    }
    
    /**
     * Check if any behavior was detected that requires a nudge
     */
    fun needsNudge(analysis: BehaviorAnalysis): Boolean {
        return analysis.isHarshTone || analysis.isTalkingFast || analysis.isMonologuing
    }
}
