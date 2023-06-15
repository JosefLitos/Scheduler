package cz.kepis.scheduler.dialogs

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View.GONE
import android.widget.Button
import cz.kepis.scheduler.R
import cz.kepis.scheduler.databinding.DialogEditScheduleBinding
import cz.kepis.scheduler.helpers.Settings
import cz.kepis.scheduler.helpers.TSBaseChangeListener
import cz.kepis.scheduler.modules.Schedule

class EditScheduleDialog(
    context: Context, schedule: Schedule? = null, val onDismiss: () -> Unit = {}
) : Dialog(context) {

    private val sched = schedule ?: Schedule(null, type = Settings.type)

    init {
        val root =
            LayoutInflater.from(context).inflate(R.layout.dialog_edit_schedule, null)
        val binding = DialogEditScheduleBinding.bind(root)
        setContentView(root)
        setTitle("Edit Schedule")

        val start = TimestampEditor(
            binding.startTimestamp.root,
            if (sched.type == 0) sched.start + sched.repeat else sched.start
        )
        val repTitle = binding.repeatTitle
        val repeat = TimestampEditor(binding.repeatTimestamp.root, sched.repeat)
        val until = TimestampEditor(binding.untilTimestamp.root, sched.until)
        val desc = binding.description

        binding.startBaseValue.apply {
            setOnSeekBarChangeListener(TSBaseChangeListener(
                binding.startBaseLabel
            ) { start.base = it })
            if (schedule != null) {
                progress = 0
                min = 0
            } else {
                progress = Settings.startBase
                min = 0
            }
        }
        repeat.base = -6 // to initiate repeat field descriptions
        binding.untilBaseValue.apply {
            setOnSeekBarChangeListener(TSBaseChangeListener(
                binding.untilBaseLabel
            ) { until.base = it })
            if (schedule != null) {
                progress = 0
                min = 0
            } else {
                progress = Settings.untilBase
                min = 0
            }
        }
        if (schedule != null) desc.setText(sched.desc)
        binding.type.apply {
            val handler = {
                text = when (sched.type) {
                    0 -> {
                        repTitle.text = "Remind prior by"
                        "Event"
                    }

                    1 -> {
                        repTitle.text = "Occurs every"
                        "Silent Reminder"
                    }

                    else -> {
                        repTitle.text = "Repeat every"
                        "Alarm"
                    }
                }
            }
            handler()
            setOnClickListener {
                it as Button
                sched.type = (sched.type + 1) % 3
                handler()
            }
        }
        if (schedule == null) sched.active = true
        binding.active.isChecked = sched.active

        binding.cancel.setOnClickListener { dismiss() }
        if (schedule != null) binding.delete.setOnClickListener {
            Thread({ schedule.delete() }, "EditorDeleteBtn").start()
            dismiss()
        }
        else binding.delete.visibility = GONE
        binding.ok.setOnClickListener {
            val active = binding.active.isChecked
            val description = desc.text.toString()
            Thread({
                val startVal = start.collect()
                sched.update(
                    startVal,
                    repeat.collect(),
                    if (until.base == 6) {
                        until.base = 0
                        startVal + until.collect()
                    } else until.collect(),
                    desc.text.toString(),
                    binding.active.isChecked,
                    sched.type
                )
            }, "EditorOkBtn").start()
            dismiss()
        }
        show()
    }

    override fun dismiss() {
        onDismiss()
        super.dismiss()
    }
}