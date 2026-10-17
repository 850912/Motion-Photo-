# 推送到 GitHub 指南

## 📦 项目已准备就绪

Git 仓库已初始化并提交完成：
```
项目文件将在安全检查完成后提交
✅ 远程仓库已配置：https://github.com/850912/Motion-Photo-.git
✅ 分支：main
```

## 🚀 推送方法

### 方法 1：在电脑上推送（推荐）

1. **将项目文件夹复制到电脑**
   - 通过 USB 连接手机和电脑
   - 或使用云盘同步（如 OneDrive、Google Drive）
   - 项目位置：`/storage/emulated/0/Android/data/com.androidharness.app/files/workspace`

2. **在电脑上打开终端，进入项目目录**
   ```bash
   cd /path/to/Motion-Photo-Converter
   ```

3. **推送到 GitHub**
   ```bash
   git push -u origin main
   ```

4. **输入 GitHub 凭据**
   - 用户名：你的 GitHub 用户名
   - 密码：使用 Personal Access Token（不是密码）

### 方法 2：在手机浏览器中推送

1. **生成 GitHub Personal Access Token**
   - 在手机浏览器打开：https://github.com/settings/tokens
   - 点击 "Generate new token (classic)"
   - 勾选权限：`repo`（完整仓库访问权限）
   - 点击 "Generate token"
   - **立即复制 token**（只显示一次）

2. **在 AndroidHarness 中配置 token**
   - 打开 AndroidHarness 设置
   - 进入 GitHub 设置
   - 粘贴你的 Personal Access Token

3. **然后在 AndroidHarness 终端执行**
   ```bash
   git push -u origin main
   ```

### 方法 3：使用 GitHub CLI（需要安装 gh）

```bash
# 如果已安装 gh
gh auth login
git push -u origin main
```

## 📝 推送命令清单

```bash
# 1. 确认当前状态
git status
git log --oneline -1

# 2. 推送到 GitHub
git push -u origin main

# 3. 验证推送成功
git remote show origin
```

## 🔐 创建 Personal Access Token 步骤

1. 访问 GitHub Settings → Developer settings → Personal access tokens
2. 点击 "Generate new token (classic)"
3. Token 名称：`Motion-Photo-Converter-Token`
4. 过期时间：选择你需要的时长（建议 90 天）
5. 权限勾选：
   - ✅ repo（完整仓库控制）
   - ✅ workflow（如果需要 GitHub Actions）
6. 点击 "Generate token"
7. **立即复制并保存 token**（只显示一次）

## 🎯 推送后的验证

推送成功后，访问你的仓库：
https://github.com/850912/Motion-Photo-

你应该能看到：
- ✅ 项目源码和 GitHub Actions 构建配置
- ✅ README.md 显示项目说明
- ✅ .github/workflows/android-build.yml（自动构建配置）
- ✅ 完整的源代码

## 🤖 GitHub Actions 自动构建

推送后，GitHub Actions 会自动触发构建：

1. 访问：https://github.com/850912/Motion-Photo-/actions
2. 点击最新的 workflow run
3. 等待构建完成（约 5-10 分钟）
4. 下载构建产物：
   - 点击 "Artifacts"
   - 下载 `app-debug.zip`
   - 解压后得到 `app-debug.apk`

## 💡 提示

- 如果推送失败显示 "403 Forbidden"，说明认证有问题，检查 token 权限
- 如果显示 "remote: Repository not found"，确认仓库 URL 是否正确
- Personal Access Token 像密码一样重要，不要分享给他人
- Token 可以随时在 GitHub Settings 中撤销和重新生成

## 🆘 常见问题

### Q: 推送时提示 "non-fast-forward"
```bash
# 如果远程仓库有其他提交，先拉取：
git pull origin main --rebase
git push -u origin main
```

### Q: 忘记保存 Personal Access Token
访问 GitHub Settings → Developer settings，重新生成一个新 token

### Q: 想要强制推送（谨慎使用）
```bash
git push -u origin main --force
# ⚠️ 警告：这会覆盖远程仓库的历史
```

---

**需要帮助？**
如果遇到问题，可以：
1. 检查 GitHub token 权限
2. 确认网络连接
3. 查看错误信息并搜索解决方案
