package com.vizinhoalerta.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class BackendSelectorTest {
    @Test fun `release nunca usa demo`() = runBlocking {
        assertEquals(
            BackendSelector.Mode.FIREBASE,
            BackendSelector.detectar(debug = false) { error("sem rede") }
        )
    }

    @Test fun `debug com backend vivo usa firebase`() = runBlocking {
        assertEquals(BackendSelector.Mode.FIREBASE, BackendSelector.detectar(debug = true) { })
    }

    @Test fun `debug sem backend usa demo`() = runBlocking {
        assertEquals(
            BackendSelector.Mode.DEMO,
            BackendSelector.detectar(debug = true) { error("unavailable") }
        )
    }

    @Test fun `debug com configuracao ficticia usa demo sem sondar`() = runBlocking {
        var sondou = false
        assertEquals(
            BackendSelector.Mode.DEMO,
            BackendSelector.detectar(debug = true, configReal = false) { sondou = true }
        )
        assertFalse(sondou)
    }

    @Test fun `release ignora configuracao ficticia`() = runBlocking {
        assertEquals(
            BackendSelector.Mode.FIREBASE,
            BackendSelector.detectar(debug = false, configReal = false) { error("sem rede") }
        )
    }
}
