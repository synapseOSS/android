package com.synapse.social.studioasinc.core.util

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.Spanned
import android.widget.TextView
import coil.ImageLoader
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.Disposable
import coil.target.Target
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.MarkwonSpansFactory
import io.noties.markwon.image.AsyncDrawable
import io.noties.markwon.image.AsyncDrawableLoader
import io.noties.markwon.image.AsyncDrawableScheduler
import io.noties.markwon.image.DrawableUtils
import io.noties.markwon.image.ImageSpanFactory
import org.commonmark.node.Image
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class CoilImagesPlugin private constructor(
    private val coilStore: CoilStore,
    private val imageLoader: ImageLoader
) : AbstractMarkwonPlugin() {

    interface CoilStore {
        fun load(drawable: AsyncDrawable): ImageRequest
        fun cancel(disposable: Disposable)
    }

    private val coilAsyncDrawableLoader = CoilAsyncDrawableLoader(coilStore, imageLoader)

    override fun configureSpansFactory(builder: MarkwonSpansFactory.Builder) {
        builder.setFactory(Image::class.java, ImageSpanFactory())
    }

    override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
        builder.asyncDrawableLoader(coilAsyncDrawableLoader)
    }

    override fun beforeSetText(textView: TextView, markdown: Spanned) {
        AsyncDrawableScheduler.unschedule(textView)
    }

    override fun afterSetText(textView: TextView) {
        AsyncDrawableScheduler.schedule(textView)
    }

    companion object {
        fun create(context: Context): CoilImagesPlugin {
            return create(context, context.imageLoader)
        }

        fun create(context: Context, imageLoader: ImageLoader): CoilImagesPlugin {
            return create(object : CoilStore {
                override fun load(drawable: AsyncDrawable): ImageRequest {
                    return ImageRequest.Builder(context)
                        .data(drawable.destination)
                        .build()
                }

                override fun cancel(disposable: Disposable) {
                    disposable.dispose()
                }
            }, imageLoader)
        }

        fun create(coilStore: CoilStore, imageLoader: ImageLoader): CoilImagesPlugin {
            return CoilImagesPlugin(coilStore, imageLoader)
        }
    }

    private class CoilAsyncDrawableLoader(
        private val coilStore: CoilStore,
        private val imageLoader: ImageLoader
    ) : AsyncDrawableLoader() {

        private val cache = ConcurrentHashMap<AsyncDrawable, Disposable>()

        override fun load(drawable: AsyncDrawable) {
            val loaded = AtomicBoolean(false)
            val target = AsyncDrawableTarget(drawable, loaded, cache)
            val request = coilStore.load(drawable).newBuilder()
                .target(target)
                .build()

            val disposable = imageLoader.enqueue(request)

            // if flag was not set, then job is running (else - finished before we got here)
            if (!loaded.get()) {
                loaded.set(true)
                cache[drawable] = disposable
            }
        }

        override fun cancel(drawable: AsyncDrawable) {
            val disposable = cache.remove(drawable)
            if (disposable != null) {
                coilStore.cancel(disposable)
            }
        }

        override fun placeholder(drawable: AsyncDrawable): Drawable? {
            return null
        }
    }

    private class AsyncDrawableTarget(
        private val drawable: AsyncDrawable,
        private val loaded: AtomicBoolean,
        private val cache: ConcurrentHashMap<AsyncDrawable, Disposable>
    ) : Target {

        override fun onSuccess(result: Drawable) {
            if (cache.remove(drawable) != null || !loaded.get()) {
                loaded.set(true)
                if (drawable.isAttached) {
                    DrawableUtils.applyIntrinsicBoundsIfEmpty(result)
                    drawable.result = result
                }
            }
        }

        override fun onStart(placeholder: Drawable?) {
            if (placeholder != null && drawable.isAttached) {
                DrawableUtils.applyIntrinsicBoundsIfEmpty(placeholder)
                drawable.result = placeholder
            }
        }

        override fun onError(error: Drawable?) {
            if (cache.remove(drawable) != null) {
                if (error != null && drawable.isAttached) {
                    DrawableUtils.applyIntrinsicBoundsIfEmpty(error)
                    drawable.result = error
                }
            }
        }
    }
}
