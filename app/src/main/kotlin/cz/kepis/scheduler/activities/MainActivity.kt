package cz.kepis.scheduler.activities

import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import cz.kepis.scheduler.App
import cz.kepis.scheduler.R
import cz.kepis.scheduler.databinding.ActivityMainBinding
import cz.kepis.scheduler.dialogs.EditScheduleDialog
import cz.kepis.scheduler.helpers.ScheduleListAdapter

class MainActivity : AppCompatActivity() {
    companion object {
        const val P_NOTIFICATION = 1
    }

    private var dialog: Dialog? = null

    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        binding.schedulerView.adapter = ScheduleListAdapter()

        binding.schedulerFab.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ActivityCompat.checkSelfPermission(
                    this, android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) requestPermissions(
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), P_NOTIFICATION
            )

            dialog = EditScheduleDialog(this, onDismiss = { dialog = null })
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(applicationContext, SettingsActivity::class.java))
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onRequestPermissionsResult(
        code: Int, permission: Array<out String>, access: IntArray
    ) {
        if (access.isNotEmpty() && code == P_NOTIFICATION) App.doNotify =
            access[0] == PackageManager.PERMISSION_GRANTED
        super.onRequestPermissionsResult(code, permission, access)
    }

    override fun onDestroy() {
        if (dialog != null) dialog?.dismiss()
        super.onDestroy()
    }
//        lifecycleScope.launch {
//            Snackbar.make(
//                view, "Val=${removeAfterDone.first()}", Snackbar.LENGTH_LONG
//            ).setAnchorView(R.id.scheduler_fab).setAction("Action", null).show()
//        }
//        removeAfterDone.asLiveData().observe(this) {
//            Toast.makeText(applicationContext, "Val=${it}", Toast.LENGTH_SHORT).show()
//        }
}
