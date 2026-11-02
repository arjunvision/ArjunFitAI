package com.arjunvision.arjunfitai.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** A detail-screen read: in progress, done, or failed with a message. */
sealed interface Loaded<out T> {
    data object Loading : Loaded<Nothing>
    data class Ready<T>(val value: T) : Loaded<T>
    data class Failed(val message: String) : Loaded<Nothing>
}

/** Runs [load] whenever [keys] change and exposes the result as [Loaded]. */
@Composable
fun <T> rememberLoaded(vararg keys: Any?, load: suspend () -> Result<T>): Loaded<T> =
    produceState<Loaded<T>>(Loaded.Loading, *keys) {
        value = Loaded.Loading
        value = load().fold({ Loaded.Ready(it) }, { Loaded.Failed(it.message ?: "Couldn't read Samsung Health") })
    }.value

/** Scrolling column with the detail screens' 20dp padding and 14dp spacing. */
@Composable
fun DetailColumn(contentPadding: PaddingValues, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(contentPadding).verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content,
    )
}
