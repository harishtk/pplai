package com.pepulai.app.commons.util

import com.pepulai.app.commons.util.time.TimeAgo
import org.jetbrains.annotations.Contract
import org.threeten.bp.DateTimeUtils
import org.threeten.bp.Instant
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

@Suppress("unused")
object DateUtil {

    fun getDateFormat(localDateTime: LocalDateTime, formatString : String) : String {
        val formatter = DateTimeFormatter.ofPattern(formatString)
        return localDateTime.format(formatter)
    }

    fun getDateFormatDefault(dateString : String) : String? {
        var resultString : String? = null
        val serverDateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        try {
            serverDateTimeFormat.parse(dateString)?.let { convertDate ->
                resultString = simpleDateFormat.format(convertDate)
            }
        } catch (e: Exception) {
            Timber.d(e)
        }
        return resultString
    }

    fun parseServerDateFormat(dateTimeString: String): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
        return try {
            val date = sdf.parse(dateTimeString)
            date?.let {
                val localTimeDate = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(it.time),
                    ZoneId.systemDefault()
                )
                val localDate = DateTimeUtils.toDate(
                    localTimeDate.atZone(ZoneId.systemDefault()).toInstant()
                )
                getTimeAgo(localDate)
            } ?: dateTimeString
        } catch (e: NumberFormatException) {
            dateTimeString
        }
    }

    fun parseUtcStringToTimeAgo(utcString: String): String {
        return getTimeAgo(parseUtcString(utcString))
    }

    fun parseUtcStringToChatTime(utcString: String): String {
        return getChatTime(parseUtcString(utcString))
    }

    fun parseUtcString(utcString: String): Instant {
        return Instant.parse(utcString).atZone(ZoneId.systemDefault()).toInstant()
    }

    fun convertOnDateFormatToOther(
        fromDateFormat : String,
        toDateFormat : String,
        dateString: String
    ) : String? {
        var resultDateString : String? = null
        try {
            val fromDateFormatter = SimpleDateFormat(fromDateFormat, Locale.getDefault())
            val toDateFormatter = SimpleDateFormat(toDateFormat, Locale.getDefault())
            fromDateFormatter.parse(dateString)?.let {
                resultDateString = toDateFormatter.format(it)
            }
        } catch (e: Exception) {
            // Noop.
        }
        return resultDateString
    }

    fun parseServerDate(dateString: String): Long {
        val serverDateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return try {
            serverDateTimeFormat.parse(dateString)?.time ?: -1
        } catch (e: NumberFormatException) {
            Timber.w(e)
            -1
        }
    }

    fun parseServerDateToPostedTime(dateString : String) : String? {
        val serverDateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val simpleTimeFormat = SimpleDateFormat("hh:mm aa", Locale.getDefault())
        val simpleDateMonthFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        var resultText : String? = null
        try {
            val oldCal = Calendar.getInstance()
            oldCal.timeInMillis = System.currentTimeMillis()
            serverDateTimeFormat.parse(dateString)?.let { date ->
                val cal = Calendar.getInstance()
                cal.time = date
                val oldYear = oldCal[Calendar.YEAR]
                val year = cal[Calendar.YEAR]
                val oldDay = oldCal[Calendar.DAY_OF_YEAR]
                val day = cal[Calendar.DAY_OF_YEAR]
                Timber.d("DaY: ${oldDay - day}")
                if (oldYear == year) {
                    resultText = when (oldDay - day) {
                        -1 -> {
                            "Yesterday, ${simpleTimeFormat.format(date)}"
                        }
                        0 -> {
                            "Today, ${simpleTimeFormat.format(date)}"
                        }
                        1 -> {
                            "Yesterday, ${simpleTimeFormat.format(date)}"
                            //simpleDateFormat5.format(date)
                        }
                        else -> {
                            simpleDateMonthFormat.format(date)
                        }
                    }
                }
            }

        } catch (e: Exception) {
            Timber.tag("DateUtilsSingleton").d(e)
        }
        return resultText
    }

    fun getLaymanDate(ldt: LocalDateTime): String {
        val monthFormatter = DateTimeFormatter.ofPattern("dd MMM")
        val yearFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
        val now = LocalDateTime.now()
        return when {
            now.dayOfYear == ldt.dayOfYear -> {
                "Today"
            }
            now.year == ldt.year -> {
                ldt.format(monthFormatter)
            }
            else -> ldt.format(yearFormatter)
        }
    }

    fun getTimeAgo(date : Date): String {
        return TimeAgo.using(date.time)
    }

    fun getTimeAgo(date: Instant): String {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.time = Date(date.toEpochMilli())

        val now = org.threeten.bp.LocalDateTime.now()
        val at = LocalDateTime.ofInstant(date, ZoneId.systemDefault())
        Timber.d("Time: now=$now at=$at in ${ZoneId.systemDefault()}")

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val hour = calendar.get(Calendar.HOUR)
        val minute = calendar.get(Calendar.MINUTE)

        val currentCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))

        val currentYear = currentCalendar.get(Calendar.YEAR)
        val currentMonth = currentCalendar.get(Calendar.MONTH)
        val currentDay = currentCalendar.get(Calendar.DAY_OF_MONTH)
        val currentHour = currentCalendar.get(Calendar.HOUR)
        val currentMinute = currentCalendar.get(Calendar.MINUTE)

        return if (year < currentYear ) {
            val interval = currentYear - year
            if (interval == 1) "$interval year ago" else "$interval years ago"
        } else if (month < currentMonth) {
            val interval = currentMonth - month
            if (interval == 1) "$interval month ago" else "$interval months ago"
        } else  if (day < currentDay) {
            val interval = currentDay - day
            if (interval == 1) "$interval day ago" else "$interval days ago"
        } else if (hour < currentHour) {
            val interval = currentHour - hour
            if (interval == 1) "$interval hour ago" else "$interval hours ago"
        } else if (minute < currentMinute) {
            val interval = currentMinute - minute
            if (interval == 1) "$interval minute ago" else "$interval minutes ago"
        } else {
            "Just now"
        }
    }

    fun getChatTime(date: Instant): String {
        val now = org.threeten.bp.LocalDateTime.now()
        val at = LocalDateTime.ofInstant(date, ZoneId.systemDefault())

        return when {
            /*at.minute == now.minute -> {
                "Just Now"
            }*/
            else -> {
                at.format(org.threeten.bp.format.DateTimeFormatter.ofPattern("hh:mm a"))
                // at.format(DateTimeFormatter.ofPattern("hh:mm:ss.SSS a"))
            }
        }
    }

    @Contract("null -> null")
    fun getDaysInterval(date: Date?): Int? {
        date ?: return null
        val c = Calendar.getInstance().apply { time = date }
        val today = Calendar.getInstance()
        return today.get(Calendar.DAY_OF_YEAR) - c.get(Calendar.DAY_OF_YEAR)
    }
}