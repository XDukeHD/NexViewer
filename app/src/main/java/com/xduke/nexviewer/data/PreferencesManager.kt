package com.xduke.nexviewer.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.xduke.nexviewer.ui.screens.WidgetType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

import java.lang.reflect.Type

private val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class PreferencesManager(private val context: Context) {
    private val gson = Gson()

    companion object {
        private val WIDGET_ORDER_KEY = stringPreferencesKey("widget_order")
        private val WIDGET_VISIBILITY_KEY = stringPreferencesKey("widget_visibility")
    }

    val widgetOrder: Flow<List<WidgetType>> = context.userDataStore.data.map { prefs ->
        val json = prefs[WIDGET_ORDER_KEY]
        if (json != null) {
            try {
                val type: Type = object : TypeToken<List<WidgetType>>() {}.type
                gson.fromJson(json, type) ?: WidgetType.values().toList()
            } catch (e: Exception) {
                e.printStackTrace()
                WidgetType.values().toList()
            }
        } else {
            WidgetType.values().toList()
        }
    }

    val widgetVisibility: Flow<Map<WidgetType, Boolean>> = context.userDataStore.data.map { prefs ->
        val json = prefs[WIDGET_VISIBILITY_KEY]
        if (json != null) {
            try {
                val type: Type = object : TypeToken<Map<WidgetType, Boolean>>() {}.type
                gson.fromJson(json, type) ?: WidgetType.values().associateWith { true }
            } catch (e: Exception) {
                e.printStackTrace()
                WidgetType.values().associateWith { true }
            }
        } else {
            WidgetType.values().associateWith { true }
        }
    }

    suspend fun saveWidgetOrder(order: List<WidgetType>) {
        context.userDataStore.edit { prefs ->
            prefs[WIDGET_ORDER_KEY] = gson.toJson(order)
        }
    }

    suspend fun saveWidgetVisibility(visibility: Map<WidgetType, Boolean>) {
        context.userDataStore.edit { prefs ->
            prefs[WIDGET_VISIBILITY_KEY] = gson.toJson(visibility)
        }
    }
}
