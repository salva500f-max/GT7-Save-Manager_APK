package com.gt7.savemanager

import android.app.Activity
import android.app.AlertDialog
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
    private lateinit var ipLabel: TextView
    private lateinit var btnSaveIp: Button
    private lateinit var spinnerLabel: TextView
    private lateinit var btnJouer: Button
    private lateinit var btnQuitter: Button
    private lateinit var btnLang: Button
    private var backupList: List<File> = emptyList()
    private var lang = "fr"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            try {
                startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:$packageName")))
            } catch (_: Exception) {}
        }

        val prefs = getSharedPreferences("gt7_prefs", MODE_PRIVATE)
        val savedIp = prefs.getString("ip", "192.168.0.150")
        lang = prefs.getString("lang", "fr") ?: "fr"

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
            setPadding(55, 45, 55, 60)
        }

        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        btnLang = Button(this).apply {
            textSize = 13f; setTextColor(Color.WHITE); typeface = Typeface.DEFAULT_BOLD; isAllCaps = false
            setPadding(28, 14, 28, 14)
            background = GradientDrawable().apply { setColor(Color.parseColor("#232D42")); cornerRadius = 50f; setStroke(2, Color.parseColor("#4A628A")) }
            setOnClickListener {
                lang = when (lang) { "fr" -> "en"; "en" -> "es"; else -> "fr" }
                prefs.edit().putString("lang", lang).apply()
                applyLanguage()
            }
        }
        topBar.addView(btnLang)

        val logo = ImageView(this).apply {
            try { setImageResource(R.drawable.banner_gt7) } catch (_: Throwable) {}
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = 14; bottomMargin = 16 }
        }

        val subtitle = TextView(this).apply {
            text = "PS4 OFFLINE PATCH • CUSA24769 • v2.0"; textSize = 11f; setTextColor(Color.parseColor("#8A99B5")); setPadding(0, 4, 0, 32)
        }
        ipLabel = TextView(this).apply {
            textSize = 11f; setTextColor(Color.parseColor("#8A99B5"))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val ipRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = 10; bottomMargin = 32 }
        }

        ipInput = EditText(this).apply {
            setText(savedIp); setTextColor(Color.WHITE); textSize = 17f; gravity = Gravity.CENTER; setPadding(24, 26, 24, 26)
            background = GradientDrawable().apply { setColor(Color.parseColor("#1E2636")); cornerRadius = 22f; setStroke(2, Color.parseColor("#33415C")) }
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.35f).apply { rightMargin = 16 }
        }

        btnSaveIp = Button(this).apply {
            textSize = 13f; setTextColor(Color.WHITE); typeface = Typeface.DEFAULT_BOLD; isAllCaps = false; setPadding(18, 26, 18, 26)
            background = GradientDrawable().apply { setColor(Color.parseColor("#2B3A55")); cornerRadius = 22f; setStroke(2, Color.parseColor("#4A628A")) }
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                val newIp = ipInput.text.toString().trim()
                if (newIp.isNotEmpty()) {
                    prefs.edit().putString("ip", newIp).apply()
                    statusText.text = t("✅ IP enregistrée : $newIp", "✅ IP saved: $newIp", "✅ IP guardada: $newIp")
                }
            }
        }
        ipRow.addView(ipInput); ipRow.addView(btnSaveIp)

        spinnerLabel = TextView(this).apply {
            textSize = 11f; setTextColor(Color.parseColor("#8A99B5"))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        backupSpinner = Spinner(this).apply {
            setPadding(24, 24, 24, 24)
            background = GradientDrawable().apply { setColor(Color.parseColor("#1E2636")); cornerRadius = 22f; setStroke(2, Color.parseColor("#33415C")) }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = 10; bottomMargin = 38 }
        }

        btnJouer = createStyledButton("#0070D1", "#004799") {
            prefs.edit().putString("ip", ipInput.text.toString().trim()).apply()
            showPreGuide(isBackup = false)
        }

        btnQuitter = createStyledButton("#E60012", "#99000C") {
            prefs.edit().putString("ip", ipInput.text.toString().trim()).apply()
            showPreGuide(isBackup = true)
        }

        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = true; visibility = ProgressBar.INVISIBLE
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 24).apply { topMargin = 16 }
        }

        statusText = TextView(this).apply {
            textSize = 14f; setTextColor(Color.parseColor("#CBD5E1")); gravity = Gravity.CENTER; setPadding(28, 28, 28, 28)
            background = GradientDrawable().apply { setColor(Color.parseColor("#141923")); cornerRadius = 22f }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = 14 }
        }

        root.addView(topBar); root.addView(logo); root.addView(subtitle)
        root.addView(ipLabel); root.addView(ipRow); root.addView(spinnerLabel); root.addView(backupSpinner)
        root.addView(btnJouer); root.addView(btnQuitter); root.addView(progressBar); root.addView(statusText)
        scrollView.addView(root); setContentView(scrollView); applyLanguage()
    }

    override fun onResume() { super.onResume(); refreshBackupList() }
    private fun t(fr: String, en: String, es: String) = when (lang) { "en" -> en; "es" -> es; else -> fr }

    private fun applyLanguage() {
        btnLang.text = t("🇫🇷 FR ▾", "🇬🇧 EN ▾", "🇪🇸 ES ▾")
        ipLabel.text = t("ADRESSE IP DE LA PS4 (PORT 2121)", "PS4 IP ADDRESS (PORT 2121)", "DIRECCIÓN IP DE PS4 (PUERTO 2121)")
        btnSaveIp.text = t("💾 Sauver IP", "💾 Save IP", "💾 Guardar IP")
        spinnerLabel.text = t("SAUVEGARDE À RESTAURER (MODE JOUER)", "BACKUP TO RESTORE (PLAY MODE)", "COPIA A RESTAURAR (MODO JUGAR)")
        btnJouer.text = t("🎮  JOUER (Restaurer)", "🎮  PLAY (Restore)", "🎮  JUGAR (Restaurar)")
        btnQuitter.text = t("💾  QUITTER (Sauvegarder)", "💾  QUIT (Backup)", "💾  SALIR (Guardar)")
        statusText.text = t("1️⃣ Lancez GT7 sur la PS4 et restez sur le menu principal.", "1️⃣ Launch GT7 on PS4 and stay on the main menu.", "1️⃣ Inicia GT7 en PS4 y quédate en el menú principal.")
        refreshBackupList()
    }
    private fun showPreGuide(isBackup: Boolean) {
        val title = if (isBackup) t("💾 Étape 1 : Préparation", "💾 Step 1: Preparation", "💾 Paso 1: Preparación")
        else t("🎮 Étape 1 : Préparation", "🎮 Step 1: Preparation", "🎮 Paso 1: Preparación")

        val body = if (isBackup) t(
            "Avant de sauvegarder :\n\n1️⃣ GT7 doit être LANCÉ sur le MENU PRINCIPAL (ne le fermez pas encore).\n2️⃣ Aucune icône de sauvegarde auto ne doit tourner en bas à droite.\n3️⃣ Le serveur FTP GoldHEN doit être actif.",
            "Before backing up:\n\n1️⃣ GT7 must be RUNNING on the MAIN MENU (do not close it yet).\n2️⃣ No auto-save icon spinning at bottom right.\n3️⃣ GoldHEN FTP server must be active.",
            "Antes de guardar:\n\n1️⃣ GT7 debe estar INICIADO en el MENÚ PRINCIPAL (no lo cierres aún).\n2️⃣ Sin icono de autoguardado girando abajo a la derecha.\n3️⃣ El servidor FTP de GoldHEN debe estar activo."
        ) else t(
            "Avant de restaurer :\n\n1️⃣ GT7 doit être LANCÉ sur le MENU PRINCIPAL.\n2️⃣ Le serveur FTP GoldHEN doit être actif.\n3️⃣ Ne touchez pas à la manette pendant le transfert.",
            "Before restoring:\n\n1️⃣ GT7 must be RUNNING on the MAIN MENU.\n2️⃣ GoldHEN FTP server must be active.\n3️⃣ Do not touch the controller during transfer.",
            "Antes de restaurar:\n\n1️⃣ GT7 debe estar INICIADO en el MENÚ PRINCIPAL.\n2️⃣ El servidor FTP de GoldHEN debe estar activo.\n3️⃣ No toques el mando durante la transferencia."
        )

        AlertDialog.Builder(this).setTitle(title).setMessage(body)
            .setPositiveButton(t("▶️ Démarrer", "▶️ Start", "▶️ Iniciar")) { _, _ -> runFtpTask(isBackup) }
            .setNegativeButton(t("Annuler", "Cancel", "Cancelar"), null).show()
    }

    private fun showPostGuide(isBackup: Boolean, detail: String) {
        val title = if (isBackup) t("✅ Sauvegarde terminée !", "✅ Backup Complete!", "✅ ¡Copia completada!")
        else t("✅ Restauration terminée !", "✅ Restore Complete!", "✅ ¡Restauración completada!")

        val body = if (isBackup) t(
            "$detail\n\n👉 QUE FAIRE MAINTENANT :\n1️⃣ Vous pouvez QUITTER Gran Turismo 7 (Bouton PS > Fermer l'application).\n2️⃣ Vous pouvez éteindre votre PS4.",
            "$detail\n\n👉 WHAT TO DO NOW:\n1️⃣ You can now CLOSE Gran Turismo 7 (PS Button > Close Application).\n2️⃣ You can turn off your PS4.",
            "$detail\n\n👉 QUÉ HACER AHORA:\n1️⃣ Ya puedes CERRAR Gran Turismo 7 (Botón PS > Cerrar aplicación).\n2️⃣ Ya puedes apagar tu PS4."
        ) else t(
            "$detail\n\n⚠️ ÉTAPE OBLIGATOIRE SUR LA PS4 :\n1️⃣ Appuyez sur le BOUTON PS et FERMEZ immédiatement le jeu.\n2️⃣ RELANCEZ Gran Turismo 7 pour charger la sauvegarde.",
            "$detail\n\n⚠️ MANDATORY STEP ON PS4:\n1️⃣ Press the PS BUTTON and CLOSE the game immediately.\n2️⃣ RESTART Gran Turismo 7 to load the save.",
            "$detail\n\n⚠️ PASO OBLIGATORIO EN PS4:\n1️⃣ Pulsa el BOTÓN PS y CIERRA el juego inmediatamente.\n2️⃣ REINICIA Gran Turismo 7 para cargar la copia."
        )

        AlertDialog.Builder(this).setTitle(title).setMessage(body).setPositiveButton("OK", null).show()
    }

    private fun refreshBackupList() {
        val baseDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Sauvegardes_GT7").apply { mkdirs() }
        backupList = baseDir.listFiles { f -> f.isDirectory && f.name.startsWith("Backup_") }?.sortedByDescending { it.name } ?: emptyList()
        val latestTag = t("(Plus récente)", "(Latest)", "(Más reciente)")
        val emptyMsg = t("Aucune sauvegarde disponible", "No backup available", "No hay copias disponibles")
        val displayNames = if (backupList.isEmpty()) listOf(emptyMsg) else backupList.mapIndexed { i, f -> if (i == 0) "${f.name} $latestTag" else f.name }

        backupSpinner.adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, displayNames) {
            override fun getView(p: Int, v: View?, g: ViewGroup) = (super.getView(p, v, g) as TextView).apply { setTextColor(Color.WHITE); textSize = 14f }
            override fun getDropDownView(p: Int, v: View?, g: ViewGroup) = (super.getDropDownView(p, v, g) as TextView).apply {
                setBackgroundColor(Color.parseColor("#1E2636")); setTextColor(Color.WHITE); textSize = 14f; setPadding(32, 28, 32, 28)
            }
        }
    }

    private fun createStyledButton(c1: String, c2: String, onClick: () -> Unit) = Button(this).apply {
        textSize = 16f; setTextColor(Color.WHITE); typeface = Typeface.DEFAULT_BOLD; isAllCaps = false
        background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(Color.parseColor(c1), Color.parseColor(c2))).apply { cornerRadius = 30f }
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 145).apply { bottomMargin = 22 }
        setOnClickListener { onClick() }
    }

    private fun runFtpTask(isBackup: Boolean) {
        val ip = ipInput.text.toString().trim()
        val baseDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Sauvegardes_GT7").apply { mkdirs() }
        progressBar.visibility = ProgressBar.VISIBLE
        statusText.text = t("⏳ Connexion FTP à $ip:2121...", "⏳ Connecting FTP to $ip:2121...", "⏳ Conectando FTP a $ip:2121...")

        thread {
            val ftp = FTPClient().apply { connectTimeout = 7000 }
            try {
                ftp.connect(ip, 2121); ftp.login("anonymous", ""); ftp.enterLocalPassiveMode(); ftp.setFileType(FTP.BINARY_FILE_TYPE)
                if (isBackup) {
                    val stamp = SimpleDateFormat("yyyy-MM-dd_HH'h'mm", Locale.getDefault()).format(Date())
                    val destDir = File(baseDir, "Backup_$stamp").apply { mkdirs() }
                    var count = 0
                    for (f in ftp.listFiles(ps4Dir)) if (f.isFile) {
                        FileOutputStream(File(destDir, f.name)).use { ftp.retrieveFile("$ps4Dir/${f.name}", it) }; count++
                    }
                    val detail = t("Dossier : Backup_$stamp ($count fichiers)", "Folder: Backup_$stamp ($count files)", "Carpeta: Backup_$stamp ($count archivos)")
                    updateUi("✅ $detail", true, true, true, detail)
                } else {
                    if (backupList.isEmpty()) updateUi(t("❌ Aucune sauvegarde trouvée !", "❌ No backup found!", "❌ ¡No se encontró ninguna copia!"))
                    else {
                        val chosen = backupList[backupSpinner.selectedItemPosition.coerceIn(0, backupList.size - 1)]
                        chosen.listFiles()?.forEach { if (it.isFile) FileInputStream(it).use { inp -> ftp.storeFile("$ps4Dir/${it.name}", inp) } }
                        val detail = t("Sauvegarde : ${chosen.name}", "Backup: ${chosen.name}", "Copia: ${chosen.name}")
                        updateUi("✅ $detail", false, true, false, detail)
                    }
                }
                ftp.logout()
            } catch (e: Exception) {
                updateUi(t("❌ Échec : Vérifiez que GT7 est lancé sur le menu principal et le FTP actif.", "❌ Failed: Make sure GT7 is running on the main menu and FTP is active.", "❌ Error: Verifica que GT7 esté en el menú principal y el FTP activo."))
            } finally { try { ftp.disconnect() } catch (_: Exception) {} }
        }
    }

    private fun updateUi(msg: String, refreshList: Boolean = false, showDialog: Boolean = false, isBackup: Boolean = false, dialogDetail: String = "") = runOnUiThread {
        progressBar.visibility = ProgressBar.INVISIBLE; statusText.text = msg
        if (refreshList) refreshBackupList()
        if (showDialog) showPostGuide(isBackup, dialogDetail)
    }
}
