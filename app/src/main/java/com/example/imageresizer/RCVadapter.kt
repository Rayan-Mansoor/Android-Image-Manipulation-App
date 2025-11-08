package com.example.imageresizer

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import java.io.File

private const val TAG = "RCVadapter"

class RCVadapter(private var recentList: ArrayList<RecentFile>) : RecyclerView.Adapter<RCVadapter.RCVholder>(){

    class RCVholder(itemView : View) : RecyclerView.ViewHolder(itemView){
        val image =  itemView.findViewById<ImageView>(R.id.rctImg)
        val name = itemView.findViewById<TextView>(R.id.rctName)
        val date = itemView.findViewById<TextView>(R.id.rectDate)
        val size = itemView.findViewById<TextView>(R.id.rectSize)
        val share = itemView.findViewById<ImageView>(R.id.shareBtn)
        val del = itemView.findViewById<ImageView>(R.id.delBtn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RCVholder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.list_item,parent,false)
        return RCVholder(itemView)
    }

    override fun getItemCount(): Int {
        return recentList.size
    }

    override fun onBindViewHolder(holder: RCVholder, position: Int) {
        val currentItem = recentList[position]
        val file = File(currentItem.imgPath)

        var imageUri : Uri? = null
        if (file.exists()) {
            imageUri = Uri.fromFile(file)
            holder.image.setImageURI(imageUri)

        }

        holder.name.text = currentItem.imgName
        holder.size.text = currentItem.imgSize + "KB"
        holder.date.text = currentItem.imgDate

        holder.share.setOnClickListener {
            val imagePath = currentItem.imgPath

            val imageFile = File(imagePath)
            val imageUri = FileProvider.getUriForFile(
                it.context,
                "com.example.imageresizer.fileprovider",  // Replace with your app's FileProvider authority
                imageFile
            )

            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "image/*"
            intent.putExtra(Intent.EXTRA_STREAM, imageUri)
            intent.setPackage("com.whatsapp")  // Specify the package name of WhatsApp


            // Grant temporary read permission to the content URI for WhatsApp to access it
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            it.context.startActivity(intent)

        }

        holder.del.setOnClickListener {
            Log.d(TAG,"Del clicked")
            val imageFile = imageUri!!.path?.let { it1 -> File(it1) }
            imageFile?.delete()
            removeItem(position)
            Toast.makeText(it.context,"Image deleted Successfully", Toast.LENGTH_SHORT).show()
        }
    }

    fun setList(recentList: ArrayList<RecentFile>){
        this.recentList = recentList
    }

    fun removeItem(position: Int) {
        recentList.removeAt(position)
        notifyItemRemoved(position)
    }

}