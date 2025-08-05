package com.botpa.turbophotos.util

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object Storage {

    //Storage specific
    private lateinit var preferences: SharedPreferences
    private val isLoaded: Boolean get() = Storage::preferences.isInitialized
    private const val LIST_SPLIT: String = "‚‗‚"


    //Load storage preferences
    @JvmStatic fun load(activity: Activity) {
        if (!isLoaded) preferences = activity.getPreferences(Context.MODE_PRIVATE)
    }

    //String list
    @JvmStatic fun getStringList(key: String): ArrayList<String> {
        val list = ArrayList<String>()
        if (isLoaded) {
            val listString = preferences.getString(key, null)
            if (listString != null) list.addAll(listString.split(LIST_SPLIT))
        }
        return list
    }

    @JvmStatic fun putStringList(key: String, value: ArrayList<String>) {
        if (isLoaded) preferences.edit { putString(key, if (value.isEmpty()) null else value.joinToString(LIST_SPLIT)) }
    }

    //String
    @JvmStatic fun getString(key: String, value: String): String? {
        return if (isLoaded) preferences.getString(key, value) else value
    }

    @JvmStatic fun putString(key: String, value: String) {
        if (isLoaded) preferences.edit { putString(key, value) }
    }

    //Boolean
    @JvmStatic fun getBool(key: String, value: Boolean): Boolean {
        return if (isLoaded) preferences.getBoolean(key, value) else value
    }

    @JvmStatic fun putBool(key: String, value: Boolean) {
        if (isLoaded) preferences.edit { putBoolean(key, value) }
    }

    //Int
    @JvmStatic fun getInt(key: String, value: Int): Int {
        return if (isLoaded) preferences.getInt(key, value) else value
    }

    @JvmStatic fun putInt(key: String, value: Int) {
        if (isLoaded) preferences.edit { putInt(key, value) }
    }

    //Float
    @JvmStatic fun getFloat(key: String, value: Float): Float {
        return if (isLoaded) preferences.getFloat(key, value) else value
    }

    @JvmStatic fun putFloat(key: String, value: Float) {
        if (isLoaded) preferences.edit { putFloat(key, value) }
    }

    //Long
    @JvmStatic fun getLong(key: String, value: Long): Long {
        return if (isLoaded) preferences.getLong(key, value) else value
    }

    @JvmStatic fun putLong(key: String, value: Long) {
        if (isLoaded) preferences.edit { putLong(key, value) }
    }
}
