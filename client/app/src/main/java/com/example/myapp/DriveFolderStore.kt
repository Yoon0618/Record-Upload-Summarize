package com.example.myapp

import android.content.Context
import android.net.Uri
import androidx.core.content.edit

object DriveFolderStore {
    private const val PREF = "drive_pref"
    private const val KEY_URI = "tree_uri"

    fun save(context: Context, treeUri: Uri) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit {
            putString(KEY_URI, treeUri.toString())
        }
    }

    fun load(context: Context): Uri? {
        val s = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString(KEY_URI, null) ?: return null
        return Uri.parse(s)
    }
}
