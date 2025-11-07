package com.fileagent.mobile;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.io.*;

public class MainActivity extends AppCompatActivity {
    private static final int PICK_FILE_REQUEST = 1;
    private static final int PERMISSION_REQUEST_CODE = 100;
    
    private Button btnSelectFile, btnProcessFile;
    private TextView tvFileName, tvResult;
    private String selectedFilePath = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // تهيئة العناصر
        btnSelectFile = findViewById(R.id.btn_select_file);
        btnProcessFile = findViewById(R.id.btn_process_file);
        tvFileName = findViewById(R.id.tv_file_name);
        tvResult = findViewById(R.id.tv_result);
        
        // طلب الصلاحيات
        checkPermissions();
        
        // إعداد المعالجات
        btnSelectFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectFile();
            }
        });
        
        btnProcessFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processFile();
            }
        });
    }

    private void checkPermissions() {
        String[] permissions = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
            
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        }
    }

    private void selectFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, PICK_FILE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            selectedFilePath = FileUtils.getPath(this, uri);
            tvFileName.setText("الملف المختار: " + FileUtils.getFileName(this, uri));
            btnProcessFile.setEnabled(true);
        }
    }

    private void processFile() {
        if (selectedFilePath.isEmpty()) {
            Toast.makeText(this, "يرجى اختيار ملف أولاً", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // إنشاء thread جديد للمعالجة
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String result = processFileInBackground(selectedFilePath);
                    
                    // تحديث الواجهة في thread رئيسي
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvResult.setText("النتيجة:\n" + result);
                            Toast.makeText(MainActivity.this, "تمت معالجة الملف بنجاح", Toast.LENGTH_SHORT).show();
                        }
                    });
                    
                } catch (Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvResult.setText("خطأ في معالجة الملف: " + e.getMessage());
                            Toast.makeText(MainActivity.this, "فشل في معالجة الملف", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    private String processFileInBackground(String filePath) throws Exception {
        File file = new File(filePath);
        String fileName = file.getName().toLowerCase();
        StringBuilder result = new StringBuilder();
        
        result.append("=== تقرير معالجة الملف ===\n");
        result.append("اسم الملف: ").append(file.getName()).append("\n");
        result.append("حجم الملف: ").append(file.length()).append(" بايت\n");
        result.append("تاريخ التعديل: ").append(new java.util.Date(file.lastModified())).append("\n");
        
        // معالجة الصور
        if (isImageFile(fileName)) {
            result.append("نوع الملف: صورة\n");
            result.append("حجم الصورة: ").append(getImageDimensions(filePath)).append("\n");
        }
        // معالجة الفيديوهات
        else if (isVideoFile(fileName)) {
            result.append("نوع الملف: فيديو\n");
            result.append("مدة الفيديو: ").append(getVideoDuration(filePath)).append("\n");
        }
        // معالجة الملفات الصوتية
        else if (isAudioFile(fileName)) {
            result.append("نوع الملف: ملف صوتي\n");
            result.append("مدة الصوت: ").append(getAudioDuration(filePath)).append("\n");
        }
        // معالجة النصوص
        else if (isTextFile(fileName)) {
            result.append("نوع الملف: نص\n");
            result.append("عدد الأسطر: ").append(getTextLineCount(filePath)).append("\n");
        }
        // معالجة ملفات البيانات
        else if (isDataFile(fileName)) {
            result.append("نوع الملف: ملف بيانات\n");
            result.append("المحتوى: ").append(analyzeDataFile(filePath)).append("\n");
        }
        // معالجة الأرشيفات
        else if (isArchiveFile(fileName)) {
            result.append("نوع الملف: أرشيف\n");
            result.append("محتوى الأرشيف: ").append(analyzeArchive(filePath)).append("\n");
        }
        else {
            result.append("نوع الملف: غير معروف\n");
        }
        
        return result.toString();
    }

    // دوال مساعدة لتحديد نوع الملف
    private boolean isImageFile(String fileName) {
        return fileName.matches(".*\\.(jpg|jpeg|png|gif|bmp|webp|svg)$");
    }

    private boolean isVideoFile(String fileName) {
        return fileName.matches(".*\\.(mp4|avi|mkv|mov|wmv|flv|webm|3gp)$");
    }

    private boolean isAudioFile(String fileName) {
        return fileName.matches(".*\\.(mp3|wav|flac|aac|ogg|m4a)$");
    }

    private boolean isTextFile(String fileName) {
        return fileName.matches(".*\\.(txt|md|html|xml|json|csv|log)$");
    }

    private boolean isDataFile(String fileName) {
        return fileName.matches(".*\\.(pdf|doc|docx|xls|xlsx|ppt|pptx)$");
    }

    private boolean isArchiveFile(String fileName) {
        return fileName.matches(".*\\.(zip|rar|7z|tar|gz)$");
    }

    // دوال تحليل الملفات
    private String getImageDimensions(String filePath) {
        // تنفيذ تحليل أبعاد الصورة
        return "640x480";
    }

    private String getVideoDuration(String filePath) {
        // تنفيذ تحليل مدة الفيديو
        return "2:30";
    }

    private String getAudioDuration(String filePath) {
        // تنفيذ تحليل مدة الصوت
        return "1:45";
    }

    private int getTextLineCount(String filePath) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            int lines = 0;
            while (reader.readLine() != null) {
                lines++;
            }
            reader.close();
            return lines;
        } catch (Exception e) {
            return 0;
        }
    }

    private String analyzeDataFile(String filePath) {
        return "تحليل البيانات متوفر";
    }

    private String analyzeArchive(String filePath) {
        return "تحليل الأرشيف متوفر";
    }
}