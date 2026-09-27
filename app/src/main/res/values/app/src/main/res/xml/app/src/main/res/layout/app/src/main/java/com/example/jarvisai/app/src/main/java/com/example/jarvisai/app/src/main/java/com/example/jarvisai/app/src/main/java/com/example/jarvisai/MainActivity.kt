package com.example.jarvisai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var tts: TextToSpeech
    private lateinit var geminiManager: GeminiManager
    private lateinit var actionExecutor: ActionExecutor

    private lateinit var tvStatus: TextView
    private lateinit var btnListen: Button

    // Replace with your Google AI Studio Gemini API Key
    private val GEMINI_API_KEY = AQ.Ab8RN6IvV0nJiTMpZAzXU0KrVfKdSPh5Q3RQc7pfO_4wzjmSnw

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        btnListen = findViewById(R.id.btnListen)

        geminiManager = GeminiManager(GEMINI_API_KEY)
        actionExecutor = ActionExecutor(this)
        tts = TextToSpeech(this, this)

        checkPermissions()
        setupSpeechRecognizer()

        btnListen.setOnClickListener {
            startListening()
        }
    }

    override fun onResume() {
        super.onResume()
        if (JarvisAccessibilityService.instance == null) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 101)
        }
    }

    private fun setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { tvStatus.text = "Listening..." }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { tvStatus.text = "Processing..." }
            override fun onError(error: Int) { tvStatus.text = "Speech Error Code: $error" }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val userQuery = matches[0]
                    tvStatus.text = "You: $userQuery"
                    processWithGemini(userQuery)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
        speechRecognizer.startListening(intent)
    }

    private fun processWithGemini(userText: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val responseJson = geminiManager.processCommand(userText)
            val speechResponse = responseJson.optString("speech", "")

            tvStatus.text = "JARVIS: $speechResponse"
            speakOut(speechResponse) {
                actionExecutor.execute(responseJson)
            }
        }
    }

    private fun speakOut(text: String, onComplete: () -> Unit) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "JARVIS_TTS")
        btnListen.postDelayed({ onComplete() }, 2000)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
        }
    }

    override fun onDestroy() {
        speechRecognizer.destroy()
        tts.shutdown()
        super.onDestroy()
    }
}
