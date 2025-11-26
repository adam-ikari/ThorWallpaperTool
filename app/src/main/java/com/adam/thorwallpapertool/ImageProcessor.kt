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
     * 使用PPI补偿确保上下屏内容在物理尺寸上保持一致
     * 
     * @param originalBitmap 原始图片
     * @param gap 两个屏幕之间的间隔（像素）
     * @return 上屏和下屏壁纸的Pair
     */
    fun processWallpaper(originalBitmap: Bitmap, gap: Int = 0): Pair<Bitmap, Bitmap> {
        val originalWidth = originalBitmap.width
        val originalHeight = originalBitmap.height

        // PPI参数
        val upperPPI = DeviceConfig.UPPER_SCREEN_PPI  // 367
        val lowerPPI = DeviceConfig.LOWER_SCREEN_PPI  // 297
        
        // 目标输出尺寸
        val upperOutputWidth = DeviceConfig.UPPER_SCREEN_WIDTH   // 1920
        val upperOutputHeight = DeviceConfig.UPPER_SCREEN_HEIGHT // 1080
        val lowerOutputWidth = DeviceConfig.LOWER_SCREEN_WIDTH   // 1240
        val lowerOutputHeight = DeviceConfig.LOWER_SCREEN_HEIGHT // 1080

        // PPI补偿的核心原理：
        // 1. 上屏PPI > 下屏PPI，意味着下屏的每个像素物理尺寸更大
        // 2. 为了让相同内容在两个屏幕上显示相同物理尺寸，从原图裁切时需要考虑PPI差异
        // 3. 如果上屏需要X像素显示某个物理尺寸，下屏需要 X * (下屏PPI/上屏PPI) 像素来显示相同物理尺寸
        
        // 计算PPI补偿因子
        val ppiRatio = upperPPI / lowerPPI  // 367/297 ≈ 1.236
        
        // 计算下屏在物理尺寸上等效的像素尺寸
        // 为了在两个屏幕上显示相同物理尺寸的内容，下屏需要更少的像素（因为每个像素更大）
        val lowerPhysicalEquivalentHeight = (lowerOutputHeight / ppiRatio).toInt()
        val lowerPhysicalEquivalentWidth = (lowerOutputWidth / ppiRatio).toInt()
        
        // 将间隔像素加到上屏的目标高度上
        val upperHeightWithGap = upperOutputHeight + gap
        
        // 计算总的物理等效裁切高度
        val totalPhysicalHeight = upperHeightWithGap + lowerPhysicalEquivalentHeight
        
        // 确定裁切宽度（取两个屏幕物理等效宽度的较大值）
        val targetCropWidth = max(upperOutputWidth, lowerPhysicalEquivalentWidth)
        
        // 确定实际裁切尺寸
        var actualCropWidth: Int
        var actualCropHeight: Int
        
        // 检查原始图片是否足够大
        if (originalWidth < targetCropWidth || originalHeight < totalPhysicalHeight) {
            // 原图较小，按比例计算裁切尺寸
            val widthScale = originalWidth.toFloat() / targetCropWidth
            val heightScale = originalHeight.toFloat() / totalPhysicalHeight
            val scale = minOf(widthScale, heightScale)  // 保持宽高比，选择较小比例
            
            actualCropWidth = (targetCropWidth * scale).toInt()
            actualCropHeight = (totalPhysicalHeight * scale).toInt()
        } else {
            // 原图足够大，直接使用原图的最大可能裁切区域
            // 下采样质量很好，所以我们可以使用最大像素来保留更多细节
            val maxWidthRatio = originalWidth.toFloat() / targetCropWidth
            val maxHeightRatio = originalHeight.toFloat() / totalPhysicalHeight
            
            // 使用原图的完整尺寸，让下采样处理缩放
            actualCropWidth = originalWidth
            actualCropHeight = originalHeight
        }
        
        // 计算裁切起始位置（居中）
        val cropStartX = maxOf(0, (originalWidth - actualCropWidth) / 2)
        val cropStartY = maxOf(0, (originalHeight - actualCropHeight) / 2)

        // 计算上屏和下屏在裁切区域中的高度
        val upperCropHeight = ((upperHeightWithGap.toFloat() / totalPhysicalHeight) * actualCropHeight).toInt()
        val remainingHeight = actualCropHeight - upperCropHeight
        val lowerCropHeight = if (remainingHeight > 0) remainingHeight else 1  // 确保至少有1像素高度

        // 裁切上屏内容（需要减去间隔像素的缩放比例）
        val gapScaleRatio = upperCropHeight.toFloat() / upperHeightWithGap
        val gapInCropPixels = (gap * gapScaleRatio).toInt()
        val actualUpperCropHeight = upperCropHeight - gapInCropPixels
        
        val upperCropBitmap = cropBitmap(
            originalBitmap,
            cropStartX,
            cropStartY,
            actualCropWidth,
            actualUpperCropHeight
        )

        // 裁切下屏内容
        val lowerCropStartY = cropStartY + upperCropHeight
        val lowerCropBitmap = cropBitmap(
            originalBitmap,
            cropStartX,
            lowerCropStartY,
            actualCropWidth,
            lowerCropHeight
        )

        // 将上屏裁切内容缩放到目标分辨率
        val upperScreenBitmap = scaleBitmapToTarget(upperCropBitmap, upperOutputWidth, upperOutputHeight)

        // 创建下屏位图，应用PPI补偿缩放
        val lowerScreenBitmap = createLowerScreenBitmapWithPPICompensation(
            lowerCropBitmap, 
            lowerOutputWidth, 
            lowerOutputHeight, 
            ppiRatio
        )

        // 回收临时图片以释放内存
        if (upperScreenBitmap != upperCropBitmap) upperCropBitmap.recycle()
        if (lowerScreenBitmap != lowerCropBitmap) lowerCropBitmap.recycle()

        return Pair(upperScreenBitmap, lowerScreenBitmap)
    }
    
    /**
     * 创建下屏位图，应用PPI补偿
     * 确保内容在物理尺寸上与上屏保持一致
     */
    private fun createLowerScreenBitmapWithPPICompensation(
        cropBitmap: Bitmap, 
        targetWidth: Int, 
        targetHeight: Int, 
        ppiRatio: Float
    ): Bitmap {
        // 创建目标尺寸的位图
        val resultBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)
        canvas.drawColor(android.graphics.Color.BLACK)
        
        val paint = Paint().apply {
            isFilterBitmap = true
            isAntiAlias = true
        }
        
        // 直接缩放到目标分辨率，因为裁切时已经考虑了PPI差异
        // 现在只需要将裁切的内容放大到下屏的实际分辨率
        val scaleX = targetWidth.toFloat() / cropBitmap.width
        val scaleY = targetHeight.toFloat() / cropBitmap.height
        val scale = maxOf(scaleX, scaleY)
        
        // 计算缩放后的尺寸
        val scaledWidth = (cropBitmap.width * scale).toInt()
        val scaledHeight = (cropBitmap.height * scale).toInt()
        
        // 计算居中位置
        val offsetX = (targetWidth - scaledWidth) / 2
        val offsetY = (targetHeight - scaledHeight) / 2
        
        // 创建变换矩阵
        val matrix = Matrix()
        matrix.postScale(scale, scale)
        matrix.postTranslate(offsetX.toFloat(), offsetY.toFloat())
        
        // 绘制到目标画布
        canvas.drawBitmap(cropBitmap, matrix, paint)
        
        return resultBitmap
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
     * 使用PPI补偿确保内容在物理尺寸上与上屏一致
     */
    private fun createLowerScreenBitmapForPPI(bitmap: Bitmap, gap: Int, scaledLowerHeight: Int): Bitmap {
        val bitmapWidth = bitmap.width
        val bitmapHeight = bitmap.height

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
        
        // 应用PPI补偿以确保内容在物理尺寸上与上屏一致
        // 由于下屏PPI较低，为了让内容在物理尺寸上与上屏匹配，需要调整内容大小
        val contentScale = DeviceConfig.LOWER_SCREEN_PPI / DeviceConfig.UPPER_SCREEN_PPI  // 297/367 ≈ 0.809
        
        // 创建目标尺寸的位图并用黑色填充
        val targetBitmap = Bitmap.createBitmap(DeviceConfig.LOWER_SCREEN_WIDTH, DeviceConfig.LOWER_SCREEN_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(targetBitmap)
        canvas.drawColor(android.graphics.Color.BLACK)
        
        // 使用高质量渲染设置
        val paint = android.graphics.Paint().apply {
            isFilterBitmap = true  // 启用双线性过滤，使缩放更平滑
            isAntiAlias = true     // 启用抗锯齿
        }
        
        // 根据PPI比率调整缩放，使得内容在物理尺寸上匹配上屏
        // 因为下屏PPI更低，相同像素的内容会显得更大，因此需要缩小内容来补偿
        val scaledWidth = (cropWidth * contentScale).toInt()
        val scaledHeight = (cropHeight * contentScale).toInt()
        
        // 计算居中位置
        val offsetX = (DeviceConfig.LOWER_SCREEN_WIDTH - scaledWidth) / 2
        val offsetY = (DeviceConfig.LOWER_SCREEN_HEIGHT - scaledHeight) / 2
        
        // 创建缩放矩阵
        val matrix = Matrix()
        matrix.postScale(contentScale, contentScale)
        matrix.postTranslate(offsetX.toFloat(), offsetY.toFloat())
        
        canvas.drawBitmap(lowerBitmap, matrix, paint)
        
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