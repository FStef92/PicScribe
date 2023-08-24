package com.fs.picscribe.viewmodels

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.camera.core.Preview
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.preference.PreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject


@HiltViewModel
class TakePicActivityViewModel @Inject constructor() : ViewModel() {




    fun sendMessage(msg : String) {
        Log.d("PicScribe", "sendMessage: $msg")}

    private lateinit var sharedPreferences: SharedPreferences

    fun initSharedPreferences(context: Context) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    }

    fun getProjectName(): String {
        return sharedPreferences.getString("proj-name", "Default")!!
    }

    fun getSubfolder(): String {
        return sharedPreferences.getString("subfolder-name", "")!!
    }

    fun isCommentInFilenameEnabled(): Boolean {
        return sharedPreferences.getBoolean("comment-in-filename", false)
    }


    fun buildPreview() : Preview
    {
        return Preview.Builder().build()
    }



}