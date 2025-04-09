package com.mariod.hourly

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun calculateHours(startHour: String, endHour: String): Double {
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

class AddNewEntry : AppCompatActivity() {
    private lateinit var date: TextView
    private lateinit var startTime: TextView
    private lateinit var endTime: TextView
    private lateinit var wage: EditText
    private lateinit var changeDateBtn: Button
    private lateinit var changeStartTimeBtn: Button
    private lateinit var changeEndTimeBtn: Button
    private lateinit var finalizeButton: Button
    private lateinit var allDays: List<Day>
    private var editing = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_new_entry)

        date = findViewById(R.id.dateText)
        startTime = findViewById(R.id.startTime)
        endTime = findViewById(R.id.endTime)
        wage = findViewById(R.id.editText)

        changeDateBtn = findViewById(R.id.changeDateBtn)
        changeStartTimeBtn = findViewById(R.id.changeStartTimeBtn)
        changeEndTimeBtn = findViewById(R.id.changeEndTimeBtn)
        finalizeButton = findViewById(R.id.finalizeAdd)


        // did we get here by clicking the edit button?
        val uidDate = intent.getStringExtra("uidDate")
        val startHour = intent.getStringExtra("startHour")
        val endHour = intent.getStringExtra("endHour")
        val wageValue = intent.getDoubleExtra("wage", 0.0)
        if (uidDate != null && startHour != null && endHour != null) {
            editing = true
            date.text = uidDate
            startTime.text = startHour
            endTime.text = endHour
            wage.setText(wageValue.toString())
            finalizeButton.text = getString(R.string.saveText)
        }
        else {
            // setting the defaults
            date.text = SimpleDateFormat("yyyy-MM-dd").format(Date())
            val db = AppDatabase.getDatabase(this)
            val dayDao = db.dayDao()

            lifecycleScope.launch {
                val lastSaved = dayDao.getLastSavedDay()
                if (lastSaved.isNotEmpty()) {
                    startTime.text = lastSaved[0].startHour
                    endTime.text = lastSaved[0].endHour
                    wage.setText(lastSaved[0].wage.toString())
                } else {
                    startTime.text = "08:00"
                    endTime.text = "16:00"
                }
                allDays = dayDao.getAllDays()
            }
        }
        if(editing) changeDateBtn.isEnabled = false
        changeDateBtn.setOnClickListener { showDateChangeDialog(date.text.toString().split("-")) }
        changeStartTimeBtn.setOnClickListener { showStartTimeDialog(startTime.text.toString().split(":"))}
        changeEndTimeBtn.setOnClickListener { showEndTimeDialog(endTime.text.toString().split(":"))}

        // save the entry in "room database"
        finalizeButton.setOnClickListener { saveEntry() }
    }

    private fun showDateChangeDialog(dateText: List<String>) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                date.text = selectedDate
            },
            dateText[0].toInt(),
            dateText[1].toInt() - 1,
            dateText[2].toInt())
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun showStartTimeDialog(time: List<String>) {
        val timePickerDialog = TimePickerDialog(
            this,
            { _, selectedHour, selectedMinute ->
                val selectedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                if(calculateHours(selectedTime, endTime.text.toString()) <= 0) {
                    Toast.makeText(this, getString(R.string.startTimeError), Toast.LENGTH_SHORT).show()
                    return@TimePickerDialog
                }
                startTime.text = selectedTime
            },
            time[0].toInt(),
            time[1].toInt(),
            true
        )
        timePickerDialog.show()
    }

    private fun showEndTimeDialog(time: List<String>) {
        val calendar = Calendar.getInstance()
        val timePickerDialog = TimePickerDialog(
            this,
            { _, selectedHour, selectedMinute ->
                val selectedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                if(calculateHours(startTime.text.toString(), selectedTime) <= 0) {
                    Toast.makeText(this, getString(R.string.endTimeError), Toast.LENGTH_SHORT).show()
                    return@TimePickerDialog
                }
                endTime.text = selectedTime
            },
            time[0].toInt(),
            time[1].toInt(),
            true
        )
        timePickerDialog.show()
    }

    private fun saveEntry() {
        val selectedDate = date.text.toString()
        val selectedStartTime = startTime.text.toString()
        val selectedEndTime = endTime.text.toString()
        val selectedWage: Number
        try {
            selectedWage = wage.text.toString().toDouble()
            if(selectedWage <= 0) {
                Toast.makeText(this, getString(R.string.wageError), Toast.LENGTH_SHORT).show()
                return
            }
        }
        catch (e: NumberFormatException) {
            Toast.makeText(this, getString(R.string.wageError), Toast.LENGTH_SHORT).show()
            return
        }

        val day = Day(selectedDate, selectedStartTime, selectedEndTime, selectedWage)

        val db = AppDatabase.getDatabase(this)
        val dayDao = db.dayDao()

        if(!editing) {
            if (allDays.any { it.uidDate == selectedDate }) {
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle(getString(R.string.alreadyExistsWindowTitle))
                    .setMessage(getString(R.string.alreadyExistsWindowText))
                    .setPositiveButton(getString(R.string.yes)) { _, _ ->
                        lifecycleScope.launch {
                            dayDao.insertDay(day)
                        }
                        Toast.makeText(
                            this,
                            getString(R.string.entrySavedToast),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .setNegativeButton(getString(R.string.no), null)
                    .show()
                return
            }
        }

        lifecycleScope.launch {
            dayDao.insertDay(day)
            allDays = dayDao.getAllDays()
        }
        Toast.makeText(this, getString(R.string.entrySavedToast), Toast.LENGTH_SHORT).show()
        if(editing) finish()
    }
}