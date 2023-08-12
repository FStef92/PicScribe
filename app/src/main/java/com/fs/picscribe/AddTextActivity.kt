package com.fs.picscribe

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.SharedPreferences
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager
import com.fs.picscribe.ui.theme.PicScribeTheme
import java.io.IOException


class AddTextActivity : ComponentActivity()
{
    private var commentFilename = false
    lateinit var filenameFirstPart: String
    lateinit var filenameSecondPart: String
    private var sharedPreferences: SharedPreferences? = null
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)




            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

            sharedPreferences?.let { shP ->
                commentFilename = shP.getBoolean("comment-in-filename", false)
            }

        // Retrieve the image URI from the intent
        val imageUriString = intent.getStringExtra("imageUri")

        filenameFirstPart = intent.getStringExtra("filenameFirstPart") ?: ""
        filenameSecondPart = intent.getStringExtra("filenameSecondPart") ?: ""
        val imageUri = Uri.parse(imageUriString)
        // Get the Bitmap from the image URI
        val bitmap = this.contentResolver.openInputStream(imageUri)?.use {
            BitmapFactory.decodeStream(it)
        }


        // Set the Jetpack Compose content
        setContent {
            PicScribeTheme {
                AddTextScreen(bitmap, imageUri)
            }
        }
    }

    fun saveTextToExif(uri: Uri, commentText: String)
    {
        if (uri.path != null)
        {
            val contentResolver = this@AddTextActivity.contentResolver

            // Get the file's content URI using MediaStore
            val contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                .buildUpon()
                .appendPath(uri.path!!)
                .build()

            try
            {
                getContentResolver().openAssetFileDescriptor(uri, "rw").use { imagePfd ->
                    val exifInterface = ExifInterface(imagePfd!!.fileDescriptor)
                    exifInterface.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, commentText)
                    exifInterface.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, commentText)

                    exifInterface.setAttribute("XPTitle", commentText)
                    exifInterface.setAttribute("XPComment", commentText)
                    exifInterface.setAttribute(ExifInterface.TAG_USER_COMMENT, commentText)
                    exifInterface.saveAttributes()
            }
            } catch (e: IOException)
            {
                e.printStackTrace()
                Toast.makeText(this, "Could not edit Exif Metadata", Toast.LENGTH_SHORT).show()
            }

            if (commentFilename)
            {
                renameFile(uri, filenameFirstPart, filenameSecondPart, commentText)
            }

        }
    }

    private fun renameFile(uri: Uri, filenameFirstPart: String, filenameSecondPart: String, commentText: String) {
        val projection = arrayOf(MediaStore.Images.Media.DISPLAY_NAME)

        // Get the current filename using ContentProvider from MediaStore
        val cursor = contentResolver.query(uri, projection, null, null, null)
        val currName: String? = cursor?.use {
            if (it.moveToFirst())
            {
                try
                {
                    return@use it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME))
                } catch (e: IllegalArgumentException)
                {
                    Toast.makeText(this, "Could not find file or edit filename", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
            }

            null
        }

        currName?.apply {
            val newName = newFileName(currName, filenameFirstPart, filenameSecondPart, commentText) ?: currName
            val values = ContentValues()
            values.put(MediaStore.Images.Media.DISPLAY_NAME, newName)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            {
                /*                    val bundle = Bundle().apply {
                                        putBoolean(MediaStore.QUERY_ARG_MATCH_PENDING, true)
                                    }*/

                contentResolver.update(uri, values, null)
            } else
            {
                //TODO
            }
        }
    }


    fun newFileName(currName: String, filenameFirstPart: String, filenameSecondPart: String, commentText: String): String
    {

        val firstPartIndex = currName.indexOf(filenameFirstPart)
        val secondPartIndex = currName.indexOf(filenameSecondPart)

        if (firstPartIndex != -1 && secondPartIndex != -1 && firstPartIndex < secondPartIndex)
        {
            return StringBuilder()
                .append(currName.substring(0, firstPartIndex + filenameFirstPart.length))
                .append("_" + commentText.take(127))
                .append(currName.substring(secondPartIndex, secondPartIndex + filenameSecondPart.length))
                .toString()
        } else
        {
            return currName
        }

    }

    @Composable
    fun AddTextScreen(bitmap: Bitmap?, uri: Uri)
    {


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
            if (bitmap != null)
            {
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
            //var commentText by remember { mutableStateOf("") }

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
            Button(
                onClick = {
                    saveTextToExif(uri, commentText)
                    Toast.makeText(this@AddTextActivity, "Saved Comment", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Save Comment")
            }

        }
    }

    // Preview the screen
    @Preview
    @Composable
    fun PreviewAddTextScreen()
    {
        val placeholderBitmap =
            loadPlaceholderBitmapFromResource(R.drawable.ic_launcher_foreground) // Provide the resource ID for the placeholder bitmap
        // Use the loaded bitmap in your UI
        AddTextScreen(placeholderBitmap, Uri.EMPTY)
    }

    private fun loadPlaceholderBitmapFromResource(resourceId: Int): Bitmap
    {
        val resources: Resources = applicationContext.resources
        return BitmapFactory.decodeResource(resources, resourceId)
    }

}
