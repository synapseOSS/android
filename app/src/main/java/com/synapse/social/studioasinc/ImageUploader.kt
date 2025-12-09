/*
ImageUploader - Modern Android image uploader for ImgBB API
Copyright (c) 2025 Ashik (StudioAs Inc.)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package com.synapse.social.studioasinc

import kotlinx.coroutines.*
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL

object ImageUploader {
    
    interface UploadCallback {
        fun onUploadComplete(imageUrl: String)
        fun onUploadError(errorMessage: String)
    }
    
    fun uploadImage(filePath: String, callback: UploadCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = uploadImageSuspend(filePath)
                withContext(Dispatchers.Main) {
                    callback.onUploadComplete(result)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onUploadError(e.message ?: "Unknown error")
                }
            }
        }
    }
    
    private suspend fun uploadImageSuspend(filePath: String): String = withContext(Dispatchers.IO) {
        val file = File(filePath)
        val boundary = "*****"
        val lineEnd = "\r\n"
        val twoHyphens = "--"
        
        val url = URL("https://api.imgbb.com/1/upload?expiration=0&key=faa85ffbac0217ff67b5f3c4baa7fb29")
        val connection = url.openConnection() as HttpURLConnection
        
        connection.doInput = true
        connection.doOutput = true
        connection.useCaches = false
        connection.requestMethod = "POST"
        connection.setRequestProperty("Connection", "Keep-Alive")
        connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=$boundary")
        
        DataOutputStream(connection.outputStream).use { dos ->
            dos.writeBytes("$twoHyphens$boundary$lineEnd")
            dos.writeBytes("Content-Disposition: form-data; name=\"image\";filename=\"${file.name}\"$lineEnd")
            dos.writeBytes(lineEnd)
            
            FileInputStream(file).use { fileInputStream ->
                val bufferSize = 1024
                val buffer = ByteArray(bufferSize)
                var bytesRead: Int
                
                while (fileInputStream.read(buffer).also { bytesRead = it } != -1) {
                    dos.write(buffer, 0, bytesRead)
                }
            }
            
            dos.writeBytes(lineEnd)
            dos.writeBytes("$twoHyphens$boundary$twoHyphens$lineEnd")
            dos.flush()
        }
        
        val responseCode = connection.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            val response = BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                reader.readText()
            }
            handleUploadResponse(response)
        } else {
            throw IOException("Error: $responseCode")
        }
    }
    
    private fun handleUploadResponse(response: String): String {
        try {
            val jsonResponse = JSONObject(response)
            val data = jsonResponse.getJSONObject("data")
            return data.getString("url")
        } catch (e: JSONException) {
            throw IOException("Error parsing JSON response: ${e.message}")
        }
    }
}
