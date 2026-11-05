package com.example.mtlsresource

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class InternalController {
    @GetMapping("/internal/ping")
    fun ping() = InternalResponse("mTLS connection successful: server certificate validated and client certificate accepted", true)
}

data class InternalResponse(val message: String, val clientCertificateRequired: Boolean)
