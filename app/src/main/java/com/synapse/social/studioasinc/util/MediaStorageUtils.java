/**
 * CONFIDENTIAL AND PROPRIETARY
 * 
 * This source code is the sole property of StudioAs Inc. Synapse. (Ashik).
 * Any reproduction, modification, distribution, or exploitation in any form
 * without explicit written permission from the owner is strictly prohibited.
 * 
 * Copyright (c) 2025 StudioAs Inc. Synapse. (Ashik)
 * All rights reserved.
 */

package com.synapse.social.studioasinc.util;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Modern utility class for downloading and saving media files using MediaStore API.
 * Replaces deprecated DownloadManager external storage methods.
 */
public class MediaStorageUtils {

    private static final String TAG = "MediaStorageUtils";
    private static final ExecutorService executor = Executors.newFixedThreadPool(3);

    /**
     * Callback interface for download operations.
     */
    public interface DownloadCallback {
        void onSuccess(Uri savedUri, String fileName);
        void onProgress(int progress);
        void onError(String error);
    }

    /**
     * Helper class to store file information.
     */
    private static class FileInfo {
        String fileName;
        String mimeType;
        String extension;

        FileInfo(String fileName, String mimeType, String extension) {
            this.fileName = fileName;
            this.mimeType = mimeType;
            this.extension = extension;
        }
    }

