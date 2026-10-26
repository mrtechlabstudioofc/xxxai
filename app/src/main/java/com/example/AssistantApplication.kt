package com.example

import android.app.Application
import com.example.actions.ActionExecutor
import com.example.data.api.AiApiService
import com.example.data.preferences.AppPreferencesRepository
import com.example.voice.TtsManager

class AssistantApplication : Application() {

    lateinit var preferencesRepository: AppPreferencesRepository
        private set

    lateinit var aiApiService: AiApiService
        private set

    lateinit var ttsManager: TtsManager
        private set

    lateinit var actionExecutor: ActionExecutor
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        preferencesRepository = AppPreferencesRepository(this)
        aiApiService = AiApiService()
        ttsManager = TtsManager(this)
        actionExecutor = ActionExecutor(this)
    }

    override fun onTerminate() {
        super.onTerminate()
        ttsManager.destroy()
    }

    companion object {
        lateinit var instance: AssistantApplication
            private set
    }
}
