package com.arjunvision.arjunfitai.data.health

/** What the user picked in Settings. AUTO follows [SourceKind] order but never picks Demo by itself. */
enum class SourcePreference { AUTO, SAMSUNG, HEALTH_CONNECT, DEMO }

/** What ArjunFit knows about one source right now. */
data class SourceState(val kind: SourceKind, val availability: Availability, val hasConsent: Boolean)

/**
 * The source to read from. [kind] null = no real source is usable (offer the choices, Demo included).
 * [needsConsent] = show that source's permission request before reading anything.
 * [switchedFrom] = AUTO moved away from the source used last time, so the UI should say so.
 */
data class SourceChoice(val kind: SourceKind?, val needsConsent: Boolean, val switchedFrom: SourceKind? = null)

/**
 * Picks the source (pure, unit-tested).
 * - A source chosen in Settings is always kept, even if it currently lacks permission: we ask, never switch.
 * - AUTO: the first real source (Samsung, then Health Connect) that is available AND has consent;
 *   otherwise the first available one, to ask for consent; otherwise none.
 * - Demo is used only when chosen, so sample data is never shown as if it were yours.
 */
fun chooseSource(
    preference: SourcePreference,
    states: List<SourceState>,
    previous: SourceKind? = null,
): SourceChoice {
    val choice = when (preference) {
        SourcePreference.SAMSUNG -> chosen(SourceKind.SAMSUNG, states)
        SourcePreference.HEALTH_CONNECT -> chosen(SourceKind.HEALTH_CONNECT, states)
        SourcePreference.DEMO -> SourceChoice(SourceKind.DEMO, needsConsent = false)
        SourcePreference.AUTO -> {
            val real = states.filter { it.kind != SourceKind.DEMO && it.availability == Availability.AVAILABLE }
                .sortedBy { it.kind.ordinal }
            real.firstOrNull { it.hasConsent }?.let { SourceChoice(it.kind, needsConsent = false) }
                ?: real.firstOrNull()?.let { SourceChoice(it.kind, needsConsent = true) }
                ?: SourceChoice(null, needsConsent = false)
        }
    }
    val switched = previous?.takeIf { preference == SourcePreference.AUTO && choice.kind != null && it != choice.kind }
    return choice.copy(switchedFrom = switched)
}

private fun chosen(kind: SourceKind, states: List<SourceState>): SourceChoice {
    val state = states.firstOrNull { it.kind == kind }
    return SourceChoice(kind, needsConsent = state != null && state.availability == Availability.AVAILABLE && !state.hasConsent)
}
