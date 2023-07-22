package com.fs.picscribe

import android.content.ContentResolver
import android.content.ContentUris
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
import com.fs.picscribe.ui.theme.PicScribeTheme


class AddTextActivity : ComponentActivity()
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
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

            // Convert the content URI to a file path
 /*           val filePath = getFileFromContentUri(contentUri, contentResolver)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            {
                val parcelFileDescriptor = contentResolver.openFile(contentUri, "rw", CancellationSignal())
                parcelFileDescriptor.fileDescriptor.
            }*/
            try
            {
                getContentResolver().openFileDescriptor(uri, "rw").use { imagePfd ->
                    val exifInterface = ExifInterface(imagePfd!!.fileDescriptor)
                    exifInterface.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, commentText)
                    exifInterface.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, commentText)
                    exifInterface.setAttribute("XPTitle", commentText)
                    exifInterface.setAttribute("XPComment", commentText)
                    exifInterface.setAttribute(ExifInterface.TAG_USER_COMMENT, commentText)
                    exifInterface.saveAttributes()
                }
            } catch (e: Exception)
            {
                e.printStackTrace()
            }

/*            if (filePath != null)
            {
                // Create a new ExifInterface instance and pass the file path of your captured image
                val exifInterface = ExifInterface(filePath)

                // Write the comment text to the 'oggetto' field in EXIF metadata
                exifInterface.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, commentText)

                // Save the changes to the EXIF metadata
                exifInterface.saveAttributes()
            }*/
        }
    }


    fun getFileFromContentUri(picUri: Uri, contentResolver: ContentResolver): String?
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        {
            MediaStore.getMediaUri(this, picUri)
        }
        val collection =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(
                    MediaStore.VOLUME_EXTERNAL
                )
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.RELATIVE_PATH,
        )

        val cursor = contentResolver.query(collection, projection, null, null, null)
        cursor?.use {
            if (it.moveToFirst())
            {
                val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                // Cache column indices.
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)



                    // Get values of columns for a given video.
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn)


                    val contentUri: Uri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                return it.getString(columnIndex)
            }
        }
        return null
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
                    Toast.makeText(this@AddTextActivity,"Saved Comment", Toast.LENGTH_SHORT).show()
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
