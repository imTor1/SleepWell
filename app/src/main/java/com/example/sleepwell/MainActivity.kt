package com.example.sleepwell

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.*
import androidx.appcompat.app.AlertDialog
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class MainActivity : AppCompatActivity() {
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val ageEditText: EditText = findViewById(R.id.editTextAge)
        val genderSpinner: Spinner = findViewById(R.id.spinnerGender)
        val dietaryHabitsSpinner: Spinner = findViewById(R.id.spinnerDietaryHabits)
        val sleepDisordersSpinner: Spinner = findViewById(R.id.spinnerSleepDisorders)
        val medicationUsageSpinner: Spinner = findViewById(R.id.spinnerMedicationUsage)
        val bedtimeEditText: EditText = findViewById(R.id.editTextBedtime)
        val wakeupTimeEditText: EditText = findViewById(R.id.editTextWakeupTime)
        val sleepQualitySpinner: Spinner = findViewById(R.id.spinnerSleepQuality)
        val dailyStepsEditText: EditText = findViewById(R.id.editTextDailySteps)
        val physicalActivitySpinner: Spinner = findViewById(R.id.spinnerPhysicalActivity)
        val submitButton: Button = findViewById(R.id.buttonSubmit)

        submitButton.setOnClickListener {
            val age = ageEditText.text.toString().trim()
            val gender = genderSpinner.selectedItem.toString().lowercase()
            val dietaryHabits = dietaryHabitsSpinner.selectedItem.toString().lowercase()
            val sleepDisorders = sleepDisordersSpinner.selectedItem.toString().lowercase()
            val medicationUsage = medicationUsageSpinner.selectedItem.toString().lowercase()
            val bedtime = bedtimeEditText.text.toString().trim()
            val wakeupTime = wakeupTimeEditText.text.toString().trim()
            val sleepQuality = sleepQualitySpinner.selectedItem.toString().lowercase()
            val dailySteps = dailyStepsEditText.text.toString().trim()
            val physicalActivity = physicalActivitySpinner.selectedItem.toString().lowercase()

            // Input validation
            if (age.isEmpty() || bedtime.isEmpty() || wakeupTime.isEmpty() || dailySteps.isEmpty()) {
                Toast.makeText(
                    this@MainActivity,
                    "Please fill in all required fields.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            try {
                val ageNumber = age.toInt()
                val bedtimeNumber = bedtime.toInt()
                val wakeupTimeNumber = wakeupTime.toInt()
                val dailyStepsNumber = dailySteps.toInt()

                // Check if the numbers are in reasonable ranges
                if (ageNumber < 0 || bedtimeNumber < 0 || bedtimeNumber > 24 || wakeupTimeNumber < 0 || wakeupTimeNumber > 24 || dailyStepsNumber < 0) {
                    Toast.makeText(
                        this@MainActivity,
                        "Please enter valid numbers for age, bedtime, wake-up time, and daily steps.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                // Send data to Flask API
                sendDataToFlaskAPI(
                    age,
                    gender,
                    dietaryHabits,
                    sleepDisorders,
                    medicationUsage,
                    bedtime,
                    wakeupTime,
                    sleepQuality,
                    dailySteps,
                    physicalActivity
                )

            } catch (e: NumberFormatException) {
                Toast.makeText(
                    this@MainActivity,
                    "Please enter valid numbers for age, bedtime, wake-up time, and daily steps.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
        private fun sendDataToFlaskAPI(
            age: String,
            gender: String,
            dietaryHabits: String,
            sleepDisorders: String,
            medicationUsage: String,
            bedtime: String,
            wakeupTime: String,
            sleepQuality: String,
            dailySteps: String,
            physicalActivity: String
        ) {
            val jsonObject = JSONObject()
            jsonObject.put("age", age)
            jsonObject.put("gender", gender)
            jsonObject.put("dietary_habits", dietaryHabits)
            jsonObject.put("sleep_disorders", sleepDisorders)
            jsonObject.put("medication_usage", medicationUsage)
            jsonObject.put("bedtime", bedtime)
            jsonObject.put("wake_up_time", wakeupTime)
            jsonObject.put("sleep_quality", sleepQuality)
            jsonObject.put("daily_steps", dailySteps)
            jsonObject.put("physical_activity_level", physicalActivity)

            val jsonString = jsonObject.toString()

            // Log JSON string for debugging
            println("Sending JSON: $jsonString")

            val requestBody = jsonString.toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url("http://192.168.1.101:3000/api/sleephealth")
                .post(requestBody)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Failed to connect to server", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    // Log response code for debugging
                    println("Response code: ${response.code}")

                    if (response.isSuccessful) {
                        val responseData = response.body?.string()
                        if (responseData != null) {
                            try {
                                val jsonObject = JSONObject(responseData)
                                val result = jsonObject.getString("sleep_health_status")

                                runOnUiThread {
                                    val message = "ผลลัพธ์จากการทำนายสุขภาพการนอน: $result"
                                    val builder = AlertDialog.Builder(this@MainActivity)
                                    builder.setTitle("ผลการทำนายสุขภาพการนอน")
                                    builder.setMessage(message)
                                    builder.setNeutralButton("OK") { dialog, _ ->
                                        dialog.dismiss()
                                    }
                                    val alert = builder.create()
                                    alert.show()
                                }
                            } catch (e: Exception) {
                                runOnUiThread {
                                    Toast.makeText(this@MainActivity, "Failed to parse response.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            runOnUiThread {
                                Toast.makeText(this@MainActivity, "Empty response from server.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "Error: ${response.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            })
        }
    }