    /**
     * Downloads an image from URL and saves it to the device's Pictures directory
     * using the modern MediaStore API.
     * 
     * @param context The context
     * @param imageUrl The URL of the image to download
     * @param fileName The desired filename (without extension)
     * @param callback Callback to handle success/error
     */
    public static void downloadImage(Context context, String imageUrl, String fileName, DownloadCallback callback) {
        if (context == null || imageUrl == null || imageUrl.isEmpty()) {
            if (callback != null) {
                callback.onError("Invalid parameters");
            }
            return;
        }

        // Make variables effectively final for lambda
        final String finalImageUrl = imageUrl;
        final String finalBaseFileName = fileName;

        executor.execute(() -> {
            try {
                // Detect file extension and mime type from URL or content
                FileInfo fileInfo = detectImageFileInfo(finalImageUrl, finalBaseFileName);
                String finalFileName = fileInfo.fileName;
                String mimeType = fileInfo.mimeType;

                // Create content values for MediaStore
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, finalFileName);
                contentValues.put(MediaStore.Images.Media.MIME_TYPE, mimeType);
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // For Android 10+ (API 29+), use relative path
                    contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Synapse");
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 1);
                } else {
                    // For older versions, use DATA column (deprecated but necessary)
                    String picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
                    contentValues.put(MediaStore.Images.Media.DATA, picturesDir + "/Synapse/" + fileName);
                }

                ContentResolver resolver = context.getContentResolver();
                Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

                if (imageUri == null) {
                    if (callback != null) {
                        callback.onError("Failed to create media entry");
                    }
                    return;
                }

                // Download and save the image
                try (OutputStream outputStream = resolver.openOutputStream(imageUri)) {
                    if (outputStream != null) {
                        downloadToStream(imageUrl, outputStream, callback);
                        
                        // Mark as not pending (for Android 10+)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            contentValues.clear();
                            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0);
                            resolver.update(imageUri, contentValues, null, null);
                        }
                        
                        if (callback != null) {
                            callback.onSuccess(imageUri, fileName);
                        }
                    } else {
                        throw new IOException("Failed to open output stream");
                    }
                } catch (Exception e) {
                    // Clean up failed entry
                    resolver.delete(imageUri, null, null);
                    throw e;
                }

            } catch (Exception e) {
                Log.e(TAG, "Error downloading image: " + e.getMessage(), e);
                if (callback != null) {
                    callback.onError("Download failed: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Downloads a video from URL and saves it to the device's Movies directory.
     * 
     * @param context The context
     * @param videoUrl The URL of the video to download
     * @param fileName The desired filename (without extension)
     * @param callback Callback to handle success/error
     */
    public static void downloadVideo(Context context, String videoUrl, String fileName, DownloadCallback callback) {
        if (context == null || videoUrl == null || videoUrl.isEmpty()) {
            if (callback != null) {
                callback.onError("Invalid parameters");
            }
            return;
        }

        // Make variables effectively final for lambda
        final String finalVideoUrl = videoUrl;
        final String finalBaseFileName = fileName;

        executor.execute(() -> {
            try {
                // Detect file extension and mime type from URL or content
                FileInfo fileInfo = detectVideoFileInfo(finalVideoUrl, finalBaseFileName);
                String finalFileName = fileInfo.fileName;
                String mimeType = fileInfo.mimeType;

                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.Video.Media.DISPLAY_NAME, finalFileName);
                contentValues.put(MediaStore.Video.Media.MIME_TYPE, mimeType);
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/Synapse");
                    contentValues.put(MediaStore.Video.Media.IS_PENDING, 1);
                } else {
                    String moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).toString();
                    contentValues.put(MediaStore.Video.Media.DATA, moviesDir + "/Synapse/" + fileName);
                }

                ContentResolver resolver = context.getContentResolver();
                Uri videoUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues);

                if (videoUri == null) {
                    if (callback != null) {
                        callback.onError("Failed to create media entry");
                    }
                    return;
                }

                try (OutputStream outputStream = resolver.openOutputStream(videoUri)) {
                    if (outputStream != null) {
                        downloadToStream(videoUrl, outputStream, callback);
                        
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            contentValues.clear();
                            contentValues.put(MediaStore.Video.Media.IS_PENDING, 0);
                            resolver.update(videoUri, contentValues, null, null);
                        }
                        
                        if (callback != null) {
                            callback.onSuccess(videoUri, fileName);
                        }
                    } else {
                        throw new IOException("Failed to open output stream");
                    }
                } catch (Exception e) {
                    resolver.delete(videoUri, null, null);
                    throw e;
                }

            } catch (Exception e) {
                Log.e(TAG, "Error downloading video: " + e.getMessage(), e);
                if (callback != null) {
                    callback.onError("Download failed: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Downloads content from URL to the provided OutputStream with progress reporting.
     * 
     * @param urlString The URL to download from
     * @param outputStream The OutputStream to write to
     * @param callback Callback for progress updates
     * @throws IOException If download fails
     */
    private static void downloadToStream(String urlString, OutputStream outputStream, DownloadCallback callback) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        try {
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);
            connection.connect();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException("Server response: " + connection.getResponseCode() + " " + connection.getResponseMessage());
            }

            int fileLength = connection.getContentLength();
            
            try (InputStream inputStream = connection.getInputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                int totalBytesRead = 0;
                
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                    
                    // Report progress
                    if (callback != null && fileLength > 0) {
                        int progress = (int) ((totalBytesRead * 100L) / fileLength);
                        callback.onProgress(progress);
                    }
                }
                
                outputStream.flush();
            }
        } finally {
            connection.disconnect();
        }
    }

    /**
     * Detects file extension and mime type from image URL.
     * 
     * @param imageUrl The image URL
     * @param baseFileName The base filename without extension
     * @return FileInfo with detected information
     */
    private static FileInfo detectImageFileInfo(String imageUrl, String baseFileName) {
        String extension = ".jpg"; // Default
        String mimeType = "image/jpeg"; // Default
        
        try {
            // First, try to detect from URL
            String urlLower = imageUrl.toLowerCase();
            if (urlLower.contains(".png")) {
                extension = ".png";
                mimeType = "image/png";
            } else if (urlLower.contains(".gif")) {
                extension = ".gif";
                mimeType = "image/gif";
            } else if (urlLower.contains(".webp")) {
                extension = ".webp";
                mimeType = "image/webp";
            } else if (urlLower.contains(".bmp")) {
                extension = ".bmp";
                mimeType = "image/bmp";
            } else if (urlLower.contains(".jpeg") || urlLower.contains(".jpg")) {
                extension = ".jpg";
                mimeType = "image/jpeg";
            }
            
            // Try to get more accurate info from URL path
            Uri uri = Uri.parse(imageUrl);
            String path = uri.getPath();
            if (path != null) {
                int lastDot = path.lastIndexOf('.');
                if (lastDot > 0 && lastDot < path.length() - 1) {
                    String urlExtension = path.substring(lastDot).toLowerCase();
                    String detectedMimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(urlExtension.substring(1));
                    if (detectedMimeType != null && detectedMimeType.startsWith("image/")) {
                        extension = urlExtension;
                        mimeType = detectedMimeType;
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error detecting file type from URL, using defaults: " + e.getMessage());
        }
        
        // Ensure filename has the correct extension
        String finalFileName = baseFileName;
        if (!finalFileName.toLowerCase().endsWith(extension.toLowerCase())) {
            finalFileName += extension;
        }
        
        return new FileInfo(finalFileName, mimeType, extension);
    }

    /**
     * Detects file extension and mime type from video URL.
     * 
     * @param videoUrl The video URL
     * @param baseFileName The base filename without extension
     * @return FileInfo with detected information
     */
    private static FileInfo detectVideoFileInfo(String videoUrl, String baseFileName) {
        String extension = ".mp4"; // Default
        String mimeType = "video/mp4"; // Default
        
        try {
            // Try to detect from URL
            String urlLower = videoUrl.toLowerCase();
            if (urlLower.contains(".mov")) {
                extension = ".mov";
                mimeType = "video/quicktime";
            } else if (urlLower.contains(".avi")) {
                extension = ".avi";
                mimeType = "video/x-msvideo";
            } else if (urlLower.contains(".mkv")) {
                extension = ".mkv";
                mimeType = "video/x-matroska";
            } else if (urlLower.contains(".webm")) {
                extension = ".webm";
                mimeType = "video/webm";
            } else if (urlLower.contains(".mp4")) {
                extension = ".mp4";
                mimeType = "video/mp4";
            }
            
            // Try to get more accurate info from URL path
            Uri uri = Uri.parse(videoUrl);
            String path = uri.getPath();
            if (path != null) {
                int lastDot = path.lastIndexOf('.');
                if (lastDot > 0 && lastDot < path.length() - 1) {
                    String urlExtension = path.substring(lastDot).toLowerCase();
                    String detectedMimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(urlExtension.substring(1));
                    if (detectedMimeType != null && detectedMimeType.startsWith("video/")) {
                        extension = urlExtension;
                        mimeType = detectedMimeType;
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error detecting file type from URL, using defaults: " + e.getMessage());
        }
        
        // Ensure filename has the correct extension
        String finalFileName = baseFileName;
        if (!finalFileName.toLowerCase().endsWith(extension.toLowerCase())) {
            finalFileName += extension;
        }
        
        return new FileInfo(finalFileName, mimeType, extension);
    }

    /**
     * Shuts down the executor service. Call this when the application is terminating.
     */
    public static void shutdown() {
        executor.shutdown();
    }
}