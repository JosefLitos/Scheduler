package cz.kepis.scheduler.modules

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import cz.kepis.scheduler.App

@Database(entities = [ScheduleRow::class], version = 1)
abstract class Database : RoomDatabase() {
    companion object {
        private lateinit var db: cz.kepis.scheduler.modules.Database
        lateinit var schedDao: ScheduleDAO

        fun loadDb(app: App): Companion {
            if (::db.isInitialized) return this

            db = Room.databaseBuilder(
                app, cz.kepis.scheduler.modules.Database::class.java, "scheduler"
            ).build()
            schedDao = db.schedDao()

            return this
        }
    }

    abstract fun schedDao(): ScheduleDAO
}

val schedules: ArrayList<Schedule> = ArrayList()
val syncObj = Any()