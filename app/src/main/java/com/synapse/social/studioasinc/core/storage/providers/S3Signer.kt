package com.synapse.social.studioasinc.core.storage.providers

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Helper class for AWS SigV4 signing.
 */
object S3Signer {

    private const val S3_SERVICE = "s3"

    fun signS3(
        conn: java.net.HttpURLConnection,
        method: String,
        canonicalPath: String,
        region: String,
        host: String,
        accessKeyId: String,
        secretAccessKey: String,
        amzDate: String
    ) {
        val dateStamp = amzDate.substring(0, 8)
        val signedHeaders = "content-type;host;x-amz-content-sha256;x-amz-date"
        val contentType = conn.getRequestProperty("Content-Type") ?: "application/octet-stream"
        val payloadHash = "UNSIGNED-PAYLOAD"
        val canonicalQuery = ""
        val canonicalHeaders = "content-type:$contentType\n" +
                "host:$host\n" +
                "x-amz-content-sha256:$payloadHash\n" +
                "x-amz-date:$amzDate\n"
        val canonicalRequest = "$method\n$canonicalPath\n$canonicalQuery\n$canonicalHeaders\n$signedHeaders\n$payloadHash"
        val credentialScope = "$dateStamp/$region/$S3_SERVICE/aws4_request"
        val stringToSign = "AWS4-HMAC-SHA256\n$amzDate\n$credentialScope\n${sha256Hex(canonicalRequest)}"

        val signingKey = getSignatureKey(secretAccessKey, dateStamp, region, S3_SERVICE)
        val signature = bytesToHex(hmacSHA256(signingKey, stringToSign))
        val authorization = "AWS4-HMAC-SHA256 Credential=$accessKeyId/$credentialScope, SignedHeaders=$signedHeaders, Signature=$signature"
        conn.setRequestProperty("Authorization", authorization)
    }

    private fun sha256Hex(s: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val d = md.digest(s.toByteArray(StandardCharsets.UTF_8))
        return d.joinToString("") { "%02x".format(it) }
    }

    private fun hmacSHA256(key: ByteArray, data: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        val keySpec = SecretKeySpec(key, "HmacSHA256")
        mac.init(keySpec)
        return mac.doFinal(data.toByteArray(StandardCharsets.UTF_8))
    }

    private fun getSignatureKey(key: String, dateStamp: String, regionName: String, serviceName: String): ByteArray {
        val kSecret = "AWS4$key".toByteArray(StandardCharsets.UTF_8)
        val kDate = hmacSHA256(kSecret, dateStamp)
        val kRegion = hmacSHA256(kDate, regionName)
        val kService = hmacSHA256(kRegion, serviceName)
        return hmacSHA256(kService, "aws4_request")
    }

    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
