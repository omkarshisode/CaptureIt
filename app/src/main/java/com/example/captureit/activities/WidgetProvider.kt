package com.example.captureit.activities

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.example.captureit.R
import com.example.captureit.services.LocationService
import com.example.captureit.services.ServiceAction

/**
 * WidgetProvider manages the behavior and updates of the app's home screen widget.
 * The widget allows users to toggle the location service and launch the camera activity.
 */
class WidgetProvider : AppWidgetProvider() {

    companion object {
        private const val SWITCH_ACTION = "com.example.appwidget.SWITCH_ACTION"
        private const val EXTRA_SWITCH_STATE = "EXTRA_SWITCH_STATE"
        private const val PREFS_NAME = "WidgetPrefs"
        private const val SWITCH_STATE_KEY_PREFIX = "switch_state_"
    }

    /**
     * Updates the widgets when triggered by the AppWidgetManager.
     * Sets up intents for interacting with the widget's buttons and toggles.
     *
     * @param context The context in which the widget is updated.
     * @param appWidgetManager The AppWidgetManager instance.
     * @param appWidgetIds The IDs of the widgets to update.
     */
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onUpdate(
        context: Context?,
        appWidgetManager: AppWidgetManager?,
        appWidgetIds: IntArray?,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        if (context != null && appWidgetIds != null) {
            for (appWidgetId in appWidgetIds) {
                val remoteViews = RemoteViews(context.packageName, R.layout.widget_layout)

                // Intent for the "Click Image" button to launch CameraActivity
                val cameraIntent = Intent(context, CameraActivity::class.java)
                val cameraPendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    cameraIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                remoteViews.setOnClickPendingIntent(R.id.widget_button_camera, cameraPendingIntent)

                // Intent for toggling the location service
                saveSwitchState(context, appWidgetId, true)
                val switchIntent = Intent(context, WidgetProvider::class.java).apply {
                    action = SWITCH_ACTION
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                val switchPendingIntent = PendingIntent.getBroadcast(
                    context,
                    appWidgetId,
                    switchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                remoteViews.setOnClickPendingIntent(R.id.widget_switch_location, switchPendingIntent)

                // Update the widget
                appWidgetManager?.updateAppWidget(appWidgetId, remoteViews)
            }
        }
    }

    /**
     * Handles broadcast intents sent to the widget, such as toggle actions.
     * Manages the state of the location service based on user interaction with the widget.
     *
     * @param context The context of the receiver.
     * @param intent The received intent containing action and data.
     */
    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)

        if (context == null || intent == null) {
            Log.e("WidgetProvider", "Received null context or intent")
            return
        }

        // Handle service toggle logic when the switch is clicked
        if (intent.action == SWITCH_ACTION) {
            val serviceIntent = Intent(context, LocationService::class.java)
            val widgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )

            // Retrieve and toggle the switch state
            val switchState = getSwitchState(context, widgetId)
            if (switchState) {
                saveSwitchState(context, widgetId, false)
                serviceIntent.action = ServiceAction.START_SERVICE.toString()
                // Start the location service
                ContextCompat.startForegroundService(context.applicationContext, serviceIntent)
            } else {
                saveSwitchState(context, widgetId, true)
                serviceIntent.action = ServiceAction.STOP_SERVICE.toString()
                // Stop the location service
                context.stopService(serviceIntent)
            }
        }
    }

    /**
     * Saves the switch state for a specific widget ID to shared preferences.
     *
     * @param context The context for accessing shared preferences.
     * @param widgetId The widget ID associated with the state.
     * @param state The new switch state to save.
     */
    private fun saveSwitchState(context: Context, widgetId: Int, state: Boolean) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean(SWITCH_STATE_KEY_PREFIX + widgetId, state).apply()
    }

    /**
     * Retrieves the saved switch state for a specific widget ID from shared preferences.
     *
     * @param context The context for accessing shared preferences.
     * @param widgetId The widget ID associated with the state.
     * @return The saved switch state, or false if no state was found.
     */
    private fun getSwitchState(context: Context, widgetId: Int): Boolean {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean(SWITCH_STATE_KEY_PREFIX + widgetId, false)
    }
}
