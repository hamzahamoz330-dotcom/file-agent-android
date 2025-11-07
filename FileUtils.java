package com.fileagent.mobile;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.webkit.MimeTypeMap;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * أداة مساعدة للتعامل مع الملفات في Android
 */
public class FileUtils {
    private static final String TAG = "FileUtils";

    /**
     * الحصول على مسار الملف من URI
     */
    public static String getPath(Context context, Uri uri) {
        // DocumentProvider
        if (DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + "/" + split[1];
                }
                
                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {
                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                
                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                } else if ("document".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    /**
     * الحصول على اسم الملف من URI
     */
    public static String getFileName(Context context, Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        result = cursor.getString(nameIndex);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result;
    }

    /**
     * الحصول على حجم الملف
     */
    public static long getFileSize(Context context, Uri uri) {
        long size = -1;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                    if (sizeIndex >= 0) {
                        size = cursor.getLong(sizeIndex);
                    }
                }
            }
        } else {
            String path = getPath(context, uri);
            if (path != null) {
                File file = new File(path);
                size = file.length();
            }
        }
        return size;
    }

    /**
     * نسخ الملف من URI إلى مجلد التطبيق
     */
    public static String copyFileToAppDir(Context context, Uri uri) {
        try {
            String fileName = getFileName(context, uri);
            if (fileName == null) {
                fileName = "file_" + System.currentTimeMillis();
            }
            
            File appDir = new File(context.getExternalFilesDir(null), "FileAgent");
            if (!appDir.exists()) {
                appDir.mkdirs();
            }
            
            File targetFile = new File(appDir, fileName);
            
            try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
                 FileOutputStream outputStream = new FileOutputStream(targetFile)) {
                
                if (inputStream == null) {
                    throw new Exception("Cannot open input stream");
                }
                
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                
                return targetFile.getAbsolutePath();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error copying file: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * الحصول على نوع MIME
     */
    public static String getMimeType(Context context, Uri uri) {
        String mimeType = context.getContentResolver().getType(uri);
        if (mimeType == null) {
            String extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return mimeType;
    }

    /**
     * الحصول على قائمة أنواع الملفات المدعومة
     */
    public static List<String> getSupportedMimeTypes() {
        List<String> mimeTypes = new ArrayList<>();
        mimeTypes.add("image/*");
        mimeTypes.add("video/*");
        mimeTypes.add("audio/*");
        mimeTypes.add("text/*");
        mimeTypes.add("application/pdf");
        mimeTypes.add("application/msword");
        mimeTypes.add("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        mimeTypes.add("application/vnd.ms-excel");
        mimeTypes.add("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        mimeTypes.add("application/vnd.ms-powerpoint");
        mimeTypes.add("application/vnd.openxmlformats-officedocument.presentationml.presentation");
        mimeTypes.add("application/zip");
        mimeTypes.add("application/x-rar-compressed");
        mimeTypes.add("application/json");
        mimeTypes.add("application/xml");
        mimeTypes.add("text/csv");
        return mimeTypes;
    }

    /**
     * التحقق من دعم نوع الملف
     */
    public static boolean isSupportedFileType(Context context, Uri uri) {
        String mimeType = getMimeType(context, uri);
        if (mimeType == null) return false;
        
        List<String> supportedTypes = getSupportedMimeTypes();
        for (String supportedType : supportedTypes) {
            if (supportedType.equals(mimeType) || mimeType.startsWith(supportedType.replace("*", ""))) {
                return true;
            }
        }
        return false;
    }

    /**
     * حذف ملف
     */
    public static boolean deleteFile(String filePath) {
        try {
            File file = new File(filePath);
            return file.exists() && file.delete();
        } catch (Exception e) {
            Log.e(TAG, "Error deleting file: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * إنشاء مجلد إذا لم يكن موجوداً
     */
    public static boolean ensureDirectoryExists(String directoryPath) {
        try {
            File directory = new File(directoryPath);
            if (!directory.exists()) {
                return directory.mkdirs();
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error creating directory: " + e.getMessage(), e);
            return false;
        }
    }
}