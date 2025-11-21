package com.adam.thorwallpapertool

import android.graphics.Bitmap
import android.graphics.Matrix

object ImageProcessor {
    
    // 索尔掌机屏幕参数
    const val UPPER_SCREEN_WIDTH = 1920
    const val UPPER_SCREEN_HEIGHT = 1080
    const val LOWER_SCREEN_WIDTH = 1240
    const val LOWER_SCREEN_HEIGHT = 1080
    
    /**
     * 根据索尔掌机的屏幕参数裁切壁纸
     * 保持内容比例不变，缩放图片到合适的分辨率，生成上下屏两张图片
     */
    fun processWallpaper(originalBitmap: Bitmap): Pair<Bitmap, Bitmap> {
        val originalWidth = originalBitmap.width
        val originalHeight = originalBitmap.height
        
        // 计算适合的缩放比例
        val scale = calculateScale(originalWidth, originalHeight)
        
        // 计算缩放后的尺寸
        val scaledWidth = (originalWidth * scale).toInt()
        val scaledHeight = (originalHeight * scale).toInt()
        
        // 缩放原始图片
        val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, scaledWidth, scaledHeight, true)
        
        // 裁切上下屏图片
        val upperScreenBitmap = createUpperScreenBitmap(scaledBitmap)
        val lowerScreenBitmap = createLowerScreenBitmap(scaledBitmap)
        
        return Pair(upperScreenBitmap, lowerScreenBitmap)
    }
    
    /**
     * 计算合适的缩放比例
     * 需要考虑两个屏幕的尺寸，选择合适的缩放比例以保持内容比例不变
     */
    private fun calculateScale(originalWidth: Int, originalHeight: Int): Float {
        // 索尔掌机总宽度和高度
        val totalWidth = UPPER_SCREEN_WIDTH + LOWER_SCREEN_WIDTH  // 3160
        val totalHeight = UPPER_SCREEN_HEIGHT  // 1080
        
        // 计算两个缩放比例
        val scaleForWidth = totalWidth.toFloat() / originalWidth
        val scaleForHeight = totalHeight.toFloat() / originalHeight
        
        // 选择较小的比例以确保图片完全适应屏幕
        return scaleForWidth.coerceAtMost(scaleForHeight)
    }
    
    /**
     * 创建上屏（主屏）壁纸
     */
    private fun createUpperScreenBitmap(bitmap: Bitmap): Bitmap {
        val bitmapWidth = bitmap.width
        val bitmapHeight = bitmap.height
        
        // 计算裁切区域
        val cropWidth = UPPER_SCREEN_WIDTH
        val cropHeight = UPPER_SCREEN_HEIGHT
        
        // 从中心裁切
        val x = (bitmapWidth - cropWidth) / 2
        val y = (bitmapHeight - cropHeight) / 2
        
        return Bitmap.createBitmap(
            bitmap,
            x.coerceAtLeast(0),
            y.coerceAtLeast(0),
            cropWidth.coerceAtMost(bitmapWidth),
            cropHeight.coerceAtMost(bitmapHeight)
        )
    }
    
    /**
     * 创建下屏（副屏）壁纸
     */
    private fun createLowerScreenBitmap(bitmap: Bitmap): Bitmap {
        val bitmapWidth = bitmap.width
        val bitmapHeight = bitmap.height
        
        // 计算裁切区域
        val cropWidth = LOWER_SCREEN_WIDTH
        val cropHeight = LOWER_SCREEN_HEIGHT
        
        // 从右侧裁切（因为下屏通常在右侧）
        val x = (bitmapWidth - cropWidth).coerceAtLeast(0)
        val y = (bitmapHeight - cropHeight) / 2  // 垂直居中
        
        return Bitmap.createBitmap(
            bitmap,
            x,
            y.coerceAtLeast(0),
            cropWidth.coerceAtMost(bitmapWidth),
            cropHeight.coerceAtMost(bitmapHeight)
        )
    }
    
    /**
     * 旋转图片
     */
    fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply {
            postRotate(degrees)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}