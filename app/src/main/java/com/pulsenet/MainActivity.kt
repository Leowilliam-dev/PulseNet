package com.pulsenet

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
    private lateinit var historyText: TextView
    private lateinit var statusPulse: View

    private lateinit var historyPreferences: SharedPreferences

    private val historyKey = "test_history"
    private val maxHistoryItems = 10
    private val uiHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        historyPreferences = getSharedPreferences("PulseNetHistory", MODE_PRIVATE)

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
        historyText = findViewById(R.id.historyText)
        statusPulse = findViewById(R.id.statusPulse)

        networkTypeText.text = "Network Type: ${getNetworkType()}"
        loadTestHistory()
        animateEntrance()
        animateSignalPulse()

        testButton.setOnClickListener {
            animatePress(testButton)
            runNetworkTest()
        }
    }

    private fun animateEntrance() {
        val views = listOf(
            testButton,
            statusText,
            networkTypeText,
            healthScoreText,
            pingText,
            jitterText,
            lossText,
            downloadText,
            uploadText,
            stabilityText,
            gamingText,
            efootballText,
            codMobileText,
            streamingText,
            videoCallText,
            historyText
        )

        views.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = 26f
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(450)
                .setStartDelay((index * 50L))
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    private fun animateSignalPulse() {
        val pulseOut = ObjectAnimator.ofFloat(statusPulse, View.SCALE_X, 1f, 1.45f, 1f)
        val pulseOutY = ObjectAnimator.ofFloat(statusPulse, View.SCALE_Y, 1f, 1.45f, 1f)
        val pulseAlpha = ObjectAnimator.ofFloat(statusPulse, View.ALPHA, 0.7f, 1f, 0.7f)

        val set = AnimatorSet().apply {
            playTogether(pulseOut, pulseOutY, pulseAlpha)
            duration = 1800
            interpolator = DecelerateInterpolator()
        }

        set.start()
        uiHandler.postDelayed({ animateSignalPulse() }, 1800)
    }

    private fun animatePress(view: View) {
        val scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, 1f, 0.96f, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1f, 0.96f, 1f)
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(scaleX, scaleY)
        animatorSet.duration = 220
        animatorSet.interpolator = DecelerateInterpolator()
        animatorSet.start()
    }

    private fun animateMetricCard(view: View) {
        view.animate()
            .scaleX(1.02f)
            .scaleY(1.02f)
            .setDuration(150)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .start()
            }
            .start()
    }

    private fun getNetworkType(): String {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return "Unknown"
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return "Unknown"

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile Data"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Unknown"
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
                    animateMetricCard(pingText)
                    animateMetricCard(jitterText)
                    animateMetricCard(lossText)
                }

                val download = runDownloadTest()
                runOnUiThread {
                    downloadText.text = "Download: ${formatValue(download)} Mbps"
                    animateMetricCard(downloadText)
                }

                val upload = runUploadTest()
                runOnUiThread {
                    uploadText.text = "Upload: ${formatValue(upload)} Mbps"
                    animateMetricCard(uploadText)
                }

                val healthScore = calculateNetworkHealthScore(ping, jitter, packetLoss, download, upload)
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

                val stability = calculateStability(ping, jitter, packetLoss)
                val gaming = calculateGamingReadiness(ping, jitter, packetLoss)
                val efootball = calculateEfootballReadiness(ping, jitter, packetLoss)
                val codMobile = calculateCodMobileReadiness(ping, jitter, packetLoss)
                val streaming = calculateStreamingReadiness(ping, jitter, packetLoss, download, upload)
                val videoCall = calculateVideoCallReadiness(ping, jitter, packetLoss, download, upload)

                saveTestHistory(
                    healthScore = healthScore,
                    ping = ping,
                    jitter = jitter,
                    packetLoss = packetLoss,
                    download = download,
                    upload = upload,
                    networkType = getNetworkType()
                )

                runOnUiThread {
                    networkTypeText.text = "Network Type: ${getNetworkType()}"
                    healthScoreText.text = "Network Health: $healthScore/100\n$healthStatus"
                    healthScoreText.setTextColor(healthColor)
                    stabilityText.text = "Network Stability: $stability"
                    gamingText.text = "Gaming Readiness: $gaming"
                    efootballText.text = "eFootball Readiness: $efootball"
                    codMobileText.text = "COD Mobile Readiness: $codMobile"
                    streamingText.text = "Streaming Readiness: $streaming"
                    videoCallText.text = "Video Call Readiness: $videoCall"
                    statusText.text = "Network test complete"
                    loadTestHistory()
                    animateMetricCard(healthScoreText)
                    animateMetricCard(stabilityText)
                    animateMetricCard(gamingText)
                    animateMetricCard(efootballText)
                    animateMetricCard(codMobileText)
                    animateMetricCard(streamingText)
                    animateMetricCard(videoCallText)
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

    private fun saveTestHistory(
        healthScore: Int,
        ping: Double,
        jitter: Double,
        packetLoss: Double,
        download: Double,
        upload: Double,
        networkType: String
    ) {
        val oldHistory = historyPreferences.getString(historyKey, "[]") ?: "[]"

        val history = try {
            JSONArray(oldHistory)
        } catch (e: Exception) {
            JSONArray()
        }

        val newResult = JSONObject().apply {
            put("healthScore", healthScore)
            put("ping", ping)
            put("jitter", jitter)
            put("packetLoss", packetLoss)
            put("download", download)
            put("upload", upload)
            put("networkType", networkType)
            put("timestamp", System.currentTimeMillis())
        }

        val newHistory = JSONArray()
        newHistory.put(newResult)

        for (i in 0 until minOf(history.length(), maxHistoryItems - 1)) {
            newHistory.put(history.getJSONObject(i))
        }

        historyPreferences.edit()
            .putString(historyKey, newHistory.toString())
            .apply()
    }

    private fun loadTestHistory() {
        val savedHistory = historyPreferences.getString(historyKey, "[]") ?: "[]"

        val history = try {
            JSONArray(savedHistory)
        } catch (e: Exception) {
            JSONArray()
        }

        if (history.length() == 0) {
            historyText.text = "No test history yet"
            return
        }

        val builder = StringBuilder()

        for (i in 0 until history.length()) {
            val result = history.getJSONObject(i)
            val healthScore = result.optInt("healthScore", 0)
            val ping = result.optDouble("ping", 0.0)
            val jitter = result.optDouble("jitter", 0.0)
            val packetLoss = result.optDouble("packetLoss", 0.0)
            val download = result.optDouble("download", 0.0)
            val upload = result.optDouble("upload", 0.0)
            val networkType = result.optString("networkType", "Unknown")
            val timestamp = result.optLong("timestamp", 0L)

            val date = if (timestamp > 0) {
                SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.getDefault()).format(Date(timestamp))
            } else {
                "Unknown time"
            }

            val healthStatus = when {
                healthScore >= 90 -> "Excellent"
                healthScore >= 75 -> "Good"
                healthScore >= 50 -> "Fair"
                else -> "Poor"
            }

            builder.append("TEST ${i + 1}\n")
            builder.append("$date\n")
            builder.append("Network: $networkType\n")
            builder.append("Health: $healthScore/100 — $healthStatus\n")
            builder.append("Ping: ${formatValue(ping)} ms\n")
            builder.append("Jitter: ${formatValue(jitter)} ms\n")
            builder.append("Packet Loss: ${formatValue(packetLoss)} %\n")
            builder.append("Download: ${formatValue(download)} Mbps\n")
            builder.append("Upload: ${formatValue(upload)} Mbps\n")

            if (i < history.length() - 1) {
                builder.append("\n────────────────────\n\n")
            }
        }

        historyText.text = builder.toString()
    }

    private fun runPingTest(): Triple<Double, Double, Double> {
        val process = Runtime.getRuntime().exec(arrayOf("ping", "-c", "10", "-W", "2", "1.1.1.1"))
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
            ?.toDoubleOrNull() ?: 10.0

        val received = times.size.toDouble()
        val packetLoss = if (transmitted > 0) {
            ((transmitted - received) / transmitted) * 100.0
        } else {
            100.0
        }

        if (times.isEmpty()) {
            return Triple(999.0, 999.0, 100.0)
        }

        val averagePing = times.average()
        val jitter = if (times.size > 1) {
            times.zipWithNext().map { abs(it.second - it.first) }.average()
        } else {
            0.0
        }

        return Triple(averagePing, jitter, packetLoss)
    }

    private fun runDownloadTest(): Double {
        val url = URL("https://speed.cloudflare.com/__down?bytes=10000000")
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

        val elapsedSeconds = (System.currentTimeMillis() - start) / 1000.0
        if (elapsedSeconds <= 0) return 0.0

        return (totalBytes * 8.0) / elapsedSeconds / 1_000_000.0
    }

    private fun runUploadTest(): Double {
        val url = URL("https://speed.cloudflare.com/__up")
        val data = ByteArray(5 * 1024 * 1024)
        val start = System.currentTimeMillis()
        val connection = url.openConnection() as HttpURLConnection

        connection.connectTimeout = 10000
        connection.readTimeout = 20000
        connection.doOutput = true
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/octet-stream")

        connection.outputStream.use { output -> output.write(data) }
        connection.responseCode
        connection.disconnect()

        val elapsedSeconds = (System.currentTimeMillis() - start) / 1000.0
        if (elapsedSeconds <= 0) return 0.0

        return (data.size * 8.0) / elapsedSeconds / 1_000_000.0
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
            packetLoss > 5 || jitter > 40 || download < 3 || upload < 1 -> "Poor"
            packetLoss > 2 || jitter > 25 || download < 8 || upload < 2 -> "Fair"
            packetLoss > 1 || jitter > 15 || download < 25 || upload < 5 || ping > 150 -> "Good"
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
            packetLoss > 5 || jitter > 40 || download < 2 || upload < 1 || ping > 250 -> "Poor"
            packetLoss > 2 || jitter > 25 || download < 5 || upload < 2 || ping > 150 -> "Fair"
            packetLoss > 1 || jitter > 15 || download < 10 || upload < 3 || ping > 100 -> "Good"
            else -> "Excellent"
        }
    }

    private fun formatValue(value: Double): String {
        return String.format(Locale.US, "%.1f", value)
    }
}
