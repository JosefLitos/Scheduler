package cz.kepis.scheduler.modules

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.WeekFields
import java.util.Locale

// base: 0=absolute, 1=month, 2=week, 3=day, 4=hour, 5=minute

data class Timestamp private constructor(
    val year: Int,
    val month: Int,
    val week: Int,
    val day: Int,
    val hour: Int,
    val minute: Int,
    val timestamp: LocalDateTime
) {
    val hasValue
        get() = month != 0 || week != 0 || day != 0 || hour != 0 || minute != 0

    companion object {
        public fun build(
            baseFrom: Int, m: Int, w: Int, d: Int, H: Int, M: Int, y: Int = 0
        ): Timestamp {
            val base = LocalDateTime.now()
            val year = base.year // extra variable for valid 31.12. midnight
            var ts = LocalDateTime.of(year, 1, 1, 0, 0)

            val month = (if (y > 0) (y - base.year) * 12
            else 0) + if (0 < baseFrom) m + base.monthValue else m
            if (month != 0) ts = ts.plusMonths(month.toLong() - if (month > 0) 1 else 0)

            val week = if (w == 0 && baseFrom != 2 || 1 >= baseFrom) w else w + base.get(
                WeekFields.of(Locale.getDefault()).weekOfMonth()
            ) - 1
            if (week != 0) {
                ts = ts.plusWeeks(week.toLong() - if (week > 0) 1 else 0)
                // we need to adjust to the beginning of the first week of the month
                ts = ts.plusDays(
                    (7 + DayOfWeek.MONDAY.value - ts.dayOfWeek.value).toLong() % 7
                )
            }

            val d = (M / 60 + H) / 24 + d
            val H = (M / 60 + H) % 24
            val M = M % 60

            val day = if (2 < baseFrom) d + if (week > 0) base.dayOfWeek.value
            else base.dayOfMonth else d
            if (day != 0) ts = ts.plusDays(day.toLong() - if (day > 0) 1 else 0)

            val hour = if (3 < baseFrom) H + base.hour else H
            if (hour != 0) ts = ts.plusHours(hour.toLong())

            val minute = if (4 < baseFrom) M + base.minute else M
            if (minute != 0) ts = ts.plusMinutes(minute.toLong())

            return Timestamp(year, month, week, day, hour, minute, ts)
        }

        val EMPTY = Timestamp(0, 0, 0, 0, 0, 0, LocalDateTime.of(0, 1, 1, 0, 0))
    }

    fun syncUsing(ts: Timestamp): Timestamp {
        val now = LocalDateTime.now()
        val period = ((now.toEpochSecond(ZoneOffset.UTC) - timestamp.toEpochSecond(
            ZoneOffset.UTC
        )) / ts.toSeconds()).toInt() + 1 // ceil to get to next period, not last passed

        // we rely on users setting repeat in days when not using weeks for start
//        week + if (week != 0) ts.week else 0,
//        day + ts.day + if (week != 0) 0 else ts.week,

        return build(
            0,
            month + ts.month * period,
            week + ts.week * period,
            day + ts.day * period,
            hour + ts.hour * period,
            minute + ts.minute * period,
            year
        )
    }

    operator fun plus(ts: Timestamp) = build(
        0,
        month + ts.month,
        week + ts.week,
        day + ts.day,
        hour + ts.hour,
        minute + ts.minute,
        year
    )

    operator fun minus(ts: Timestamp) = build(
        0,
        month - ts.month,
        week - ts.week,
        day - ts.day,
        hour - ts.hour,
        minute - ts.minute,
        year
    )

    override fun toString(): String = if (!hasValue) ""
    else "${
        if (timestamp.year != year) timestamp.toLocalDate()
        else "${timestamp.dayOfMonth}.${timestamp.monthValue}."
    }${
        if (0 == timestamp.hour && timestamp.minute == 0) ""
        else " ${timestamp.toLocalTime()}"
    }"

    val periodString: String
        get() = "${
            if (month > 0) "${month}m" else ""
        }${if (week > 0) "${week}w" else ""}${
            if (day > 0) "${day}d" else ""
        }${if (hour > 0 || minute > 0) " ${hour}h ${minute}" else ""}"

    fun toSeconds(): Long =
        (((month.toLong() * 30 + week * 7 + day) * 24 + hour) * 60 + minute) * 60
}

/*fun main(args: Array<String>) {
    println("${build(3, 0, 0, 0, 0, 0)} = today")
    println("${build(0, 4, 1, 0) + build(0, 12)} = 2024-4-1")
    println("${build(0, 16, 0, 1)} = 2024-4-1")
    println("${build(1, 0, 0, 2)} = cur m, 2nd day")
    println("${build(2, 0, 1, -4)} = cur m, cur w, Tuesday")
    println("${build(2, 1, 1, 2)} = next m, next w, Tuesday")
    println("${build(0, 0, 1, 0) + build(base = 5)} = next w, this time")
}*/
