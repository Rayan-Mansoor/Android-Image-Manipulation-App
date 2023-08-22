package com.example.imageresizer

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.imageresizer.databinding.ActivityCompressBinding
import com.example.imageresizer.databinding.TestBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class CompressActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCompressBinding
    private lateinit var imageUri: Uri
    private lateinit var checkboxes: List<CheckBox>

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityCompressBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val imageUriString = intent.getStringExtra("imageURI")
        imageUri = Uri.parse(imageUriString)

        binding.compImgView.setImageURI(imageUri)


        binding.percentET.isEnabled = false
        binding.percentET.alpha = 0.5f


        checkboxes = listOf(
            findViewById(R.id.autoCheck),
            findViewById(R.id.manualCheck),
        )

        for (checkbox in checkboxes) {
            checkbox.setOnClickListener { onCheckboxClicked(checkbox) }
        }

        binding.comBack.setOnClickListener {
            finish()
        }

        binding.decPercent.setOnClickListener {
            val currentValue = binding.percentET.text.toString().toIntOrNull()
            if (currentValue != null) {
                val newValue = currentValue - 10
                binding.percentET.setText(newValue.toString())
            }
        }


        binding.cpsBtn.setOnClickListener {

            if(binding.autoCheck.isChecked){
                val dimensions = getImageDimensionsFromUri(applicationContext,imageUri)
                val imgWidth = dimensions.x
                val imgHeight = dimensions.y

                var bitmap = Glide.with(this)
                    .asBitmap()
                    .load(imageUri)
                    .override(imgWidth, imgHeight)
                    .encodeFormat(Bitmap.CompressFormat.JPEG) // Set the desired image format (JPEG)
                    .encodeQuality(99) // Set the desired compression quality (0-100)
                    .submit()


                GlobalScope.launch(Dispatchers.Default) {
                    saveImageToGallery(applicationContext, bitmap.get(),99)
                    launch(Dispatchers.Main) {
                        Toast.makeText(applicationContext, "Image saved to gallery", Toast.LENGTH_SHORT).show()
                        finish()
                    }

                }
            }

            else if(binding.manualCheck.isChecked){

                    val dimensions = getImageDimensionsFromUri(applicationContext,imageUri)
                    val imgWidth = dimensions.x
                    val imgHeight = dimensions.y

                    var bitmap = Glide.with(this)
                        .asBitmap()
                        .load(imageUri)
                        .override(imgWidth, imgHeight)
                        .encodeFormat(Bitmap.CompressFormat.JPEG) // Set the desired image format (JPEG)
                        .encodeQuality(99) // Set the desired compression quality (0-100)
                        .submit()

                GlobalScope.launch(Dispatchers.Default) {
                    saveImageToGallery(applicationContext, bitmap.get(),binding.percentET.text.toString().toInt())
                    launch(Dispatchers.Main) {
                        Toast.makeText(applicationContext, "Image saved to gallery", Toast.LENGTH_SHORT).show()
                        finish()
                    }

                }


            }

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
            R.id.autoCheck -> {
                // Checkbox 1 is checked
                binding.percentET.isEnabled = false
                binding.percentET.alpha = 0.5f
            }
            R.id.manualCheck -> {
                // Checkbox 2 is checked

                binding.percentET.isEnabled = true
                binding.percentET.alpha = 1f
            }
        }

        val allUnchecked = checkboxes.all { !it.isChecked }
        if (allUnchecked) {

            binding.percentET.isEnabled = false

            binding.percentET.alpha = 0.5f
        }

    }


    fun getImageDimensionsFromUri(context: Context, uri: Uri): Point {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri), null, options)

        val imageWidth = options.outWidth
        val imageHeight = options.outHeight

        val exif = ExifInterface(context.contentResolver.openInputStream(uri)!!)
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        if (orientation == ExifInterface.ORIENTATION_ROTATE_90 || orientation == ExifInterface.ORIENTATION_ROTATE_270) {
            // Swap width and height if the image is rotated 90 or 270 degrees
            return Point(imageHeight, imageWidth)
        }

        return Point(imageWidth, imageHeight)
    }


     fun saveImageToGallery(context: Context, bitmap: Bitmap, quality : Int): Uri? {
        val resolver: ContentResolver = context.contentResolver

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "ImgResizer_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }

        var imageUri: Uri? = null
        try {
            imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            imageUri?.let { uri ->
                val outputStream = resolver.openOutputStream(uri)
                outputStream?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream) // Compress and save the bitmap as JPEG with maximum quality
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return imageUri
    }

    private fun calculateImageSizeKB(bitmap: Bitmap): Long {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        val byteArray = stream.toByteArray()
        val imageSizeBytes = byteArray.size.toLong()
        return imageSizeBytes / 1024
    }

}
