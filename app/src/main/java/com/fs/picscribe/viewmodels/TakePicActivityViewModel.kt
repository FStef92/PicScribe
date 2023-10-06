package com.fs.picscribe.viewmodels

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.preference.PreferenceManager
import com.fs.picscribe.ApplicationClass
import dagger.hilt.android.lifecycle.HiltViewModel
import java.lang.ref.WeakReference
import javax.inject.Inject


@HiltViewModel
class TakePicActivityViewModel @Inject constructor(application: ApplicationClass) : AndroidViewModel(application) {
    private lateinit var projectName: String
    internal var isFlashOn = false
    private lateinit var sharedPreferences: SharedPreferences
    private val contextRef: WeakReference<Context> = WeakReference(application.applicationContext)

    fun init()
    {
        projectName = sharedPreferences.getString("proj-name", "Default")!!
        val contextResolver = contextRef.get()?.contentResolver
        if(contextResolver != null) {
                getFoldersUnderDCIM(contextResolver, projectName)
            }
    }



    fun getFoldersUnderDCIM(contentResolver: ContentResolver, completePath: String): List<String> {
        val folders = mutableListOf<String>()
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val selection = "${MediaStore.Images.Media.DATA} LIKE ?"
        val selectionArgs = arrayOf("%/DCIM/$completePath/%")

        val cursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )

        cursor?.use {
            val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            while (it.moveToNext()) {
                val path = it.getString(columnIndex)
                val folder = path.substringAfter("/DCIM/$completePath/").substringBefore("/")
                if (!folders.contains(folder)) {
                    folders.add(folder)
                }
            }
        }

        return folders
    }

    fun sendMessage(msg : String) {
        Log.d("PicScribe", "sendMessage: $msg")}



    fun initSharedPreferences() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplication<Application>().applicationContext)
    }

    fun getProjectName(): String {
        projectName = sharedPreferences.getString("proj-name", "Default")!!
        return projectName
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

    fun toggleFlash() {
        isFlashOn = !isFlashOn
        setFlashStatus(isFlashOn)
    }

    fun setFlashStatus(flashStatus: Boolean) {

    }


}