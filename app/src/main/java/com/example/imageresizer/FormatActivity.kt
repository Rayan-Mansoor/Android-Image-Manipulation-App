package com.example.imageresizer

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.widget.CheckBox
import android.widget.Toast
import com.example.imageresizer.databinding.ActivityFormatBinding
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FormatActivity : AppCompatActivity() {
    private lateinit var checkboxes: List<CheckBox>
    private lateinit var binding : ActivityFormatBinding
    private lateinit var format : String
    private lateinit var imageUri: Uri

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityFormatBinding.inflate(layoutInflater)

        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val imageUriString = intent.getStringExtra("imageURI")
        imageUri = Uri.parse(imageUriString)


        checkboxes = listOf(
            findViewById(R.id.jpgCheck),
            findViewById(R.id.pngCheck),
            findViewById(R.id.webpCheck)
        )

        for (checkbox in checkboxes) {
            checkbox.setOnClickListener { onCheckboxClicked(checkbox) }
        }

        binding.frtImgView.setImageURI(imageUri)

        binding.fortBack.setOnClickListener {
            finish()
        }
    }

    private fun onCheckboxClicked(clickedCheckbox: CheckBox) {
        for (checkbox in checkboxes) {
            if (checkbox != clickedCheckbox) {
                checkbox.isChecked = false
            }
        }

        // Perform actions based on the checked checkbox
        when (clickedCheckbox.id) {
            R.id.jpgCheck -> {
                // Checkbox 1 is checked
                format = "jpeg"
            }
            R.id.pngCheck -> {
                // Checkbox 2 is checked
                format = "png"
            }
            R.id.webpCheck -> {
                // Checkbox 3 is checked
                format = "webp"
            }
        }

        binding.frtBtn.setOnClickListener {
            convertAndSaveImage(applicationContext,imageUri, format)
        }




    }

    fun convertAndSaveImage(context: Context, imageUri: Uri, outputFormat: String) {
        val contentResolver: ContentResolver = context.contentResolver
        val inputStream = contentResolver.openInputStream(imageUri)

        // Decode the image file into a Bitmap
        val bitmap: Bitmap = BitmapFactory.decodeStream(inputStream)

        // Get the original image format
        val originalFormat: String? = contentResolver.getType(imageUri)

        // Convert the image format to the desired format
        val outputFileExtension = outputFormat.substringAfterLast('.')
        val compressFormat = when (outputFileExtension) {
            "jpeg", "jpg" -> Bitmap.CompressFormat.JPEG
            "png" -> Bitmap.CompressFormat.PNG
            "webp" -> Bitmap.CompressFormat.WEBP
            else -> Bitmap.CompressFormat.JPEG // Default to JPEG if the desired format is not recognized
        }

        // Create a new file to save the converted image
        val outputDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val outputFileName = "ImgResizer_$timeStamp.$outputFileExtension"
        val outputFile = File(outputDir, outputFileName)

        try {
            val outputStream = FileOutputStream(outputFile)

            // Compress and save the bitmap with the desired format
            bitmap.compress(compressFormat, 100, outputStream)
            outputStream.flush()
            outputStream.close()

            // Notify the media scanner about the new image
            MediaScannerConnection.scanFile(context, arrayOf(outputFile.absolutePath), null, null)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        Toast.makeText(applicationContext,"Image saved to gallery",Toast.LENGTH_SHORT).show()
        finish()
    }

}