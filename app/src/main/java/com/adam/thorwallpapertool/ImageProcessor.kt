package com.adam.thorwallpapertool

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import kotlin.math.max
import kotlin.math.min

object ImageProcessor {
    
    /**
     * 根据索尔掌机的屏幕参数裁切壁纸
     * 保持内容比例不变，缩放图片到合适的分辨率，生成上下屏两张图片
     * 壁纸将以上下拼接的方式生成：上屏在上，下屏在下
     * 根据选择启用PPI补偿或1.1倍像素补偿，确保内容在不同屏幕上视觉/物理尺寸一致
     * 额外的分辨率部分使用黑色像素填充
     * 优化处理流程以确保最佳画质
     * 
     * @param originalBitmap 原始图片
     * @param gap 两个屏幕之间的间隔（像素）
     * @param enablePPICompensation 是否启用PPI补偿（下屏放大1.1倍补偿）
     * @return 上屏和下屏壁纸的Pair
     */
    fun processWallpaper(originalBitmap: Bitmap, gap: Int = 0, enablePPICompensation: Boolean = true): Pair<Bitmap, Bitmap> {
        val originalWidth = originalBitmap.width
        val originalHeight = originalBitmap.height

        // 根据选项决定是否启用补偿方法
        val compensationFactor = if (enablePPICompensation) {
            // 使用1.1倍补偿（保持与现有行为一致）
            1.1f
        } else {
            // 使用PPI补偿（物理尺寸一致）
            DeviceConfig.LOWER_SCREEN_PPI / DeviceConfig.UPPER_SCREEN_PPI
        }

        // 计算拼接后图片的总尺寸（上下拼接：上屏在上，下屏在下，中间有间隔）
        val combinedWidth = maxOf(DeviceConfig.UPPER_SCREEN_WIDTH, (DeviceConfig.LOWER_SCREEN_WIDTH * compensationFactor).toInt())
        // 为保证补偿效果，下屏区域应有相应的额外分辨率
        val scaledLowerHeight = (DeviceConfig.LOWER_SCREEN_HEIGHT * compensationFactor).toInt()
        val combinedHeight = DeviceConfig.UPPER_SCREEN_HEIGHT + gap + scaledLowerHeight

        // 计算缩放比例，确保原始图片能够覆盖整个拼接区域
        // 保持原始图片的宽高比
        val scaleForWidth = combinedWidth.toFloat() / originalWidth
        val scaleForHeight = combinedHeight.toFloat() / originalHeight
        val scale = maxOf(scaleForWidth, scaleForHeight) // 使用较大比例确保覆盖整个区域

        // 对于高分辨率图片，先进行初步缩放到接近目标尺寸以减少内存使用
        val optimizedBitmap = if (originalWidth * originalHeight > 4000 * 4000) { // 超过16MP的图片先缩放
            val preliminaryScale = min(scale * 0.75f, 1.0f) // 最多缩小到原尺寸的75%
            val preliminaryWidth = (originalWidth * preliminaryScale).toInt()
            val preliminaryHeight = (originalHeight * preliminaryScale).toInt()
            
            // 创建初步缩放的图片
            val preliminaryBitmap = Bitmap.createScaledBitmap(
                originalBitmap,
                preliminaryWidth,
                preliminaryHeight,
                true // 启用过滤以获得更平滑的缩放效果
            )
            
            // 使用Matrix进行高质量缩放（比createScaledBitmap更灵活）
            val matrix = Matrix()
            val finalScale = scale / preliminaryScale
            matrix.postScale(finalScale, finalScale)
            
            val scaledBitmap = Bitmap.createBitmap(
                preliminaryBitmap,
                0, 0,
                preliminaryBitmap.width,
                preliminaryBitmap.height,
                matrix,
                true
            )
            
            // 回收临时图片以释放内存
            if (preliminaryBitmap != scaledBitmap) {
                preliminaryBitmap.recycle()
            }
            
            scaledBitmap
        } else {
            // 使用高质量缩放
            Bitmap.createScaledBitmap(
                originalBitmap, 
                (originalWidth * scale).toInt(), 
                (originalHeight * scale).toInt(), 
                true // 启用过滤以获得更平滑的缩放效果
            )
        }
        
        // 创建拼接画布
        val combinedBitmap = Bitmap.createBitmap(combinedWidth, combinedHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(combinedBitmap)
        
        // 使用高质量渲染设置
        val paint = android.graphics.Paint().apply {
            isFilterBitmap = true  // 启用双线性过滤
            isAntiAlias = true     // 启用抗锯齿
        }
        
        // 用黑色填充整个画布
        canvas.drawColor(android.graphics.Color.BLACK)

        // 将缩放后的图片绘制到画布中央，这样可以确保整体内容居中
        val scaledWidth = optimizedBitmap.width
        val scaledHeight = optimizedBitmap.height
        val bitmapX = (combinedWidth - scaledWidth) / 2f
        val bitmapY = (combinedHeight - scaledHeight) / 2f
        canvas.drawBitmap(optimizedBitmap, bitmapX, bitmapY, paint)

        // 回收临时图片以释放内存
        optimizedBitmap.recycle()

        // 从拼接画布中裁切上下屏壁纸
        val upperScreenBitmap = createUpperScreenBitmap(combinedBitmap)
        val lowerScreenBitmap = createLowerScreenBitmapForPPI(combinedBitmap, gap, scaledLowerHeight, enablePPICompensation)

        return Pair(upperScreenBitmap, lowerScreenBitmap)
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
    private fun createLowerScreenBitmapForPPI(bitmap: Bitmap, gap: Int, scaledLowerHeight: Int, enablePPICompensation: Boolean = true): Bitmap {
        val bitmapWidth = bitmap.width
        val bitmapHeight = bitmap.height

        // 根据是否启用补偿来确定最终输出尺寸
        val compensationFactor = if (enablePPICompensation) {
            // 使用1.1倍补偿
            1.1f
        } else {
            // 使用PPI补偿
            DeviceConfig.LOWER_SCREEN_PPI / DeviceConfig.UPPER_SCREEN_PPI
        }
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