package com.fs.picscribe


import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.preference.*

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity()
{
    private val TAG: String = "PLCSCRIBE"

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        PreferenceManager.setDefaultValues(this, R.xml.preferences, true)
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        setContent {

            val projectName = remember { mutableStateOf(sharedPreferences.getString("proj-name", "Default")?: "default") }
            val subfolder = remember { mutableStateOf(sharedPreferences.getString("subfolder-name", "")?: "")  }
            val writeCommentInFilename = remember { mutableStateOf(sharedPreferences.getBoolean("comment-in-filename", false)) }

            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Project Name:")
                    OutlinedTextField(
                        value = projectName.value,
                        onValueChange = { projectName.value = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Subfolder:")
                    OutlinedTextField(
                        value = subfolder.value,
                        onValueChange = { subfolder.value = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = writeCommentInFilename.value,
                        onCheckedChange = { writeCommentInFilename.value = it

                                // do something with this preference

                                          },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(text = "Write comment also in filename")
                }
                Button(
                    onClick = { checkPermissionsAndLaunch() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Launch Camera")
                }
            }
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
            val intent = Intent(this@MainActivity, TakePictureActivity::class.java)
            startActivity(intent)
        }

}


