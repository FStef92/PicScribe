package com.fs.picscribe.viewmodels

import android.app.Application
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Surface
import android.view.WindowManager
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.AndroidViewModel
import androidx.preference.PreferenceManager
import com.fs.picscribe.AddTextActivity
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.lang.ref.WeakReference
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors
import javax.inject.Inject


@HiltViewModel
class TakePicActivityViewModel @Inject constructor(application: Application) : AndroidViewModel(application) {
    private val TAG: String = "PLCSCRIBE"
    private lateinit var projectName: String
    var subfolder = ""
    lateinit var imageCapture: ImageCapture
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
        imageCapture = ImageCapture.Builder().build()
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


    fun launchCamera() {
        // val outputFileOptions = ImageCapture.OutputFileOptions.Builder(createImageFile(this, null, null, null, null)).build()
        val currentDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val currentTime = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss"))
        } else {
            "HHmmss"
        }

        val filenameFirstPart = "$currentDate-$currentTime"

        val filenameSecondPart = "_PicScribe.heic"

        val filename = "$filenameFirstPart$filenameSecondPart"
        val baseFolder = "PicScribe"

        val completePath = run {
            if (subfolder.isNullOrEmpty()) {
                "$baseFolder/$projectName/"
            } else {
                "$baseFolder/$projectName/$subfolder/"
            }
        }
        val values = ContentValues()
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
        values.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/$completePath")

        values.put(MediaStore.MediaColumns.IS_PENDING, true)
        //TODO: ottenere la rotazione quando scatto la foto
        createFileUri(baseFolder, projectName, filename, values)?.let {
            val outputFileOptions = ImageCapture.OutputFileOptions.Builder(
                contextResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            ).build()

            val rotation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                this.display?.rotation ?: Surface.ROTATION_0
            } else {
                val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay.rotation
            }


            imageCapture.targetRotation = rotation
            imageCapture.flashMode = ImageCapture.FLASH_MODE_AUTO
            imageCapture.takePicture(outputFileOptions, Executors.newSingleThreadExecutor(),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onError(error: ImageCaptureException) {
                        Log.d(TAG, "onError: ")// insert your code here.
                        error.printStackTrace()
                    }


                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        val savedUri = outputFileResults.savedUri
                        val intent = Intent(this@TakePicActivity, AddTextActivity::class.java)
                        intent.putExtra("imageUri", savedUri.toString())
                        intent.putExtra("filenameFirstPart", filenameFirstPart)
                        intent.putExtra("filenameSecondPart", filenameSecondPart)
                        startActivity(intent)
                        Log.d(TAG, "onImageSaved: ")// insert your code here.
                    }
                })
        }



    }


     fun createFileUri(
        appName: String,
        directoryName: String,
        filename: String,
        values: ContentValues
    ): Uri? {
        var retFile: Uri? = null

        if (Build.VERSION.SDK_INT >= 29) {
            val uri: Uri? =
                contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                //values.put(MediaStore.Images.Media.IS_PENDING, false)
                //contentResolver.update(uri, values, null, null)
                retFile = uri
            }

        } else {
            val relativePath = "$appName/$directoryName/"
            val directory: File = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                relativePath
            )
            directory.mkdirs()
            retFile = Uri.fromFile(directory)
        }

        return retFile
    }

}