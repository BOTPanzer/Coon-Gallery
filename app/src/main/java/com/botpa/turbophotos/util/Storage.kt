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
        if (!isInit) preferences = activity.getSharedPreferences("preferences", Context.MODE_PRIVATE)
    }

    //String list
    @JvmStatic fun getStringList(key: String): MutableList<String> {
        val list = ArrayList<String>()
        if (isInit) {
            val listString = preferences.getString(key, null)
            if (listString != null) list.addAll(listString.split(LIST_SPLIT))
        }
        return list
    }

    @JvmStatic fun putStringList(key: String, value: MutableList<String>) {
        if (isInit) preferences.edit { putString(key, if (value.isEmpty()) null else value.joinToString(LIST_SPLIT)) }
    }

    //String
    @JvmStatic fun getString(key: String, fallback: String): String? {
        return if (isInit) preferences.getString(key, fallback) else fallback
    }

    @JvmStatic fun putString(key: String, value: String) {
        if (isInit) preferences.edit { putString(key, value) }
    }

    @JvmStatic fun getString(pair: StoragePair<String>): String? {
        return getString(pair.key, pair.value)
    }

    //Boolean
    @JvmStatic fun getBool(key: String, fallback: Boolean): Boolean {
        return if (isInit) preferences.getBoolean(key, fallback) else fallback
    }

    @JvmStatic fun putBool(key: String, value: Boolean) {
        if (isInit) preferences.edit { putBoolean(key, value) }
    }

    @JvmStatic fun getBool(pair: StoragePair<Boolean>): Boolean {
        return getBool(pair.key, pair.value)
    }

    //Int
    @JvmStatic fun getInt(key: String, fallback: Int): Int {
        return if (isInit) preferences.getInt(key, fallback) else fallback
    }

    @JvmStatic fun putInt(key: String, value: Int) {
        if (isInit) preferences.edit { putInt(key, value) }
    }

    @JvmStatic fun getInt(pair: StoragePair<Int>): Int {
        return getInt(pair.key, pair.value)
    }

    //Float
    @JvmStatic fun getFloat(key: String, fallback: Float): Float {
        return if (isInit) preferences.getFloat(key, fallback) else fallback
    }

    @JvmStatic fun putFloat(key: String, value: Float) {
        if (isInit) preferences.edit { putFloat(key, value) }
    }

    @JvmStatic fun getFloat(pair: StoragePair<Float>): Float {
        return getFloat(pair.key, pair.value)
    }

    //Long
    @JvmStatic fun getLong(key: String, fallback: Long): Long {
        return if (isInit) preferences.getLong(key, fallback) else fallback
    }

    @JvmStatic fun putLong(key: String, value: Long) {
        if (isInit) preferences.edit { putLong(key, value) }
    }

    @JvmStatic fun getLong(pair: StoragePair<Long>): Long {
        return getLong(pair.key, pair.value)
    }

    //Storage pairs
    class StoragePair<T>(val key: String, val value: T)

}
