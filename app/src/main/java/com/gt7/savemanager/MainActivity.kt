package com.gt7.savemanager

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.Gravity
import android.widget.*
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread

class MainActivity : Activity() {
    private val ps4Dir = "/mnt/sandbox/CUSA24769_000/download0"
    private lateinit var ipInput: EditText
    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            try {
                startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:$packageName")))
            } catch (_: Exception) {}
        }

        val prefs = getSharedPreferences("gt7_prefs", MODE_PRIVATE)
        val savedIp = prefs.getString("ip", "192.168.0.150")

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(60, 80, 60, 60)
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.parseColor("#182030"), Color.parseColor("#0A0C10"))
            )
        }

        val logo = ImageView(this).apply {
            setImageResource(R.drawable.ic_gt7)
            layoutParams = LinearLayout.LayoutParams(280, 280).apply { bottomMargin = 20 }
        }

        val title = TextView(this).apply {
            text = "GT7 SAVE MANAGER"
            textSize = 24f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.12f
        }

        val subtitle = TextView(this).apply {
            text = "PS4 OFFLINE PATCH • CUSA24769"
            textSize = 12f
            setTextColor(Color.parseColor("#8A99B5"))
            setPadding(0, 6, 0, 50)
        }

        val ipLabel = TextView(this).apply {
            text = "ADRESSE IP DE LA PS4 (PORT 2121)"
            textSize = 11f
            setTextColor(Color.parseColor("#8A99B5"))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        ipInput = EditText(this).apply {
            setText(savedIp)
            setTextColor(Color.WHITE)
            textSize = 18f
            gravity = Gravity.CENTER
            setPadding(30, 28, 30, 28)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1E2636"))
                cornerRadius = 24f
                setStroke(2, Color.parseColor("#33415C"))
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = 12
                bottomMargin = 50
            }
        }

        val btnJouer = createStyledButton("🎮  JOUER (Restaurer)", "#0070D1", "#004799") {
            prefs.edit().putString("ip", ipInput.text.toString().trim()).apply()
            runFtpTask(isBackup = false)
        }

        val btnQuitter = createStyledButton("💾  QUITTER (Sauvegarder)", "#E60012", "#99000C") {
            prefs.edit().putString("ip", ipInput.text.toString().trim()).apply()
            runFtpTask(isBackup = true)
        }

        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = true
            visibility = ProgressBar.INVISIBLE
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 24).apply { topMargin = 30 }
        }

        statusText = TextView(this).apply {
            text = "Prêt. Assurez-vous que GT7 est sur le menu principal."
            textSize = 14f
            setTextColor(Color.parseColor("#CBD5E1"))
            gravity = Gravity.CENTER
            setPadding(30, 30, 30, 30)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#141923"))
                cornerRadius = 24f
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = 20 }
        }

        root.addView(logo)
        root.addView(title)
        root.addView(subtitle)
        root.addView(ipLabel)
        root.addView(ipInput)
        root.addView(btnJouer)
        root.addView(btnQuitter)
        root.addView(progressBar)
        root.addView(statusText)
        setContentView(root)
    }

    private fun createStyledButton(label: String, c1: String, c2: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = label
            textSize = 17f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            isAllCaps = false
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.parseColor(c1), Color.parseColor(c2))
            ).apply { cornerRadius = 32f }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 155).apply { bottomMargin = 28 }
            setOnClickListener { onClick() }
        }
    }

    private fun runFtpTask(isBackup: Boolean) {
        val ip = ipInput.text.toString().trim()
        val baseDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Sauvegardes_GT7")
        baseDir.mkdirs()

        progressBar.visibility = ProgressBar.VISIBLE
        statusText.text = "Connexion FTP à $ip:2121..."

        thread {
            val ftp = FTPClient().apply { connectTimeout = 7000 }
            try {
                ftp.connect(ip, 2121)
                ftp.login("anonymous", "")
                ftp.enterLocalPassiveMode()
                ftp.setFileType(FTP.BINARY_FILE_TYPE)

                if (isBackup) {
                    val stamp = SimpleDateFormat("yyyy-MM-dd_HH'h'mm", Locale.getDefault()).format(Date())
                    val destDir = File(baseDir, "Backup_$stamp").apply { mkdirs() }
                    val files = ftp.listFiles(ps4Dir)
                    var count = 0
                    for (f in files) {
                        if (f.isFile) {
                            FileOutputStream(File(destDir, f.name)).use { out ->
                                ftp.retrieveFile("$ps4Dir/${f.name}", out)
                            }
                            count++
                        }
                    }
                    updateUi("✅ Sauvegarde réussie ($count fichiers) !\nDossier : Download/Sauvegardes_GT7/Backup_$stamp")
                } else {
                    val backups = baseDir.listFiles { f -> f.isDirectory && f.name.startsWith("Backup_") }
                        ?.sortedByDescending { it.name }
                    if (backups.isNullOrEmpty()) {
                        updateUi("❌ Aucune sauvegarde trouvée dans Download/Sauvegardes_GT7 !")
                    } else {
                        val latest = backups.first()
                        var count = 0
                        latest.listFiles()?.forEach { file ->
                            if (file.isFile) {
                                FileInputStream(file).use { input ->
                                    ftp.storeFile("$ps4Dir/${file.name}", input)
                                }
                                count++
                            }
                        }
                        updateUi("✅ Restauration terminée (${latest.name}) !\nFermez et relancez GT7 sur la PS4.")
                    }
                }
                ftp.logout()
            } catch (e: Exception) {
                updateUi("❌ Erreur : ${e.message}\nVérifiez que GT7 est lancé et le FTP GoldHEN actif.")
            } finally {
                try { ftp.disconnect() } catch (_: Exception) {}
            }
        }
    }

    private fun updateUi(msg: String) {
        runOnUiThread {
            progressBar.visibility = ProgressBar.INVISIBLE
            statusText.text = msg
        }
    }
}
