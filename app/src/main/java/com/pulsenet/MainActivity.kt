package com.pulsenet

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var testButton: Button
    private lateinit var statusText: TextView
    private lateinit var pingText: TextView
    private lateinit var jitterText: TextView
    private lateinit var lossText: TextView
    private lateinit var downloadText: TextView
    private lateinit var uploadText: TextView
    private lateinit var stabilityText: TextView
    private lateinit var gamingText: TextView
    private lateinit var streamingText: TextView
    private lateinit var efootballText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        testButton = findViewById(R.id.testButton)
        statusText = findViewById(R.id.statusText)
        pingText = findViewById(R.id.pingText)
        jitterText = findViewById(R.id.jitterText)
        lossText = findViewById(R.id.lossText)
        downloadText = findViewById(R.id.downloadText)
        uploadText = findViewById(R.id.uploadText)
        stabilityText = findViewById(R.id.stabilityText)
        gamingText = findViewById(R.id.gamingText)
        streamingText = findViewById(R.id.streamingText)
        efootballText = findViewById(R.id.efootballText)

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
        downloadText.text = "Download: waiting..."
        uploadText.text = "Upload: waiting..."
        stabilityText.text = "Network Stability: testing..."
        gamingText.text = "Gaming Readiness: testing..."
        streamingText.text = "Streaming Readiness: waiting..."
        efootballText.text = "eFootball Readiness: testing..."

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

                val stability =
                    calculateStability(ping, jitter, packetLoss)

                val gaming =
                    calculateGamingReadiness(ping, jitter, packetLoss)

                val efootball =
                    calculateEfootballReadiness(ping, jitter, packetLoss)

                runOnUiThread {
                    pingText.text =
                        if (ping != null)
                            "Ping: %.1f ms".format(ping)
                        else
                            "Ping: unavailable"

                    jitterText.text =
                        if (jitter != null)
                            "Jitter: %.1f ms".format(jitter)
                        else
                            "Jitter: unavailable"

                    lossText.text =
                        "Packet Loss: %.1f%%".format(packetLoss)

                    stabilityText.text =
                        "Network Stability: $stability"

                    gamingText.text =
                        "Gaming Readiness: $gaming"

                    efootballText.text =
                        "eFootball Readiness: $efootball"

                    statusText.text = "Testing download speed..."
                    downloadText.text = "Download: testing..."
                }

                val downloadSpeed = runDownloadTest()

                runOnUiThread {
                    downloadText.text =
                        if (downloadSpeed != null)
                            "Download: %.2f Mbps".format(downloadSpeed)
                        else
                            "Download: unavailable"

                    statusText.text = "Testing upload speed..."
                    uploadText.text = "Upload: testing..."
                }

                val uploadSpeed = runUploadTest()

                val streaming = calculateStreamingReadiness(
                    ping,
                    jitter,
                    packetLoss,
                    downloadSpeed,
                    uploadSpeed
                )

                runOnUiThread {
                    uploadText.text =
                        if (uploadSpeed != null)
                            "Upload: %.2f Mbps".format(uploadSpeed)
                        else
                            "Upload: unavailable"

                    streamingText.text =
                        "Streaming Readiness: $streaming"

                    statusText.text = "Network test complete"
                    testButton.isEnabled = true
                }

            } catch (e: Exception) {
                runOnUiThread {
                    statusText.text = "Test error"
                    testButton.isEnabled = true
                }
            }
        }
    }

    private fun calculateEfootballReadiness(
        ping: Double?,
        jitter: Double?,
        packetLoss: Double
    ): String {

        if (ping == null || jitter == null) {
            return "Unavailable"
        }

        return when {
            packetLoss > 5.0 ||
                jitter > 30.0 ||
                ping > 220.0 ->
                "Poor"

            packetLoss > 2.0 ||
                jitter > 20.0 ||
                ping > 150.0 ->
                "Fair"

            packetLoss > 1.0 ||
                jitter > 10.0 ||
                ping > 80.0 ->
                "Good"

            else ->
                "Excellent"
        }
    }

    private fun runDownloadTest(): Double? {
        var connection: HttpURLConnection? = null

        return try {
            val url = URL(
                "https://speed.cloudflare.com/__down?bytes=10000000"
            )

            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 20000
            connection.useCaches = false
            connection.setRequestProperty("Accept-Encoding", "identity")

            val startTime = System.nanoTime()

            connection.inputStream.use { input ->
                val buffer = ByteArray(64 * 1024)
                var totalBytes = 0L

                while (true) {
                    val bytesRead = input.read(buffer)

                    if (bytesRead == -1) {
                        break
                    }

                    totalBytes += bytesRead
                }

                val elapsedSeconds =
                    (System.nanoTime() - startTime) /
                        1_000_000_000.0

                if (
                    elapsedSeconds <= 0.0 ||
                    totalBytes <= 0
                ) {
                    return null
                }

                (totalBytes * 8.0) /
                    elapsedSeconds /
                    1_000_000.0
            }

        } catch (e: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun runUploadTest(): Double? {
        var connection: HttpURLConnection? = null

        return try {
            val url = URL(
                "https://speed.cloudflare.com/__up"
            )

            connection =
                url.openConnection() as HttpURLConnection

            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = 10000
            connection.readTimeout = 20000
            connection.useCaches = false

            connection.setRequestProperty(
                "Content-Type",
                "application/octet-stream"
            )

            val uploadSize = 5 * 1024 * 1024

            connection.setFixedLengthStreamingMode(
                uploadSize
            )

            val buffer = ByteArray(64 * 1024)

            val startTime = System.nanoTime()

            connection.outputStream.use { output ->
                var remaining = uploadSize

                while (remaining > 0) {
                    val amount =
                        minOf(buffer.size, remaining)

                    output.write(
                        buffer,
                        0,
                        amount
                    )

                    remaining -= amount
                }

                output.flush()
            }

            val responseCode =
                connection.responseCode

            val elapsedSeconds =
                (System.nanoTime() - startTime) /
                    1_000_000_000.0

            if (
                responseCode !in 200..299 ||
                elapsedSeconds <= 0.0
            ) {
                null
            } else {
                (uploadSize * 8.0) /
                    elapsedSeconds /
                    1_000_000.0
            }

        } catch (e: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun calculateStreamingReadiness(
        ping: Double?,
        jitter: Double?,
        packetLoss: Double,
        download: Double?,
        upload: Double?
    ): String {

        if (
            ping == null ||
            jitter == null ||
            download == null ||
            upload == null
        ) {
            return "Unavailable"
        }

        return when {
            packetLoss > 5.0 ||
                jitter > 40.0 ||
                download < 3.0 ||
                upload < 1.0 ->
                "Poor"

            packetLoss > 2.0 ||
                jitter > 25.0 ||
                download < 8.0 ||
                upload < 2.0 ->
                "Fair"

            packetLoss > 1.0 ||
                jitter > 15.0 ||
                download < 25.0 ||
                upload < 5.0 ||
                ping > 150.0 ->
                "Good"

            else ->
                "Excellent"
        }
    }

    private fun extractAveragePing(
        output: String
    ): Double? {

        val match = Regex(
            """=\s*([0-9.]+)/([0-9.]+)/([0-9.]+)/"""
        ).find(output)

        return match
            ?.groupValues
            ?.get(2)
            ?.toDoubleOrNull()
    }

    private fun extractPingTimes(
        output: String
    ): List<Double> {

        val times =
            mutableListOf<Double>()

        val regex = Regex(
            """time[=<]([0-9.]+)\s*ms"""
        )

        regex.findAll(output).forEach {
            it.groupValues[1]
                .toDoubleOrNull()
                ?.let { value ->
                    times.add(value)
                }
        }

        return times
    }

    private fun calculatePacketLoss(
        output: String
    ): Double {

        val match = Regex(
            """(\d+(?:\.\d+)?)%\s*packet loss"""
        ).find(output)

        return match
            ?.groupValues
            ?.get(1)
            ?.toDoubleOrNull()
            ?: 100.0
    }

    private fun calculateJitter(
        times: List<Double>
    ): Double? {

        if (times.size < 2) {
            return null
        }

        var totalDifference = 0.0

        for (i in 1 until times.size) {
            totalDifference +=
                abs(
                    times[i] -
                        times[i - 1]
                )
        }

        return totalDifference /
            (times.size - 1)
    }

    private fun calculateStability(
        ping: Double?,
        jitter: Double?,
        packetLoss: Double
    ): String {

        if (
            ping == null ||
            jitter == null
        ) {
            return "Unavailable"
        }

        return when {
            packetLoss > 5.0 ||
                jitter > 30.0 ->
                "Poor"

            packetLoss > 2.0 ||
                jitter > 15.0 ->
                "Fair"

            ping > 150.0 ||
                jitter > 10.0 ->
                "Good"

            else ->
                "Excellent"
        }
    }

    private fun calculateGamingReadiness(
        ping: Double?,
        jitter: Double?,
        packetLoss: Double
    ): String {

        if (
            ping == null ||
            jitter == null
        ) {
            return "Unavailable"
        }

        return when {
            packetLoss > 5.0 ||
                jitter > 30.0 ||
                ping > 250.0 ->
                "Poor"

            packetLoss > 2.0 ||
                jitter > 15.0 ||
                ping > 150.0 ->
                "Fair"

            ping > 80.0 ||
                jitter > 10.0 ->
                "Good"

            else ->
                "Excellent"
        }
    }
}
