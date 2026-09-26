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
    fun init(activity: Activity) {
        if (!isInit) preferences = activity.getSharedPreferences("preferences", Context.MODE_PRIVATE)
    }

    //String list
    fun getStringList(key: String): MutableList<String> {
        val list = ArrayList<String>()
        if (isInit) {
            val listString = preferences.getString(key, null)
            if (listString != null) list.addAll(listString.split(LIST_SPLIT))
        }
        return list
    }

    fun putStringList(key: String, value: MutableList<String>) {
        if (isInit) preferences.edit { putString(key, if (value.isEmpty()) null else value.joinToString(LIST_SPLIT)) }
    }

    //String
    fun getString(key: String, fallback: String): String? {
        return if (isInit) preferences.getString(key, fallback) else fallback
    }

    fun getString(pair: StoragePair<String>): String? {
        return getString(pair.key, pair.value)
    }

    fun putString(key: String, value: String) {
        if (isInit) preferences.edit { putString(key, value) }
    }

    fun putString(pair: StoragePair<String>, value: String) {
        putString(pair.key, value)
    }

    //Boolean
    fun getBool(key: String, fallback: Boolean): Boolean {
        return if (isInit) preferences.getBoolean(key, fallback) else fallback
    }

    fun getBool(pair: StoragePair<Boolean>): Boolean {
        return getBool(pair.key, pair.value)
    }

    fun putBool(key: String, value: Boolean) {
        if (isInit) preferences.edit { putBoolean(key, value) }
    }

    fun putBool(pair: StoragePair<Boolean>, value: Boolean) {
        putBool(pair.key, value)
    }

    //Int
    fun getInt(key: String, fallback: Int): Int {
        return if (isInit) preferences.getInt(key, fallback) else fallback
    }

    fun getInt(pair: StoragePair<Int>): Int {
        return getInt(pair.key, pair.value)
    }

    fun putInt(key: String, value: Int) {
        if (isInit) preferences.edit { putInt(key, value) }
    }

    fun putInt(pair: StoragePair<Int>, value: Int) {
        putInt(pair.key, value)
    }

    //Float
    fun getFloat(key: String, fallback: Float): Float {
        return if (isInit) preferences.getFloat(key, fallback) else fallback
    }

    fun getFloat(pair: StoragePair<Float>): Float {
        return getFloat(pair.key, pair.value)
    }

    fun putFloat(key: String, value: Float) {
        if (isInit) preferences.edit { putFloat(key, value) }
    }

    fun putFloat(pair: StoragePair<Float>, value: Float) {
        putFloat(pair.key, value)
    }

    //Long
    fun getLong(key: String, fallback: Long): Long {
        return if (isInit) preferences.getLong(key, fallback) else fallback
    }

    fun getLong(pair: StoragePair<Long>): Long {
        return getLong(pair.key, pair.value)
    }

    fun putLong(key: String, value: Long) {
        if (isInit) preferences.edit { putLong(key, value) }
    }

    fun putLong(pair: StoragePair<Long>, value: Long) {
        putLong(pair.key, value)
    }

    //Enum
    inline fun <reified T : Enum<T>> getEnum(pair: StoragePair<String>, default: T): T {
        val name = getString(pair) ?: return default
        return runCatching { enumValueOf(name) as T }.getOrDefault(default)
    }

    //Storage pairs
    class StoragePair<T>(val key: String, val value: T)

}
