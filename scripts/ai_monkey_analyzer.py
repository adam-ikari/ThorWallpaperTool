#!/usr/bin/env python3

"""
AI驱动的Monkey测试分析器
该脚本分析Monkey测试结果，并使用AI技术提供智能反馈
"""

import os
import re
import json
import sys
import subprocess
from datetime import datetime
from pathlib import Path
from collections import defaultdict

def analyze_logcat(logcat_file):
    """分析logcat日志文件"""
    print(f"分析日志文件: {logcat_file}")
    
    with open(logcat_file, 'r', encoding='utf-8', errors='ignore') as f:
        content = f.read()
    
    # 定义要搜索的模式
    patterns = {
        'exceptions': r'Exception|Error',
        'anrs': r'ANR|Application Not Responding',
        'crashes': r'FATAL EXCEPTION|killed by signal|Native crash',
        'warnings': r'Warning|warn',
        'memory_issues': r'OutOfMemory|memory',
        'ui_issues': r'NotResponding|Choreographer|Skipped frames'
    }
    
    results = defaultdict(list)
    
    for category, pattern in patterns.items():
        matches = re.findall(pattern, content, re.IGNORECASE)
        results[category] = matches
        print(f"  {category}: {len(matches)} 个匹配项")
    
    return results

def ai_analysis(monkey_results, log_analysis, crash_log):
    """AI分析Monkey测试结果"""
    print("\n" + "="*60)
    print("AI分析结果:")
    print("="*60)
    
    # 计算稳定性指标
    total_events = monkey_results.get('events_count', 0)
    exceptions_count = len(log_analysis['exceptions'])
    anrs_count = len(log_analysis['anrs'])
    crashes_count = len(log_analysis['crashes'])
    
    print(f"总测试事件数: {total_events}")
    print(f"异常数量: {exceptions_count}")
    print(f"ANR数量: {anrs_count}")
    print(f"崩溃数量: {crashes_count}")
    
    # 计算稳定性评分
    stability_score = 100
    if exceptions_count > 0:
        stability_score -= min(exceptions_count * 5, 30)  # 每个异常扣5分，最多扣30
    if anrs_count > 0:
        stability_score -= min(anrs_count * 10, 40)  # 每个ANR扣10分，最多扣40
    if crashes_count > 0:
        stability_score -= min(crashes_count * 20, 50)  # 每个崩溃扣20分，最多扣50
    
    stability_score = max(0, stability_score)  # 确保分数不小于0
    
    print(f"稳定性评分: {stability_score}/100")
    
    # AI建议
    ai_recommendations = []
    
    if crashes_count > 0:
        ai_recommendations.append("严重：检测到应用崩溃，请优先修复崩溃问题")
    if anrs_count > 0:
        ai_recommendations.append("重要：检测到ANR，请优化主线程性能")
    if exceptions_count > 0:
        ai_recommendations.append("建议：检查并修复异常处理逻辑")
    
    if stability_score >= 90:
        ai_status = "优秀"
        ai_recommendations.append("应用稳定性表现优秀，继续保持")
    elif stability_score >= 70:
        ai_status = "良好"
        ai_recommendations.append("应用稳定性良好，可进一步优化")
    elif stability_score >= 50:
        ai_status = "一般"
        ai_recommendations.append("应用存在稳定性问题，需要优化")
    else:
        ai_status = "较差"
        ai_recommendations.append("应用稳定性差，需要优先修复问题")
    
    print(f"AI评估状态: {ai_status}")
    
    print("\nAI建议:")
    for i, rec in enumerate(ai_recommendations, 1):
        print(f"  {i}. {rec}")
    
    return {
        'stability_score': stability_score,
        'status': ai_status,
        'recommendations': ai_recommendations
    }

def main():
    if len(sys.argv) < 2:
        print("使用方法: python ai_monkey_analyzer.py <test_results_directory>")
        sys.exit(1)
    
    test_results_dir = Path(sys.argv[1])
    
    if not test_results_dir.exists():
        print(f"错误: 测试结果目录不存在: {test_results_dir}")
        sys.exit(1)
    
    # 读取monkey输出日志
    monkey_output_path = test_results_dir / "monkey_output.log"
    if monkey_output_path.exists():
        with open(monkey_output_path, 'r', encoding='utf-8', errors='ignore') as f:
            monkey_output = f.read()
        
        # 尝试从输出中提取事件数
        events_match = re.search(r'events (\d+)', monkey_output)
        events_count = int(events_match.group(1)) if events_match else 500
        
        monkey_results = {
            'events_count': events_count,
            'output': monkey_output
        }
    else:
        print("警告: 未找到monkey输出日志")
        monkey_results = {
            'events_count': 500,  # 默认值
            'output': ''
        }
    
    # 分析logcat日志
    logcat_path = test_results_dir / "monkey_test_logcat.log"
    if logcat_path.exists():
        log_analysis = analyze_logcat(logcat_path)
    else:
        print("警告: 未找到logcat日志")
        log_analysis = defaultdict(list)
    
    # 分析崩溃日志
    crash_log_path = test_results_dir / "monkey_test_crash_log.log"
    if crash_log_path.exists():
        crash_analysis = analyze_logcat(crash_log_path)
    else:
        print("警告: 未找到崩溃日志")
        crash_analysis = defaultdict(list)
    
    # AI分析
    ai_result = ai_analysis(monkey_results, log_analysis, crash_analysis)
    
    # 生成详细报告
    report = {
        'test_type': 'AI Monkey Test',
        'timestamp': datetime.now().isoformat(),
        'monkey_results': monkey_results,
        'log_analysis': dict(log_analysis),
        'crash_analysis': dict(crash_analysis),
        'ai_analysis': ai_result
    }
    
    # 保存AI分析报告
    report_path = test_results_dir / "ai_monkey_analysis_report.json"
    with open(report_path, 'w', encoding='utf-8') as f:
        json.dump(report, f, ensure_ascii=False, indent=2)
    
    print(f"\nAI分析报告已保存到: {report_path}")
    
    # 根据稳定性评分返回适当的退出码
    if ai_result['stability_score'] < 70:
        print("\n稳定性评分低于70，返回失败退出码")
        sys.exit(1)
    else:
        print("\n稳定性评分达到要求，返回成功退出码")
        sys.exit(0)

if __name__ == "__main__":
    main()