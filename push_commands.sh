#!/bin/bash

# Motion Photo Converter - GitHub 推送命令
# 项目：https://github.com/850912/Motion-Photo-

echo "==================================="
echo "Motion Photo Converter"
echo "GitHub 推送准备"
echo "==================================="
echo ""

# 显示当前状态
echo "📊 当前 Git 状态："
git status
echo ""

# 显示最新提交
echo "📝 最新提交："
git log --oneline -1
echo ""

# 显示远程仓库
echo "🌐 远程仓库："
git remote -v
echo ""

echo "==================================="
echo "准备推送到 GitHub"
echo "==================================="
echo ""
echo "执行以下命令推送："
echo "  git push -u origin main"
echo ""
echo "⚠️  注意："
echo "1. 如果是第一次推送，需要输入 GitHub 凭据"
echo "2. 密码处请使用 Personal Access Token"
echo "3. Token 创建地址：https://github.com/settings/tokens"
echo ""
echo "推送命令："
echo ""
echo "git push -u origin main"
echo ""
