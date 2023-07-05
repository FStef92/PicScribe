package com.fs.picscribe


import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Surface.ROTATION_0
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)


        imageCapture = ImageCapture.Builder()
            .setTargetRotation(ROTATION_0)
            .build()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
cameraProviderFuture.addListener( {
    val cameraProvider = cameraProviderFuture.get()
    val preview = Preview.Builder()
        .build()

    val cameraSelector = CameraSelector.Builder()
        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
        .build()
    cameraProvider.bindToLifecycle(this, cameraSelector, imageCapture)
    // Your code using cameraProvider goes here
}, ContextCompat.getMainExecutor(this))



        setContent {
            PicScribeTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    Column {
                        Greeting("Android")
                        CameraButton()
                    }
                }
            }
        }
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
    fun CameraButton()
    {
        Button(
            onClick = {
                checkPermissionsAndLaunch()
            }, modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Capture Photo")
        }
    }


    private fun checkPermissionsAndLaunch()
    {
        var hasReadStoragePermission = true
        var hasWritePermission = true

        val permissionsToRequest = mutableListOf<String>()

        val hasCameraPermission= ContextCompat.checkSelfPermission(
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

        val values = ContentValues()
        values.put(MediaStore.MediaColumns.TITLE, filename)
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
        values.put(MediaStore.MediaColumns.DATE_ADDED, System.currentTimeMillis() / 1000)
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
        values.put(MediaStore.MediaColumns.MIME_TYPE, "image/heic")
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/PicScribe")

        val outPhotoUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI //.buildUpon().appendPath("PicScribe").build()


        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(
            contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values).build()

        imageCapture.takePicture(outputFileOptions, Executors.newSingleThreadExecutor(),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(error: ImageCaptureException)
                {
                    Log.d(TAG, "onError: ")// insert your code here.
                    error.printStackTrace()
                }
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    Log.d(TAG, "onImageSaved: ")// insert your code here.
                }
            })

    }

    private lateinit var currentPhotoPath: String


    private fun createFolderUri(directoryName: String, filename: String): Uri {


        val values = ContentValues()
        //values.put(MediaStore.MediaColumns.TITLE, filename)
        //values.put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
        //values.put(MediaStore.MediaColumns.DATE_ADDED, System.currentTimeMillis() / 1000)

        var retFile: Uri?
        val newUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI.buildUpon().appendPath("PicScribe").build()
        val directory = File(newUri.toString())
        directory.mkdirs()
        retFile = newUri
        return retFile
        if (Build.VERSION.SDK_INT >= 29) {
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/$directoryName")
            //values.put(MediaStore.MediaColumns.DATE_TAKEN, System.currentTimeMillis())
            values.put(MediaStore.MediaColumns.IS_PENDING, true)

            //contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            val uri: Uri? = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                //saveImageToStream(bitmap, context.contentResolver.openOutputStream(uri))
                values.put(MediaStore.Images.Media.IS_PENDING, false)
                contentResolver.update(uri, values, null, null)
                retFile = uri
            }

        } else {
            val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), directoryName)
            directory.mkdirs()
            retFile = Uri.fromFile(directory)
        }

        return retFile!!
    }
    override fun onDestroy() {
        super.onDestroy()
        cameraLauncher.unregister()
    }
}


@Composable
fun Greeting(name: String, modifier: Modifier = Modifier)
{
    Text(
        text = "Hello $name!", modifier = modifier
    )
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun GreetingPreview()
{
    PicScribeTheme {
        Greeting("Android")
    }
}