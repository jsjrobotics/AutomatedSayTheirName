package com.spookyrobotics.automatedsaytheirnames

import android.os.Bundle
import android.speech.tts.TextToSpeech
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

    private var isPlaying: Boolean = false
    private var processingComplete: Boolean = false
    private var textToSpeechInit: Boolean = false
    private lateinit var parseResources: ParseResources
    private val victims: MutableList<PoliceVictimEntry> = mutableListOf()
    private var startDisposable: Disposable? = null
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var mainScope: CoroutineScope
    private lateinit var ioScope: CoroutineScope
    private lateinit var database: OfflineContentDao
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        database = provideDatabase()
        mainScope = provideMainCoroutineScope()
        ioScope = provideIoCoroutineScope()
        textToSpeech = TextToSpeech(this, this)
        parseResources = ParseResources(this, database, ioScope)
        parseResources.main()

    }

    override fun onStart() {
        super.onStart()
        startDisposable = parseResources.onProcessingComplete().subscribe {
            if (it == true) {
                processingComplete = true
                startPlaybackIfPossible()
            }
        }
    }

    private fun startPlaybackIfPossible() {
        if (processingComplete && textToSpeechInit && !isPlaying) {
            isPlaying = true
            victims.clear()
            ioScope.launch {
                victims.addAll(database.getAll())
                victims.forEach {
                    speakTheirName(it)
                }
            }
        }
    }

    private fun speakTheirName(victim: PoliceVictimEntry) {
        textToSpeech.speak(victim.name, TextToSpeech.QUEUE_ADD, null)
        textToSpeech.playSilentUtterance(3000, TextToSpeech.QUEUE_ADD, victim.toString())
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

            val result = textToSpeech . setLanguage(Locale.US);

            if (result == TextToSpeech.LANG_MISSING_DATA
                || result == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                println("Failed to set locale to us")
            } else {
                textToSpeechInit = true
                for (tmpVoice in textToSpeech.voices) {
                    if (tmpVoice.name == "_voiceName") {
                        textToSpeech.voice = tmpVoice
                    }
                }
                startPlaybackIfPossible()
            }

        } else {
            println("failed to init")
        }
    }

    override fun onDestroy() {
// Don't forget to shutdown tts!
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}
