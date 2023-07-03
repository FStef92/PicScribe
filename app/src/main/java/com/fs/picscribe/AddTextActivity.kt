package com.fs.picscribe

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fs.picscribe.R
import com.fs.picscribe.ui.theme.PicScribeTheme


class AddTextActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Retrieve the image URI from the intent
        val imageUriString = intent.getStringExtra("imageUri")
        val imageUri = Uri.parse(imageUriString)
        // Get the Bitmap from the image URI
        val bitmap = this.contentResolver.openInputStream(imageUri)?.use {
            BitmapFactory.decodeStream(it)
        }

        // Set the Jetpack Compose content
        setContent {
            PicScribeTheme {
                AddTextScreen(bitmap)
            }
        }
    }

    @Composable
    fun AddTextScreen(bitmap: Bitmap?) {


        // State for the comment text
        var commentText by rememberSaveable { mutableStateOf("") }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Display the image
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Image",
                    modifier = Modifier
                        .aspectRatio(1f)
                        .fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Add a text box for adding comments
            var commentText by remember { mutableStateOf("") }

            OutlinedTextField(
                value = commentText,
                onValueChange = { newCommentText ->
                    commentText = newCommentText
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = "Add Comment") },
                maxLines = 5,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Add more content as needed...
        }
    }

    // Preview the screen
    @Preview
    @Composable
    fun PreviewAddTextScreen() {
        val placeholderBitmap = loadPlaceholderBitmapFromResource(R.drawable.ic_launcher_foreground) // Provide the resource ID for the placeholder bitmap
        // Use the loaded bitmap in your UI
        AddTextScreen(placeholderBitmap)
    }

    private fun loadPlaceholderBitmapFromResource(resourceId: Int): Bitmap {
        val resources: Resources = applicationContext.resources
        return BitmapFactory.decodeResource(resources, resourceId)
    }

}
