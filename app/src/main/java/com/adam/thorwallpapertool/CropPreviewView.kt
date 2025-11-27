package com.adam.thorwallpapertool

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class CropPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var originalBitmap: Bitmap? = null
    private var scaledBitmap: Bitmap? = null
    private var gapPixels: Int = 0
    
    // 绘制相关
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cropAreaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.parseColor("#2196F3") // 蓝色边框
    }
    
    private val gapAreaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#44000000") // 半透明黑色
    }
    
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 36f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#88000000") // 半透明黑色背景
    }
    
    // 缩放和位置计算
    private var viewWidth = 0
    private var viewHeight = 0
    private var bitmapScale = 1f
    private var bitmapLeft = 0f
    private var bitmapTop = 0f
    private var scaledBitmapWidth = 0f
    private var scaledBitmapHeight = 0f
    
    // 裁切区域
    private var upperCropRect = RectF()
    private var lowerCropRect = RectF()
    private var gapRect = RectF()

    fun setBitmap(bitmap: Bitmap?, gap: Int = 0) {
        originalBitmap = bitmap
        gapPixels = gap
        requestLayout()
        calculateScaledBitmap()
        invalidate()
    }
    
    private fun calculateScaledBitmap() {
        originalBitmap?.let { bitmap ->
            if (viewWidth > 0 && viewHeight > 0) {
                // 计算缩放比例以适应视图
                val widthRatio = viewWidth.toFloat() / bitmap.width
                val heightRatio = viewHeight.toFloat() / bitmap.height
                bitmapScale = minOf(widthRatio, heightRatio)
                
                // 计算缩放后的尺寸
                scaledBitmapWidth = bitmap.width * bitmapScale
                scaledBitmapHeight = bitmap.height * bitmapScale
                
                // 计算居中位置
                bitmapLeft = (viewWidth - scaledBitmapWidth) / 2f
                bitmapTop = (viewHeight - scaledBitmapHeight) / 2f
                
                // 创建缩放后的位图
                val matrix = Matrix().apply {
                    postScale(bitmapScale, bitmapScale)
                }
                scaledBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                
                calculateCropAreas()
            }
        }
    }
    
    private fun calculateCropAreas() {
        originalBitmap?.let { original ->
            scaledBitmap?.let { bitmap ->
                // 完全复制ImageProcessor.processWallpaper方法的计算逻辑
                val originalWidth = original.width
                val originalHeight = original.height

                // PPI参数
                val upperPPI = DeviceConfig.UPPER_SCREEN_PPI
                val lowerPPI = DeviceConfig.LOWER_SCREEN_PPI
                
                // 目标输出尺寸
                val upperOutputWidth = DeviceConfig.UPPER_SCREEN_WIDTH
                val upperOutputHeight = DeviceConfig.UPPER_SCREEN_HEIGHT
                val lowerOutputWidth = DeviceConfig.LOWER_SCREEN_WIDTH
                val lowerOutputHeight = DeviceConfig.LOWER_SCREEN_HEIGHT

                // 计算PPI补偿因子
                val ppiRatio = upperPPI / lowerPPI
                
                // 计算下屏在物理尺寸上等效的像素尺寸
                val lowerPhysicalEquivalentHeight = (lowerOutputHeight / ppiRatio).toInt()
                val lowerPhysicalEquivalentWidth = (lowerOutputWidth / ppiRatio).toInt()
                
                // 将间隔像素加到上屏的目标高度上
                val upperHeightWithGap = upperOutputHeight + gapPixels
                
                // 计算总的物理等效裁切高度
                val totalPhysicalHeight = upperHeightWithGap + lowerPhysicalEquivalentHeight
                
                // 确定裁切宽度（取两个屏幕物理等效宽度的较大值）
                val targetCropWidth = maxOf(upperOutputWidth, lowerPhysicalEquivalentWidth)
                
                // 确定实际裁切尺寸
                var actualCropWidth: Int
                var actualCropHeight: Int
                
                // 检查原始图片是否足够大
                if (originalWidth < targetCropWidth || originalHeight < totalPhysicalHeight) {
                    // 原图较小，按比例计算裁切尺寸
                    val widthScale = originalWidth.toFloat() / targetCropWidth
                    val heightScale = originalHeight.toFloat() / totalPhysicalHeight
                    val scale = minOf(widthScale, heightScale)
                    
                    actualCropWidth = (targetCropWidth * scale).toInt()
                    actualCropHeight = (totalPhysicalHeight * scale).toInt()
                } else {
                    // 原图足够大，直接使用原图的最大可能裁切区域
                    actualCropWidth = originalWidth
                    actualCropHeight = originalHeight
                }
                
                // 计算裁切起始位置（居中）
                val cropStartX = maxOf(0, (originalWidth - actualCropWidth) / 2)
                val cropStartY = maxOf(0, (originalHeight - actualCropHeight) / 2)

                // 计算上屏和下屏在裁切区域中的高度
                val upperCropHeight = ((upperHeightWithGap.toFloat() / totalPhysicalHeight) * actualCropHeight).toInt()
                val remainingHeight = actualCropHeight - upperCropHeight
                val lowerCropHeight = if (remainingHeight > 0) remainingHeight else 1

                // 裁切上屏内容（需要减去间隔像素的缩放比例）
                val gapScaleRatio = upperCropHeight.toFloat() / upperHeightWithGap
                val gapInCropPixels = (gapPixels * gapScaleRatio).toInt()
                val actualUpperCropHeight = upperCropHeight - gapInCropPixels
                
                // 计算上屏和下屏在裁切区域中的实际宽度
                // 上屏使用实际裁切宽度
                val upperCropWidth = actualCropWidth
                
                // 下屏使用物理等效宽度（考虑PPI差异）
                val lowerCropWidth = (actualCropWidth * lowerPhysicalEquivalentWidth.toFloat() / targetCropWidth).toInt()
                val lowerCropOffsetX = (actualCropWidth - lowerCropWidth) / 2  // 居中对齐
                
                // 计算这些裁切区域在原始图片中的实际位置
                val upperCropStartX = cropStartX
                val upperCropStartY = cropStartY
                val upperCropEndX = cropStartX + upperCropWidth
                val upperCropEndY = cropStartY + actualUpperCropHeight
                
                val lowerCropStartX = cropStartX + lowerCropOffsetX
                val lowerCropStartY = cropStartY + upperCropHeight
                val lowerCropEndX = lowerCropStartX + lowerCropWidth
                val lowerCropEndY = cropStartY + upperCropHeight + lowerCropHeight
                
                // 计算这些区域在预览视图中的位置
                // 首先计算原始图片到预览视图的缩放比例
                val originalToPreviewScale = bitmapScale
                
                // 计算原始图片裁切区域在预览视图中的对应位置
                val upperPreviewStartX = upperCropStartX * originalToPreviewScale + bitmapLeft
                val upperPreviewStartY = upperCropStartY * originalToPreviewScale + bitmapTop
                val upperPreviewEndX = upperCropEndX * originalToPreviewScale + bitmapLeft
                val upperPreviewEndY = upperCropEndY * originalToPreviewScale + bitmapTop
                
                val lowerPreviewStartX = lowerCropStartX * originalToPreviewScale + bitmapLeft
                val lowerPreviewStartY = lowerCropStartY * originalToPreviewScale + bitmapTop
                val lowerPreviewEndX = lowerCropEndX * originalToPreviewScale + bitmapLeft
                val lowerPreviewEndY = lowerCropEndY * originalToPreviewScale + bitmapTop
                
                // 计算间隔区域在预览中的位置（使用上屏的宽度）
                val gapPreviewStartY = upperPreviewEndY
                val gapPreviewEndY = lowerPreviewStartY
                
                // 设置裁切区域
                upperCropRect = RectF(
                    upperPreviewStartX,
                    upperPreviewStartY,
                    upperPreviewEndX,
                    upperPreviewEndY
                )
                
                gapRect = RectF(
                    upperPreviewStartX,
                    gapPreviewStartY,
                    upperPreviewEndX,
                    gapPreviewEndY
                )
                
                lowerCropRect = RectF(
                    lowerPreviewStartX,
                    lowerPreviewStartY,
                    lowerPreviewEndX,
                    lowerPreviewEndY
                )
            }
        }
    }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewWidth = w
        viewHeight = h
        calculateScaledBitmap()
    }
    
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        
        originalBitmap?.let { bitmap ->
            val width = MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
            
            if (width > 0) {
                // 计算适合宽度的缩放比例
                val scale = width.toFloat() / bitmap.width
                val scaledHeight = (bitmap.height * scale).toInt()
                
                // 确保高度不小于最小值
                val finalHeight = maxOf(scaledHeight, 200)
                
                setMeasuredDimension(width + paddingLeft + paddingRight, finalHeight + paddingTop + paddingBottom)
            }
        }
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // 绘制背景
        canvas.drawColor(Color.parseColor("#f5f5f5"))
        
        // 绘制缩放后的图片
        scaledBitmap?.let { bitmap ->
            canvas.drawBitmap(bitmap, bitmapLeft, bitmapTop, paint)
        }
        
        // 绘制裁切区域
        drawCropAreas(canvas)
    }
    
    private fun drawCropAreas(canvas: Canvas) {
        if (upperCropRect.isEmpty || lowerCropRect.isEmpty) return
        
        // 绘制上屏裁切区域
        canvas.drawRoundRect(upperCropRect, 8f, 8f, cropAreaPaint)
        
        // 绘制间隔区域（如果有间隔）
        if (gapPixels > 0) {
            canvas.drawRect(gapRect, gapAreaPaint)
        }
        
        // 绘制下屏裁切区域
        canvas.drawRoundRect(lowerCropRect, 8f, 8f, cropAreaPaint)
        
        // 绘制标签
        drawLabels(canvas)
    }
    
    private fun drawLabels(canvas: Canvas) {
        // 上屏标签
        val upperLabel = "上屏 1920×1080"
        val upperLabelBounds = Rect()
        labelPaint.getTextBounds(upperLabel, 0, upperLabel.length, upperLabelBounds)
        
        val upperLabelBackground = RectF(
            upperCropRect.centerX() - upperLabelBounds.width() / 2f - 16f,
            upperCropRect.top - 50f,
            upperCropRect.centerX() + upperLabelBounds.width() / 2f + 16f,
            upperCropRect.top - 10f
        )
        canvas.drawRoundRect(upperLabelBackground, 4f, 4f, backgroundPaint)
        canvas.drawText(upperLabel, upperCropRect.centerX(), upperCropRect.top - 20f, labelPaint)
        
        // 下屏标签
        val lowerLabel = "下屏 1240×1080"
        val lowerLabelBounds = Rect()
        labelPaint.getTextBounds(lowerLabel, 0, lowerLabel.length, lowerLabelBounds)
        
        val lowerLabelBackground = RectF(
            lowerCropRect.centerX() - lowerLabelBounds.width() / 2f - 16f,
            lowerCropRect.bottom + 10f,
            lowerCropRect.centerX() + lowerLabelBounds.width() / 2f + 16f,
            lowerCropRect.bottom + 50f
        )
        canvas.drawRoundRect(lowerLabelBackground, 4f, 4f, backgroundPaint)
        canvas.drawText(lowerLabel, lowerCropRect.centerX(), lowerCropRect.bottom + 35f, labelPaint)
        
        // 间隔标签（如果有间隔）
        if (gapPixels > 0) {
            val gapLabel = "间隔 ${gapPixels}px"
            val gapLabelBounds = Rect()
            labelPaint.getTextBounds(gapLabel, 0, gapLabel.length, gapLabelBounds)
            
            canvas.drawText(gapLabel, gapRect.centerX(), gapRect.centerY(), labelPaint)
        }
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // 清理位图资源
        scaledBitmap?.recycle()
        scaledBitmap = null
    }
}