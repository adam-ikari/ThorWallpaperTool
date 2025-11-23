#!/bin/bash

# AI驱动的端到端测试分析脚本
# 该脚本会在Android端到端测试完成后运行，分析测试结果

echo "开始AI分析端到端测试结果..."

# 检查测试结果
TEST_RESULTS_PATH="app/build/reports/androidTests/connected"
LOGCAT_PATH="app/build/outputs/logs"

echo "检查测试结果路径: $TEST_RESULTS_PATH"

if [ -d "$TEST_RESULTS_PATH" ]; then
    echo "找到测试结果目录，正在分析..."
    
    # 检查测试是否通过
    if find "$TEST_RESULTS_PATH" -name "*.xml" -exec grep -l "failure\|error" {} \; > /dev/null; then
        echo "检测到测试失败，正在使用AI分析失败原因..."
        
        # AI分析失败的测试
        for result_file in "$TEST_RESULTS_PATH"/*/*.xml; do
            if [ -f "$result_file" ]; then
                echo "分析测试结果文件: $result_file"
                # 这里可以调用AI API来分析测试失败的原因
                # 示例：分析XML中的失败信息
                if grep -q "failure\|error" "$result_file"; then
                    echo "发现失败的测试案例:"
                    grep -A 3 -B 3 "failure\|error" "$result_file"
                fi
            fi
        done
    else
        echo "所有测试通过！"
    fi
else
    echo "未找到测试结果目录，可能需要先运行测试"
    exit 1
fi

# 分析logcat日志
if [ -d "$LOGCAT_PATH" ]; then
    echo "分析Logcat日志..."
    # 查找异常日志
    find "$LOGCAT_PATH" -name "*.txt" -exec grep -l "Exception\|Error" {} \; 2>/dev/null
fi

echo "AI分析完成!"