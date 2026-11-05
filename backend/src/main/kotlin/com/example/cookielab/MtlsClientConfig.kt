package com.example.cookielab

import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder
import org.apache.hc.core5.ssl.SSLContexts
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.RestTemplate
import java.io.FileInputStream
import java.security.KeyStore

@Configuration
class MtlsClientConfig(
    @Value("\${app.mtls.client-key-store}") private val keyStorePath: String,
    @Value("\${app.mtls.trust-store}") private val trustStorePath: String,
    @Value("\${app.mtls.store-password}") private val storePassword: String,
) {
    @Bean
    fun mtlsRestTemplate(): RestTemplate {
        val keyStore = loadStore(keyStorePath)
        val trustStore = loadStore(trustStorePath)
        val sslContext = SSLContexts.custom()
            .loadKeyMaterial(keyStore, storePassword.toCharArray())
            .loadTrustMaterial(trustStore, null)
            .build()
        val socketFactory = SSLConnectionSocketFactoryBuilder.create().setSslContext(sslContext).build()
        val connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
            .setSSLSocketFactory(socketFactory)
            .build()
        val httpClient = HttpClients.custom().setConnectionManager(connectionManager).build()
        return RestTemplate(HttpComponentsClientHttpRequestFactory(httpClient))
    }

    private fun loadStore(location: String): KeyStore {
        val path = location.removePrefix("file:")
        return KeyStore.getInstance("PKCS12").apply {
            FileInputStream(path).use { load(it, storePassword.toCharArray()) }
        }
    }
}
