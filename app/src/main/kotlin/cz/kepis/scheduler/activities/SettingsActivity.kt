package cz.kepis.scheduler.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import cz.kepis.scheduler.R
import cz.kepis.scheduler.databinding.ActivitySettingsBinding
import cz.kepis.scheduler.helpers.Settings
import cz.kepis.scheduler.helpers.TSBaseChangeListener
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        binding.startBaseValue.apply {
            setOnSeekBarChangeListener(TSBaseChangeListener(binding.startBaseLabel) {})
            progress = Settings.startBase
            min = 0
        }
        binding.untilBaseValue.apply {
            setOnSeekBarChangeListener(TSBaseChangeListener(binding.untilBaseLabel) {})
            progress = Settings.untilBase
            min = 0
        }
        binding.removeAfterDone.isChecked = Settings.removeAfterDone
        binding.removeAfterDoneHolder.setOnClickListener {
            binding.removeAfterDone.isChecked = !binding.removeAfterDone.isChecked
        }
    }

    override fun onDestroy() {
        val removeAfterDone = binding.removeAfterDone.isChecked
        val startBase = binding.startBaseValue.progress
        val untilBase = binding.untilBaseValue.progress
        runBlocking {// using lifecycleScope cuts execution (we're already ending)
            launch { // writing changes can be postponed
                Settings.setRemoveAfterDone(removeAfterDone)
                Settings.setStartBase(startBase)
                Settings.setUntilBase(untilBase)
            }
            super.onDestroy()
        }
    }
}
