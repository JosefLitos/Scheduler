package cz.kepis.scheduler

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager.STREAM_ALARM
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.bumptech.glide.util.Util
import com.google.android.material.R
import cz.kepis.scheduler.activities.MainActivity
import cz.kepis.scheduler.helpers.ScheduleListAdapter
import cz.kepis.scheduler.helpers.Settings
import cz.kepis.scheduler.modules.Database
import cz.kepis.scheduler.modules.Schedule
import cz.kepis.scheduler.modules.Schedule.Type.ALARM
import cz.kepis.scheduler.modules.Schedule.Type.EVENT
import cz.kepis.scheduler.modules.Schedule.Type.REMINDER
import cz.kepis.scheduler.modules.ScheduleEvent
import cz.kepis.scheduler.modules.schedules
import cz.kepis.scheduler.modules.syncObj
import kotlinx.coroutines.runBlocking
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.time.LocalDateTime
import java.time.ZoneOffset

class App : Application() {

    private lateinit var openingIntent: PendingIntent
    var adapter: ScheduleListAdapter? = null

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate() {
        super.onCreate()
        app = this
        handler = Handler.createAsync(Looper.getMainLooper())
        Thread(this::loadApp, "AppLoader").start()
        doNotify = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true
        openingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }, PendingIntent.FLAG_IMMUTABLE
        )

        bus.register(this)
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).apply {
            createNotificationChannel(
                NotificationChannel(
                    EVENT_CHANNEL_ID,
                    "Scheduled event",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
            val alarmChan = NotificationChannel(
                ALARM_CHANNEL_ID, "Scheduled alarm", NotificationManager.IMPORTANCE_HIGH
            )
            alarmChan.setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setLegacyStreamType(STREAM_ALARM)
                    .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED).build()
            )
            createNotificationChannel(alarmChan)
        }

    }

    companion object {
        const val EVENT_CHANNEL_ID = "cz.kepis.scheduler.eventid"
        const val ALARM_CHANNEL_ID = "cz.kepis.scheduler.alarmid"
        const val EVENT_GROUP = "cz.kepis.scheduler.Event"
        var doNotify = false // permission for notifying

        var app: App? = null
            private set
        val bus = EventBus.getDefault()
        lateinit var handler: Handler
    }

    private fun loadApp() = runBlocking {
        Settings.loadSettings(this@App)
        Database.loadDb(this@App).schedDao.getAll().forEach { bus.post(+it) }
        synchronized(syncObj) { schedules.sort() }
        Util.postOnUiThread { adapter?.notifyDataSetChanged() }
        scheduleNextSchedule(0)
    }

    private fun scheduleNextSchedule(safeIndex: Int) = synchronized(syncObj) {
        for (i in safeIndex until schedules.size) if (schedules[i].active) {
            val sched = schedules[i]
            val hashCode = schedules.hashCode()
            handler.removeCallbacksAndMessages(schedules)
            val diff =
                sched.start.timestamp.toEpochSecond(ZoneOffset.UTC) - LocalDateTime.now()
                    .toEpochSecond(ZoneOffset.UTC)
            handler.postAtTime(
                { Thread(this::onNotifyTime, "BackgroundNotifier").start() },
                schedules,
                SystemClock.uptimeMillis() + diff * 1000
            )
            return
        }
    }

    private fun onNotifyTime() {
        val now = LocalDateTime.now()
        val toNotify = ArrayList<Schedule>()
        var i = 0
        synchronized(syncObj) {
            for (s in schedules) {
                if (s.active) {
                    if (s.start.timestamp <= now) toNotify.add(s)
                    else break
                } else ++i
            }
        }
        Util.postOnUiThread {
            for (sched in toNotify) {
                onScheduleNotify(ScheduleEvent.Notify(sched))
                sched.justNotified()
            }
            adapter?.notifyDataSetChanged()
        }
        scheduleNextSchedule(i)
    }

    override fun onTerminate() {
        EventBus.getDefault().unregister(this)
        if (app == this) app = null
        super.onTerminate()
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onScheduleLoaded(e: ScheduleEvent.Loaded) = synchronized(syncObj) {
        val pos = schedules.size
        schedules.add(e.sched)
        if (adapter != null) Util.postOnUiThread { adapter?.notifyItemInserted(pos) }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onScheduleCreated(e: ScheduleEvent.Created) {
        synchronized(syncObj) {
            for (i in 0 until schedules.size) if (e.sched <= schedules[i]) {
                schedules.add(i, e.sched)
                if (adapter != null) Util.postOnUiThread {
                    adapter?.notifyItemInserted(i)
                }
                return
            }
            schedules.add(e.sched)
            if (adapter != null) Util.postOnUiThread {
                adapter?.notifyItemInserted(schedules.size - 1)
            }
        }
        scheduleNextSchedule(0)
    }


    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onScheduleUpdated(e: ScheduleEvent.Updated) {
        synchronized(syncObj) {
            val pos = schedules.indexOf(e.sched)
            if (pos == -1) return
            if (pos > 0 && schedules[pos - 1] > e.sched) {
                schedules.removeAt(pos)
                for (i in pos - 1 downTo 0) if (e.sched >= schedules[i]) {
                    schedules.add(i + 1, e.sched)
                    if (adapter != null) Util.postOnUiThread {
                        adapter?.notifyItemMoved(pos, i + 1)
                        adapter?.notifyItemChanged(i + 1)
                    }
                    return
                }
                schedules.add(0, e.sched)
                if (adapter != null) Util.postOnUiThread {
                    adapter?.notifyItemMoved(pos, 0)
                    adapter?.notifyItemChanged(0)
                }
            } else if (pos < schedules.size - 1 && e.sched > schedules[pos + 1]) {
                schedules.removeAt(pos)
                for (i in pos + 1 until schedules.size) if (e.sched <= schedules[i]) {
                    schedules.add(i, e.sched)
                    if (adapter != null) Util.postOnUiThread {
                        adapter?.notifyItemMoved(pos, i)
                        adapter?.notifyItemChanged(i)
                    }
                    return
                }
                val i = schedules.size
                schedules.add(e.sched)
                if (adapter != null) Util.postOnUiThread {
                    adapter?.notifyItemMoved(pos, i)
                    adapter?.notifyItemChanged(i)
                }
            } else if (adapter != null) Util.postOnUiThread {
                adapter?.notifyItemChanged(pos)
            }
        }
        scheduleNextSchedule(0)
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onScheduleDeleted(e: ScheduleEvent.Deleted) {
        synchronized(syncObj) {
            val pos = schedules.indexOf(e.sched)
            if (pos == -1) return
            schedules.removeAt(pos)
            if (adapter != null) Util.postOnUiThread { adapter?.notifyItemRemoved(pos) }
        }
        scheduleNextSchedule(0)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onScheduleNotify(e: ScheduleEvent.Notify) {
        if (doNotify) try {
            val builder = if (e.sched.type == ALARM) NotificationCompat.Builder(
                this, ALARM_CHANNEL_ID
            ).setSmallIcon(R.drawable.ic_clock_black_24dp).setOngoing(true)
                .setAutoCancel(true)
            else NotificationCompat.Builder(this, EVENT_CHANNEL_ID).setSmallIcon(
                R.drawable.material_ic_calendar_black_24dp
            ).setOngoing(e.sched.type == EVENT).setPriority(
                if (e.sched.type == REMINDER) NotificationManager.IMPORTANCE_LOW
                else NotificationManager.IMPORTANCE_HIGH
            ).setAutoCancel(e.sched.type == EVENT).setGroup(EVENT_GROUP)
                .setSilent(e.sched.type == REMINDER)

            builder.setContentTitle(e.sched.toString()).setContentText(e.sched.desc)
                .setContentIntent(openingIntent)

            NotificationManagerCompat.from(this).notify(
                if ((e.sched.id ?: 0) < 0) -(e.sched.id!!) else e.sched.id!!,
                builder.build()
            )
        } catch (_: SecurityException) {
        }
    }


}