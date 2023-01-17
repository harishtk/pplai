package com.aiavatar.app.commons.util.net

import com.aiavatar.app.BuildConfig
import com.aiavatar.app.di.ApplicationDependencies
import com.aiavatar.app.eventbus.UnAuthorizedEvent
import okhttp3.Interceptor
import okhttp3.Response
import org.greenrobot.eventbus.EventBus
import java.net.HttpURLConnection

const val DEFAULT_USER_AGENT = "Android"

class AndroidHeaderInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response = chain.run {
        proceed(
            request()
                .newBuilder()
                .addHeader("App-Version-Code", BuildConfig.VERSION_CODE.toString())
                .addHeader("App-Version-Name", BuildConfig.VERSION_NAME)
                .build()
        )
    }
}

class UserAgentInterceptor(
    private val value: String = System.getProperty("http.agent") ?: DEFAULT_USER_AGENT
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response = chain.run {
        proceed(
            request()
                .newBuilder()
                .addHeader("User-Agent", value)
                .build()
        )
    }
}

/*class JwtInterceptor(preferencesRepository: AppPreferencesRepository) : Interceptor {
    private val userPreferences = preferencesRepository.userPreferencesFlow

    override fun intercept(chain: Interceptor.Chain): Response = chain.run {
        val jwt = runBlocking { userPreferences.first().jwt }
        proceed(
            request()
                .newBuilder()
                .addHeader("Authorization", "Bearer $jwt")
                .build()
        )
    }
}*/

class JwtInterceptor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response = chain.run {
        val jwt = ApplicationDependencies.getPersistentStore().deviceToken
        proceed(
            request()
                .newBuilder()
                .addHeader("Authorization", "Bearer $jwt")
                .build()
        )
    }
}


/**
 * Catches the [HttpURLConnection.HTTP_FORBIDDEN] and dispatches an [UnAuthorizedEvent] event
 */
class ForbiddenInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response = chain.run {
        return proceed(request()).also { response ->
            if (response.code == HttpURLConnection.HTTP_UNAUTHORIZED) {
                EventBus.getDefault().post(UnAuthorizedEvent(System.currentTimeMillis()))
            }
        }
    }
}