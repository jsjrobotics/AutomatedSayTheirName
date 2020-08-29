package com.spookyrobotics.automatedsaytheirnames

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID
import android.speech.tts.UtteranceProgressListener
import android.widget.ImageView
import android.widget.TextView
import android.widget.ViewFlipper
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import com.spookyrobotics.automatedsaytheirnames.room.DatabaseWrapper
import com.spookyrobotics.automatedsaytheirnames.room.OfflineContentDao
import com.spookyrobotics.automatedsaytheirnames.room.OfflineDatabaseConstants
import com.spookyrobotics.automatedsaytheirnames.room.PoliceVictimEntry
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import kotlin.coroutines.CoroutineContext

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private val SILENCE_ID: String = "silenceId"
    private val ENDING_UTTERANCE_ID: String = "ending"
    private val utteranceProgressListener: UtteranceProgressListener = buildProgressListener()

    private fun buildProgressListener(): UtteranceProgressListener {
        return object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) {
                mainScope.launch {
                    val currentVictim = victims.indexOfFirst { it.utteranceId == utteranceId }
                    if (currentVictim != -1) {
                        val nextVictim = victims.getOrNull(currentVictim+1)?.utteranceId ?: ""
                        nextRemembered.text = nextVictim.removePrefix(ParseResources.UTTERANCE_ID_PREFIX)
                    }
                }
            }

            override fun onDone(utteranceId: String) {
                noteRemembered(utteranceId)
            }

            override fun onError(utteranceId: String) {
                println("Error speaking: $utteranceId")
            }

        }
    }

    private val PAUSE_VIEW_INDEX: Int = 1
    private val PLAY_VIEW_INDEX: Int = 0

    private var isPlaying: Boolean = false
    private var processingComplete: Boolean = false
    private var textToSpeechInit: Boolean = false
    private lateinit var parseResources: ParseResources
    private val victims: MutableList<PoliceVictimEntry> = mutableListOf()
    private var startDisposable: Disposable? = null
    private var textToSpeech: TextToSpeech? = null
    private lateinit var mainScope: CoroutineScope
    private lateinit var ioScope: CoroutineScope
    private lateinit var database: OfflineContentDao
    private lateinit var playPauseButton: ViewFlipper
    private lateinit var justRemembered: TextView
    private lateinit var nextRemembered: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        justRemembered = findViewById(R.id.justRemembered)
        nextRemembered = findViewById(R.id.nextRemembered)
        setupPlayPauseButton()
        database = provideDatabase()
        mainScope = provideMainCoroutineScope()
        ioScope = provideIoCoroutineScope()
        textToSpeech = TextToSpeech(this, this).apply {
            setOnUtteranceProgressListener(utteranceProgressListener)
        }
        parseResources = ParseResources(this, database, ioScope)
        parseResources.main()
        startDisposable = parseResources.onProcessingComplete().subscribe {
            if (it == true) {
                mainScope.launch {
                    setVictims(database.getAll())
                    setNextVictimToFirstInList()
                    processingComplete = true
                }
            }
        }
    }

    private fun setupPlayPauseButton() {
        playPauseButton = findViewById(R.id.playPauseButton)
        val playView = ImageView(this).apply {
            setImageResource(R.drawable.ic_baseline_play_circle_outline_24)
        }
        val pauseView = ImageView(this).apply {
            setImageResource(R.drawable.ic_baseline_stop_24)
        }
        playPauseButton.addView(playView, PLAY_VIEW_INDEX)
        playPauseButton.addView(pauseView, PAUSE_VIEW_INDEX)
        playPauseButton.displayedChild = PLAY_VIEW_INDEX
        playPauseButton.setOnClickListener {
            if (playPauseButton.displayedChild == PLAY_VIEW_INDEX) {
                startPlaybackIfPossible()
            } else {
                stopPlayback()
            }
        }
    }

    private fun stopPlayback() {
        if (isPlaying) {
            mainScope.launch {
                setNextVictimToFirstInList()
                textToSpeech?.playSilentUtterance(3000, TextToSpeech.QUEUE_FLUSH, ENDING_UTTERANCE_ID)
                playPauseButton.displayedChild = PLAY_VIEW_INDEX
                isPlaying = false
            }
        }
    }

    @Synchronized
    private fun noteRemembered(utteranceId: String) {
        if (utteranceId == SILENCE_ID || utteranceId == ENDING_UTTERANCE_ID) {
            return
        }
        mainScope.launch {
            justRemembered.text = utteranceId.removePrefix(ParseResources.UTTERANCE_ID_PREFIX)
            val rememberedVictimIndex = victims.indexOfFirst { it.utteranceId == utteranceId }
            victims.removeAt(rememberedVictimIndex)
        }
    }

    private fun setNextVictimToFirstInList() {
        val nextVictimId = victims.firstOrNull()?.utteranceId?.removePrefix(ParseResources.UTTERANCE_ID_PREFIX)
        nextRemembered.text = nextVictimId ?: ""
    }

    private fun startPlaybackIfPossible() {
        if (processingComplete && textToSpeechInit && !isPlaying) {
            mainScope.launch {
                isPlaying = true
                playPauseButton.displayedChild = PAUSE_VIEW_INDEX
                victims.forEach {
                    speakTheirName(it)
                }
            }
        }
    }

    @Synchronized
    private fun setVictims(victimList: List<PoliceVictimEntry>) {
        victims.clear()
        victims.addAll(victimList)
    }

    private fun speakTheirName(victim: PoliceVictimEntry) {
        val paramsMap = hashMapOf<String,String>(
            Pair(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, victim.utteranceId)
        )
        textToSpeech?.speak(victim.name, TextToSpeech.QUEUE_ADD, paramsMap)
        textToSpeech?.playSilentUtterance(3000, TextToSpeech.QUEUE_ADD, SILENCE_ID)
    }

    fun provideDatabase(): OfflineContentDao {
        return Room.databaseBuilder(
            application,
            DatabaseWrapper::class.java,
            OfflineDatabaseConstants.DATABASE_FILENAME
        )
            .build()
            .contentDao()
    }

    fun provideMainCoroutineScope(): CoroutineScope {
        return object : CoroutineScope {
            override val coroutineContext: CoroutineContext
                get() = Dispatchers.Main
        }
    }

    fun provideIoCoroutineScope(): CoroutineScope {
        return object : CoroutineScope {
            override val coroutineContext: CoroutineContext
                get() = Dispatchers.IO
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {

            val result = textToSpeech?. setLanguage(Locale.US) ?: run {
                println("Text to speech is null. Failing to set language and start playback")
                return
            }

            if (result == TextToSpeech.LANG_MISSING_DATA
                || result == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                println("Failed to set locale to us")
            } else {
                textToSpeechInit = true
            }

        } else {
            println("failed to init")
        }
    }

    override fun onDestroy() {
        textToSpeech?.stop();
        textToSpeech?.shutdown();
        super.onDestroy();
    }
}
