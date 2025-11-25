package com.adam.thorwallpapertool

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import kotlin.math.max
import kotlin.math.min

object ImageProcessor {
    
    /**
     * 根据索尔掌机的屏幕参数裁切壁纸
     * 先计算考虑PPI的裁切坐标和尺寸，然后裁切，最后缩放到目标分辨率
     * 确保上下屏的壁纸在物理尺寸上可以拼接
     * 使用PPI计算确保内容在不同屏幕上视觉/物理尺寸一致
     * 
     * @param originalBitmap 原始图片
     * @param gap 两个屏幕之间的间隔（像素）
     * @return 上屏和下屏壁纸的Pair
     */
    fun processWallpaper(originalBitmap: Bitmap, gap: Int = 0): Pair<Bitmap, Bitmap> {
        val originalWidth = originalBitmap.width
        val originalHeight = originalBitmap.height

        // PPI比率：下屏PPI / 上屏PPI
        val ppiRatio = DeviceConfig.LOWER_SCREEN_PPI / DeviceConfig.UPPER_SCREEN_PPI  // 297/367 ≈ 0.809
        
        // 目标输出尺寸
        val upperOutputWidth = DeviceConfig.UPPER_SCREEN_WIDTH   // 1920
        val upperOutputHeight = DeviceConfig.UPPER_SCREEN_HEIGHT // 1080
        val lowerOutputWidth = DeviceConfig.LOWER_SCREEN_WIDTH   // 1240
        val lowerOutputHeight = DeviceConfig.LOWER_SCREEN_HEIGHT // 1080

        // 为了物理尺寸一致，需要根据PPI计算在原始图片中裁切的尺寸
        // 下屏在原始图片中需要裁切的高度 = 目标高度 / PPI比率
        val lowerRequiredHeight = (lowerOutputHeight / ppiRatio).toInt()
        
        // 计算总的裁切高度需求
        val totalRequiredHeight = upperOutputHeight + lowerRequiredHeight
        val targetCropWidth = max(upperOutputWidth, lowerOutputWidth)
        
        // 确定实际裁切尺寸
        val actualCropWidth: Int
        val actualCropHeight: Int
        
        // 检查原始图片是否足够大
        if (originalWidth < targetCropWidth || originalHeight < totalRequiredHeight) {
            // 原图较小，按比例计算裁切尺寸
            val widthScale = originalWidth.toFloat() / targetCropWidth
            val heightScale = originalHeight.toFloat() / totalRequiredHeight
            val scale = minOf(widthScale, heightScale)  // 保持宽高比，选择较小比例
            
            actualCropWidth = (targetCropWidth * scale).toInt()
            actualCropHeight = (totalRequiredHeight * scale).toInt()
        } else {
            // 原图足够大，按需求裁切
            actualCropWidth = targetCropWidth
            actualCropHeight = totalRequiredHeight
        }
        
        // 计算裁切起始位置（居中）
        val cropStartX = maxOf(0, (originalWidth - actualCropWidth) / 2)
        val cropStartY = maxOf(0, (originalHeight - actualCropHeight) / 2)

        // 计算上屏在裁切区域中的高度
        val upperCropHeight = minOf(
            ((upperOutputHeight.toFloat() * actualCropHeight) / totalRequiredHeight).toInt(),
            actualCropHeight
        )
        
        // 裁切上屏内容
        val upperCropBitmap = cropBitmap(
            originalBitmap,
            cropStartX,
            cropStartY,
            actualCropWidth,
            upperCropHeight
        )

        // 裁切下屏内容
        var lowerCropBitmap: Bitmap? = null
        if (actualCropHeight > upperCropHeight) {
            val lowerCropStartY = cropStartY + upperCropHeight
            val lowerCropActualHeight = actualCropHeight - upperCropHeight
            lowerCropBitmap = cropBitmap(
                originalBitmap,
                cropStartX,
                lowerCropStartY,
                actualCropWidth,
                lowerCropActualHeight
            )
        } else {
            // 如果裁切区域高度不足，创建最小尺寸的下屏
            lowerCropBitmap = Bitmap.createBitmap(
                actualCropWidth,
                maxOf(1, actualCropHeight),
                Bitmap.Config.ARGB_8888
            ).apply {
                val canvas = Canvas(this)
                canvas.drawColor(android.graphics.Color.BLACK)
            }
        }

        // 将裁切内容缩放到目标分辨率
        val upperScreenBitmap = scaleBitmapToTarget(upperCropBitmap, upperOutputWidth, upperOutputHeight)
        
        // 应用PPI补偿到下屏内容（确保物理尺寸一致）
        val lowerWithPPI = if (ppiRatio != 1.0f) {
            // 如果下屏PPI较低，需要放大内容以补偿
            val ppiAdjustedBitmap = Bitmap.createBitmap(
                lowerCropBitmap!!.width, 
                (lowerCropBitmap.height * ppiRatio).toInt(), 
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(ppiAdjustedBitmap)
            canvas.drawColor(android.graphics.Color.BLACK)
            
            val paint = Paint().apply {
                isFilterBitmap = true
                isAntiAlias = true
            }
            
            val centerY = maxOf(0, (ppiAdjustedBitmap.height - lowerCropBitmap.height) / 2)
            canvas.drawBitmap(lowerCropBitmap, 0f, centerY.toFloat(), paint)
            
            ppiAdjustedBitmap
        } else {
            lowerCropBitmap!!
        }
        
        val lowerScreenBitmap = scaleBitmapToTarget(lowerWithPPI, lowerOutputWidth, lowerOutputHeight)

        // 回收临时图片以释放内存
        if (upperScreenBitmap != upperCropBitmap) upperCropBitmap.recycle()
        if (lowerScreenBitmap != lowerWithPPI && lowerWithPPI != lowerCropBitmap!!) lowerWithPPI.recycle()
        if (lowerCropBitmap != lowerScreenBitmap && lowerCropBitmap != lowerWithPPI) lowerCropBitmap.recycle()

        return Pair(upperScreenBitmap, lowerScreenBitmap)
    }
    
    /**
     * 从原始图片中裁切指定区域
     */
    private fun cropBitmap(bitmap: Bitmap, x: Int, y: Int, width: Int, height: Int): Bitmap {
        // 确保裁切区域在图片范围内
        val safeX = maxOf(0, minOf(x, bitmap.width - 1))
        val safeY = maxOf(0, minOf(y, bitmap.height - 1))
        val safeWidth = minOf(width, bitmap.width - safeX)
        val safeHeight = minOf(height, bitmap.height - safeY)
        
        return Bitmap.createBitmap(bitmap, safeX, safeY, safeWidth, safeHeight)
    }
    
    /**
     * 将图片缩放到目标尺寸，保持原始宽高比
     */
    private fun scaleBitmapToTarget(bitmap: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        if (bitmap.width == targetWidth && bitmap.height == targetHeight) {
            return bitmap
        }
        
        // 计算缩放比例，保持宽高比
        val scaleX = targetWidth.toFloat() / bitmap.width
        val scaleY = targetHeight.toFloat() / bitmap.height
        val scale = maxOf(scaleX, scaleY) // 使用较大的比例，确保图片覆盖整个目标区域
        
        // 计算缩放后的尺寸
        val scaledWidth = (bitmap.width * scale).toInt()
        val scaledHeight = (bitmap.height * scale).toInt()
        
        // 使用Matrix进行高质量缩放
        val matrix = Matrix()
        matrix.postScale(scale, scale)
        
        val scaledBitmap = Bitmap.createBitmap(
            bitmap,
            0, 0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
        
        // 从缩放后的图片中裁切目标尺寸（居中裁切）
        val x = maxOf(0, (scaledWidth - targetWidth) / 2)
        val y = maxOf(0, (scaledHeight - targetHeight) / 2)
        
        val result = Bitmap.createBitmap(scaledBitmap, x, y,
            minOf(targetWidth, scaledWidth),
            minOf(targetHeight, scaledHeight))
        
        // 回收中间图片以释放内存
        if (scaledBitmap != result) scaledBitmap.recycle()
        
        return result
    }
    
    /**
     * 创建上屏（主屏）壁纸（位于拼接图的上方）
     * 使用高质量渲染以优化画质
     */
    private fun createUpperScreenBitmap(bitmap: Bitmap): Bitmap {
        val bitmapWidth = bitmap.width
        val bitmapHeight = bitmap.height
        
        // 确定裁切区域 - 从顶部开始
        val cropWidth = minOf(DeviceConfig.UPPER_SCREEN_WIDTH, bitmapWidth)
        val cropHeight = minOf(DeviceConfig.UPPER_SCREEN_HEIGHT, bitmapHeight)
        val x = maxOf(0, (bitmapWidth - cropWidth) / 2)  // 水平居中
        val y = 0  // 从顶部开始裁切

        // 创建目标尺寸的位图
        val resultBitmap = Bitmap.createBitmap(
            bitmap,
            x,
            y,
            cropWidth,
            cropHeight
        )
        
        // 如果尺寸恰好符合要求，直接返回；否则创建新位图以确保质量
        if (cropWidth == DeviceConfig.UPPER_SCREEN_WIDTH && cropHeight == DeviceConfig.UPPER_SCREEN_HEIGHT) {
            return resultBitmap
        } else {
            // 创建标准尺寸的位图
            val targetBitmap = Bitmap.createBitmap(DeviceConfig.UPPER_SCREEN_WIDTH, DeviceConfig.UPPER_SCREEN_HEIGHT, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(targetBitmap)
            
            // 使用高质量渲染设置
            val paint = android.graphics.Paint().apply {
                isFilterBitmap = true  // 启用双线性过滤
                isAntiAlias = true     // 启用抗锯齿
            }
            
            // 将裁切的内容绘制到目标画布上
            canvas.drawBitmap(resultBitmap, 
                android.graphics.Rect(0, 0, resultBitmap.width, resultBitmap.height),  // 源矩形
                android.graphics.Rect(0, 0, DeviceConfig.UPPER_SCREEN_WIDTH, DeviceConfig.UPPER_SCREEN_HEIGHT), // 目标矩形
                paint)
            
            // 回收临时图片以释放内存
            if (resultBitmap != targetBitmap) {
                resultBitmap.recycle()
            }
            
            return targetBitmap
        }
    }
    
    /**
     * 创建下屏（副屏）壁纸（位于拼接图的下方）
     * 确保有足够的分辨率以实现像素完美，只进行下采样
     * 额外的分辨率部分使用黑色像素填充
     * 使用高质量渲染以优化画质
     */
    private fun createLowerScreenBitmapForPPI(bitmap: Bitmap, gap: Int, scaledLowerHeight: Int): Bitmap {
        val bitmapWidth = bitmap.width
        val bitmapHeight = bitmap.height

        // 使用PPI补偿（物理尺寸一致）- 默认计算两个屏幕的PPI
        val compensationFactor = DeviceConfig.LOWER_SCREEN_PPI / DeviceConfig.UPPER_SCREEN_PPI
        val outputWidth = (DeviceConfig.LOWER_SCREEN_WIDTH * compensationFactor).toInt()
        val outputHeight = (DeviceConfig.LOWER_SCREEN_HEIGHT * compensationFactor).toInt()
        
        // 确定裁切区域 - 裁切中心1240x1080的内容区域
        val cropWidth = minOf(DeviceConfig.LOWER_SCREEN_WIDTH, bitmapWidth)  // 裁切标准分辨率的内容
        val cropHeight = minOf(DeviceConfig.LOWER_SCREEN_HEIGHT, bitmapHeight)
        
        // 水平居中
        val x = maxOf(0, (bitmapWidth - cropWidth) / 2)
        
        // 从上屏+间隔之后开始，但确保不超出边界
        val startY = DeviceConfig.UPPER_SCREEN_HEIGHT + gap
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
        
        // 创建目标尺寸的位图（补偿后的尺寸）并用黑色填充
        val targetBitmap = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(targetBitmap)
        canvas.drawColor(android.graphics.Color.BLACK)
        
        // 使用高质量渲染设置
        val paint = android.graphics.Paint().apply {
            isFilterBitmap = true  // 启用双线性过滤，使缩放更平滑
            isAntiAlias = true     // 启用抗锯齿
        }
        
        // 将标准分辨率的内容放置在目标画布的中心位置
        val centerX = (outputWidth - DeviceConfig.LOWER_SCREEN_WIDTH) / 2f
        val centerY = (outputHeight - DeviceConfig.LOWER_SCREEN_HEIGHT) / 2f
        
        canvas.drawBitmap(lowerBitmap, 
            centerX, 
            centerY, 
            paint)
        
        // 回收临时图片以释放内存
        lowerBitmap.recycle()
        
        return targetBitmap
    }
    
    /**
     * 创建下屏（副屏）壁纸（位于拼接图的下方）
     * 此方法保留为向后兼容
     */
    private fun createLowerScreenBitmap(bitmap: Bitmap, gap: Int): Bitmap {
        return createLowerScreenBitmapForPPI(bitmap, gap, DeviceConfig.LOWER_SCREEN_HEIGHT)
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