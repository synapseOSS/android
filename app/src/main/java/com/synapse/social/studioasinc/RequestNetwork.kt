package com.synapse.social.studioasinc

import android.app.Activity

class RequestNetwork(private val activity: Activity) {
    private var params: MutableMap<String, Any> = mutableMapOf()
    private var headers: MutableMap<String, Any> = mutableMapOf()
    private var requestType: Int = 0
    
    fun setHeaders(headers: Map<String, Any>) {
        this.headers = headers.toMutableMap()
    }
    
    fun setParams(params: Map<String, Any>, requestType: Int) {
        this.params = params.toMutableMap()
        this.requestType = requestType
    }
    
    fun getParams(): Map<String, Any> = params
    
    fun getHeaders(): Map<String, Any> = headers
    
    fun getActivity(): Activity = activity
    
    fun getRequestType(): Int = requestType
    
    fun startRequestNetwork(method: String, url: String, tag: String, requestListener: RequestListener) {
        RequestNetworkController.getInstance().execute(this, method, url, tag, requestListener)
    }
    
    interface RequestListener {
        fun onResponse(tag: String, response: String, responseHeaders: Map<String, Any>)
        fun onErrorResponse(tag: String, message: String)
    }
}
