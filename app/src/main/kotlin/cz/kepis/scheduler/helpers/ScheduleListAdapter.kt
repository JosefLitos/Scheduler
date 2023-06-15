package cz.kepis.scheduler.helpers

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cz.kepis.scheduler.App
import cz.kepis.scheduler.R
import cz.kepis.scheduler.databinding.ItemSchedulerBinding
import cz.kepis.scheduler.dialogs.EditScheduleDialog
import cz.kepis.scheduler.modules.Schedule
import cz.kepis.scheduler.modules.schedules

class ScheduleListAdapter : RecyclerView.Adapter<ScheduleListAdapter.ViewHolder>() {

    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        private val start: TextView
        private val description: TextView
        private val repeat: TextView
        private val frequency: TextView

        fun setSched(sched: Schedule) {
            start.setText(sched.start.toString())
            start.typeface = Typeface.defaultFromStyle(
                if (sched.active) Typeface.NORMAL else Typeface.ITALIC
            )
            description.setText(sched.desc)
            frequency.text = when (sched.type) {
                0 -> "Prior event"
                1 -> "Frequency"
                else -> "Snoozing"
            }
            repeat.setText(sched.repeat.periodString)
            view.setOnClickListener { EditScheduleDialog(view.context, sched) }
        }

        init {
            val binding = ItemSchedulerBinding.bind(view)
            start = binding.start
            description = binding.desc
            repeat = binding.repeat
            frequency = binding.repeatTitle
        }
    }

    init {
        App.app?.adapter = this
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int) = ViewHolder(
        LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.item_scheduler, viewGroup, false)
    )

    override fun onBindViewHolder(viewHolder: ViewHolder, idx: Int) {
        var sched: Schedule? = null
        synchronized(schedules) { sched = schedules[idx] }
        viewHolder.setSched(sched ?: return)
    }

    override fun getItemCount() = schedules.size

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        if (App.app?.adapter == this) App.app?.adapter = null
        super.onDetachedFromRecyclerView(recyclerView)
    }
}
