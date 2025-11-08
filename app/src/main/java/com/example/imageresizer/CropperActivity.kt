package com.example.imageresizer

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import com.canhub.cropper.CropImageView
import com.example.imageresizer.R
import com.example.imageresizer.databinding.ActivityCropperBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CropperActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCropperBinding
    private var square = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCropperBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Crop Image"
        binding.toolbar.setNavigationOnClickListener { finish() }

        val inUri = intent.getStringExtra(EXTRA_IMAGE_URI)?.let(Uri::parse)
        square = intent.getBooleanExtra(EXTRA_SQUARE, false)

        // Configure CropImageView
        binding.cropImageView.apply {
            setImageUriAsync(inUri)
            guidelines = CropImageView.Guidelines.ON
            isAutoZoomEnabled = true
            if (square) {
                setFixedAspectRatio(true)
                setAspectRatio(1, 1)
            } else {
                setFixedAspectRatio(false)
            }
        }

        binding.btnCancel.setOnClickListener { finish() }
        binding.btnDone.setOnClickListener {
            // Do the synchronous crop off the main thread for compatibility across versions.
            GlobalScope.launch(Dispatchers.Default) {
                val bmp: Bitmap? = binding.cropImageView.getCroppedImage()
                val resultUri = bmp?.let { saveBitmapToMediaStore(this@CropperActivity, it, 95) }

                withContext(Dispatchers.Main) {
                    if (resultUri != null) {
                        setResult(Activity.RESULT_OK, Intent().setData(resultUri))
                    } else {
                        setResult(
                            Activity.RESULT_CANCELED,
                            Intent().putExtra("error", "Crop returned null")
                        )
                    }
                    finish()
                }
            }
        }
    }

    private fun saveBitmapToMediaStore(ctx: Context, bitmap: Bitmap, quality: Int): Uri? {
        val resolver = ctx.contentResolver
        val values = ContentValues().apply {
            put(
                MediaStore.Images.Media.DISPLAY_NAME,
                "ImgResizer_${System.currentTimeMillis()}.jpg"
            )
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        uri?.let {
            resolver.openOutputStream(it)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(0, 100), out)
            }
        }
        return uri
    }

    companion object {
        const val EXTRA_IMAGE_URI = "imageUri"
        const val EXTRA_SQUARE = "square"
        const val REQUEST_CROP = 4242
    }
}