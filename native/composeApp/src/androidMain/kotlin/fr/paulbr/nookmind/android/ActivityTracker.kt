package fr.paulbr.nookmind.android

import android.app.Activity
import android.app.Application
import android.os.Bundle

/**
 * Keeps a reference to the visible Activity: Credential Manager and the runtime permission
 * request both need one, and the shared code only knows about the application context.
 */
object ActivityTracker : Application.ActivityLifecycleCallbacks {
    var current: Activity? = null
        private set

    fun install(application: Application) = application.registerActivityLifecycleCallbacks(this)

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) { current = activity }
    override fun onActivityResumed(activity: Activity) { current = activity }
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) { if (current === activity) current = null }
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) { if (current === activity) current = null }
}
