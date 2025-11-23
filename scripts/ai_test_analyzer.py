#!/usr/bin/env python3

"""
AI驱动的端到端测试分析器
该脚本分析Android端到端测试结果，并使用AI技术提供测试反馈
"""

import os
import sys
import json
import xml.etree.ElementTree as ET
from pathlib import Path

def analyze_test_results(test_results_dir):
    """分析测试结果XML文件"""
    print("正在分析测试结果...")
    
    results = {
        'total_tests': 0,
        'passed_tests': 0,
        'failed_tests': 0,
        'errors': [],
        'failures': []
    }
    
    for xml_file in Path(test_results_dir).rglob("*.xml"):
        print(f"分析测试结果文件: {xml_file}")
        try:
            tree = ET.parse(xml_file)
            root = tree.getroot()
            
            # 提取测试统计信息
            if 'tests' in root.attrib:
                results['total_tests'] += int(root.attrib['tests'])
            if 'failures' in root.attrib:
                results['failed_tests'] += int(root.attrib['failures'])
            if 'errors' in root.attrib:
                results['errors'].extend(root.attrib['errors'])
                
            # 提取具体的失败和错误信息
            for testcase in root.findall(".//failure"):
                results['failures'].append({
                    'test': testcase.getparent().get('name'),
                    'message': testcase.text,
                    'type': testcase.get('type', 'failure')
                })
                
            for testcase in root.findall(".//error"):
                results['errors'].append({
                    'test': testcase.getparent().get('name'),
                    'message': testcase.text,
                    'type': testcase.get('type', 'error')
                })
                
        except ET.ParseError as e:
            print(f"解析XML文件时出错: {xml_file}, 错误: {e}")
    
    results['passed_tests'] = results['total_tests'] - results['failed_tests'] - len(results['errors'])
    
    return results

def ai_analysis_simulation(results):
    """模拟AI分析过程"""
    print("\n" + "="*50)
    print("AI分析结果:")
    print("="*50)
    
    print(f"总测试数: {results['total_tests']}")
    print(f"通过测试: {results['passed_tests']}")
    print(f"失败测试: {results['failed_tests']}")
    print(f"错误数: {len(results['errors'])}")
    
    if results['failed_tests'] > 0 or len(results['errors']) > 0:
        print("\n发现问题:")
        for failure in results['failures']:
            print(f"- 测试 {failure['test']} 失败: {failure['message'][:100]}...")
        
        for error in results['errors']:
            print(f"- 测试 {error['test']} 错误: {error['message'][:100]}...")
        
        # AI建议
        print("\nAI建议:")
        print("- 检查UI元素是否正确加载")
        print("- 验证图片处理逻辑")
        print("- 确认权限设置正确")
        print("- 检查设备兼容性问题")
    else:
        print("\n所有测试通过！应用功能正常。")
        
    # 生成AI分析报告
    report = {
        "summary": f"测试完成，{results['passed_tests']}/{results['total_tests']} 通过",
        "status": "passed" if results['failed_tests'] == 0 and len(results['errors']) == 0 else "failed",
        "ai_recommendations": generate_ai_recommendations(results)
    }
    
    return report

def generate_ai_recommendations(results):
    """生成AI建议"""
    recommendations = []
    
    if results['failed_tests'] > 0:
        recommendations.append("优先修复失败的测试案例")
        recommendations.append("检查UI交互流程")
    
    if len(results['errors']) > 0:
        recommendations.append("解决代码中的错误")
        recommendations.append("检查异常处理逻辑")
    
    if results['passed_tests'] > 0:
        recommendations.append("继续维护高质量测试")
        recommendations.append("考虑增加边界条件测试")
    
    return recommendations

def main():
    if len(sys.argv) < 2:
        print("使用方法: python ai_test_analyzer.py <test_results_directory>")
        sys.exit(1)
    
    test_results_dir = sys.argv[1]
    
    if not os.path.exists(test_results_dir):
        print(f"错误: 测试结果目录不存在: {test_results_dir}")
        sys.exit(1)
    
    # 分析测试结果
    results = analyze_test_results(test_results_dir)
    
    # AI分析
    report = ai_analysis_simulation(results)
    
    # 保存AI分析报告
    report_path = Path(test_results_dir) / "ai_analysis_report.json"
    with open(report_path, 'w', encoding='utf-8') as f:
        json.dump(report, f, ensure_ascii=False, indent=2)
    
    print(f"\nAI分析报告已保存到: {report_path}")
    
    # 根据结果返回适当的退出码
    if report['status'] == 'failed':
        sys.exit(1)
    else:
        sys.exit(0)

if __name__ == "__main__":
    main()
