package com.example.weatherflexalarm

import android.Manifest
import android.app.AlarmManager
import android.app.TimePickerDialog
import android.media.AudioManager
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.ZonedDateTime
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var timeValueText: TextView
    private lateinit var advanceMinutesInput: EditText
    private lateinit var rainCheck: CheckBox
    private lateinit var snowCheck: CheckBox
    private lateinit var latitudeInput: EditText
    private lateinit var longitudeInput: EditText
    private lateinit var statusText: TextView
    private lateinit var requestExactAlarmPermissionButton: Button

    private var selectedHour = 7
    private var selectedMinute = 30

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bindViews()
        requestNotificationPermissionIfNeeded()

        val settings = SettingsStore.load(this)
        selectedHour = settings.baseHour
        selectedMinute = settings.baseMinute
        advanceMinutesInput.setText(settings.advanceMinutes.toString())
        rainCheck.isChecked = settings.advanceOnRain
        snowCheck.isChecked = settings.advanceOnSnow
        latitudeInput.setText(settings.latitude.toString())
        longitudeInput.setText(settings.longitude.toString())
        renderTime()
        renderStatus()

        findViewById<Button>(R.id.pickTimeButton).setOnClickListener {
            TimePickerDialog(
                this,
                { _, hourOfDay, minute ->
                    selectedHour = hourOfDay
                    selectedMinute = minute
                    renderTime()
                },
                selectedHour,
                selectedMinute,
                true
            ).show()
        }

        findViewById<Button>(R.id.useLocationButton).setOnClickListener {
            fillLocationFromDevice()
        }

        requestExactAlarmPermissionButton.setOnClickListener {
            requestExactAlarmPermissionIfNeeded()
        }

        findViewById<Button>(R.id.saveButton).setOnClickListener {
            saveAndSchedule()
        }

        findViewById<Button>(R.id.testRingButton).setOnClickListener {
            AlarmRingingService.start(this, "手动测试闹钟")
            statusText.text = withAlarmVolumeHint("已触发测试响铃，请点通知里的“停止闹钟”结束")
        }

        findViewById<Button>(R.id.stopRingButton).setOnClickListener {
            AlarmRingingService.stop(this)
            statusText.text = withAlarmVolumeHint("已请求停止响铃")
        }
    }

    override fun onResume() {
        super.onResume()
        updateExactAlarmPermissionButton()
        renderStatus()
    }

    private fun bindViews() {
        timeValueText = findViewById(R.id.timeValueText)
        advanceMinutesInput = findViewById(R.id.advanceMinutesInput)
        rainCheck = findViewById(R.id.rainCheck)
        snowCheck = findViewById(R.id.snowCheck)
        latitudeInput = findViewById(R.id.latitudeInput)
        longitudeInput = findViewById(R.id.longitudeInput)
        statusText = findViewById(R.id.statusText)
        requestExactAlarmPermissionButton = findViewById(R.id.requestExactAlarmPermissionButton)
    }

    private fun renderTime() {
        timeValueText.text = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute)
    }

    private fun saveAndSchedule() {
        val current = SettingsStore.load(this)

        val advanceMinutes = advanceMinutesInput.text.toString().toIntOrNull()?.coerceAtLeast(0)
            ?: current.advanceMinutes
        val latitude = latitudeInput.text.toString().toDoubleOrNull() ?: current.latitude
        val longitude = longitudeInput.text.toString().toDoubleOrNull() ?: current.longitude

        val settings = AlarmSettings(
            baseHour = selectedHour,
            baseMinute = selectedMinute,
            advanceMinutes = advanceMinutes,
            advanceOnRain = rainCheck.isChecked,
            advanceOnSnow = snowCheck.isChecked,
            latitude = latitude,
            longitude = longitude
        )

        SettingsStore.save(this, settings)
        // Always schedule a local base alarm immediately so ringing does not depend on network/weather worker timing.
        val baseTime = AlarmMath.nextBaseAlarmTime(settings, ZonedDateTime.now())
        AlarmScheduler(this).schedule(baseTime, "基础闹钟（已保存）")
        WeatherWorkScheduler.scheduleAll(this)
        statusText.text = withAlarmVolumeHint("已保存，正在按天气重算闹钟...")
    }

    private fun fillLocationFromDevice() {
        val locationPermissionsGranted =
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!locationPermissionsGranted) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
                REQ_LOCATION
            )
            return
        }

        val location = LocationHelper.lastKnownLocation(this)
        if (location != null) {
            latitudeInput.setText(location.latitude.toString())
            longitudeInput.setText(location.longitude.toString())
            statusText.text = withAlarmVolumeHint("已填入当前位置")
        } else {
            statusText.text = withAlarmVolumeHint("未拿到定位，请手动填写经纬度")
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQ_NOTIFICATION
            )
        }
    }

    private fun requestExactAlarmPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val alarmManager = getSystemService(AlarmManager::class.java)
        if (alarmManager.canScheduleExactAlarms()) {
            statusText.text = withAlarmVolumeHint("精确闹钟权限已授予")
            return
        }

        val intent = android.content.Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.parse("package:$packageName")
        }
        startActivity(intent)
    }

    private fun updateExactAlarmPermissionButton() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            requestExactAlarmPermissionButton.isEnabled = false
            requestExactAlarmPermissionButton.text = "当前系统无需此权限"
            return
        }

        val alarmManager = getSystemService(AlarmManager::class.java)
        val granted = alarmManager.canScheduleExactAlarms()
        requestExactAlarmPermissionButton.isEnabled = !granted
        requestExactAlarmPermissionButton.text = if (granted) {
            "精确闹钟权限已授予"
        } else {
            getString(R.string.request_exact_alarm_permission)
        }
    }

    private fun renderStatus() {
        val (millis, reason) = SettingsStore.loadLastScheduled(this)
        if (millis <= 0L) {
            statusText.text = withAlarmVolumeHint("还没有生成下一次闹钟")
            return
        }

        val time = Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        statusText.text = withAlarmVolumeHint("下次闹钟：$time\n原因：$reason")
    }

    private fun withAlarmVolumeHint(base: String): String {
        val audioManager = getSystemService(AudioManager::class.java)
        val alarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
        return if (alarmVolume == 0) {
            "$base\n提醒：系统闹钟音量当前为 0，请先调高闹钟音量"
        } else {
            base
        }
    }

    companion object {
        private const val REQ_LOCATION = 2001
        private const val REQ_NOTIFICATION = 2002
    }
}
