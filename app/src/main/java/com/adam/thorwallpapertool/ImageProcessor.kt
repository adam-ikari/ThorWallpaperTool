package com.adam.thorwallpapertool

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix

object ImageProcessor {
    
    // 索尔掌机屏幕参数
    const val UPPER_SCREEN_WIDTH = 1920
    const val UPPER_SCREEN_HEIGHT = 1080
    const val LOWER_SCREEN_WIDTH = 1240
    const val LOWER_SCREEN_HEIGHT = 1080

    // 索尔掌机屏幕PPI信息
    // 上屏：6寸 1920x1080 -> 约 367 PPI
    // 下屏：3.92寸 1240x1080 -> 约 297 PPI
    private const val UPPER_SCREEN_PPI = 367f
    private const val LOWER_SCREEN_PPI = 297f
    
    /**
     * 根据索尔掌机的屏幕参数裁切壁纸
     * 保持内容比例不变，缩放图片到合适的分辨率，生成上下屏两张图片
     * 壁纸将以上下拼接的方式生成：上屏在上，下屏在下
     * 考虑PPI差异，确保内容在不同屏幕上物理尺寸一致
     * 
     * @param originalBitmap 原始图片
     * @param gap 两个屏幕之间的间隔（像素）
     * @return 上屏和下屏壁纸的Pair
     */
    fun processWallpaper(originalBitmap: Bitmap, gap: Int = 0): Pair<Bitmap, Bitmap> {
        val originalWidth = originalBitmap.width
        val originalHeight = originalBitmap.height

        // 计算PPI缩放因子
        val ppiScaleFactor = LOWER_SCREEN_PPI / UPPER_SCREEN_PPI

        // 计算拼接后图片的总尺寸（上下拼接：上屏在上，下屏在下，中间有间隔）
        val combinedWidth = maxOf(UPPER_SCREEN_WIDTH, LOWER_SCREEN_WIDTH)
        // 考虑PPI差异，对下屏区域进行缩放
        val scaledLowerHeight = (LOWER_SCREEN_HEIGHT * ppiScaleFactor).toInt()
        val combinedHeight = UPPER_SCREEN_HEIGHT + gap + scaledLowerHeight

        // 计算缩放比例，确保原始图片能够覆盖整个拼接区域
        // 保持原始图片的宽高比
        val scaleForWidth = combinedWidth.toFloat() / originalWidth
        val scaleForHeight = combinedHeight.toFloat() / originalHeight
        val scale = maxOf(scaleForWidth, scaleForHeight) // 使用较大比例确保覆盖整个区域

        // 缩放原始图片
        val scaledWidth = (originalWidth * scale).toInt()
        val scaledHeight = (originalHeight * scale).toInt()
        val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, scaledWidth, scaledHeight, true)

        // 创建拼接画布
        val combinedBitmap = Bitmap.createBitmap(combinedWidth, combinedHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(combinedBitmap)

        // 将缩放后的图片绘制到画布中央，这样可以确保整体内容居中
        val bitmapX = (combinedWidth - scaledWidth) / 2f
        val bitmapY = (combinedHeight - scaledHeight) / 2f
        canvas.drawBitmap(scaledBitmap, bitmapX, bitmapY, null)

        // 从拼接画布中裁切上下屏壁纸
        val upperScreenBitmap = createUpperScreenBitmap(combinedBitmap)
        val lowerScreenBitmap = createLowerScreenBitmapForPPI(combinedBitmap, gap, scaledLowerHeight)

        return Pair(upperScreenBitmap, lowerScreenBitmap)
    }
    
    /**
     * 创建上屏（主屏）壁纸（位于拼接图的上方）
     */
    private fun createUpperScreenBitmap(bitmap: Bitmap): Bitmap {
        val bitmapWidth = bitmap.width
        val bitmapHeight = bitmap.height
        
        // 确定裁切区域 - 从顶部开始
        val cropWidth = minOf(UPPER_SCREEN_WIDTH, bitmapWidth)
        val cropHeight = minOf(UPPER_SCREEN_HEIGHT, bitmapHeight)
        val x = maxOf(0, (bitmapWidth - cropWidth) / 2)  // 水平居中
        val y = 0  // 从顶部开始裁切

        return Bitmap.createBitmap(
            bitmap,
            x,
            y,
            cropWidth,
            cropHeight
        )
    }
    
    /**
     * 创建下屏（副屏）壁纸（位于拼接图的下方）
     * 考虑PPI差异，对内容进行适当调整以保持视觉一致性
     */
    private fun createLowerScreenBitmapForPPI(bitmap: Bitmap, gap: Int, scaledLowerHeight: Int): Bitmap {
        val bitmapWidth = bitmap.width
        val bitmapHeight = bitmap.height

        // 确定裁切区域 - 从指定位置开始
        val cropWidth = minOf(LOWER_SCREEN_WIDTH, bitmapWidth)
        val cropHeight = minOf(scaledLowerHeight, bitmapHeight)
        
        // 水平居中
        val x = maxOf(0, (bitmapWidth - cropWidth) / 2)
        
        // 从上屏+间隔之后开始，但确保不超出边界
        val startY = UPPER_SCREEN_HEIGHT + gap
        var y = startY
        
        // 如果计算出的位置超出边界，则从底部对齐
        if (y + cropHeight > bitmapHeight) {
            y = maxOf(0, bitmapHeight - cropHeight)
        }

        val lowerBitmap = Bitmap.createBitmap(
            bitmap,
            x,
            y,
            cropWidth,
            cropHeight
        )
        
        // 将缩放后的下屏图片重新缩放到实际分辨率以保持PPI一致性
        return Bitmap.createScaledBitmap(lowerBitmap, LOWER_SCREEN_WIDTH, LOWER_SCREEN_HEIGHT, true)
    }
    
    /**
     * 创建下屏（副屏）壁纸（位于拼接图的下方）
     * 此方法保留为向后兼容
     */
    private fun createLowerScreenBitmap(bitmap: Bitmap, gap: Int): Bitmap {
        return createLowerScreenBitmapForPPI(bitmap, gap, LOWER_SCREEN_HEIGHT)
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