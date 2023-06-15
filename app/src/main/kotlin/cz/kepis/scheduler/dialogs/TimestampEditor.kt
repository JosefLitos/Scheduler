package cz.kepis.scheduler.dialogs

import android.view.View
import android.widget.TextView
import cz.kepis.scheduler.databinding.EditTimestampBinding
import cz.kepis.scheduler.modules.Timestamp
import kotlin.math.abs

class TimestampEditor(root: View, ts: Timestamp) {
    var base = 0
        set(it) {
            field = it
            monthTitle.text = if (0 < abs(it)) "Months" else "Month"
            weekTitle.text = if (1 < abs(it)) "Weeks" else "Week"
            dayTitle.text = if (2 < abs(it)) "Days" else "Day"
            hourTitle.text = if (3 < abs(it)) "Hours" else "Hour"
            minuteTitle.text = if (4 < abs(it)) "Minutes" else "Minute"
        }

    private val monthTitle: TextView
    private val monthValue: TextView
    private val weekTitle: TextView
    private val weekValue: TextView
    private val dayTitle: TextView
    private val dayValue: TextView
    private val hourTitle: TextView
    private val hourValue: TextView
    private val minuteTitle: TextView
    private val minuteValue: TextView

    private val year = ts.year

    init {
        val binding = EditTimestampBinding.bind(root)
        monthTitle = binding.monthTitle
        monthValue = binding.monthValue
        weekTitle = binding.weekTitle
        weekValue = binding.weekValue
        dayTitle = binding.dayTitle
        dayValue = binding.dayValue
        hourTitle = binding.hourTitle
        hourValue = binding.hourValue
        minuteTitle = binding.minuteTitle
        minuteValue = binding.minuteValue
        if (ts.month != 0) monthValue.setText(ts.month.toString())
        if (ts.week != 0) weekValue.setText(ts.week.toString())
        if (ts.day != 0) dayValue.setText(ts.day.toString())
        if (ts.hour != 0) hourValue.setText(ts.hour.toString())
        if (ts.minute != 0) minuteValue.setText(ts.minute.toString())
    }

    fun collect() = Timestamp.build(
        base,
        monthValue.text.toString().toIntOrNull() ?: 0,
        weekValue.text.toString().toIntOrNull() ?: 0,
        dayValue.text.toString().toIntOrNull() ?: 0,
        hourValue.text.toString().toIntOrNull() ?: 0,
        minuteValue.text.toString().toIntOrNull() ?: 0,
        if (base > 0) year else 0,
    )
}