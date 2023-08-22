package com.example.imageresizer

import android.app.Activity
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Point
import android.media.ExifInterface
import android.media.MediaScannerConnection
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.WindowManager
import android.webkit.MimeTypeMap
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.FutureTarget
import com.example.imageresizer.databinding.ActivityMainBinding
import com.example.imageresizer.databinding.HomePageBinding
import com.theartofdev.edmodo.cropper.CropImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import java.util.Locale

const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    private lateinit var binding : HomePageBinding
    private val SELECT_IMAGE_REQUEST = 1

    private lateinit var imgView :ImageView

    private lateinit var bitmap: FutureTarget<Bitmap>
    private lateinit var imageURI : Uri
    private var check = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = HomePageBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.convBtn.setOnClickListener {
            check = "converter"
            selectImageFromGallery()
        }

        binding.crpBtn.setOnClickListener {
            check = "cropper"
            selectImageFromGallery()
        }

        binding.sqCrpBtn.setOnClickListener {
            check = "SQcropper"
            selectImageFromGallery()
        }

        binding.resizeBtn.setOnClickListener {
            check = "resizer"
            selectImageFromGallery()
        }

        binding.rctBtn.setOnClickListener {
            startActivity(Intent(this,RecentFiles::class.java))
        }

        binding.compBtn.setOnClickListener {
            check = "compresser"
            selectImageFromGallery()
        }
    }

    private fun selectImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, SELECT_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SELECT_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            imageURI = data.data!!


            if (check == "converter"){
                val intent = Intent(this,FormatActivity::class.java)
                intent.putExtra("imageURI", imageURI.toString())
                startActivity(intent)
            }

            else if (check == "cropper"){
                startCropActivity(imageURI)
            }

            else if (check == "SQcropper"){
                startSquareCropActivity(imageURI)
            }

            else if (check == "resizer"){
                val intent = Intent(this,ResizeActivity::class.java)
                intent.putExtra("imageURI", imageURI.toString())
                startActivity(intent)
            }

            else if (check == "compresser"){
                val intent = Intent(this,CompressActivity::class.java)
                intent.putExtra("imageURI", imageURI.toString())
                startActivity(intent)
            }


        }

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            val result = CropImage.getActivityResult(data)
            if (resultCode == RESULT_OK) {
                val resultUri = result.uri;
                saveImageToGallery(applicationContext, resultUri)
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                val error = result.error;
            }
        }

    }




    private val REQUEST_CODE_CROP = 100

    private fun startCropActivity(imageUri: Uri) {
        CropImage.activity(imageUri).start(this);
    }

    private fun startSquareCropActivity(imageUri: Uri) {
        CropImage.activity(imageUri).setFixAspectRatio(true).start(this);
    }

    fun saveImageToGallery(context: Context, imageUri: Uri) {
        val contentResolver = context.contentResolver

        // Create a ContentValues object to hold the image details
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "ImgResizer_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.WIDTH, 0)  // Set to 0 to use the original image width
            put(MediaStore.Images.Media.HEIGHT, 0) // Set to 0 to use the original image height
        }

        // Insert the image details into the MediaStore
        val imageUriResult = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        // Open an output stream for the image URI and copy the image data
        imageUriResult?.let { resultUri ->
            contentResolver.openOutputStream(resultUri)?.use { outputStream ->
                contentResolver.openInputStream(imageUri)?.use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
                Toast.makeText(context, "Image saved to gallery", Toast.LENGTH_SHORT).show()
            }
        }
    }

}