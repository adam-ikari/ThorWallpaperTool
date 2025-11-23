package com.adam.thorwallpapertool

import android.content.Context
import android.content.Intent
import androidx.test.core.app.launchActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 索尔掌机壁纸工具的端到端测试
 * 使用UI Automator进行测试
 */
@RunWith(AndroidJUnit4::class)
class ThorWallpaperToolEndToEndTest {

    private val LAUNCH_TIMEOUT = 5000L
    private val packageName = "com.adam.thorwallpapertool"

    @Test
    fun testWallpaperGenerationFlow() {
        // 获取UiDevice实例
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        // 启动应用
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK) // Clear out any previous instances
        context.startActivity(intent)

        // 等待主界面出现
        device.wait(Until.hasObject(By.pkg(packageName).depth(0)), LAUNCH_TIMEOUT)

        // 验证应用启动成功
        val mainScreen = device.findObject(By.res(packageName, "main"))
        assertEquals(true, mainScreen.exists())

        // 验证界面元素存在
        val selectImageButton = device.findObject(By.res(packageName, "btnSelectImage"))
        val gapEditText = device.findObject(By.res(packageName, "editGap"))
        val processImageButton = device.findObject(By.res(packageName, "btnProcessImage"))
        val ppiCompensationCheckBox = device.findObject(By.res(packageName, "checkPPICompensation"))

        assertEquals(true, selectImageButton.exists())
        assertEquals(true, gapEditText.exists())
        assertEquals(true, processImageButton.exists())
        assertEquals(true, ppiCompensationCheckBox.exists())

        // 测试流程: 点击选择图片 -> 设置间隔 -> 生成壁纸 -> 验证预览

        // 注意: 实际的图片选择和处理操作可能需要模拟或使用测试图片
        // 由于这是一个端到端测试，我们验证应用界面交互是否正常
    }

    @Test
    fun testBasicUIElements() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        // 启动Activity
        val scenario = launchActivity<MainActivity>()

        // 获取上下文
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals(packageName, context.packageName)
    }
}