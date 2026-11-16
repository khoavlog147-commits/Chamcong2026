#!/bin/bash
set -e
# Script hỗ trợ khởi tạo git và chuẩn bị push lên GitHub

echo "=== Khởi tạo Git repository ==="
if [ ! -d ".git" ]; then
  git init -b main 2>/dev/null || git init
fi
git branch -M main

echo "=== Cấu hình user git (nếu chưa có) ==="
if [ -z "$(git config user.name || true)" ]; then
  git config --local user.name "User"
fi
if [ -z "$(git config user.email || true)" ]; then
  git config --local user.email "user@example.com"
fi

echo "=== Thêm tất cả các file vào git (loại bỏ build/apk qua .gitignore) ==="
git add -A

echo "=== Tạo commit ==="
if ! git diff --cached --quiet; then
  git commit -m "Update TimeSnap App"
else
  echo "Không có thay đổi mới để commit."
fi

if git remote get-url origin >/dev/null 2>&1; then
  echo "=== Đang push lên origin/main ==="
  git push -u origin main
else
  echo "=== ĐÃ HOÀN TẤT CHUẨN BỊ LOCAL ==="
  echo ""
  echo "Để push lên GitHub và build + nén APK tải về:"
  echo "1. Tạo một repository mới trên GitHub (trống, không tích chọn Add README)."
  echo "2. Chạy lệnh sau (thay thế URL bằng link repository của bạn):"
  echo "   git remote add origin https://github.com/USERNAME/REPOSITORY_NAME.git"
  echo "   git push -u origin main"
fi

