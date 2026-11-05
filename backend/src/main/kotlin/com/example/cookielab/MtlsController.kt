package com.example.cookielab

import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate

@RestController
@RequestMapping("/api/mtls")
class MtlsController(
    private val mtlsRestTemplate: RestTemplate,
    @Value("\${app.mtls.resource-url}") private val resourceUrl: String,
) {
    @GetMapping("/ping")
    fun ping(): MtlsResponse = mtlsRestTemplate.getForObject("$resourceUrl/internal/ping", MtlsResponse::class.java)
        ?: error("mTLS resource returned an empty response")
}

data class MtlsResponse(val message: String, val clientCertificateRequired: Boolean)
