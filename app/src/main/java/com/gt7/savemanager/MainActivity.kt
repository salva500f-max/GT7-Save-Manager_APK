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
import android.view.View
import android.view.ViewGroup
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
    private lateinit var backupSpinner: Spinner
    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar
    private var backupList: List<File> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            try {
                startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:$packageName")))
            } catch (_: Exception) {}
        }

        val prefs = getSharedPreferences("gt7_prefs", MODE_PRIVATE)
        val savedIp = prefs.getString("ip", "192.168.0.150")

        val scrollView = ScrollView(this).apply {
            isFillViewport = true
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.parseColor("#182030"), Color.parseColor("#0A0C10"))
            )
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(55, 65, 55, 60)
        }

        val logo = ImageView(this).apply {
            setImageResource(R.drawable.ic_gt7)
            layoutParams = LinearLayout.LayoutParams(230, 230).apply { bottomMargin = 14 }
        }

        val title = TextView(this).apply {
            text = "GT7 SAVE MANAGER"
            textSize = 23f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.12f
        }

        val subtitle = TextView(this).apply {
            text = "PS4 OFFLINE PATCH • CUSA24769"
            textSize = 11f
            setTextColor(Color.parseColor("#8A99B5"))
            setPadding(0, 6, 0, 40)
        }

        val ipLabel = TextView(this).apply {
            text = "ADRESSE IP DE LA PS4 (PORT 2121)"
            textSize = 11f
            setTextColor(Color.parseColor("#8A99B5"))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val ipRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = 10
                bottomMargin = 35
            }
        }

        ipInput = EditText(this).apply {
            setText(savedIp)
            setTextColor(Color.WHITE)
            textSize = 17f
            gravity = Gravity.CENTER
            setPadding(24, 26, 24, 26)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1E2636"))
                cornerRadius = 22f
                setStroke(2, Color.parseColor("#33415C"))
            }
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.4f).apply {
                rightMargin = 16
            }
        }

        val btnSaveIp = Button(this).apply {
            text = "💾 Sauvegarder l'IP"
            textSize = 13f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            isAllCaps = false
            setPadding(20, 26, 20, 26)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#2B3A55"))
                cornerRadius = 22f
                setStroke(2, Color.parseColor("#4A628A"))
            }
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                val newIp = ipInput.text.toString().trim()
                if (newIp.isNotEmpty()) {
                    prefs.edit().putString("ip", newIp).apply()
                    statusText.text = "✅ Adresse IP enregistrée : $newIp"
                    Toast.makeText(this@MainActivity, "IP enregistrée ($newIp)", Toast.LENGTH_SHORT).show()
                }
            }
        }

        ipRow.addView(ipInput)
        ipRow.addView(btnSaveIp)

        val spinnerLabel = TextView(this).apply {
            text = "SAUVEGARDE À RESTAURER (POUR LE MODE JOUER)"
            textSize = 11f
            setTextColor(Color.parseColor("#8A99B5"))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        backupSpinner = Spinner(this).apply {
            setPadding(24, 24, 24, 24)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1E2636"))
                cornerRadius = 22f
                setStroke(2, Color.parseColor("#33415C"))
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = 10
                bottomMargin = 40
            }
        }

        val btnJouer = createStyledButton("🎮  JOUER (Restaurer la sélection)", "#0070D1", "#004799") {
            prefs.edit().putString("ip", ipInput.text.toString().trim()).apply()
            runFtpTask(isBackup = false)
        }

        val btnQuitter = createStyledButton("💾  QUITTER (Nouvelle sauvegarde)", "#E60012", "#99000C") {
            prefs.edit().putString("ip", ipInput.text.toString().trim()).apply()
            runFtpTask(isBackup = true)
        }

        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = true
            visibility = ProgressBar.INVISIBLE
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 24).apply { topMargin = 20 }
        }

        statusText = TextView(this).apply {
            text = "Prêt. Assurez-vous que GT7 est sur le menu principal."
            textSize = 14f
            setTextColor(Color.parseColor("#CBD5E1"))
            gravity = Gravity.CENTER
            setPadding(28, 28, 28, 28)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#141923"))
                cornerRadius = 22f
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = 16 }
        }

        root.addView(logo)
        root.addView(title)
        root.addView(subtitle)
        root.addView(ipLabel)
        root.addView(ipRow)
        root.addView(spinnerLabel)
        root.addView(backupSpinner)
        root.addView(btnJouer)
        root.addView(btnQuitter)
        root.addView(progressBar)
        root.addView(statusText)
        scrollView.addView(root)
        setContentView(scrollView)

        refreshBackupList()
    }

    override fun onResume() {
        super.onResume()
        refreshBackupList()
    }

    private fun refreshBackupList() {
        val baseDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Sauvegardes_GT7")
        baseDir.mkdirs()
        backupList = baseDir.listFiles { f -> f.isDirectory && f.name.startsWith("Backup_") }
            ?.sortedByDescending { it.name } ?: emptyList()

        val displayNames = if (backupList.isEmpty()) {
            listOf("Aucune sauvegarde disponible")
        } else {
            backupList.mapIndexed { index, file ->
                if (index == 0) "${file.name} (Plus récente)" else file.name
            }
        }

        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, displayNames) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                return (super.getView(position, convertView, parent) as TextView).apply {
                    setTextColor(Color.WHITE)
                    textSize = 15f
                }
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                return (super.getDropDownView(position, convertView, parent) as TextView).apply {
                    setBackgroundColor(Color.parseColor("#1E2636"))
                    setTextColor(Color.WHITE)
                    textSize = 15f
                    setPadding(32, 28, 32, 28)
                }
            }
        }
        backupSpinner.adapter = adapter
    }

    private fun createStyledButton(label: String, c1: String, c2: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = label
            textSize = 16f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            isAllCaps = false
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.parseColor(c1), Color.parseColor(c2))
            ).apply { cornerRadius = 30f }
            }
        }
