package com.adam.thorwallpapertool

/**
 * 索尔掌机屏幕参数配置
 */
object DeviceConfig {
    // 索尔掌机屏幕参数
    const val UPPER_SCREEN_WIDTH = 1920
    const val UPPER_SCREEN_HEIGHT = 1080
    const val LOWER_SCREEN_WIDTH = 1240
    const val LOWER_SCREEN_HEIGHT = 1080

    // 索尔掌机屏幕PPI信息
    // 上屏：6寸 1920x1080 -> 约 367 PPI
    // 下屏：3.92寸 1240x1080 -> 约 297 PPI
    const val UPPER_SCREEN_PPI = 367f
    const val LOWER_SCREEN_PPI = 297f
}