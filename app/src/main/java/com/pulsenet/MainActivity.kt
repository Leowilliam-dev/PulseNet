package com.pulsenet

import android.content.Context
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var testButton: Button
    private lateinit var statusText: TextView
    private lateinit var networkTypeText: TextView
    private lateinit var healthScoreText: TextView
    private lateinit var pingText: TextView
    private lateinit var jitterText: TextView
    private lateinit var lossText: TextView
    private lateinit var downloadText: TextView
    private lateinit var uploadText: TextView
    private lateinit var stabilityText: TextView
    private lateinit var gamingText: TextView
    private lateinit var efootballText: TextView
    private lateinit var codMobileText: TextView
    private lateinit var streamingText: TextView
    private lateinit var videoCallText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        testButton = findViewById(R.id.testButton)
        statusText = findViewById(R.id.statusText)
        networkTypeText = findViewById(R.id.networkTypeText)
        healthScoreText = findViewById(R.id.healthScoreText)
        pingText = findViewById(R.id.pingText)
        jitterText = findViewById(R.id.jitterText)
        lossText = findViewById(R.id.lossText)
        downloadText = findViewById(R.id.downloadText)
        uploadText = findViewById(R.id.uploadText)
        stabilityText = findViewById(R.id.stabilityText)
        gamingText = findViewById(R.id.gamingText)
        efootballText = findViewById(R.id.efootballText)
        codMobileText = findViewById(R.id.codMobileText)
        streamingText = findViewById(R.id.streamingText)
        videoCallText = findViewById(R.id.videoCallText)

        networkTypeText.text = "Network Type: ${getNetworkType()}"

        testButton.setOnClickListener {
            runNetworkTest()
        }
    }

    private fun getNetworkType(): String {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val network = connectivityManager.activeNetwork
            ?: return "Unknown"

        val capabilities =
            connectivityManager.getNetworkCapabilities(network)
                ?: return "Unknown"

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ->
                "Wi-Fi"

            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ->
                "Mobile Data"

            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ->
                "Ethernet"

            else ->
                "Unknown"
        }
    }

    private fun runNetworkTest() {
        testButton.isEnabled = false
        statusText.text = "Testing network..."
        networkTypeText.text = "Network Type: ${getNetworkType()}"

        healthScoreText.text = "Network Health: testing..."
        healthScoreText.setTextColor(Color.WHITE)

        pingText.text = "Ping: testing..."
        jitterText.text = "Jitter: testing..."
        lossText.text = "Packet Loss: testing..."
        downloadText.text = "Download: testing..."
        uploadText.text = "Upload: testing..."
        stabilityText.text = "Network Stability: testing..."
        gamingText.text = "Gaming Readiness: testing..."
        efootballText.text = "eFootball Readiness: testing..."
        codMobileText.text = "COD Mobile Readiness: testing..."
        streamingText.text = "Streaming Readiness: testing..."
        videoCallText.text = "Video Call Readiness: testing..."

        thread {
            try {
                val pingResults = runPingTest()

                val ping = pingResults.first
                val jitter = pingResults.second
                val packetLoss = pingResults.third

                runOnUiThread {
                    pingText.text = "Ping: ${formatValue(ping)} ms"
                    jitterText.text = "Jitter: ${formatValue(jitter)} ms"
                    lossText.text = "Packet Loss: ${formatValue(packetLoss)} %"
                }

                val download = runDownloadTest()

                runOnUiThread {
                    downloadText.text = "Download: ${formatValue(download)} Mbps"
                }

                val upload = runUploadTest()

                runOnUiThread {
                    uploadText.text = "Upload: ${formatValue(upload)} Mbps"
                }

                val healthScore = calculateNetworkHealthScore(
                    ping,
                    jitter,
                    packetLoss,
                    download,
                    upload
                )

                val healthStatus = when {
                    healthScore >= 90 -> "Excellent"
                    healthScore >= 75 -> "Good"
                    healthScore >= 50 -> "Fair"
                    else -> "Poor"
                }

                val healthColor = when {
                    healthScore >= 90 -> Color.rgb(46, 204, 113)
                    healthScore >= 75 -> Color.rgb(52, 152, 219)
                    healthScore >= 50 -> Color.rgb(243, 156, 18)
                    else -> Color.rgb(231, 76, 60)
                }

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

                val efootball = calculateEfootballReadiness(
                    ping,
                    jitter,
                    packetLoss
                )

                val codMobile = calculateCodMobileReadiness(
                    ping,
                    jitter,
                    packetLoss
                )

                val streaming = calculateStreamingReadiness(
                    ping,
                    jitter,
                    packetLoss,
                    download,
                    upload
                )

                val videoCall = calculateVideoCallReadiness(
                    ping,
                    jitter,
                    packetLoss,
                    download,
                    upload
                )

                runOnUiThread {
                    networkTypeText.text =
                        "Network Type: ${getNetworkType()}"

                    healthScoreText.text =
                        "Network Health: $healthScore/100\n$healthStatus"

                    healthScoreText.setTextColor(healthColor)

                    stabilityText.text =
                        "Network Stability: $stability"

                    gamingText.text =
                        "Gaming Readiness: $gaming"

                    efootballText.text =
                        "eFootball Readiness: $efootball"

                    codMobileText.text =
                        "COD Mobile Readiness: $codMobile"

                    streamingText.text =
                        "Streaming Readiness: $streaming"

                    videoCallText.text =
                        "Video Call Readiness: $videoCall"

                    statusText.text = "Network test complete"
                    testButton.isEnabled = true
                }

            } catch (e: Exception) {
                runOnUiThread {
                    statusText.text = "Test failed"
                    healthScoreText.text = "Network Health: unavailable"
                    healthScoreText.setTextColor(Color.WHITE)
                    testButton.isEnabled = true
                }
            }
        }
    }

    private fun runPingTest(): Triple<Double, Double, Double> {
        val process = Runtime.getRuntime().exec(
            arrayOf("ping", "-c", "10", "-W", "2", "1.1.1.1")
        )

        val output = process.inputStream.bufferedReader().readText()
        process.waitFor()

        val times = Regex("time=([0-9.]+)")
            .findAll(output)
            .map { it.groupValues[1].toDouble() }
            .toList()

        val transmitted = Regex("(\\d+) packets transmitted")
            .find(output)
            ?.groupValues
            ?.getOrNull(1)
            ?.toDoubleOrNull()
            ?: 10.0

        val received = times.size.toDouble()

        val packetLoss =
            if (transmitted > 0) {
                ((transmitted - received) / transmitted) * 100.0
            } else {
                100.0
            }

        if (times.isEmpty()) {
            return Triple(999.0, 999.0, 100.0)
        }

        val averagePing = times.average()

        val jitter =
            if (times.size > 1) {
                times.zipWithNext()
                    .map { abs(it.second - it.first) }
                    .average()
            } else {
                0.0
            }

        return Triple(
            averagePing,
            jitter,
            packetLoss
        )
    }

    private fun runDownloadTest(): Double {
        val url = URL(
            "https://speed.cloudflare.com/__down?bytes=10000000"
        )

        val start = System.currentTimeMillis()

        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 10000
        connection.readTimeout = 20000
        connection.requestMethod = "GET"

        var totalBytes = 0L

        connection.inputStream.use { input ->
            val buffer = ByteArray(8192)

            while (true) {
                val read = input.read(buffer)

                if (read == -1) break

                totalBytes += read
            }
        }

        connection.disconnect()

        val elapsedSeconds =
            (System.currentTimeMillis() - start) / 1000.0

        if (elapsedSeconds <= 0) return 0.0

        return (totalBytes * 8.0) /
                elapsedSeconds /
                1_000_000.0
    }

    private fun runUploadTest(): Double {
        val url = URL(
            "https://speed.cloudflare.com/__up"
        )

        val data = ByteArray(5 * 1024 * 1024)

        val start = System.currentTimeMillis()

        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 10000
        connection.readTimeout = 20000
        connection.doOutput = true
        connection.requestMethod = "POST"
        connection.setRequestProperty(
            "Content-Type",
            "application/octet-stream"
        )

        connection.outputStream.use { output ->
            output.write(data)
        }

        connection.responseCode

        connection.disconnect()

        val elapsedSeconds =
            (System.currentTimeMillis() - start) / 1000.0

        if (elapsedSeconds <= 0) return 0.0

        return (data.size * 8.0) /
                elapsedSeconds /
                1_000_000.0
    }

    private fun calculateNetworkHealthScore(
        ping: Double,
        jitter: Double,
        packetLoss: Double,
        download: Double,
        upload: Double
    ): Int {

        val pingScore = when {
            ping <= 30 -> 100
            ping <= 60 -> 90
            ping <= 100 -> 75
            ping <= 150 -> 55
            ping <= 250 -> 30
            else -> 10
        }

        val jitterScore = when {
            jitter <= 5 -> 100
            jitter <= 10 -> 90
            jitter <= 15 -> 75
            jitter <= 25 -> 55
            jitter <= 40 -> 30
            else -> 10
        }

        val lossScore = when {
            packetLoss <= 0 -> 100
            packetLoss <= 1 -> 90
            packetLoss <= 2 -> 75
            packetLoss <= 5 -> 40
            else -> 10
        }

        val downloadScore = when {
            download >= 50 -> 100
            download >= 25 -> 90
            download >= 10 -> 75
            download >= 5 -> 55
            download >= 2 -> 30
            else -> 10
        }

        val uploadScore = when {
            upload >= 10 -> 100
            upload >= 5 -> 90
            upload >= 3 -> 75
            upload >= 2 -> 55
            upload >= 1 -> 30
            else -> 10
        }

        return (
            pingScore * 0.25 +
            jitterScore * 0.20 +
            lossScore * 0.20 +
            downloadScore * 0.20 +
            uploadScore * 0.15
        ).toInt()
    }

    private fun calculateStability(
        ping: Double,
        jitter: Double,
        packetLoss: Double
    ): String {
        return when {
            packetLoss > 5 || jitter > 40 || ping > 250 -> "Poor"
            packetLoss > 2 || jitter > 25 || ping > 150 -> "Fair"
            packetLoss > 1 || jitter > 15 || ping > 100 -> "Good"
            else -> "Excellent"
        }
    }

    private fun calculateGamingReadiness(
        ping: Double,
        jitter: Double,
        packetLoss: Double
    ): String {
        return when {
            packetLoss > 5 || jitter > 40 || ping > 250 -> "Poor"
            packetLoss > 2 || jitter > 25 || ping > 150 -> "Fair"
            packetLoss > 1 || jitter > 15 || ping > 100 -> "Good"
            else -> "Excellent"
        }
    }

    private fun calculateEfootballReadiness(
        ping: Double,
        jitter: Double,
        packetLoss: Double
    ): String {
        return when {
            packetLoss > 5 || jitter > 30 || ping > 220 -> "Poor"
            packetLoss > 2 || jitter > 20 || ping > 150 -> "Fair"
            packetLoss > 1 || jitter > 10 || ping > 80 -> "Good"
            else -> "Excellent"
        }
    }

    private fun calculateCodMobileReadiness(
        ping: Double,
        jitter: Double,
        packetLoss: Double
    ): String {
        return when {
            packetLoss > 5 || jitter > 35 || ping > 250 -> "Poor"
            packetLoss > 2 || jitter > 20 || ping > 160 -> "Fair"
            packetLoss > 1 || jitter > 12 || ping > 90 -> "Good"
            else -> "Excellent"
        }
    }

    private fun calculateStreamingReadiness(
        ping: Double,
        jitter: Double,
        packetLoss: Double,
        download: Double,
        upload: Double
    ): String {
        return when {
            packetLoss > 5 ||
                    jitter > 40 ||
                    download < 3 ||
                    upload < 1 -> "Poor"

            packetLoss > 2 ||
                    jitter > 25 ||
                    download < 8 ||
                    upload < 2 -> "Fair"

            packetLoss > 1 ||
                    jitter > 15 ||
                    download < 25 ||
                    upload < 5 ||
                    ping > 150 -> "Good"

            else -> "Excellent"
        }
    }

    private fun calculateVideoCallReadiness(
        ping: Double,
        jitter: Double,
        packetLoss: Double,
        download: Double,
        upload: Double
    ): String {
        return when {
            packetLoss > 5 ||
                    jitter > 40 ||
                    download < 2 ||
                    upload < 1 ||
                    ping > 250 -> "Poor"

            packetLoss > 2 ||
                    jitter > 25 ||
                    download < 5 ||
                    upload < 2 ||
                    ping > 150 -> "Fair"

            packetLoss > 1 ||
                    jitter > 15 ||
                    download < 10 ||
                    upload < 3 ||
                    ping > 100 -> "Good"

            else -> "Excellent"
        }
    }

    private fun formatValue(value: Double): String {
        return String.format("%.1f", value)
    }
}
