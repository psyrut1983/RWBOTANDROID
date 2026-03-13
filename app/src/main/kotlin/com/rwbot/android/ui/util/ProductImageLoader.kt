package com.rwbot.android.ui.util

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Загрузка фото товара из assets/product_images по артикулу продавца.
 * Имя файла в папке должно совпадать с артикулом (с расширением .png, .jpg или .jpeg).
 * При сборке папка res/product_images копируется в assets/product_images.
 */
object ProductImageLoader {

    private const val ASSETS_FOLDER = "product_images"
    private val EXTENSIONS = listOf(".png", ".jpg", ".jpeg")

    /**
     * Загружает изображение по артикулу продавца.
     * @return [BitmapPainter] или null, если файл не найден или артикул пустой.
     */
    fun load(context: Context, supplierArticle: String?): BitmapPainter? {
        if (supplierArticle.isNullOrBlank()) return null
        for (ext in EXTENSIONS) {
            val path = "$ASSETS_FOLDER/$supplierArticle$ext"
            runCatching {
                context.assets.open(path).use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()?.let { BitmapPainter(it) }
                }
            }.getOrNull()?.let { return it }
        }
        return null
    }

    /**
     * Загружает ImageBitmap в фоне (вызывать с Dispatchers.IO или from coroutine).
     */
    suspend fun loadAsImageBitmap(context: Context, supplierArticle: String?): androidx.compose.ui.graphics.ImageBitmap? =
        withContext(Dispatchers.IO) {
            if (supplierArticle.isNullOrBlank()) return@withContext null
            for (ext in EXTENSIONS) {
                val path = "$ASSETS_FOLDER/$supplierArticle$ext"
                runCatching {
                    context.assets.open(path).use { stream ->
                        BitmapFactory.decodeStream(stream)?.asImageBitmap()
                    }
                }.getOrNull()?.let { return@withContext it }
            }
            null
        }
}

/**
 * Показывает фото товара по артикулу продавца (загрузка в фоне).
 * Если артикул пустой или файл не найден — ничего не рисуется.
 */
@Composable
fun ProductImageByArticle(
    supplierArticle: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    val context = LocalContext.current
    var painter by remember(supplierArticle) { mutableStateOf<BitmapPainter?>(null) }
    LaunchedEffect(supplierArticle) {
        painter = withContext(Dispatchers.IO) {
            ProductImageLoader.load(context, supplierArticle)
        }
    }
    painter?.let { Image(painter = it, contentDescription = null, modifier = modifier, contentScale = contentScale) }
}
