package com.fs.picscribe


import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
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
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date

class MainActivity : ComponentActivity()
{

    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        val imageCapture = ImageCapture.Builder()
            .setTargetRotation(ROTATION_0)
            .build()
val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
cameraProviderFuture.addListener(Runnable {
    val cameraProvider = cameraProviderFuture.get()
    val preview = Preview.Builder()
        .build()

    val cameraSelector = CameraSelector.Builder()
        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
        .build()
    cameraProvider.bindToLifecycle(this, cameraSelector, imageCapture,
        null, preview)
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
        var hasCameraPermission = true
        var hasStoragePermission = true
        val permissionsToRequest = mutableListOf<String>()

        hasCameraPermission = ContextCompat.checkSelfPermission(
            this@MainActivity, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasCameraPermission)
        {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
        {
            hasStoragePermission = ContextCompat.checkSelfPermission(
                this@MainActivity, Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasStoragePermission)
            {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }


        if (hasCameraPermission && hasStoragePermission)
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
                    this@MainActivity, "Permissions not granted", Toast.LENGTH_SHORT
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
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(File(...)).build()
        imageCapture.takePicture(outputFileOptions, Executors.,
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(error: ImageCaptureException)
                {
                    // insert your code here.
                }
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    // insert your code here.
                }
            })

    }

    lateinit var currentPhotoPath: String

    @Throws(IOException::class)
    private fun createImageFile(): File
    {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        if (storageDir.exists())
        {
            storageDir.delete()
        } else
        {
            storageDir.parentFile?.mkdirs()
        }
        try
        {
            storageDir.mkdirs()

        } catch (ex: IOException)
        {
            Toast.makeText(
                this@MainActivity, "Error Creating File", Toast.LENGTH_SHORT
            ).show()
        }
        return File.createTempFile(
            "${timeStamp}_", /* prefix */
            ".heic", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
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