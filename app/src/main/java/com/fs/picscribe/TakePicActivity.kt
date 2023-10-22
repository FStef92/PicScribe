package com.fs.picscribe

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.Surface
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.fs.picscribe.ui.theme.PicScribeTheme
import com.fs.picscribe.viewmodels.TakePicActivityViewModel
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class TakePicActivity : ComponentActivity() {
    private var commentFilename: Boolean = false
    private val TAG: String = "PLCSCRIBE"
    private lateinit var preview: androidx.camera.core.Preview
    var projectName = "Default"
    var subfolder = ""
    lateinit var takePicAVM: TakePicActivityViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        takePicAVM = ViewModelProvider(this).get(TakePicActivityViewModel::class.java)

        takePicAVM.initSharedPreferences()

        projectName = takePicAVM.getProjectName()
        subfolder = takePicAVM.getSubfolder()
        commentFilename = takePicAVM.isCommentInFilenameEnabled()

        preview = takePicAVM.buildPreview()


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





        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()


            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, takePicAVM.imageCapture)
            // Your code using cameraProvider goes here
        }, ContextCompat.getMainExecutor(this))


    }

    @Composable
    private fun CameraScreen() {
        val context = LocalContext.current
        val previewView = remember {
            PreviewView(context)
        }


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
                onClick = { toggleFlash() },
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = if (takePicAVM.isFlashOn) Icons.Filled.KeyboardArrowRight else Icons.Default.KeyboardArrowLeft,
                    contentDescription = "Toggle Flash"
                )
            }
        }
    }

    fun toggleFlash()
    {
        takePicAVM.toggleFlash()
    }

    @Composable
    fun CameraButton() {
        Button(
            onClick = {
                takePicAVM.launchCamera()
            }, modifier = Modifier.fillMaxWidth(0.4f)
        ) {
            Text(text = "Capture Photo")
        }

    }




    override fun onDestroy() {
        super.onDestroy()
    }
}