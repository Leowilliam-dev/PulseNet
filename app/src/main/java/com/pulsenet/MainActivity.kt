package com.pulsenet

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var testButton: Button
    private lateinit var statusText: TextView
    private lateinit var pingText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        testButton = findViewById(R.id.testButton)
        statusText = findViewById(R.id.statusText)
        pingText = findViewById(R.id.pingText)

        testButton.setOnClickListener {
            runPingTest()
        }
    }

    private fun runPingTest() {
        testButton.isEnabled = false
        statusText.text = "Testing ping..."
        pingText.text = "Ping: testing..."

        thread {
            try {
                val process = Runtime.getRuntime().exec(
                    arrayOf("ping", "-c", "4", "-W", "2", "1.1.1.1")
                )

                val reader = BufferedReader(
                    InputStreamReader(process.inputStream)
                )

                val output = StringBuilder()
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    output.append(line).append("\n")
                }

                process.waitFor()

                val result = output.toString()
                val ping = extractAveragePing(result)

                runOnUiThread {
                    if (ping != null) {
                        statusText.text = "Ping test complete"
                        pingText.text = "Ping: %.1f ms".format(ping)
                    } else {
                        statusText.text = "Ping test failed"
                        pingText.text = "Ping: unavailable"
                    }

                    testButton.isEnabled = true
                }

            } catch (e: Exception) {
                runOnUiThread {
                    statusText.text = "Test error"
                    pingText.text = "Ping: unavailable"
                    testButton.isEnabled = true
                }
            }
        }
    }

    private fun extractAveragePing(output: String): Double? {
        val match = Regex(
            """=\s*([0-9.]+)/([0-9.]+)/([0-9.]+)/"""
        ).find(output)

        return match?.groupValues?.get(2)?.toDoubleOrNull()
    }
}
