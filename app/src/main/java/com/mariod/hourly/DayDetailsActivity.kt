package com.mariod.hourly

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class DayDetailsActivity : AppCompatActivity() {
    private lateinit var deleteBtn : Button
    private lateinit var editBtn : Button
    private fun calculateEarnings(startHour: String, endHour: String, wage: Double): Double {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val startTime = timeFormat.parse(startHour)
        val endTime = timeFormat.parse(endHour)

        if (startTime != null && endTime != null) {
            val differenceInMillis = endTime.time - startTime.time
            val hoursWorked = differenceInMillis / (1000 * 60 * 60).toDouble() // Convert milliseconds to hours
            return hoursWorked * wage
        }
        return 0.0
    }
    private fun calculateHours(startHour: String, endHour: String): Double {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val startTime = timeFormat.parse(startHour)
        val endTime = timeFormat.parse(endHour)

        if (startTime != null && endTime != null) {
            val differenceInMillis = endTime.time - startTime.time
            val hoursWorked = differenceInMillis / (1000 * 60 * 60).toDouble() // Convert milliseconds to hours
            return hoursWorked
        }
        return 0.0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_day_details)
        deleteBtn = findViewById(R.id.deleteBtn)
        deleteBtn.setOnClickListener { deleteButtonOnClick() }
        editBtn = findViewById(R.id.editBtn)
        editBtn.setOnClickListener { editButtonOnClick() }
        val uidDate = intent.getStringExtra("uidDate")
        val startHour = intent.getStringExtra("startHour")
        val endHour = intent.getStringExtra("endHour")
        val wage = intent.getDoubleExtra("wage", 0.0)
        if (uidDate == null || startHour == null || endHour == null) {
            finish() // Close the activity if data is missing
            return
        }

        findViewById<TextView>(R.id.dateTextView).text = getString(R.string.dateText) + " $uidDate"
        findViewById<TextView>(R.id.startHourTextView).text = getString(R.string.startTimeText) + " $startHour"
        findViewById<TextView>(R.id.endHourTextView).text = getString(R.string.endTimeText) + " $endHour"
        findViewById<TextView>(R.id.wageTextView).text = getString(R.string.wageText) + " $wage"
        findViewById<TextView>(R.id.totalHoursTextView).text = getString(R.string.totalHoursText) + " " + String.format("%.2f", calculateHours(startHour, endHour))
        findViewById<TextView>(R.id.totalPayTextView).text = getString(R.string.totalPayText) + " " + String.format("%.2f", calculateEarnings(startHour, endHour, wage))
    }

    private fun editButtonOnClick() {
        val intent = Intent(this, AddNewEntry::class.java).apply {
            putExtra("uidDate", intent.getStringExtra("uidDate"))
            putExtra("startHour", intent.getStringExtra("startHour"))
            putExtra("endHour", intent.getStringExtra("endHour"))
            putExtra("wage", intent.getDoubleExtra("wage", 0.0))
        }
        finish()
        startActivity(intent)
    }

    private fun deleteButtonOnClick() {
        val uidDate = intent.getStringExtra("uidDate") ?: return
        val db = AppDatabase.getDatabase(this)
        val dayDao = db.dayDao()

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setMessage(getString(R.string.ruSure))
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                lifecycleScope.launch {
                    dayDao.deleteDay(uidDate)
                }
                finish()
            }
            .setNegativeButton(getString(R.string.no), null)
            .show()


    }
}