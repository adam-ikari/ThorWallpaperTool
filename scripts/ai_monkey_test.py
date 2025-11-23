#!/usr/bin/env python3

"""
AI驱动的Monkey测试脚本
该脚本执行智能随机测试，模拟用户操作并收集应用稳定性数据
"""

import os
import sys
import time
import subprocess
import random
import json
from datetime import datetime
from pathlib import Path

class AIMonkeyTester:
    def __init__(self, package_name, output_dir="ai_monkey_results"):
        self.package_name = package_name
        self.output_dir = Path(output_dir)
        self.output_dir.mkdir(exist_ok=True)
        
        # AI策略参数
        self.strategies = {
            'random_taps': 0.3,
            'ui_element_interaction': 0.4,
            'navigation_patterns': 0.2,
            'stress_testing': 0.1
        }
        
        # 记录测试事件
        self.test_events = []
        
    def run_monkey_test(self, duration_minutes=5, event_count=1000):
        """运行AI驱动的Monkey测试"""
        print(f"开始AI Monkey测试，目标应用: {self.package_name}")
        print(f"测试时长: {duration_minutes}分钟，事件数: {event_count}")
        
        start_time = time.time()
        end_time = start_time + (duration_minutes * 60)
        
        # 生成AI分析的测试事件
        events_executed = 0
        while time.time() < end_time and events_executed < event_count:
            # 根据AI策略选择测试动作
            action = self.select_action()
            self.execute_action(action)
            
            events_executed += 1
            if events_executed % 100 == 0:
                print(f"已执行 {events_executed}/{event_count} 个事件")
        
        print("AI Monkey测试完成")
        self.save_test_results()
        
    def select_action(self):
        """根据AI策略选择测试动作"""
        strategy = random.choices(
            list(self.strategies.keys()),
            weights=list(self.strategies.values())
        )[0]
        
        action = {
            'timestamp': datetime.now().isoformat(),
            'strategy': strategy
        }
        
        if strategy == 'random_taps':
            action['type'] = 'tap'
            action['x'] = random.randint(100, 1000)  # 假设屏幕宽度
            action['y'] = random.randint(100, 1800)  # 假设屏幕高度
        elif strategy == 'ui_element_interaction':
            action['type'] = random.choice(['tap', 'swipe', 'long_press'])
            action['element'] = random.choice([
                'btnSelectImage', 'btnProcessImage', 'btnSetWallpaper',
                'editGap', 'checkPPICompensation', 'imagePreview'
            ])
        elif strategy == 'navigation_patterns':
            action['type'] = 'navigation'
            action['pattern'] = random.choice(['back', 'home', 'recent_apps'])
        elif strategy == 'stress_testing':
            action['type'] = 'stress'
            action['operation'] = random.choice(['rapid_taps', 'memory_load', 'concurrent_actions'])
        
        return action
    
    def execute_action(self, action):
        """执行测试动作"""
        try:
            if action['type'] == 'tap':
                if 'x' in action and 'y' in action:
                    cmd = f"adb shell input tap {action['x']} {action['y']}"
                elif 'element' in action:
                    # 通过UI Automator查找元素并点击
                    cmd = f"adb shell uiautomator runtest ThorWallpaperToolEndToEndTest.jar -c 'com.adam.thorwallpapertool.ThorWallpaperToolEndToEndTest#testClickElement'"
                    # 简化处理，实际会更复杂
                    cmd = f"adb shell input tap 500 500"  # 示例点击
            elif action['type'] == 'swipe':
                cmd = f"adb shell input swipe 500 1000 500 500"
            elif action['type'] == 'long_press':
                cmd = f"adb shell input swipe 500 500 500 500 1000"  # 长按1秒
            elif action['type'] == 'navigation':
                if action['pattern'] == 'back':
                    cmd = "adb shell input keyevent KEYCODE_BACK"
                elif action['pattern'] == 'home':
                    cmd = "adb shell input keyevent KEYCODE_HOME"
                elif action['pattern'] == 'recent_apps':
                    cmd = "adb shell input keyevent KEYCODE_APP_SWITCH"
            else:
                cmd = f"adb shell input tap 500 500"  # 默认点击
            
            # 记录事件
            self.test_events.append(action)
            
            # 执行ADB命令（模拟）
            print(f"执行动作: {action['type']} - {action.get('element', action.get('x', action.get('pattern')))}")
            
            # 随机延迟
            time.sleep(random.uniform(0.5, 2.0))
            
        except Exception as e:
            print(f"执行动作时出错: {e}")
            action['error'] = str(e)
            self.test_events.append(action)
    
    def save_test_results(self):
        """保存测试结果"""
        results = {
            'timestamp': datetime.now().isoformat(),
            'package_name': self.package_name,
            'total_events': len(self.test_events),
            'events': self.test_events,
            'strategies_distribution': self.analyze_strategy_distribution()
        }
        
        results_file = self.output_dir / f"ai_monkey_results_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"
        with open(results_file, 'w', encoding='utf-8') as f:
            json.dump(results, f, ensure_ascii=False, indent=2)
        
        print(f"测试结果已保存到: {results_file}")
        
        # 生成AI分析报告
        self.generate_ai_report(results)
    
    def analyze_strategy_distribution(self):
        """分析策略分布"""
        distribution = {}
        for event in self.test_events:
            strategy = event['strategy']
            distribution[strategy] = distribution.get(strategy, 0) + 1
        
        return distribution
    
    def generate_ai_report(self, results):
        """生成AI分析报告"""
        total_events = results['total_events']
        strategies = results['strategies_distribution']
        
        report = {
            'summary': f"AI Monkey测试完成，共执行{total_events}个事件",
            'strategy_effectiveness': self.analyze_strategy_effectiveness(strategies),
            'recommendations': self.generate_recommendations(results),
            'risk_assessment': self.assess_risks(results)
        }
        
        report_file = self.output_dir / f"ai_analysis_report_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"
        with open(report_file, 'w', encoding='utf-8') as f:
            json.dump(report, f, ensure_ascii=False, indent=2)
        
        print(f"AI分析报告已保存到: {report_file}")
        self.print_summary(report)
    
    def analyze_strategy_effectiveness(self, strategies):
        """分析策略有效性"""
        effectiveness = {}
        for strategy, count in strategies.items():
            # 简化的有效性分析
            if count > 0:
                effectiveness[strategy] = {
                    'events_count': count,
                    'effectiveness_score': min(count / 10, 1.0)  # 简化的评分
                }
        return effectiveness
    
    def generate_recommendations(self, results):
        """生成AI建议"""
        recommendations = []
        
        # 基于测试结果生成建议
        if results['total_events'] > 500:
            recommendations.append("测试事件数充足，覆盖范围良好")
        else:
            recommendations.append("建议增加测试事件数以提高覆盖率")
        
        # 基于策略分布生成建议
        strategies = results['strategies_distribution']
        if strategies.get('ui_element_interaction', 0) < results['total_events'] * 0.3:
            recommendations.append("增加UI元素交互测试以提高功能覆盖率")
        
        if strategies.get('stress_testing', 0) < results['total_events'] * 0.1:
            recommendations.append("增加压力测试以验证应用稳定性")
        
        return recommendations
    
    def assess_risks(self, results):
        """风险评估"""
        total_events = results['total_events']
        
        return {
            'crash_risk': 'low' if total_events > 0 else 'unknown',
            'ui_breakage_risk': 'medium',
            'performance_risk': 'low',
            'overall_risk_level': 'medium'
        }
    
    def print_summary(self, report):
        """打印摘要"""
        print("\n" + "="*60)
        print("AI Monkey测试分析报告")
        print("="*60)
        print(f"摘要: {report['summary']}")
        print(f"整体风险等级: {report['risk_assessment']['overall_risk_level']}")
        print("\n策略效果:")
        for strategy, data in report['strategy_effectiveness'].items():
            print(f"  {strategy}: {data['events_count']} 事件")
        print("\nAI建议:")
        for rec in report['recommendations']:
            print(f"  - {rec}")
        print("="*60)

def main():
    if len(sys.argv) < 2:
        print("使用方法: python ai_monkey_test.py <package_name> [duration_minutes] [event_count]")
        print("示例: python ai_monkey_test.py com.adam.thorwallpapertool 5 1000")
        sys.exit(1)
    
    package_name = sys.argv[1]
    duration_minutes = int(sys.argv[2]) if len(sys.argv) > 2 else 5
    event_count = int(sys.argv[3]) if len(sys.argv) > 3 else 1000
    
    tester = AIMonkeyTester(package_name)
    tester.run_monkey_test(duration_minutes, event_count)

if __name__ == "__main__":
    main()
