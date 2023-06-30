package com.example.picscribe

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.example.picscribe.ui.theme.PicScribeTheme

class MainActivity : ComponentActivity() {

    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PicScribeTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column {
                        Greeting("Android")
                        CameraButton()
                    }
                }
            }
        }
        cameraLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    val imageUri: Uri? = result.data?.data
                    // Start the AddTextActivity and pass the image URI
                    val intent = Intent(this, AddTextActivity::class.java)
                    intent.putExtra("imageUri", imageUri.toString())
                    startActivity(intent)
                }
            }

    }

    @Composable
    fun CameraButton() {
        Button(
            onClick = {
                val hasCameraPermission =
                    ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED

                val hasStoragePermission =
                    ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED

                if (hasCameraPermission && hasStoragePermission) {
                    launchCamera()
                } else {
                    val permissionsToRequest = mutableListOf<String>()

                    if (!hasCameraPermission) {
                        permissionsToRequest.add(Manifest.permission.CAMERA)
                    }

                    if (!hasStoragePermission) {
                        permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }

                    if (permissionsToRequest.isNotEmpty()) {
                        // Define a separate function to handle permission requests
                        fun requestNextPermission() {
                            if (permissionsToRequest.isNotEmpty()) {
                                val permission = permissionsToRequest.removeAt(0)
                                requestPermissionLauncher.launch(permission)
                            } else {
                                // All permissions have been requested
                                launchCamera()
                            }
                        }

                        // Request the first permission
                        requestNextPermission()
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Permissions not granted",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Capture Photo")
        }
    }


    private fun launchCamera() {
        val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
        cameraLauncher.launch(intent)
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()).newParam(ret: boolean) { isGranted: Boolean ->
            if (isGranted) {
                launchCamera()
            } else {
                Toast.makeText(this, "\"Permission Not Granted\"", Toast.LENGTH_SHORT).show()
            }
            return true;
        }


}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    PicScribeTheme {
        Greeting("Android")
    }
}