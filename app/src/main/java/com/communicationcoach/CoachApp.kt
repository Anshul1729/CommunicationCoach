package com.communicationcoach

import android.app.Application
import com.communicationcoach.worker.DailyDigestWorker

class CoachApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Schedule the 10 PM daily digest (KEEP policy — safe to call on every launch)
        DailyDigestWorker.schedule(this)
    }
}
