package cz.kepis.scheduler.modules

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import cz.kepis.scheduler.App
import cz.kepis.scheduler.helpers.Settings
import org.greenrobot.eventbus.EventBus
import java.io.Serializable
import java.time.LocalDateTime

data class Schedule(
    var id: Int?,
    var start: Timestamp = Timestamp.EMPTY.copy(),
    var repeat: Timestamp = Timestamp.EMPTY.copy(),
    var until: Timestamp = Timestamp.EMPTY.copy(),
    var desc: String = "",
    var active: Boolean = true,
    var type: Int = Type.REMINDER
) : Comparable<Schedule> {

    companion object Type {
        const val EVENT = 0 // repeat = notify time before event -> start=start-repeat
        const val REMINDER = 1 // silent notify-only alarm
        const val ALARM = 2 // until ignored, repeat until dismissed (set inactive)
    }

    init {
        if (id == null && type == EVENT) start -= repeat
        alignTime(id == null)
        if ((id ?: -1) >= 0) App.bus.post(ScheduleEvent.Loaded(this))
    }

    private fun alignTime(byUser: Boolean) {
        if (!active) return

        if ((start.timestamp <= until.timestamp || !until.hasValue) && start.timestamp < LocalDateTime.now()) {
            // missed a notification, time to catch up
            if (!byUser && type != ALARM && Settings.notifyMissed) App.bus.post(
                ScheduleEvent.Notify(copy(id = -(id ?: this.hashCode())))
            )
            if (!repeat.hasValue) {
                deactivate(byUser)
                return
            } else {
                start = start.syncUsing(repeat)
                if (!byUser) Database.schedDao.update(+this)
            }
        }
        if (until.hasValue && start.timestamp > until.timestamp) deactivate(byUser)
    }

    // User updates only, not for after-notify start adjustment
    fun update(
        s: Timestamp, r: Timestamp, u: Timestamp, d: String, a: Boolean, t: Int = type
    ) {
        start = if (t == EVENT && r.hasValue) s - r else s
        repeat = r
        until = u
        desc = d
        active = a
        type = t
        alignTime(true)
        if (id == null) {
            id = Database.schedDao.insert(+this).toInt()
            EventBus.getDefault().post(ScheduleEvent.Created(this))
        } else if (this.id != null) {
            Database.schedDao.update(+this)
            EventBus.getDefault().post(ScheduleEvent.Updated(this))
        }
    }

    private fun deactivate(byUser: Boolean) {
        active = false
        if (byUser) return // we don't save because it will be saved shortly after ↑↑
        if (Settings.removeAfterDone) {
            if (id != null) delete()
        } else ensureBackgroundThread { Database.schedDao.deactivate(id!!) }
    }

    fun delete() {
        val id = id!!
        ensureBackgroundThread { Database.schedDao.delete(id) }
        EventBus.getDefault().post(ScheduleEvent.Deleted(this))
        this.id = -id
    }

    // called after notifying -> move to next notify time or deactivate
    fun justNotified() {
        if (repeat.hasValue) start += repeat
        if (type == EVENT || !repeat.hasValue || until.hasValue && start.timestamp > until.timestamp) {
            deactivate(false)
        }
        // keep last time of notification
        if (id != null && active) Thread { Database.schedDao.update(+this) }.start()
    }

    private operator fun unaryPlus() = ScheduleRow(
        id,

        start.year, start.month, start.week, start.day, start.hour, start.minute,

        repeat.year, repeat.month, repeat.week, repeat.day, repeat.hour, repeat.minute,

        until.year, until.month, until.week, until.day, until.hour, until.minute,

        desc, active, type
    )

    override fun compareTo(other: Schedule): Int =
        start.timestamp.compareTo(other.start.timestamp)

    override fun toString(): String = if (type == EVENT) {
        val ts = (start + repeat).timestamp // actual event start, now reminding of it
        "Event ${
            if (repeat.month < 1 && repeat.week < 1 && repeat.day < 1) ""
            else "${ts.dayOfMonth}.${ts.monthValue}. "
        }${ts.toLocalTime()}${if (until.hasValue) " — ${until.toString()}" else ""}"
    } else start.toString()
}

@Entity("sched")
data class ScheduleRow(
    @PrimaryKey(autoGenerate = true) var id: Int?,
    // all Timestamps are schedule-specific -> extra entity of little value
    var start_year: Int,
    var start_month: Int,
    var start_week: Int,
    var start_day: Int,
    var start_hour: Int,
    var start_minute: Int,

    var repeat_year: Int,
    var repeat_month: Int,
    var repeat_week: Int,
    var repeat_day: Int,
    var repeat_hour: Int,
    var repeat_minute: Int,

    var until_year: Int,
    var until_month: Int,
    var until_week: Int,
    var until_day: Int,
    var until_hour: Int,
    var until_minute: Int,

    var desc: String,
    var active: Boolean,
    var type: Int
) : Serializable {
    operator fun unaryPlus() = Schedule(
        id, Timestamp.build(
            0, start_month, start_week, start_day, start_hour, start_minute, start_year
        ), Timestamp.build(
            0,
            repeat_month,
            repeat_week,
            repeat_day,
            repeat_hour,
            repeat_minute,
            repeat_year
        ), Timestamp.build(
            0, until_month, until_week, until_day, until_hour, until_minute, until_year
        ), desc, active, type
    )
}

@Dao
interface ScheduleDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(sched: ScheduleRow): Long

    @Update
    fun update(sched: ScheduleRow)

    @Query("DELETE FROM sched WHERE id = :id")
    fun delete(id: Int)

    @Query("UPDATE sched SET active = 0 WHERE id = :id")
    fun deactivate(id: Int)

    @Query("SELECT * FROM sched ORDER BY id DESC")
    fun getAll(): Array<ScheduleRow>
}