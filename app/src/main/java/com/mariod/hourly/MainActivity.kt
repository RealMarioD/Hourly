package com.mariod.hourly

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.launch

@Entity(tableName = "hours_database")
data class Day(
    @PrimaryKey val uidDate: String,
    @ColumnInfo(name = "startHour") val startHour: String,
    @ColumnInfo(name = "endHour") val endHour: String,
    @ColumnInfo(name = "wage") val wage: Double,
)

@Dao
interface DayDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDay(day: Day)

    @Query("SELECT * FROM hours_database ORDER BY uidDate DESC")
    suspend fun getAllDays(): List<Day>

    @Query("SELECT * FROM hours_database ORDER BY uidDate DESC LIMIT 1")
    suspend fun getLastSavedDay(): List<Day>
    // You can add query methods here as needed.

    @Query("DELETE FROM hours_database WHERE uidDate = :uidDate")
    suspend fun deleteDay(uidDate: String)
}

@Database(entities = [Day::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dayDao(): DayDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hours_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class MainActivity : AppCompatActivity() {
    private lateinit var hoursList: ListView
    private lateinit var noHoursText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        hoursList = findViewById(R.id.hours)
        noHoursText = findViewById(R.id.noHoursText)

        fillList()
    }

    override fun onResume() {
        super.onResume()
        hoursList.adapter = null
        fillList()
        if(hoursList.adapter == null) noHoursText.visibility = View.VISIBLE
        else noHoursText.visibility = View.GONE
    }

    private fun fillList() {
        val db = AppDatabase.getDatabase(this)
        val dayDao = db.dayDao()

        // Use Kotlin coroutine to fetch data and update the ListView
        lifecycleScope.launch {
            val days = dayDao.getAllDays()
            val adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_list_item_1,
                days.map { "${it.uidDate}\n${it.startHour} - ${it.endHour}" }
            )
            if(!adapter.isEmpty) {
                Log.d("MainActivity", "Entries found: ${adapter.count}")
                noHoursText.visibility = View.GONE
            }
            hoursList.adapter = adapter
            hoursList.setOnItemClickListener { _, _, position, _ ->
                val selectedDay = days[position]
                val intent = Intent(this@MainActivity, DayDetailsActivity::class.java)
                intent.putExtra("uidDate", selectedDay.uidDate)
                intent.putExtra("startHour", selectedDay.startHour)
                intent.putExtra("endHour", selectedDay.endHour)
                intent.putExtra("wage", selectedDay.wage)
                startActivity(intent)
            }
        }
    }



    fun switchActivity(view: View?) {
        val intent = Intent(this, AddNewEntry::class.java)
        startActivity(intent)
    }
}