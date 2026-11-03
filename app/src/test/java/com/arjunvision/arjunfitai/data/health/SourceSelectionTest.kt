package com.arjunvision.arjunfitai.data.health

import com.arjunvision.arjunfitai.data.health.Availability.AVAILABLE
import com.arjunvision.arjunfitai.data.health.Availability.NOT_INSTALLED
import com.arjunvision.arjunfitai.data.health.Availability.UNSUPPORTED
import com.arjunvision.arjunfitai.data.health.SourceKind.DEMO
import com.arjunvision.arjunfitai.data.health.SourceKind.HEALTH_CONNECT
import com.arjunvision.arjunfitai.data.health.SourceKind.SAMSUNG
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceSelectionTest {
    private fun s(kind: SourceKind, availability: Availability = AVAILABLE, consent: Boolean = false) =
        SourceState(kind, availability, consent)

    @Test fun autoPrefersSamsungWhenItHasConsent() {
        val choice = chooseSource(SourcePreference.AUTO, listOf(s(SAMSUNG, consent = true), s(HEALTH_CONNECT, consent = true)))
        assertEquals(SAMSUNG, choice.kind)
        assertFalse(choice.needsConsent)
    }

    @Test fun autoUsesHealthConnectWhenOnlyItHasConsent() {
        val choice = chooseSource(SourcePreference.AUTO, listOf(s(SAMSUNG), s(HEALTH_CONNECT, consent = true)))
        assertEquals(HEALTH_CONNECT, choice.kind)
        assertFalse(choice.needsConsent)
    }

    @Test fun autoAsksTheFirstAvailableSourceWhenNoneHasConsent() {
        val choice = chooseSource(SourcePreference.AUTO, listOf(s(SAMSUNG, NOT_INSTALLED), s(HEALTH_CONNECT)))
        assertEquals(HEALTH_CONNECT, choice.kind)
        assertTrue(choice.needsConsent)
    }

    @Test fun autoNeverPicksDemoByItself() {
        val choice = chooseSource(SourcePreference.AUTO, listOf(s(SAMSUNG, UNSUPPORTED), s(HEALTH_CONNECT, NOT_INSTALLED), s(DEMO, consent = true)))
        assertNull(choice.kind)
        assertFalse(choice.needsConsent)
    }

    @Test fun autoWithNoSourcesInTheBuild() {
        assertNull(chooseSource(SourcePreference.AUTO, emptyList()).kind)
    }

    @Test fun chosenSourceIsKeptWithoutPermissionAndAsks() {
        val choice = chooseSource(SourcePreference.SAMSUNG, listOf(s(SAMSUNG), s(HEALTH_CONNECT, consent = true)))
        assertEquals(SAMSUNG, choice.kind)
        assertTrue(choice.needsConsent)
    }

    @Test fun chosenSourceIsKeptEvenWhenNotInstalled() {
        val choice = chooseSource(SourcePreference.HEALTH_CONNECT, listOf(s(SAMSUNG, consent = true), s(HEALTH_CONNECT, NOT_INSTALLED)))
        assertEquals(HEALTH_CONNECT, choice.kind)
        assertFalse(choice.needsConsent)
    }

    @Test fun demoWhenChosen() {
        val choice = chooseSource(SourcePreference.DEMO, listOf(s(SAMSUNG, consent = true)))
        assertEquals(DEMO, choice.kind)
        assertFalse(choice.needsConsent)
    }

    @Test fun autoReportsASwitchFromLastTime() {
        val states = listOf(s(SAMSUNG), s(HEALTH_CONNECT, consent = true))
        assertEquals(SAMSUNG, chooseSource(SourcePreference.AUTO, states, previous = SAMSUNG).switchedFrom)
        assertNull(chooseSource(SourcePreference.AUTO, states, previous = HEALTH_CONNECT).switchedFrom)
        assertNull(chooseSource(SourcePreference.HEALTH_CONNECT, states, previous = SAMSUNG).switchedFrom)
    }

    @Test fun permissionStatusCountsOnlySupportedMetricsAsMissing() {
        val status = PermissionStatus(
            granted = setOf(HealthMetric.STEPS),
            supported = setOf(HealthMetric.STEPS, HealthMetric.SLEEP),
        )
        assertEquals(setOf(HealthMetric.SLEEP), status.missing)
        assertFalse(status.allGranted)
        assertFalse(status.noneGranted)
    }
}
