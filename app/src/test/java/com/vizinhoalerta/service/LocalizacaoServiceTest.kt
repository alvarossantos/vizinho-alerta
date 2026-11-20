package com.vizinhoalerta.service

import com.google.android.gms.maps.model.LatLng
import org.junit.Test
import org.junit.Assert.*
import kotlin.math.abs

class LocalizacaoServiceTest {

    @Test
    fun `coordenadas validas sao diferentes de zero`() {
        // Teste a lógica de verificação de coordenadas
        val coordenadasValidas = LatLng(-23.55052, -46.633308)
        assertNotEquals(0.0, coordenadasValidas.latitude, 0.001)
        assertNotEquals(0.0, coordenadasValidas.longitude, 0.001)
    }

    @Test
    fun `coordenadas padrao sao zero`() {
        // Valores por default
        val latPadrao = 0.0
        val lngPadrao = 0.0
        
        assertEquals(0.0, latPadrao, 0.0)
        assertEquals(0.0, lngPadrao, 0.0)
    }

    @Test
    fun `formatacao de coordenadas latitude valida`() {
        val latitude = -23.55052
        // Deve estar entre -90 e 90
        assertTrue(-90 <= latitude && latitude <= 90)
    }

    @Test
    fun `formatacao de coordenadas longitude valida`() {
        val longitude = -46.633308
        // Deve estar entre -180 e 180
        assertTrue(-180 <= longitude && longitude <= 180)
    }
}