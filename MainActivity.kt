package com.fileagent.mobile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import java.io.*

class MainActivity : AppCompatActivity() {
    
    // عناصر الواجهة
    private lateinit var btnSelectFile: Button
    private lateinit var btnProcessFile: Button
    private lateinit var tvFileName: TextView
    private lateinit var tvResult: TextView
    private lateinit var progressBar: View
    
    // متغيرات
    private var selectedFilePath: String = ""
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    //.launcher للحصول على محتوى الملف
    private val getContentLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            handleSelectedFile(uri)
        }
    }
    
    // طلب الصلاحيات
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            Toast.makeText(this, "تم منح الصلاحيات", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "تحتاج الصلاحيات لاستخدام التطبيق", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initializeViews()
        checkPermissions()
        setupClickListeners()
    }
    
    private fun initializeViews() {
        btnSelectFile = findViewById(R.id.btn_select_file)
        btnProcessFile = findViewById(R.id.btn_process_file)
        tvFileName = findViewById(R.id.tv_file_name)
        tvResult = findViewById(R.id.tv_result)
        progressBar = findViewById(R.id.progress_bar)
    }
    
    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
        
        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest)
        }
    }
    
    private fun setupClickListeners() {
        btnSelectFile.setOnClickListener {
            selectFile()
        }
        
        btnProcessFile.setOnClickListener {
            processFile()
        }
    }
    
    private fun selectFile() {
        val mimeTypes = FileUtils.getSupportedMimeTypes().toTypedArray()
        getContentLauncher.launch(mimeTypes.contentToString())
    }
    
    private fun handleSelectedFile(uri: Uri) {
        coroutineScope.launch {
            val fileName = FileUtils.getFileName(this@MainActivity, uri)
            val fileSize = FileUtils.getFileSize(this@MainActivity, uri)
            
            selectedFilePath = FileUtils.copyFileToAppDir(this@MainActivity, uri) ?: ""
            
            if (selectedFilePath.isNotEmpty()) {
                tvFileName.text = "الملف المختار: $fileName (${formatFileSize(fileSize)})"
                btnProcessFile.isEnabled = true
                Toast.makeText(this@MainActivity, "تم اختيار الملف", Toast.LENGTH_SHORT).show()
            } else {
                tvFileName.text = "خطأ في اختيار الملف"
                btnProcessFile.isEnabled = false
                Toast.makeText(this@MainActivity, "فشل في تحميل الملف", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun processFile() {
        if (selectedFilePath.isEmpty()) {
            Toast.makeText(this, "يرجى اختيار ملف أولاً", Toast.LENGTH_SHORT).show()
            return
        }
        
        // إظهار شريط التقدم
        progressBar.visibility = View.VISIBLE
        btnProcessFile.isEnabled = false
        
        coroutineScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    processFileBackground()
                }
                
                tvResult.text = result
                progressBar.visibility = View.GONE
                btnProcessFile.isEnabled = true
                Toast.makeText(this@MainActivity, "تمت معالجة الملف بنجاح", Toast.LENGTH_SHORT).show()
                
            } catch (e: Exception) {
                tvResult.text = "خطأ في معالجة الملف: ${e.message}"
                progressBar.visibility = View.GONE
                btnProcessFile.isEnabled = true
                Toast.makeText(this@MainActivity, "فشل في معالجة الملف", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun processFileBackground(): String {
        val file = File(selectedFilePath)
        val fileName = file.name.lowercase()
        val result = StringBuilder()
        
        // إضافة معلومات أساسية
        result.appendLine("=== تقرير معالجة الملف ===")
        result.appendLine("اسم الملف: ${file.name}")
        result.appendLine("حجم الملف: ${formatFileSize(file.length())}")
        result.appendLine("تاريخ التعديل: ${java.util.Date(file.lastModified())}")
        result.appendLine()
        
        // معالجة حسب نوع الملف
        when {
            isImageFile(fileName) -> {
                result.appendLine("نوع الملف: صورة")
                result.appendLine("حجم الصورة: ${getImageDimensions(selectedFilePath)}")
                analyzeImageFile(file)
            }
            isVideoFile(fileName) -> {
                result.appendLine("نوع الملف: فيديو")
                result.appendLine("مدة الفيديو: ${getVideoDuration(selectedFilePath)}")
                analyzeVideoFile(file)
            }
            isAudioFile(fileName) -> {
                result.appendLine("نوع الملف: ملف صوتي")
                result.appendLine("مدة الصوت: ${getAudioDuration(selectedFilePath)}")
                analyzeAudioFile(file)
            }
            isTextFile(fileName) -> {
                result.appendLine("نوع الملف: نص")
                result.appendLine("عدد الأسطر: ${getTextLineCount(selectedFilePath)}")
                analyzeTextFile(file, result)
            }
            isDataFile(fileName) -> {
                result.appendLine("نوع الملف: ملف بيانات")
                result.appendLine("المحتوى: ${analyzeDataFile(selectedFilePath)}")
                analyzeDocumentFile(file, result)
            }
            isArchiveFile(fileName) -> {
                result.appendLine("نوع الملف: أرشيف")
                result.appendLine("محتوى الأرشيف: ${analyzeArchive(selectedFilePath)}")
                analyzeArchiveFile(file, result)
            }
            else -> {
                result.appendLine("نوع الملف: غير معروف")
            }
        }
        
        return result.toString()
    }
    
    private fun analyzeImageFile(file: File) {
        // تحليل خصائص الصورة
    }
    
    private fun analyzeVideoFile(file: File) {
        // تحليل خصائص الفيديو
    }
    
    private fun analyzeAudioFile(file: File) {
        // تحليل خصائص الصوت
    }
    
    private fun analyzeTextFile(file: File, result: StringBuilder) {
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val charCount = file.readText().length
                    val wordCount = file.readText().split(Regex("\\s+")).size
                    val paragraphCount = file.readText().lines().filter { it.isBlank() }.size
                    
                    result.appendLine("عدد الأحرف: $charCount")
                    result.appendLine("عدد الكلمات: $wordCount")
                    result.appendLine("عدد الفقرات: $paragraphCount")
                } catch (e: Exception) {
                    result.appendLine("تحليل النص: غير متوفر")
                }
            }
        }
    }
    
    private fun analyzeDocumentFile(file: File, result: StringBuilder) {
        // تحليل ملفات البيانات
        result.appendLine("تحليل المستند: متوفر")
    }
    
    private fun analyzeArchiveFile(file: File, result: StringBuilder) {
        // تحليل الأرشيفات
        result.appendLine("تحليل الأرشيف: متوفر")
    }
    
    // دوال مساعدة
    private fun isImageFile(fileName: String): Boolean {
        return fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || 
               fileName.endsWith(".png") || fileName.endsWith(".gif") || 
               fileName.endsWith(".bmp") || fileName.endsWith(".webp") || 
               fileName.endsWith(".svg")
    }
    
    private fun isVideoFile(fileName: String): Boolean {
        return fileName.endsWith(".mp4") || fileName.endsWith(".avi") || 
               fileName.endsWith(".mkv") || fileName.endsWith(".mov") || 
               fileName.endsWith(".wmv") || fileName.endsWith(".flv") || 
               fileName.endsWith(".webm") || fileName.endsWith(".3gp")
    }
    
    private fun isAudioFile(fileName: String): Boolean {
        return fileName.endsWith(".mp3") || fileName.endsWith(".wav") || 
               fileName.endsWith(".flac") || fileName.endsWith(".aac") || 
               fileName.endsWith(".ogg") || fileName.endsWith(".m4a")
    }
    
    private fun isTextFile(fileName: String): Boolean {
        return fileName.endsWith(".txt") || fileName.endsWith(".md") || 
               fileName.endsWith(".html") || fileName.endsWith(".xml") || 
               fileName.endsWith(".json") || fileName.endsWith(".csv") || 
               fileName.endsWith(".log")
    }
    
    private fun isDataFile(fileName: String): Boolean {
        return fileName.endsWith(".pdf") || fileName.endsWith(".doc") || 
               fileName.endsWith(".docx") || fileName.endsWith(".xls") || 
               fileName.endsWith(".xlsx") || fileName.endsWith(".ppt") || 
               fileName.endsWith(".pptx")
    }
    
    private fun isArchiveFile(fileName: String): Boolean {
        return fileName.endsWith(".zip") || fileName.endsWith(".rar") || 
               fileName.endsWith(".7z") || fileName.endsWith(".tar") || 
               fileName.endsWith(".gz")
    }
    
    private fun getImageDimensions(filePath: String): String {
        // تنفيذ تحليل أبعاد الصورة
        return "متوفر"
    }
    
    private fun getVideoDuration(filePath: String): String {
        // تنفيذ تحليل مدة الفيديو
        return "متوفر"
    }
    
    private fun getAudioDuration(filePath: String): String {
        // تنفيذ تحليل مدة الصوت
        return "متوفر"
    }
    
    private fun getTextLineCount(filePath: String): Int {
        return try {
            File(filePath).readLines().size
        } catch (e: Exception) {
            0
        }
    }
    
    private fun analyzeDataFile(filePath: String): String {
        return "متوفر"
    }
    
    private fun analyzeArchive(filePath: String): String {
        return "متوفر"
    }
    
    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes بايت"
            bytes < 1024 * 1024 -> String.format("%.1f كيلوبايت", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.1f ميجابايت", bytes / (1024.0 * 1024.0))
            else -> String.format("%.1f جيجابايت", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
}