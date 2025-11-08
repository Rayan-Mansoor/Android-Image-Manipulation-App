package com.example.imageresizer

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.FutureTarget
import com.example.imageresizer.databinding.ActivityResizeBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.IOException

private const val TAG = "ResizeActivity"

class ResizeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResizeBinding
    private lateinit var imageUri: Uri
    private lateinit var checkboxes: List<CheckBox>
    private lateinit var bitmapFuture: FutureTarget<Bitmap>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResizeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val imageUriString = intent.getStringExtra("imageURI")
        if (imageUriString.isNullOrEmpty()) {
            Toast.makeText(this, "Missing image URI", Toast.LENGTH_SHORT).show()
            finish(); return
        }
        imageUri = Uri.parse(imageUriString)

        binding.rzImgView.setImageURI(imageUri)

        // Disable inputs initially
        binding.widthET.isEnabled = false
        binding.heightET.isEnabled = false
        binding.percentET.isEnabled = false
        binding.widthET.alpha = 0.5f
        binding.heightET.alpha = 0.5f
        binding.percentET.alpha = 0.5f

        binding.rzBack.setOnClickListener { finish() }

        binding.decPercent.setOnClickListener {
            val current = binding.percentET.text.toString().toIntOrNull() ?: return@setOnClickListener
            binding.percentET.setText((current - 10).toString())
        }

        binding.rzBtn.setOnClickListener {
            when {
                binding.widthET.text.toString().isNotEmpty() && binding.heightET.text.toString().isEmpty() -> {
                    Log.d(TAG, "Auto height by aspect ratio")
                    resizeImage(imageUri, desiredWidth = binding.widthET.text.toString().toInt())
                }
                binding.widthET.text.toString().isNotEmpty() && binding.heightET.text.toString().isNotEmpty() -> {
                    Log.d(TAG, "Explicit width/height")
                    resizeImage(
                        imageUri,
                        desiredWidth = binding.widthET.text.toString().toInt(),
                        desiredHeight = binding.heightET.text.toString().toInt()
                    )
                }
                binding.percentET.text.toString().isNotEmpty() -> {
                    resizeImage(imageUri, percentage = binding.percentET.text.toString().toFloat() / 100f)
                }
                else -> Toast.makeText(this, "Enter width/height or percent", Toast.LENGTH_SHORT).show()
            }
        }

        checkboxes = listOf(binding.pxCheck, binding.pcCheck)
        checkboxes.forEach { cb -> cb.setOnClickListener { onCheckboxClicked(cb) } }
    }

    private fun onCheckboxClicked(clicked: CheckBox) {
        checkboxes.forEach { cb -> if (cb != clicked) cb.isChecked = false }

        when (clicked.id) {
            R.id.pxCheck -> {
                binding.widthET.isEnabled = true
                binding.heightET.isEnabled = true
                binding.percentET.isEnabled = false
                binding.widthET.alpha = 1f
                binding.heightET.alpha = 1f
                binding.percentET.alpha = 0.5f
            }
            R.id.pcCheck -> {
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

    private fun getImageDimensionsFromUri(context: Context, uri: Uri): Point {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }

        var w = options.outWidth
        var h = options.outHeight

        try {
            context.contentResolver.openInputStream(uri)?.use { ins ->
                val exif = ExifInterface(ins)
                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
                )
                if (orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
                    orientation == ExifInterface.ORIENTATION_ROTATE_270
                ) {
                    val tmp = w; w = h; h = tmp
                }
            }
        } catch (_: Exception) { /* ignore */ }

        return Point(w, h)
    }

    fun resizeImage(
        imageUri: Uri,
        desiredWidth: Int = 0,
        desiredHeight: Int = 0,
        percentage: Float = 0f
    ) {
        val dims = getImageDimensionsFromUri(applicationContext, imageUri)
        val imgW = dims.x
        val imgH = dims.y

        when {
            desiredWidth != 0 && desiredHeight == 0 -> {
                val aspect = imgW.toFloat() / imgH.toFloat()
                val targetH = (desiredWidth / aspect).toInt()
                bitmapFuture = Glide.with(this)
                    .asBitmap()
                    .load(imageUri)
                    .override(desiredWidth, targetH)
                    .submit()
            }
            desiredWidth != 0 && desiredHeight != 0 -> {
                bitmapFuture = Glide.with(this)
                    .asBitmap()
                    .load(imageUri)
                    .centerCrop()
                    .override(desiredWidth, desiredHeight)
                    .submit()
            }
            percentage != 0f -> {
                val newW = (imgW * percentage).toInt().coerceAtLeast(1)
                val newH = (imgH * percentage).toInt().coerceAtLeast(1)
                bitmapFuture = Glide.with(this)
                    .asBitmap()
                    .load(imageUri)
                    .override(newW, newH)
                    .submit()
            }
            else -> {
                Toast.makeText(this, "Invalid resize parameters", Toast.LENGTH_SHORT).show()
                return
            }
        }

        GlobalScope.launch(Dispatchers.Default) {
            try {
                val bmp = bitmapFuture.get()
                saveImageToGallery(applicationContext, bmp)
                launch(Dispatchers.Main) {
                    Toast.makeText(applicationContext, "Image saved to gallery", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                launch(Dispatchers.Main) {
                    Toast.makeText(applicationContext, "Resize failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun saveImageToGallery(context: Context, bitmap: Bitmap) {
        val cr = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "ImgResizer_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }
        val uri = cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        uri?.let { out ->
            try {
                cr.openOutputStream(out)?.use { stream ->
                    // Use a sane default (85) to avoid huge outputs
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}
