package com.example.imageresizer

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.media.ExifInterface
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.CheckBox
import android.widget.Toast
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.request.FutureTarget
import com.example.imageresizer.databinding.ActivityResizeBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ResizeActivity : AppCompatActivity() {
    private lateinit var binding : ActivityResizeBinding
    private lateinit var checkboxes: List<CheckBox>
    private lateinit var imageUri: Uri
    private lateinit var bitmap: FutureTarget<Bitmap>



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResizeBinding.inflate(layoutInflater)

        setContentView(binding.root)

        val imageUriString = intent.getStringExtra("imageURI")
        imageUri = Uri.parse(imageUriString)

        binding.rzImgView.setImageURI(imageUri)

        binding.widthET.isEnabled = false
        binding.heightET.isEnabled = false
        binding.percentET.isEnabled = false

        binding.widthET.alpha = 0.5f
        binding.heightET.alpha = 0.5f
        binding.percentET.alpha = 0.5f

        binding.rzBack.setOnClickListener {
            finish()
        }


        binding.decPercent.setOnClickListener {
            val currentValue = binding.percentET.text.toString().toIntOrNull()
            if (currentValue != null) {
                val newValue = currentValue - 10
                binding.percentET.setText(newValue.toString())
            }
        }


        binding.rzBtn.setOnClickListener {
            if (binding.widthET.text.toString() != "" && binding.heightET.text.toString() == ""){
                Log.d(TAG,"Calling For Auto")
                resizeImage(imageUri, desiredWidth = binding.widthET.text.toString().toInt())

            }

            else if (binding.widthET.text.toString() != "" && binding.heightET.text.toString() != ""){
                Log.d(TAG,"Calling For Not Auto")
                resizeImage(imageUri, desiredWidth = binding.widthET.text.toString().toInt(), desiredHeight = binding.heightET.text.toString().toInt())

            }

            else if (binding.percentET.text.toString() != ""){
                resizeImage(imageUri, percentage = binding.percentET.text.toString().toFloat()/100)

            }

        }

        checkboxes = listOf(
            findViewById(R.id.pxCheck),
            findViewById(R.id.pcCheck),
        )

        for (checkbox in checkboxes) {
            checkbox.setOnClickListener { onCheckboxClicked(checkbox) }
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
            R.id.pxCheck -> {
                // Checkbox 1 is checked
                binding.widthET.isEnabled = true
                binding.heightET.isEnabled = true
                binding.percentET.isEnabled = false

                binding.widthET.alpha = 1f
                binding.heightET.alpha = 1f
                binding.percentET.alpha = 0.5f
            }
            R.id.pcCheck -> {
                // Checkbox 2 is checked
                binding.widthET.isEnabled = false
                binding.heightET.isEnabled = false
                binding.percentET.isEnabled = true

                binding.widthET.alpha = 0.5f
                binding.heightET.alpha = 0.5f
                binding.percentET.alpha = 1f
            }
        }

        val allUnchecked = checkboxes.all { !it.isChecked }
        if (allUnchecked) {
            binding.widthET.isEnabled = false
            binding.heightET.isEnabled = false
            binding.percentET.isEnabled = false

            binding.widthET.alpha = 0.5f
            binding.heightET.alpha = 0.5f
            binding.percentET.alpha = 0.5f
        }


    }

    fun resizeImage(imageUri: Uri, desiredWidth: Int = 0, desiredHeight: Int = 0, percentage : Float = 0f){
        val dimensions = getImageDimensionsFromUri(applicationContext,imageUri)
        val imgWidth = dimensions.x
        val imgHeight = dimensions.y

        if (desiredWidth != 0 && desiredHeight == 0){
            Log.d(TAG,"Auto")
            val aspectRatio = dimensions.x.toFloat()/dimensions.y.toFloat()
            val targetHeight = desiredWidth.toFloat()/aspectRatio

            bitmap = Glide.with(this)
                .asBitmap()
                .load(imageUri)
                .override(desiredWidth, targetHeight.toInt())
                .encodeQuality(100)
                .submit()

        }

        if (desiredWidth != 0 && desiredHeight != 0){
            Log.d(TAG,"Not Auto")
            bitmap = Glide.with(this)
                .asBitmap()
                .load(imageUri)
                .centerCrop()
                .override(desiredWidth, desiredHeight)
                .encodeQuality(100)
                .submit()

        }

        else if (percentage != 0f){
            val newWidth = imgWidth*percentage
            val newHeight = imgHeight*percentage

            bitmap = Glide.with(this)
                .asBitmap()
                .load(imageUri)  // Provide the URL or path of the image
                .override(newWidth.toInt(), newHeight.toInt())  // Specify the target width and height
                .submit()  // ImageView where you want to display the resized image
        }

        GlobalScope.launch(Dispatchers.Default) {
            saveImageToGallery(applicationContext, bitmap.get())
            launch(Dispatchers.Main) {
                Toast.makeText(applicationContext, "Image saved to gallery", Toast.LENGTH_SHORT).show()
                finish()
            }

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


    fun saveImageToGallery(context: Context, bitmap: Bitmap){
        val resolver: ContentResolver = context.contentResolver

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "ImgResizer_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.WIDTH, 0)  // Set to 0 to use the original image width
            put(MediaStore.Images.Media.HEIGHT, 0) // Set to 0 to use the original image height
        }

        var imageUri: Uri? = null
        try {
            imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            imageUri?.let { uri ->
                val outputStream = resolver.openOutputStream(uri)
                outputStream?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 99, stream) // Compress and save the bitmap as JPEG with maximum quality
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }


}