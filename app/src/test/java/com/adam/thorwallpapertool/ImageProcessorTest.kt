package com.adam.thorwallpapertool

import org.junit.Test
import org.junit.Assert.*

class ImageProcessorTest {

    @Test
    fun `test PPI ratio calculation`() {
        // 验证PPI比率计算
        val ppiRatio = DeviceConfig.LOWER_SCREEN_PPI / DeviceConfig.UPPER_SCREEN_PPI
        val expectedRatio = 297f / 367f  // ≈ 0.809
        assertEquals(expectedRatio, ppiRatio, 0.001f)
    }

    @Test
    fun `test DeviceConfig values`() {
        // 验证设备配置值
        assertEquals(1920, DeviceConfig.UPPER_SCREEN_WIDTH)
        assertEquals(1080, DeviceConfig.UPPER_SCREEN_HEIGHT)
        assertEquals(1240, DeviceConfig.LOWER_SCREEN_WIDTH)
        assertEquals(1080, DeviceConfig.LOWER_SCREEN_HEIGHT)
        assertEquals(367f, DeviceConfig.UPPER_SCREEN_PPI, 0.01f)
        assertEquals(297f, DeviceConfig.LOWER_SCREEN_PPI, 0.01f)
    }
}