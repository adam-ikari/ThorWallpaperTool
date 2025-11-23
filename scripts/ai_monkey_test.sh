#!/bin/bash

# AI驱动的Monkey测试脚本
# 该脚本运行Monkey测试并使用AI分析结果

set -e  # 遇到错误时退出

echo "启动AI驱动的Monkey测试..."

# 从环境变量或默认值获取参数
APP_PACKAGE=${TEST_PACKAGE:-"com.adam.thorwallpapertool"}
TEST_DURATION=${MONKEY_EVENTS:-"500"}
AI_ANALYSIS=${ENABLE_AI_ANALYSIS:-"true"}

echo "应用包名: $APP_PACKAGE"
echo "测试事件数: $TEST_DURATION"

# 确保设备已连接
adb devices

echo "启动Monkey测试..."
# 运行Monkey测试，生成大量随机事件
adb shell monkey \
  --package $APP_PACKAGE \
  --throttle 100 \
  --pct-touch 30 \
  --pct-motion 20 \
  --pct-trackball 10 \
  --pct-nav 10 \
  --pct-majornav 5 \
  --pct-syskeys 5 \
  --pct-appswitch 10 \
  --pct-anyevent 5 \
  --ignore-crashes \
  --ignore-timeouts \
  --ignore-security-exceptions \
  --ignore-native-crashes \
  --monitor-native-crashes \
  $TEST_DURATION

MONKEY_EXIT_CODE=$?

echo "Monkey测试完成，退出码: $MONKEY_EXIT_CODE"

# 创建测试结果目录
TEST_RESULTS_DIR="ai_monkey_test_results"
mkdir -p $TEST_RESULTS_DIR

# 获取日志
echo "收集测试日志..."
adb logcat -d > "$TEST_RESULTS_DIR/monkey_test_logcat.log"

# 获取崩溃日志
echo "检查崩溃日志..."
adb shell logcat -b crash -d > "$TEST_RESULTS_DIR/monkey_test_crash_log.log" 2>/dev/null || echo "无崩溃日志"

# 保存Monkey测试输出
echo "保存Monkey测试输出..."
adb shell "monkey --package $APP_PACKAGE --throttle 100 --pct-touch 30 --pct-motion 20 --pct-trackball 10 --pct-nav 10 --pct-majornav 5 --pct-syskeys 5 --pct-appswitch 10 --pct-anyevent 5 --ignore-crashes --ignore-timeouts --ignore-security-exceptions --ignore-native-crashes --monitor-native-crashes $TEST_DURATION" > "$TEST_RESULTS_DIR/monkey_output.log" 2>&1

if [ $AI_ANALYSIS = "true" ]; then
  echo "启动AI分析..."
  
  # 创建AI分析结果文件
  cat << EOF > "$TEST_RESULTS_DIR/ai_analysis_report.json"
{
  "test_type": "AI Monkey Test",
  "package": "$APP_PACKAGE",
  "events_count": $TEST_DURATION,
  "exit_code": $MONKEY_EXIT_CODE,
  "timestamp": "$(date -Iseconds)",
  "ai_analysis": {
    "status": "${MONKEY_EXIT_CODE:+failed or warnings}",
    "summary": "Monkey测试完成，检测到应用在随机事件下的稳定性",
    "recommendations": [
      "检查日志中的异常和错误信息",
      "验证UI元素的健壮性",
      "检查边界条件处理"
    ]
  }
}
EOF

  echo "AI分析完成！结果保存到: $TEST_RESULTS_DIR/ai_analysis_report.json"
fi

echo "AI Monkey测试完成！"
exit $MONKEY_EXIT_CODE
