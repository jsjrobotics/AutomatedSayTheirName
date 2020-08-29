package com.spookyrobotics.automatedsaytheirnames

import android.content.Context
import android.util.Log
import com.spookyrobotics.automatedsaytheirnames.room.OfflineContentDao
import com.spookyrobotics.automatedsaytheirnames.room.PoliceVictimEntry
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class ParseResources(
    private val ctx: Context,
    private val database: OfflineContentDao
) {
    companion object {
        val UTTERANCE_ID_PREFIX: String = "id:"
    }
    private var nextIndex = 0
    private val nextEntry = mutableListOf<String>()
    private val entries = mutableListOf<String>()
    private val processingComplete: BehaviorSubject<Boolean> = BehaviorSubject.createDefault(false)
    fun onProcessingComplete(): Observable<Boolean> = processingComplete
    val dateFormatDefault = SimpleDateFormat("MMMM dd, yyyy", Locale.ENGLISH)
    val dateFormatSecondary = SimpleDateFormat("yyyy", Locale.ENGLISH)

    suspend fun main() {
        database.deleteAll()
        val inputStream: InputStream = ctx.resources.openRawResource(R.raw.raw_names)
        val reader = BufferedReader(InputStreamReader(inputStream))
        do {
            val nextLine: String = reader.readLine() ?: break
            if (nextLine.isEmpty()) {
                processNextEntry()
                nextEntry.clear()
            } else {
                nextEntry.add(nextLine)
            }
        } while (true)
        entries.forEach {
            println("Processed $it")
        }
        processingComplete.onNext(true)
    }

    private fun parseNameCalendarEntryName2(): Pair < String, String > {
        val startLine = nextEntry[0]
        val name = startLine.split(",").first()
        val calendar = startLine.substring(nextEntry[0].indexOf(",") + 1)
        return Pair(name, calendar)

    }

    private suspend fun processNextEntry() {
        if (nextEntry.size != 3) {
            Log.e("ParseResources","------- Failed to parse entry ---------")
            nextEntry.forEach { println(it) }
            return
        }
        val nameCalendar = parseNameCalendarEntryName2()
        val name = nameCalendar.first
        val calendar = nameCalendar.second
        val lifeString = calendar.split("-")
        val birthString = if (lifeString.isNotEmpty()) lifeString[0].trim() else ""
        val deathString = if (lifeString.size >= 2) calendar.split("-")[1].trim() else ""
        try {
            val birthTime: Long = parseDateString(birthString)
            val deathTime : Long = parseDateString(deathString)
            val location = nextEntry[1]
            val details = nextEntry[2]
            entries.add(name)
            val victim = PoliceVictims(name, birthTime, deathTime, location, details, "$UTTERANCE_ID_PREFIX$name")
            nextIndex += 1
            val entry = PoliceVictimEntry (
                birthDate = victim.birthDate,
                deathDate = victim.deathDate,
                name = victim.name,
                details = victim.details,
                location = victim.location,
                utteranceId = victim.utteranceId
            )

            database.insertEntry(entry)
        } catch (exception: Exception) {
            Log.e("ParseResources","Failed to parse $name")
            println(exception)
        }
    }

    private fun parseDateString(dateString: String): Long {
        try {
            if (dateString.isNotEmpty()) {
                val birthDate = dateFormatDefault.parse(dateString)
                return birthDate.time
            }
        } catch (exception: Exception) { /* Silent Error */ }

        try {
            if (dateString.isNotEmpty()) {
                val birthDate = dateFormatSecondary.parse(dateString)
                return birthDate.time
            }
        } catch (exception: Exception) { /* Silent Error */ }

        Log.e("ParseResources","Failed to parse dateString: ${nextEntry[0]} -> $dateString")
        return -1
    }
}