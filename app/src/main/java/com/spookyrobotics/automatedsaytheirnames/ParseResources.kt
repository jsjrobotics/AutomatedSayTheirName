package com.spookyrobotics.automatedsaytheirnames

import android.content.Context
import com.spookyrobotics.automatedsaytheirnames.room.OfflineContentDao
import com.spookyrobotics.automatedsaytheirnames.room.PoliceVictimEntry
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class ParseResources(
    private val ctx: Context,
    private val database: OfflineContentDao,
    private val ioScope: CoroutineScope
) {
    private var nextIndex = 0
    private val nextEntry = mutableListOf<String>()
    private val entries = mutableListOf<String>()
    private val processingComplete: BehaviorSubject<Boolean> = BehaviorSubject.createDefault(false)
    fun onProcessingComplete(): Observable<Boolean> = processingComplete

    fun main() {
        ioScope.launch {
            database.deleteAll()
        }
        val inputStream: InputStream = ctx.resources.openRawResource(R.raw.raw_names)
        val reader = BufferedReader(InputStreamReader(inputStream))
        reader.forEachLine {
            if (it.isEmpty()) {
                processNextEntry()
                nextEntry.clear()
            } else {
                nextEntry.add(it)
            }
        }
        entries.forEach {
            println("Processed $it")
        }
        processingComplete.onNext(true)
    }

    private fun processNextEntry() {
        if (nextEntry.size != 3) {
            println("------- Failed to parse entry ---------")
            nextEntry.forEach { println(it) }
            return
        }
        val name = nextEntry[0].split(",").first()
        val calendar = nextEntry[0].substring(nextEntry[0].indexOf(",") + 1)
        val lifeString = calendar.split("-")
        val birthString = if (lifeString.isNotEmpty()) lifeString[0].trim() else ""
        val deathString = if (lifeString.size >= 2) calendar.split("-")[1].trim() else ""
        val simpleDateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.ENGLISH)
        try {
            val birthTime : Long = if (birthString.isNotEmpty()) {
                val birthDate = simpleDateFormat.parse(birthString)
                birthDate.time
            } else {
                -1
            }

            val deathTime : Long = if (deathString.isNotEmpty()) {
                val birthDate = simpleDateFormat.parse(deathString)
                birthDate.time
            } else {
                -1
            }
            val location = nextEntry[1]
            val details = nextEntry[2]
            entries.add(name)
            val victim = PoliceVictims(name, birthTime, deathTime, location, details, "id:$name")
            nextIndex += 1
            ioScope.launch {
                val entry = PoliceVictimEntry (
                    birthDate = victim.birthDate,
                    deathDate = victim.deathDate,
                    name = victim.name,
                    details = victim.details,
                    location = victim.location,
                    utteranceId = victim.utteranceId
                )

                database.insertEntry(entry)
            }
        } catch (exception: Exception) {
            println("Failed to parse $name")
            println(exception)
        }
    }
}