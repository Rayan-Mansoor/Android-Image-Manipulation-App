package com.example.imageresizer

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.imageresizer.databinding.ActivityRecentFilesBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val TAG = "RecentFiles"

class RecentFiles : AppCompatActivity() {
    private lateinit var binding : ActivityRecentFilesBinding
    private lateinit var filesList : ArrayList<RecentFile>
    private lateinit var imgadapter : RCVadapter

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityRecentFilesBinding.inflate(layoutInflater)

        super.onCreate(savedInstanceState)
        setContentView(binding.root)


        filesList = ArrayList()


        Log.d(TAG,"Im here")


        val selection = "${MediaStore.Images.Media.DISPLAY_NAME} LIKE ? COLLATE NOCASE"
        val selectionArgs = arrayOf("ImgResizer_%")



        val projection = arrayOf(
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_TAKEN

        )

        Log.d(TAG,"Im here2")

        val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"

        Log.d(TAG,"Im here3")

        val cursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )

        Log.d(TAG,"Im here4")

        cursor?.use { cursor ->
            val pathColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val nameColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val sizeColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)



            while (cursor.moveToNext()) {
                val imgPath = cursor.getString(pathColumnIndex)
                val imgName = cursor.getString(nameColumnIndex)
                val imgSize = cursor.getLong(sizeColumnIndex)
                val imgDate = cursor.getLong(dateColumnIndex)

                val file = File(imgPath)
                val fileSizeInKB = file.length() / 1024 // Size in kilobytes

//                val calendar = Calendar.getInstance()
//                calendar.timeInMillis = imgDate

                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val formattedDate = dateFormat.format(Date(imgDate))

                val recentFile = RecentFile(imgPath, imgName, formattedDate, fileSizeInKB.toString())
                filesList.add(recentFile)

            }
        }
        Log.d(TAG,filesList.toString())

        imgadapter = RCVadapter(filesList)
        binding.imgrcv.adapter = imgadapter
        binding.imgrcv.layoutManager = LinearLayoutManager(this)

        binding.rctfBack.setOnClickListener {
            finish()
        }

    }
}