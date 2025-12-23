package com.synapse.social.studioasinc

import android.app.Activity
import android.os.Handler
import android.os.Looper
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit
import com.google.gson.Gson

/**
 * Secure, modern networking wrapper using OkHttp.
 * Replaces the insecure RequestNetworkController.
 */
class RequestNetwork(activity: Activity) {

    private val activityRef = WeakReference(activity)
    private var params: MutableMap<String, Any> = mutableMapOf()
    private var headers: MutableMap<String, Any> = mutableMapOf()
    private var requestType: Int = 0

    companion object {
        const val GET = "GET"
        const val POST = "POST"
        const val PUT = "PUT"
        const val DELETE = "DELETE"

        const val REQUEST_PARAM = 0
        const val REQUEST_BODY = 1

        private const val SOCKET_TIMEOUT = 15000L
        private const val READ_TIMEOUT = 25000L

        // Secure Singleton OkHttpClient
        private val okHttpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(SOCKET_TIMEOUT, TimeUnit.MILLISECONDS)
                .readTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS)
                .writeTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS)
                .build()
        }

        private val mainHandler = Handler(Looper.getMainLooper())
    }

    fun setHeaders(headers: Map<String, Any>) {
        this.headers = headers.toMutableMap()
    }

    fun setParams(params: Map<String, Any>, requestType: Int) {
        this.params = params.toMutableMap()
        this.requestType = requestType
    }

    fun getParams(): Map<String, Any> = params

    fun getHeaders(): Map<String, Any> = headers

    fun getActivity(): Activity? = activityRef.get()

    fun getRequestType(): Int = requestType

    fun startRequestNetwork(method: String, url: String, tag: String, requestListener: RequestListener) {
        val currentActivity = activityRef.get()
        if (currentActivity == null || currentActivity.isFinishing) {
            // If activity is gone, we might still want to execute the request but ignore the callback,
            // or just abort. Here we abort to prevent unexpected behavior.
            return
        }

        try {
            val reqBuilder = Request.Builder()

            headers.forEach { (key, value) ->
                reqBuilder.addHeader(key, value.toString())
            }

            if (requestType == REQUEST_PARAM) {
                if (method == GET) {
                    val httpBuilder = url.toHttpUrlOrNull()?.newBuilder()
                        ?: throw IllegalArgumentException("Invalid URL: $url")

                    params.forEach { (key, value) ->
                        httpBuilder.addQueryParameter(key, value.toString())
                    }
                    reqBuilder.url(httpBuilder.build()).get()
                } else {
                    val formBuilder = FormBody.Builder()
                    params.forEach { (key, value) ->
                        formBuilder.add(key, value.toString())
                    }
                    val reqBody = formBuilder.build()
                    reqBuilder.url(url).method(method, reqBody)
                }
            } else {
                val json = Gson().toJson(params)
                val reqBody = json.toRequestBody("application/json".toMediaType())

                if (method == GET) {
                     // GET with body is non-standard but technically possible in some libs.
                     // OkHttp disallows GET with body. We fall back to standard GET without body
                     // or throw. For now, we assume GET doesn't use JSON body.
                     reqBuilder.url(url).get()
                } else {
                    reqBuilder.url(url).method(method, reqBody)
                }
            }

            val request = reqBuilder.build()

            okHttpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUi {
                        requestListener.onErrorResponse(tag, e.message ?: "Network error")
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    val responseBody = response.body?.string()?.trim() ?: ""
                    val headersMap = mutableMapOf<String, Any>()
                    response.headers.names().forEach { name ->
                        headersMap[name] = response.headers[name] ?: ""
                    }

                    runOnUi {
                        requestListener.onResponse(tag, responseBody, headersMap)
                    }
                }
            })

        } catch (e: Exception) {
             runOnUi {
                 requestListener.onErrorResponse(tag, e.message ?: "Error preparing request")
             }
        }
    }

    private fun runOnUi(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            mainHandler.post(action)
        }
    }

    interface RequestListener {
        fun onResponse(tag: String, response: String, responseHeaders: Map<String, Any>)
        fun onErrorResponse(tag: String, message: String)
    }
}
