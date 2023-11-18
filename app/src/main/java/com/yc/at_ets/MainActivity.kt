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


class MainActivity : AppCompatActivity() {
    private val REQUEST_CODE = 0
    //var directoryUri =
        //Uri.parse("content://com.android.externalstorage.documents/document/primary%3AAndroid%2Fdata%2Fcom.ets100.secondary")

    var directoryUri =
        Uri.parse("content://com.android.externalstorage.documents/document/primary%3AAndroid%2Fdata%2Fcom.ets100.secondary%2Ffiles%2FDownload%2FETS_SECONDARY%2Fresource")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val textView = findViewById<TextView>(R.id.textView)
        val title = findViewById<TextView>(R.id.title)
        val instruction = findViewById<TextView>(R.id.instruction)

        val buttonC = findViewById<Button>(R.id.buttonC)
        buttonC.setOnClickListener {
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

        }
        val buttonA = findViewById<Button>(R.id.buttonA)
        buttonA.setOnClickListener {
            if (directoryUri != null) {
                val stringBuilder = StringBuilder()
                stringBuilder.append("开始尝试获取：\n")

                val directory = DocumentFile.fromTreeUri(this, directoryUri!!)
                val files = directory?.listFiles()
                val folders = files?.filter { it.isDirectory }?.sortedByDescending { it.lastModified() }?.take(3)

                //stringBuilder.append("获取到的文件夹名称: ${folders?.map { it.name }}\n")

                if (folders != null) {

                    for (folder in folders) {
                        val file = folder.findFile("content.json")
                        if (file != null) {
                            //stringBuilder.append("读取文件: ${file.uri}\n")
                            try {
                                val inputStream = contentResolver.openInputStream(file.uri)
                                val data = JSONObject(inputStream?.bufferedReader().use { it?.readText() })
                                if (data.getJSONObject("info").has("question")) {
                                    val questions = data.getJSONObject("info").getJSONArray("question")
                                    for (j in 0 until questions.length()) {
                                        val question = questions.getJSONObject(j)
                                        stringBuilder.append("问题 ${j + 1} 标准答案:\n")
                                        val answers = question.getJSONArray("std")
                                        for (k in 0 until answers.length()) {
                                            val answer = answers.getJSONObject(k)
                                            stringBuilder.append("${k + 1}. ${answer.getString("value")}\n")
                                        }
                                    }
                                } else {
                                    stringBuilder.append("标准答案:\n")
                                    val answers = data.getJSONObject("info").getJSONArray("std")
                                    for (k in 0 until answers.length()) {
                                        val answer = answers.getJSONObject(k)
                                        stringBuilder.append("${k + 1}. ${answer.getString("value")}\n")
                                    }
                                }
                                stringBuilder.append("\n")
                            } catch (e: Exception) {
                                //stringBuilder.append("错误: $e\n")
                            }
                        }
                    }
                }
                title.text = ""
                instruction.text = ""
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
        }
    }
}
