package com.example.cookielab

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.util.Date

@RestController
@RequestMapping("/api/auth")
class AuthController(
    @Value("\${app.auth.username}") private val username: String,
    @Value("\${app.auth.password}") private val password: String,
    @Value("\${app.auth.jwt-secret-base64}") secretBase64: String,
) {
    private val signingKey = Keys.hmacShaKeyFor(java.util.Base64.getDecoder().decode(secretBase64))

    @PostMapping("/session/login")
    fun sessionLogin(@RequestBody credentials: LoginRequest, session: jakarta.servlet.http.HttpSession): AuthResponse {
        verifyCredentials(credentials)
        session.setAttribute("user", username)
        return AuthResponse("Session created", username)
    }

    @GetMapping("/session/me")
    fun sessionMe(session: jakarta.servlet.http.HttpSession): AuthResponse =
        (session.getAttribute("user") as? String)?.let { AuthResponse("Authenticated by server session", it) }
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "No authenticated session")

    @PostMapping("/session/logout")
    fun sessionLogout(session: jakarta.servlet.http.HttpSession): AuthResponse {
        session.invalidate()
        return AuthResponse("Session invalidated", username)
    }

    @PostMapping("/jwt/login")
    fun jwtLogin(@RequestBody credentials: LoginRequest): JwtResponse {
        verifyCredentials(credentials)
        val now = Date()
        val token = Jwts.builder()
            .subject(username)
            .issuedAt(now)
            .expiration(Date(now.time + 15 * 60 * 1000))
            .signWith(signingKey)
            .compact()
        return JwtResponse(token, "Bearer", 900)
    }

    @GetMapping("/jwt/me")
    fun jwtMe(@RequestHeader(name = "Authorization", required = false) authorization: String?): AuthResponse {
        val token = authorization?.takeIf { it.startsWith("Bearer ") }?.removePrefix("Bearer ")
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing Bearer token")
        return try {
            val claims = Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).payload
            AuthResponse("Authenticated by verified JWT", claims.subject)
        } catch (_: Exception) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired JWT")
        }
    }

    private fun verifyCredentials(credentials: LoginRequest) {
        if (credentials.username != username || credentials.password != password) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid demo credentials")
        }
    }
}

data class LoginRequest(val username: String, val password: String)
data class AuthResponse(val message: String, val username: String)
data class JwtResponse(val accessToken: String, val tokenType: String, val expiresInSeconds: Long)
