package com.fs.picscribe

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.PersistableBundle
import android.provider.MediaStore
import android.util.Log
import android.view.Surface
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.impl.ImageCaptureConfig
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight

import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.fs.picscribe.ui.theme.PicScribeTheme
import com.fs.picscribe.viewmodels.TakePicActivityViewModel
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.processor.internal.definecomponent.codegen._dagger_hilt_android_components_ViewModelComponent
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors
import javax.inject.Inject


@AndroidEntryPoint
class TakePicActivity : ComponentActivity() {
    private var commentFilename: Boolean = false
    private val TAG: String = "PLCSCRIBE"
    private lateinit var imageCapture: ImageCapture
    private lateinit var preview: androidx.camera.core.Preview
    var projectName = "Default"
    var subfolder = ""
    lateinit var takePicActivityViewModel: TakePicActivityViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        takePicActivityViewModel = ViewModelProvider(this).get(TakePicActivityViewModel::class.java)

        takePicActivityViewModel.initSharedPreferences(this)

        projectName = takePicActivityViewModel.getProjectName()
        subfolder = takePicActivityViewModel.getSubfolder()
        commentFilename = takePicActivityViewModel.isCommentInFilenameEnabled()


        preview = takePicActivityViewModel.buildPreview()

        setContent {
            PicScribeTheme {

                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CameraScreen()
                }
            }
        }


        val rotation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            this.display?.rotation ?: Surface.ROTATION_0
        } else {
            val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.rotation
        }



        imageCapture = ImageCapture.Builder()
            .setTargetRotation(rotation)
            .build()

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()


            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            // Your code using cameraProvider goes here
        }, ContextCompat.getMainExecutor(this))


    }

    @Composable
    private fun CameraScreen() {
        val context = LocalContext.current
        val previewView = remember {
            PreviewView(context)
        }

        var isFlashOn by remember { mutableStateOf(false) }

        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier.fillMaxSize()
        ) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
            ) {
                preview.setSurfaceProvider(previewView.surfaceProvider)
            }
            CameraButton()
            Spacer(modifier = Modifier.height(16.dp))
            IconButton(
                onClick = { isFlashOn = !isFlashOn },
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = if (isFlashOn) Icons.Filled.KeyboardArrowRight else Icons.Default.KeyboardArrowLeft,
                    contentDescription = "Toggle Flash"
                )
            }
        }
    }


    @Composable
    fun CameraButton() {
        Button(
            onClick = {
                launchCamera()
            }, modifier = Modifier.fillMaxWidth(0.4f)
        ) {
            Text(text = "Capture Photo")
        }

    }


    private fun launchCamera() {
        // val outputFileOptions = ImageCapture.OutputFileOptions.Builder(createImageFile(this, null, null, null, null)).build()
        val currentDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val currentTime = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss"))
        } else {
            "HHmmss"
        }

        val filenameFirstPart = "$currentDate-$currentTime"

        /*run {
                if (commentFilename)
                {*/

        /*            } else
                    {
                        ""
                    }}*/

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

        createFileUri(baseFolder, projectName, filename, values)?.let {
            val outputFileOptions = ImageCapture.OutputFileOptions.Builder(
                contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            )
                .build()

            val rotation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                this.display?.rotation ?: Surface.ROTATION_0
            } else {
                val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay.rotation
            }

            imageCapture.targetRotation = rotation
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

    private lateinit var currentPhotoPath: String


    private fun createFileUri(
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

    override fun onDestroy() {
        super.onDestroy()
    }
}