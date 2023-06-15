package cz.kepis.scheduler.modules

sealed class ScheduleEvent private constructor(val sched: Schedule) {
    class Loaded(sched: Schedule) : ScheduleEvent(sched)
    class Created(sched: Schedule) : ScheduleEvent(sched)
    class Updated(sched: Schedule) : ScheduleEvent(sched)
    class Deleted(sched: Schedule) : ScheduleEvent(sched)
    class Notify(sched: Schedule) : ScheduleEvent(sched)
}