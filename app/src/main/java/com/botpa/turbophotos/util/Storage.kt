package com.botpa.turbophotos.util

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object Storage {

    //Storage specific
    private lateinit var preferences: SharedPreferences
    private val isInit: Boolean get() = Storage::preferences.isInitialized
    private const val LIST_SPLIT: String = "‚‗‚"


    //Init storage preferences
    @JvmStatic fun init(activity: Activity) {
        if (!isInit) preferences = activity.getPreferences(Context.MODE_PRIVATE)
    }

    //String list
    @JvmStatic fun getStringList(key: String): List<String> {
        val list = ArrayList<String>()
        if (isInit) {
            val listString = preferences.getString(key, null)
            if (listString != null) list.addAll(listString.split(LIST_SPLIT))
        }
        return list
    }

    @JvmStatic fun putStringList(key: String, value: List<String>) {
        if (isInit) preferences.edit { putString(key, if (value.isEmpty()) null else value.joinToString(LIST_SPLIT)) }
    }

    //String
    @JvmStatic fun getString(key: String, value: String): String? {
        return if (isInit) preferences.getString(key, value) else value
    }

    @JvmStatic fun putString(key: String, value: String) {
        if (isInit) preferences.edit { putString(key, value) }
    }

    //Boolean
    @JvmStatic fun getBool(key: String, value: Boolean): Boolean {
        return if (isInit) preferences.getBoolean(key, value) else value
    }

    @JvmStatic fun putBool(key: String, value: Boolean) {
        if (isInit) preferences.edit { putBoolean(key, value) }
    }

    //Int
    @JvmStatic fun getInt(key: String, value: Int): Int {
        return if (isInit) preferences.getInt(key, value) else value
    }

    @JvmStatic fun putInt(key: String, value: Int) {
        if (isInit) preferences.edit { putInt(key, value) }
    }

    //Float
    @JvmStatic fun getFloat(key: String, value: Float): Float {
        return if (isInit) preferences.getFloat(key, value) else value
    }

    @JvmStatic fun putFloat(key: String, value: Float) {
        if (isInit) preferences.edit { putFloat(key, value) }
    }

    //Long
    @JvmStatic fun getLong(key: String, value: Long): Long {
        return if (isInit) preferences.getLong(key, value) else value
    }

    @JvmStatic fun putLong(key: String, value: Long) {
        if (isInit) preferences.edit { putLong(key, value) }
    }
}
