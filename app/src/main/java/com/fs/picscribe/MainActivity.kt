package com.fs.picscribe


import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
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
import java.io.IOException
import java.io.InputStream
import java.nio.file.attribute.FileAttribute
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
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(createImageFile(this, null, null, null, null)).build()
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

    @Throws(IOException::class)
    private fun createImageFile(
            context: Context, bitmap: Bitmap, format: Bitmap.CompressFormat,
            mimeType: String, displayName: String
        ): File {
        val directoryName = "PicScribe"
        val currentTime = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            LocalTime.now().format(DateTimeFormatter.ofPattern("HHmmss"))
        } else
        {
            "HHmmss"
        }


        val filename = kotlin.io.path.createTempFile(currentTime, ".heic")

        val values = ContentValues()
        values.put(MediaStore.MediaColumns.TITLE, filename)
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
        values.put(MediaStore.MediaColumns.DATE_ADDED, System.currentTimeMillis() / 1000)

        if( android.os.Build.VERSION.SDK_INT >= 29 )
        {

            values.put( MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/" + directoryName );
            values.put( MediaStore.MediaColumns.DATE_TAKEN, System.currentTimeMillis() );
            values.put( MediaStore.MediaColumns.IS_PENDING, true );

            val uri = context.contentResolver.insert( MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values );
            if( uri != null )
            {
                return File(uri.path)
            }
        }
        else
        {
            val directory = File( Environment.getExternalStoragePublicDirectory( Environment.DIRECTORY_DCIM ), directoryName );
            directory.mkdirs();

            var file
            val fileIndex = 1;
            String filenameWithoutExtension = extension.length() > 0 ? filename.substring( 0, filename.length() - extension.length() - 1 ) : filename;
            String newFilename = filename;
            do
            {
                file = new File( directory, newFilename );
                newFilename = filenameWithoutExtension + fileIndex++;
                if( extension.length() > 0 )
                    newFilename += "." + extension;
            } while( file.exists() );

            try
            {
                if( WriteFileToStream( originalFile, new FileOutputStream( file ) ) )
                {
                    values.put( MediaStore.MediaColumns.DATA, file.getAbsolutePath() );
                    context.getContentResolver().insert( externalContentUri, values );

                    Log.d( "Unity", "Saved media to: " + file.getPath() );

                    // Refresh the Gallery
                    Intent mediaScanIntent = new Intent( Intent.ACTION_MEDIA_SCANNER_SCAN_FILE );
                    mediaScanIntent.setData( Uri.fromFile( file ) );
                    context.sendBroadcast( mediaScanIntent );
                }
            }
            catch( Exception e )
            {
                Log.e( "Unity", "Exception:", e );
            }
        }
    }

    fun WriteFileToStream( File file, OutputStream out )
    {
        try
        {
            InputStream in = new FileInputStream( file );
            try
            {
                byte[] buf = new byte[1024];
                int len;
                while( ( len = in.read( buf ) ) > 0 )
                    out.write( buf, 0, len );
            }
            finally
            {
                try
                {
                    in.close();
                }
                catch( Exception e )
                {
                    Log.e( "Unity", "Exception:", e );
                }
            }
        }
        catch( Exception e )
        {
            Log.e( "Unity", "Exception:", e );
            return false;
        }
        finally
        {
            try
            {
                out.close();
            }
            catch( Exception e )
            {
                Log.e( "Unity", "Exception:", e );
            }
        }

        return true;
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