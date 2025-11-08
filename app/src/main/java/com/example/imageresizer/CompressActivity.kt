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
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.imageresizer.databinding.ActivityCompressBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.IOException

class CompressActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCompressBinding
    private lateinit var imageUri: Uri
    private lateinit var checkboxes: List<CheckBox>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCompressBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val imageUriString = intent.getStringExtra("imageURI")
        if (imageUriString.isNullOrEmpty()) {
            Toast.makeText(this, "Missing image URI", Toast.LENGTH_SHORT).show()
            finish(); return
        }
        imageUri = Uri.parse(imageUriString)

        binding.compImgView.setImageURI(imageUri)

        // default: auto checked
        binding.percentET.isEnabled = false
        binding.percentET.alpha = 0.5f

        checkboxes = listOf(binding.autoCheck, binding.manualCheck)
        checkboxes.forEach { cb -> cb.setOnClickListener { onCheckboxClicked(cb) } }

        binding.comBack.setOnClickListener { finish() }

        binding.decPercent.setOnClickListener {
            val current = binding.percentET.text.toString().toIntOrNull() ?: return@setOnClickListener
            binding.percentET.setText((current - 10).toString())
        }

        binding.cpsBtn.setOnClickListener {
            GlobalScope.launch(Dispatchers.Default) {
                try {
                    val dims = getImageDimensionsFromUri(applicationContext, imageUri)
                    val bmp = Glide.with(this@CompressActivity)
                        .asBitmap()
                        .load(imageUri)
                        .override(dims.x, dims.y)
                        .submit()
                        .get()

                    if (binding.autoCheck.isChecked) {
                        val origBytes = getSourceSizeBytes(applicationContext, imageUri)
                        val target = if (origBytes > 0) (origBytes * 0.6).toLong() else (800 * 1024) // aim ~60% of original or ~0.8MB
                        val (data, _) = compressToTargetBytes(bmp, startQ = 90, minQ = 60, targetBytes = target)
                        saveJpegBytesToGallery(applicationContext, data)
                    } else {
                        // Manual: interpret as JPEG quality (10..95)
                        val q = binding.percentET.text.toString().toIntOrNull()?.coerceIn(10, 95) ?: 85
                        val baos = ByteArrayOutputStream()
                        bmp.compress(Bitmap.CompressFormat.JPEG, q, baos)
                        saveJpegBytesToGallery(applicationContext, baos.toByteArray())
                    }

                    launch(Dispatchers.Main) {
                        Toast.makeText(applicationContext, "Image saved to gallery", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    launch(Dispatchers.Main) {
                        Toast.makeText(applicationContext, "Compression failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun onCheckboxClicked(clicked: CheckBox) {
        checkboxes.forEach { cb -> if (cb != clicked) cb.isChecked = false }
        val allUnchecked = checkboxes.all { !it.isChecked }
        if (allUnchecked || clicked.id == R.id.autoCheck) {
            binding.percentET.isEnabled = false
            binding.percentET.alpha = 0.5f
        } else {
            binding.percentET.isEnabled = true
            binding.percentET.alpha = 1f
        }
    }

    fun getImageDimensionsFromUri(context: Context, uri: Uri): Point {
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
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
                if (orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
                    orientation == ExifInterface.ORIENTATION_ROTATE_270
                ) {
                    // swap
                    val tmp = w; w = h; h = tmp
                }
            }
        } catch (_: Exception) { /* ignore */ }

        return Point(w, h)
    }

    private fun getSourceSizeBytes(context: Context, uri: Uri): Long =
        context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: -1L

    private fun compressToTargetBytes(
        bmp: Bitmap,
        startQ: Int,
        minQ: Int,
        targetBytes: Long
    ): Pair<ByteArray, Int> {
        var q = startQ
        val baos = ByteArrayOutputStream()
        var out: ByteArray
        while (true) {
            baos.reset()
            bmp.compress(Bitmap.CompressFormat.JPEG, q, baos)
            out = baos.toByteArray()
            if (out.size <= targetBytes || q <= minQ) break
            q -= 5
        }
        return out to q
    }

    private fun saveJpegBytesToGallery(context: Context, data: ByteArray): Uri? {
        val cr = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "ImgResizer_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }
        val uri = cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        uri?.let {
            try {
                cr.openOutputStream(it)?.use { os -> os.write(data) }
            } catch (e: IOException) {
                e.printStackTrace()
                return null
            }
        }
        return uri
    }
}
