package com.pulsenet

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.concurrent.thread
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var testButton: Button
    private lateinit var statusText: TextView
    private lateinit var pingText: TextView
    private lateinit var jitterText: TextView
    private lateinit var lossText: TextView
    private lateinit var stabilityText: TextView
    private lateinit var gamingText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        testButton = findViewById(R.id.testButton)
        statusText = findViewById(R.id.statusText)
        pingText = findViewById(R.id.pingText)
        jitterText = findViewById(R.id.jitterText)
        lossText = findViewById(R.id.lossText)
        stabilityText = findViewById(R.id.stabilityText)
        gamingText = findViewById(R.id.gamingText)

        testButton.setOnClickListener {
            runNetworkTest()
        }
    }

    private fun runNetworkTest() {
        testButton.isEnabled = false
        statusText.text = "Testing network..."
        pingText.text = "Ping: testing..."
        jitterText.text = "Jitter: testing..."
        lossText.text = "Packet Loss: testing..."
        stabilityText.text = "Network Stability: testing..."
        gamingText.text = "Gaming Readiness: testing..."

        thread {
            try {
                val process = Runtime.getRuntime().exec(
                    arrayOf("ping", "-c", "10", "-W", "2", "1.1.1.1")
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
                val times = extractPingTimes(result)

                val packetLoss = calculatePacketLoss(result)
                val jitter = calculateJitter(times)

                val stability = calculateStability(
                    ping,
                    jitter,
                    packetLoss
                )

                val gaming = calculateGamingReadiness(
                    ping,
                    jitter,
                    packetLoss
                )

                runOnUiThread {
                    if (ping != null) {
                        pingText.text = "Ping: %.1f ms".format(ping)
                    } else {
                        pingText.text = "Ping: unavailable"
                    }

                    if (jitter != null) {
                        jitterText.text = "Jitter: %.1f ms".format(jitter)
                    } else {
                        jitterText.text = "Jitter: unavailable"
                    }

                    lossText.text = "Packet Loss: %.1f%%".format(packetLoss)

                    stabilityText.text =
                        "Network Stability: $stability"

                    gamingText.text =
                        "Gaming Readiness: $gaming"

                    statusText.text = "Network test complete"
                    testButton.isEnabled = true
                }

            } catch (e: Exception) {
                runOnUiThread {
                    statusText.text = "Test error"
                    pingText.text = "Ping: unavailable"
                    jitterText.text = "Jitter: unavailable"
                    lossText.text = "Packet Loss: unavailable"
                    stabilityText.text = "Network Stability: unavailable"
                    gamingText.text = "Gaming Readiness: unavailable"
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

    private fun extractPingTimes(output: String): List<Double> {
        val times = mutableListOf<Double>()

        val regex = Regex(
            """time[=<]([0-9.]+)\s*ms"""
        )

        regex.findAll(output).forEach {
            it.groupValues[1].toDoubleOrNull()?.let { value ->
                times.add(value)
            }
        }

        return times
    }

    private fun calculatePacketLoss(output: String): Double {
        val match = Regex(
            """(\d+(?:\.\d+)?)%\s*packet loss"""
        ).find(output)

        return match?.groupValues?.get(1)?.toDoubleOrNull() ?: 100.0
    }

    private fun calculateJitter(times: List<Double>): Double? {
        if (times.size < 2) return null

        var totalDifference = 0.0

        for (i in 1 until times.size) {
            totalDifference += abs(times[i] - times[i - 1])
        }

        return totalDifference / (times.size - 1)
    }

    private fun calculateStability(
        ping: Double?,
        jitter: Double?,
        packetLoss: Double
    ): String {

        if (ping == null || jitter == null) {
            return "Unavailable"
        }

        return when {
            packetLoss > 5.0 || jitter > 30.0 -> "Poor"
            packetLoss > 2.0 || jitter > 15.0 -> "Fair"
            ping > 150.0 || jitter > 10.0 -> "Good"
            else -> "Excellent"
        }
    }

    private fun calculateGamingReadiness(
        ping: Double?,
        jitter: Double?,
        packetLoss: Double
    ): String {

        if (ping == null || jitter == null) {
            return "Unavailable"
        }

        return when {
            packetLoss > 5.0 || jitter > 30.0 || ping > 250.0 ->
                "Poor"

            packetLoss > 2.0 || jitter > 15.0 || ping > 150.0 ->
                "Fair"

            ping > 80.0 || jitter > 10.0 ->
                "Good"

            else ->
                "Excellent"
        }
    }
}
