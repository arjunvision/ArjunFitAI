package com.arjunvision.arjunfitai.ui.workout

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.arjunvision.arjunfitai.ui.components.AfIcons
import com.arjunvision.arjunfitai.ui.theme.AfColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Decodes bundled exercise photos off the main thread, keeping recent ones in memory. */
private object PhotoCache {
    private val cache = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    suspend fun load(context: Context, path: String, thumb: Boolean): Bitmap? {
        val key = "$path#$thumb"
        cache.get(key)?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching {
                context.assets.open("exercises/$path").use { input ->
                    BitmapFactory.decodeStream(input, null, BitmapFactory.Options().apply { inSampleSize = if (thumb) 2 else 1 })
                }
            }.getOrNull()?.also { cache.put(key, it) }
        }
    }
}

/** A bundled exercise photo, or a dark placeholder with [label] while loading / if missing. */
@Composable
fun ExercisePhoto(path: String?, contentDescription: String?, modifier: Modifier, thumb: Boolean = false, label: String? = null) {
    val context = LocalContext.current
    val bitmap: ImageBitmap? by produceState<ImageBitmap?>(null, path, thumb) {
        value = path?.let { PhotoCache.load(context, it, thumb)?.asImageBitmap() }
    }
    Box(modifier.background(AfColors.Inner), contentAlignment = Alignment.Center) {
        val b = bitmap
        if (b != null) {
            Image(b, contentDescription, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(AfIcons.Workout, contentDescription = null, tint = AfColors.TextMuted, modifier = Modifier.size(if (thumb) 22.dp else 30.dp))
                label?.let { Text(it, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = AfColors.TextMuted) }
            }
        }
    }
}

