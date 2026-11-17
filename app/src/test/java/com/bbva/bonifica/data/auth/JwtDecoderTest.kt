package com.bbva.bonifica.data.auth

import com.bbva.bonifica.domain.model.Role
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Base64

/** §17: JwtDecoder extrae grupo (rol) y customerReference del IdToken. */
class JwtDecoderTest {

    @Test
    fun `extrae email, rol y customerReference del payload`() {
        val claims = JwtDecoder.decode(
            token(
                """{"email":"cliente1@example.com","cognito:groups":["cliente"],""" +
                    """"custom:customerReference":"PARTY-000024","exp":1767225600}"""
            )
        )

        assertEquals("cliente1@example.com", claims?.email)
        assertEquals(Role.CLIENTE, claims?.role)
        assertEquals("PARTY-000024", claims?.customerReference)
        assertEquals(1_767_225_600L, claims?.expiresAtEpochSec)
    }

    @Test
    fun `rol negocio y sin customerReference`() {
        val claims = JwtDecoder.decode(token("""{"email":"negocio1@example.com","cognito:groups":["negocio"]}"""))

        assertEquals(Role.NEGOCIO, claims?.role)
        assertNull(claims?.customerReference)
        assertEquals(0L, claims?.expiresAtEpochSec)
    }

    @Test
    fun `sin el claim de grupos el rol es nulo`() {
        val claims = JwtDecoder.decode(token("""{"email":"cliente1@example.com"}"""))

        assertNull(claims?.role)
        assertEquals("cliente1@example.com", claims?.email)
    }

    @Test
    fun `grupos desconocidos no asignan rol`() {
        val claims = JwtDecoder.decode(token("""{"cognito:groups":["otro-grupo"]}"""))

        assertNull(claims?.role)
    }

    @Test
    fun `token malformado devuelve nulo`() {
        assertNull(JwtDecoder.decode("no-es-un-jwt"))
        assertNull(JwtDecoder.decode("solo.una parte"))
        assertNull(JwtDecoder.decode("a.!!!.c"))
        assertNull(JwtDecoder.decode(""))
    }

    /** Cabecera y firma de mentira: solo importa el payload, en Base64 URL sin relleno. */
    private fun token(payload: String): String {
        fun encode(raw: String): String =
            Base64.getUrlEncoder().withoutPadding().encodeToString(raw.toByteArray(Charsets.UTF_8))
        return listOf(encode("""{"alg":"HS256","typ":"JWT"}"""), encode(payload), "firma")
            .joinToString(".")
    }
}
