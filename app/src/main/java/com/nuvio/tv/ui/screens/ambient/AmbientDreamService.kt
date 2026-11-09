package com.nuvio.tv.ui.screens.ambient

import android.service.dreams.DreamService
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.nuvio.tv.data.local.AmbientModeDataStore
import com.nuvio.tv.ui.theme.NuvioTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Nuvio Launcher: Ambient Mode as the TV's screensaver (Phase 8). The same slideshow, sources and settings as the
 * launcher's own Ambient Mode (catalogs, r/Wallpaper, r/EarthPorn or a custom subreddit), shown by Android wherever
 * the TV goes idle, in any app. Chosen once as the TV's screensaver; any key press ends it, as with every screensaver.
 */
@AndroidEntryPoint
class AmbientDreamService : DreamService(), LifecycleOwner, SavedStateRegistryOwner {

    @Inject lateinit var loader: AmbientSlideLoader
    @Inject lateinit var dataStore: AmbientModeDataStore

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    private var slides by mutableStateOf<List<AmbientSlide>>(emptyList())

    override fun onCreate() {
        super.onCreate()
        savedStateController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false   // any key ends the screensaver
        isFullscreen = true
        isScreenBright = true
        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@AmbientDreamService)
            setViewTreeSavedStateRegistryOwner(this@AmbientDreamService)
            setContent {
                NuvioTheme {
                    AmbientSlideshow(slides = slides, modifier = Modifier.fillMaxSize())
                }
            }
        }
        setContentView(view)
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        lifecycleScope.launch {
            slides = try {
                val settings = dataStore.settings.first()
                val subreddit = settings.subreddit
                if (subreddit != null) loader.loadSubreddit(subreddit) else loader.load(settings.catalogKeys)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    override fun onDreamingStopped() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        super.onDreamingStopped()
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        super.onDestroy()
    }
}
