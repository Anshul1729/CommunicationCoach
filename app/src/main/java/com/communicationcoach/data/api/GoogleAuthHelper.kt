package com.communicationcoach.data.api

import android.content.Context
import android.util.Base64
import android.util.Log
import com.communicationcoach.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec

class GoogleAuthHelper(private val context: Context) {

    companion object {
        private const val TAG = "GoogleAuthHelper"
        private const val TOKEN_URI = "https://oauth2.googleapis.com/token"
        private const val SCOPE = "https://www.googleapis.com/auth/cloud-platform"
    }

    private val httpClient = OkHttpClient()

    private val serviceAccount: JSONObject by lazy {
        val json = context.resources.openRawResource(R.raw.service_account)
            .bufferedReader().readText()
        JSONObject(json)
    }

    @Volatile private var cachedToken: String? = null
    @Volatile private var tokenExpiry: Long = 0L

    suspend fun getAccessToken(): String = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis() / 1000
        if (cachedToken != null && tokenExpiry > now + 60) {
            return@withContext cachedToken!!
        }
        val token = fetchNewToken()
        cachedToken = token
        tokenExpiry = now + 3600
        Log.d(TAG, "Access token refreshed")
        token
    }

    private fun fetchNewToken(): String {
        val now = System.currentTimeMillis() / 1000
        val email = serviceAccount.getString("client_email")
        val privateKeyPem = serviceAccount.getString("private_key")
        val jwt = buildJwt(email, privateKeyPem, now)

        val body = FormBody.Builder()
            .add("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
            .add("assertion", jwt)
            .build()

        val request = Request.Builder()
            .url(TOKEN_URI)
            .post(body)
            .build()

        val response = httpClient.newCall(request).execute()
        val responseStr = response.body?.string() ?: throw Exception("Empty token response")

        if (!response.isSuccessful) {
            throw Exception("Token exchange failed (${response.code}): $responseStr")
        }

        return JSONObject(responseStr).getString("access_token")
    }

    private fun buildJwt(email: String, privateKeyPem: String, now: Long): String {
        val header = base64url("""{"alg":"RS256","typ":"JWT"}""".toByteArray())
        val payload = base64url(
            """{"iss":"$email","scope":"$SCOPE","aud":"$TOKEN_URI","exp":${now + 3600},"iat":$now}"""
                .toByteArray()
        )
        val signingInput = "$header.$payload"
        val signature = sign(signingInput.toByteArray(), parsePrivateKey(privateKeyPem))
        return "$signingInput.$signature"
    }

    private fun parsePrivateKey(pem: String): PrivateKey {
        val clean = pem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\\n", "")
            .replace("\n", "")
            .trim()
        val keyBytes = Base64.decode(clean, Base64.DEFAULT)
        return KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(keyBytes))
    }

    private fun sign(data: ByteArray, privateKey: PrivateKey): String {
        val sig = Signature.getInstance("SHA256withRSA")
        sig.initSign(privateKey)
        sig.update(data)
        return base64url(sig.sign())
    }

    private fun base64url(data: ByteArray): String =
        Base64.encodeToString(data, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
}
