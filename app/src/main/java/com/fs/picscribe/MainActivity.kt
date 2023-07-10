package com.fs.picscribe


import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Surface.ROTATION_0
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.fs.picscribe.ui.theme.PicScribeTheme
import java.io.File
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors

class MainActivity : ComponentActivity()
{
    private val TAG: String = "PLCSCRIBE"
    private lateinit var imageCapture: ImageCapture
    private lateinit var preview: Preview
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        preview = Preview.Builder().build()

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


        val rotation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
        {
            this.display?.rotation ?: ROTATION_0
        } else
        {
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


        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK)
            {
                val imageUri: Uri? = result.data?.data
                // Start the AddTextActivity and pass the image URI
                val intent = Intent(this, AddTextActivity::class.java)
                intent.putExtra("imageUri", imageUri.toString())
                startActivity(intent)
            }
        }

    }

    @Composable
    private fun CameraScreen()
    {

        val context = LocalContext.current
        val previewView = remember {
            PreviewView(context)
        }

        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier.fillMaxSize()
        ){
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxWidth().fillMaxHeight()
            )
            {
                preview.setSurfaceProvider(previewView.surfaceProvider)
            }
            CameraButton()
        }


    }



    @Composable
    fun CameraButton()
    {
        Button(
            onClick = {
                checkPermissionsAndLaunch()
            }, modifier = Modifier.fillMaxWidth(0.4f)
        ) {
            Text(text = "Capture Photo")
        }
    }


    private fun checkPermissionsAndLaunch()
    {
        var hasReadStoragePermission = true
        var hasWritePermission = true

        val permissionsToRequest = mutableListOf<String>()

        val hasCameraPermission = ContextCompat.checkSelfPermission(
            this@MainActivity, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasCameraPermission)
        {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
        {
            hasReadStoragePermission = ContextCompat.checkSelfPermission(
                this@MainActivity, Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasReadStoragePermission)
            {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R)
        {

            hasWritePermission = ContextCompat.checkSelfPermission(
                this@MainActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasWritePermission)
            {
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        if (hasCameraPermission && hasReadStoragePermission && hasWritePermission)
        {
            launchCamera()
        } else
        {
            if (permissionsToRequest.size > 0)
            {
                requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
            } else
            {
                Toast.makeText(
                    this@MainActivity, "Permiss not granted", Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { isGranted ->
        if (isGranted.all { it.value })
        {
            launchCamera()
        } else
        {
            Toast.makeText(this, "\"Permissions Not Granted\"", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchCamera()
    {
        // val outputFileOptions = ImageCapture.OutputFileOptions.Builder(createImageFile(this, null, null, null, null)).build()
        val currentTime = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            LocalTime.now().format(DateTimeFormatter.ofPattern("HHmmss"))
        } else
        {
            "HHmmss"
        }
        val filename = currentTime + "_PicScribe.heic"
        val folder = "PicScribe"
        val projectName = "100_10_1 Fine"

        val values = ContentValues()

        //values.put(MediaStore.MediaColumns.TITLE, filename)
        //values.put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
        //values.put(MediaStore.MediaColumns.DATE_ADDED, System.currentTimeMillis() / 1000)
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
        values.put(MediaStore.MediaColumns.MIME_TYPE, "image/heic")
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/$folder/$projectName/")

        values.put(MediaStore.MediaColumns.IS_PENDING, true)

        createFileUri(folder, projectName, filename, values)?.let {
            val outputFileOptions = ImageCapture.OutputFileOptions.Builder(
                contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            ).build()

            imageCapture.takePicture(outputFileOptions, Executors.newSingleThreadExecutor(),
                object : ImageCapture.OnImageSavedCallback
                {
                    override fun onError(error: ImageCaptureException)
                    {
                        Log.d(TAG, "onError: ")// insert your code here.
                        error.printStackTrace()
                    }

                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults)
                    {
                        Log.d(TAG, "onImageSaved: ")// insert your code here.
                    }
                })

        }





    }

    private lateinit var currentPhotoPath: String


    private fun createFileUri(appName: String, directoryName: String, filename: String, values: ContentValues): Uri?
    {
        var retFile: Uri? = null

        if (Build.VERSION.SDK_INT >= 29)
        {
            val uri: Uri? = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null)
            {
                //values.put(MediaStore.Images.Media.IS_PENDING, false)
                //contentResolver.update(uri, values, null, null)
                retFile = uri
            }

        } else
        {
            val relativePath = "$appName/$directoryName/"
            val directory: File = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), relativePath)
            directory.mkdirs()
            retFile = Uri.fromFile(directory)
        }

        return retFile
    }

    override fun onDestroy()
    {
        super.onDestroy()
        cameraLauncher.unregister()
    }
}


