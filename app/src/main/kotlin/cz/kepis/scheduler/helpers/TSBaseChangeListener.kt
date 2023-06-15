package cz.kepis.scheduler.helpers

import android.widget.SeekBar
import android.widget.TextView

inline fun TSBaseChangeListener(label: TextView, crossinline setter: (Int) -> Unit) =
    object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, state: Int, fromUser: Boolean) {
            setter(state)
            label.setText(
                when (state) {
                    0 -> "From this year"
                    1 -> "From this month"
                    2 -> "From this week"
                    3 -> "From today"
                    4 -> "From this hour"
                    5 -> "From now"
                    else -> "From start"
                }
            )

        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {}
        override fun onStopTrackingTouch(seekBar: SeekBar?) {}
    }