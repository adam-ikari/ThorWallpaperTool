package com.adam.thorwallpapertool

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    
    private lateinit var imagePreview: ImageView
    private lateinit var btnSelectImage: Button
    private lateinit var btnProcessImage: Button
    private lateinit var selectedImageInfo: TextView
    private lateinit var editGap: EditText
    private lateinit var checkPPICompensation: CheckBox
    private lateinit var progressBar: ProgressBar
    
    private var selectedImageUri: Uri? = null
    private var selectedBitmap: Bitmap? = null
    
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            loadAndDisplayImage(it)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        
        initViews()
        setupClickListeners()
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
    
    private fun initViews() {
        imagePreview = findViewById(R.id.imagePreview)
        btnSelectImage = findViewById(R.id.btnSelectImage)
        btnProcessImage = findViewById(R.id.btnProcessImage)
        selectedImageInfo = findViewById(R.id.selectedImageInfo)
        editGap = findViewById(R.id.editGap)
        checkPPICompensation = findViewById(R.id.checkPPICompensation)
        progressBar = findViewById(R.id.progressBar)
    }
    
    private fun setupClickListeners() {
        btnSelectImage.setOnClickListener {
            openImagePicker()
        }
        
        btnProcessImage.setOnClickListener {
            processImage()
        }
    }
    
    private fun openImagePicker() {
        imagePickerLauncher.launch("image/*")
    }
    
    private fun loadAndDisplayImage(uri: Uri) {
        try {
            // 获取图片尺寸以预先判断是否为高分辨率图片
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }
            
            // 计算缩放比例以适应预览，避免在UI线程加载过大图片
            val reqWidth = 800  // 预览最大宽度
            val reqHeight = 600 // 预览最大高度
            
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.ARGB_8888
            options.inMutable = true
            
            // 加载缩略图用于预览
            val inputStream = contentResolver.openInputStream(uri)
            selectedBitmap = BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()
            
            if (selectedBitmap != null) {
                imagePreview.setImageBitmap(selectedBitmap)
                btnProcessImage.isEnabled = true
                selectedImageInfo.text = "已选择图片: ${selectedBitmap?.width}x${selectedBitmap?.height}"
            }
        } catch (e: Exception) {
            Toast.makeText(this, "加载图片失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 计算合适的inSampleSize值，用于加载缩略图
     */
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            // 计算最大的inSampleSize值，该值保证各边长度都大于所需尺寸
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }
    
    private fun processImage() {
        selectedBitmap?.let { bitmap ->
            // 获取用户输入的间隔值
            val gapInput = editGap.text.toString().trim()
            val gap = if (gapInput.isEmpty()) 0 else {
                try {
                    gapInput.toInt()
                } catch (e: NumberFormatException) {
                    0 // 如果输入无效，默认为0
                }
            }
            
            // 显示进度条
            progressBar.visibility = View.VISIBLE
            btnProcessImage.isEnabled = false
            
            // 在后台线程处理图片
            Thread {
                try {
                    // 调用图片处理函数
                    val result = processWallpaperImage(bitmap, gap)
                    
                    runOnUiThread {
                        // 隐藏进度条
                        progressBar.visibility = View.GONE
                        btnProcessImage.isEnabled = true
                        
                        if (result) {
                            Toast.makeText(this, "壁纸生成成功！", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "壁纸生成失败", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        progressBar.visibility = View.GONE
                        btnProcessImage.isEnabled = true
                        Toast.makeText(this, "处理图片时出错: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }.start()
        } ?: run {
            Toast.makeText(this, "请先选择一张图片", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun processWallpaperImage(originalBitmap: Bitmap, gap: Int = 0): Boolean {
        try {
            // 从UI控件获取PPI补偿设置
            val enablePPICompensation = checkPPICompensation.isChecked
            
            // 使用ImageProcessor处理图片
            val (upperBitmap, lowerBitmap) = ImageProcessor.processWallpaper(originalBitmap, gap, enablePPICompensation)
            
            // 保存处理后的图片
            saveProcessedImages(upperBitmap, lowerBitmap)
            
            // 回收生成的位图以释放内存
            if (upperBitmap != originalBitmap && lowerBitmap != originalBitmap) {
                upperBitmap.recycle()
                lowerBitmap.recycle()
            }
            
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
    
    private fun saveProcessedImages(upperBitmap: Bitmap, lowerBitmap: Bitmap) {
        try {
            val fileNamePrefix = "thor_wallpaper_" + System.currentTimeMillis()
            
            // 保存上屏壁纸
            val upperWallpaperUri = saveBitmapToGallery(upperBitmap, "${fileNamePrefix}_upper.jpg")
            
            // 保存下屏壁纸
            val lowerWallpaperUri = saveBitmapToGallery(lowerBitmap, "${fileNamePrefix}_lower.jpg")
            
            if (upperWallpaperUri != null && lowerWallpaperUri != null) {
                runOnUiThread {
                    Toast.makeText(this, "壁纸已保存到相册", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            runOnUiThread {
                Toast.makeText(this, "保存壁纸时出错: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun saveBitmapToGallery(bitmap: Bitmap, displayName: String): Uri? {

        return try {

            val contentValues = android.content.ContentValues().apply {

                put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, displayName)

                put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")

                put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/ThorWallpaperTool/")

            }

            

            val contentResolver = contentResolver

            val uri = contentResolver.insert(

                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,

                contentValues

            )

            

            if (uri != null) {

                contentResolver.openOutputStream(uri)?.use { outputStream ->

                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, outputStream)

                }

            }

            

            uri

        } catch (e: Exception) {

            e.printStackTrace()

            null

        }

    }
    
    override fun onDestroy() {
        super.onDestroy()
        // 回收选中的位图以释放内存
        selectedBitmap?.let { 
            if (!it.isRecycled) {
                it.recycle()
            }
        }
    }
}