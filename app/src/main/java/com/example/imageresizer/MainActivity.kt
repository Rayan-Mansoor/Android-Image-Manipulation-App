package com.example.imageresizer

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.imageresizer.CropperActivity
import com.example.imageresizer.databinding.HomePageBinding

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    private lateinit var binding: HomePageBinding
    private lateinit var imageURI: Uri
    private var actionKey: String = ""

    /** Cropper launcher (our custom CropperActivity) */
    private val cropperActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // CropperActivity already saved the image to MediaStore and returns its Uri
                val cropped = result.data?.data
                if (cropped != null) {
                    Toast.makeText(this, "Image saved to gallery", Toast.LENGTH_SHORT).show()
                    // If you want to pass this Uri to another screen, do it here.
                    // e.g., startActivity(Intent(this, NextActivity::class.java).setData(cropped))
                }
            } else {
                val error = result.data?.getStringExtra("error")
                if (!error.isNullOrBlank()) {
                    Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Crop canceled", Toast.LENGTH_SHORT).show()
                }
            }
        }

    /** Gallery picker */
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri == null) return@registerForActivityResult
            imageURI = uri

            when (actionKey) {
                "converter" -> {
                    startActivity(Intent(this, FormatActivity::class.java).putExtra("imageURI", uri.toString()))
                }
                "resizer" -> {
                    startActivity(Intent(this, ResizeActivity::class.java).putExtra("imageURI", uri.toString()))
                }
                "compresser" -> {
                    startActivity(Intent(this, CompressActivity::class.java).putExtra("imageURI", uri.toString()))
                }
                "cropper" -> startCustomCrop(uri, square = false)
                "SQcropper" -> startCustomCrop(uri, square = true)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = HomePageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.convBtn.setOnClickListener { actionKey = "converter";   pickImage() }
        binding.crpBtn.setOnClickListener  { actionKey = "cropper";     pickImage() }
        binding.sqCrpBtn.setOnClickListener{ actionKey = "SQcropper";   pickImage() }
        binding.resizeBtn.setOnClickListener{actionKey = "resizer";     pickImage() }
        binding.compBtn.setOnClickListener { actionKey = "compresser";  pickImage() }
        binding.rctBtn.setOnClickListener  { startActivity(Intent(this, RecentFiles::class.java)) }
    }

    private fun pickImage() = pickImageLauncher.launch("image/*")

    /** Launch our own CropperActivity (with explicit Cancel/Done buttons) */
    private fun startCustomCrop(uri: Uri, square: Boolean) {
        val intent = Intent(this, CropperActivity::class.java)
            .putExtra(CropperActivity.EXTRA_IMAGE_URI, uri.toString())
            .putExtra(CropperActivity.EXTRA_SQUARE, square)
        cropperActivityLauncher.launch(intent)
    }

    /** Kept for other flows that might need a gallery copy from an existing Uri */
    fun saveImageToGallery(context: Context, imageUri: Uri) {
        val cr = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "ImgResizer_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.WIDTH, 0)
            put(MediaStore.Images.Media.HEIGHT, 0)
        }
        val outUri = cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        outUri?.let { dest ->
            cr.openOutputStream(dest)?.use { os ->
                cr.openInputStream(imageUri)?.use { ins -> ins.copyTo(os) }
                Toast.makeText(context, "Image saved to gallery", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
