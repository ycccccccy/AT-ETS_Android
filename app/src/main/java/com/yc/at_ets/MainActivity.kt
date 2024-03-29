package com.yc.at_ets

import android.Manifest
import android.content.Intent
import android.app.Activity
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import org.json.JSONObject
import java.io.File
import android.widget.Switch
import android.content.res.Configuration
import android.media.MediaPlayer
import android.view.View
import android.widget.Toast
import kotlinx.coroutines.*
import android.graphics.Color
import android.content.Context
import android.content.SharedPreferences
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale

class FirstRunCheck(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("MyApp", Context.MODE_PRIVATE)

    fun isFirstRun(): Boolean {
        return sharedPreferences.getBoolean("isFirstRun", true)
    }

    fun setFirstRun() {
        sharedPreferences.edit().putBoolean("isFirstRun", false).apply()
    }

    fun isSingleAnswerMode(): Boolean {
        return sharedPreferences.getBoolean("isSingleAnswerMode", false)
    }

    fun setSingleAnswerMode(isSingleAnswerMode: Boolean) {
        sharedPreferences.edit().putBoolean("isSingleAnswerMode", isSingleAnswerMode).apply()
    }
}

class MainActivity : AppCompatActivity() {
    private val REQUEST_CODE = 0
    private lateinit var job: Job
    var directoryUri =
        Uri.parse("content://com.android.externalstorage.documents/document/primary%3AAndroid%2Fdata%2Fcom.ets100.secondary%2Ffiles%2FDownload%2FETS_SECONDARY%2Fresource")



    // 判断颜色是否为亮色
    private fun isLightColor(color: Int): Boolean {
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        val brightness = (red * 299 + green * 587 + blue * 114) / 1000
        return brightness >= 128
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)



        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 检查是否开启了深色模式
            val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
                // 如果开启了深色模式，设置状态栏颜色为黑色，字体颜色为白色
                window.statusBarColor = Color.BLACK
                window.decorView.systemUiVisibility = 0
            } else {
                // 如果没有开启深色模式，设置状态栏颜色为白色，字体颜色为黑色
                window.statusBarColor = Color.WHITE
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }


        val firstRunCheck = FirstRunCheck(this)
        if (firstRunCheck.isFirstRun()) {
            // 这是首次运行，显示消息
            Toast.makeText(this, "欢迎使用，请点击下方授权！", Toast.LENGTH_LONG).show()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (!Environment.isExternalStorageManager()) {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } else {
                    // 已经获得权限，可以执行访问数据目录和所有文件的操作
                }
            } else {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
                } else {
                    // 已经获得权限，可以执行访问数据目录和所有文件的操作
                }
            }

            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, directoryUri)
            startActivityForResult(intent, REQUEST_CODE)

            // 设置为非首次运行
            firstRunCheck.setFirstRun()
        }

        // 读取URI
        val sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val uriString = sharedPreferences.getString("directory_uri", null)
        if (uriString != null) {
            directoryUri = Uri.parse(uriString)
        }

        val textView = findViewById<TextView>(R.id.textView)
        val title = findViewById<TextView>(R.id.title)

        val buttonA = findViewById<Button>(R.id.buttonA)
        buttonA.setOnClickListener {
            if (directoryUri != null) {
                val stringBuilder = StringBuilder()
                stringBuilder.append("开始尝试获取：\n")

                val directory = DocumentFile.fromTreeUri(this, directoryUri!!)
                val files = directory?.listFiles()
                val folders = files?.filter { it.isDirectory }?.sortedByDescending { it.lastModified() }?.take(3)

                fun removeHtmlTags(text: String): String {
                    return text.replace(Regex("<.*?>"), "")
                }

                if (directoryUri != null) {
                    val stringBuilder = StringBuilder()
                    stringBuilder.append("开始尝试获取：\n")

                    val directory = DocumentFile.fromTreeUri(this, directoryUri!!)
                    val files = directory?.listFiles()
                    val folders = files?.filter { it.isDirectory }?.sortedByDescending { it.lastModified() }?.take(3)

                    if (folders != null) {
                        for (folder in folders) {
                            val sdf = SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.getDefault())
                            val creationTime = Date(folder.lastModified())
                            stringBuilder.append("此小题的下载时间: ${sdf.format(creationTime)}\n")

                            val file = folder.findFile("content.json")
                            if (file != null) {
                                try {
                                    val inputStream = contentResolver.openInputStream(file.uri)
                                    val data = JSONObject(inputStream?.bufferedReader().use { it?.readText() })
                                    val switch: Switch = findViewById(R.id.switch_single_answer_mode)
                                    val isSwitchChecked: Boolean = switch.isChecked

                                    if (data.getJSONObject("info").has("question")) {
                                        val questions = data.getJSONObject("info").getJSONArray("question")
                                        for (j in 0 until Math.min(questions.length(), 8)) {
                                            val question = questions.getJSONObject(j)
                                            stringBuilder.append("角色扮演 ${j + 1} :\n")
                                            val answers = question.getJSONArray("std")
                                            for (k in 0 until answers.length()) {
                                                val answer = answers.getJSONObject(k)
                                                val answerText = answer.getString("value")
                                                val plainText = removeHtmlTags(answerText)
                                                stringBuilder.append("${k + 1}. $plainText\n")
                                                if (isSwitchChecked) break
                                            }
                                        }
                                    }

                                    if (data.getJSONObject("info").has("std")) {
                                        stringBuilder.append("故事复述:\n")
                                        val answers = data.getJSONObject("info").getJSONArray("std")
                                        for (k in 0 until answers.length()) {
                                            val answer = answers.getJSONObject(k)
                                            val answerText = answer.getString("value")
                                            val plainText = removeHtmlTags(answerText)
                                            stringBuilder.append("${k + 1}. $plainText\n")
                                            if (isSwitchChecked) break
                                        }
                                    }

                                    stringBuilder.append("\n")
                                } catch (e: Exception) {
                                    stringBuilder.append("错误: $e\n")
                                }
                            }
                        }
                    }
                }
                title.text = ""
                textView.text = stringBuilder.toString()

            }
        }

        val buttonB = findViewById<Button>(R.id.buttonB)
        buttonB.setOnClickListener {
            textView.text = ""
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)

        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            directoryUri = resultData?.data

            // 请求一个持久的URI权限授予
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            contentResolver.takePersistableUriPermission(directoryUri!!, takeFlags)

            // 保存URI
            val sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putString("directory_uri", directoryUri.toString())
            editor.apply()
        }
    }
}
