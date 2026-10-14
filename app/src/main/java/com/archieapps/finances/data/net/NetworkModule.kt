package com.archieapps.finances.data.net

import com.archieapps.finances.core.session.CodeUnavailableException
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

class SessionHolder {
    @Volatile
    var token: String = ""

    @Volatile
    var baseUrl: String = SessionStore.DEFAULT_BASE_URL

    @Volatile
    var code: () -> String? = { null }
}

private class BaseUrlInterceptor(private val holder: SessionHolder) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val configured = holder.baseUrl.trim().trimEnd('/').toHttpUrlOrNull()
            ?: return chain.proceed(request)

        val prefix = configured.encodedPath.trimEnd('/')
        val url = request.url.newBuilder()
            .scheme(configured.scheme)
            .host(configured.host)
            .port(configured.port)
            .encodedPath(prefix + request.url.encodedPath)
            .build()

        return chain.proceed(request.newBuilder().url(url).build())
    }
}

private val UNGATED = setOf("api/auth/login", "api/user/test")

private class AuthInterceptor(private val holder: SessionHolder) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        val builder = request.newBuilder()
            .header("Accept", "application/json")

        if (holder.token.isNotBlank()) {
            builder.header("Authorization", "Bearer ${holder.token}")
        }

        if (!isUngated(request)) {
            val code = holder.code.invoke() ?: throw CodeUnavailableException()

            builder.header("code", code)
        }

        return chain.proceed(builder.build())
    }

    private fun isUngated(request: okhttp3.Request): Boolean {
        val path = request.url.encodedPath.trimStart('/')

        return UNGATED.any { path.endsWith(it) }
    }
}

object NetworkModule {
    val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    fun api(holder: SessionHolder): FinancesApi {
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(holder))
            .addInterceptor(BaseUrlInterceptor(holder))
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl("http://127.0.0.1/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(FinancesApi::class.java)
    }
}
