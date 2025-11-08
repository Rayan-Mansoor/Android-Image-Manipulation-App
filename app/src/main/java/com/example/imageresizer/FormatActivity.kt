package com.example.imageresizer

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.imageresizer.databinding.ActivityFormatBinding
import java.io.IOException
import java.util.Locale

class FormatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFormatBinding
    private lateinit var imageUri: Uri
    private var selectedFormat: String? = null
    private lateinit var checkboxes: List<CheckBox>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFormatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val imageUriString = intent.getStringExtra("imageURI")
        if (imageUriString.isNullOrEmpty()) {
            Toast.makeText(this, "Missing image URI", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        imageUri = Uri.parse(imageUriString)

        binding.frtImgView.setImageURI(imageUri)
        binding.fortBack.setOnClickListener { finish() }

        checkboxes = listOf(binding.jpgCheck, binding.pngCheck, binding.webpCheck)
        checkboxes.forEach { cb ->
            cb.setOnClickListener { onCheckboxClicked(cb) }
        }

        binding.frtBtn.setOnClickListener {
            val fmt = selectedFormat
            if (fmt.isNullOrBlank()) {
                Toast.makeText(this, "Please select a format", Toast.LENGTH_SHORT).show()
            } else {
                convertAndSaveImage(applicationContext, imageUri, fmt)
            }
        }
    }

    private fun onCheckboxClicked(clicked: CheckBox) {
        checkboxes.forEach { cb -> if (cb != clicked) cb.isChecked = false }
        selectedFormat = when (clicked.id) {
            R.id.jpgCheck -> "jpeg"
            R.id.pngCheck -> "png"
            R.id.webpCheck -> "webp"
            else -> null
        }
    }

    private fun computeInSampleSize(
        srcW: Int,
        srcH: Int,
        reqW: Int,
        reqH: Int
    ): Int {
        var inSampleSize = 1
        if (srcH > reqH || srcW > reqW) {
            var halfH = srcH / 2
            var halfW = srcW / 2
            while ((halfH / inSampleSize) >= reqH && (halfW / inSampleSize) >= reqW) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    fun convertAndSaveImage(context: Context, imageUri: Uri, outputFormat: String) {
        val cr = context.contentResolver

        // 1) Probe dimensions only
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        cr.openInputStream(imageUri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

        // 2) Downsample if ridiculously large (tune maxDim to your liking)
        val maxDim = 3000
        val inSample = computeInSampleSize(bounds.outWidth, bounds.outHeight, maxDim, maxDim)

        // 3) Decode (downsampled if needed)
        val decode = BitmapFactory.Options().apply {
            inSampleSize = inSample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val bmp = cr.openInputStream(imageUri)?.use { BitmapFactory.decodeStream(it, null, decode) }
        if (bmp == null) {
            Toast.makeText(context, "Decode failed", Toast.LENGTH_SHORT).show()
            return
        }

        // 4) Choose compress format + quality
        val (format, mime, quality) = when (outputFormat.lowercase(Locale.US)) {
            "jpeg", "jpg" -> Triple(Bitmap.CompressFormat.JPEG, "image/jpeg", 85)
            "png" -> Triple(Bitmap.CompressFormat.PNG, "image/png", 100) // lossless, bigger
            "webp" -> Triple(Bitmap.CompressFormat.WEBP, "image/webp", 80)
            else -> Triple(Bitmap.CompressFormat.JPEG, "image/jpeg", 85)
        }

        // 5) Save via MediaStore
        val displayName = "ImgResizer_${System.currentTimeMillis()}.${
            when (outputFormat.lowercase(Locale.US)) {
                "jpg" -> "jpg"
                "jpeg" -> "jpeg"
                "png" -> "png"
                "webp" -> "webp"
                else -> "jpeg"
            }
        }"

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, mime)
        }

        val outUri = cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        try {
            outUri?.let { uri ->
                cr.openOutputStream(uri)?.use { os ->
                    bmp.compress(format, quality, os)
                }
                Toast.makeText(context, "Image saved to gallery", Toast.LENGTH_SHORT).show()
                finish()
            } ?: Toast.makeText(context, "Save failed", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(context, "Save failed", Toast.LENGTH_SHORT).show()
        }
    }
}
