package com.vizinhoalerta.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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
}
