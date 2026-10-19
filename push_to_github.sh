#!/bin/bash
# Script hỗ trợ khởi tạo git và chuẩn bị push lên GitHub

echo "=== Khởi tạo Git repository ==="
git init

echo "=== Cấu hình user git (nếu chưa có) ==="
git config --local user.name "User"
git config --local user.email "user@example.com"

echo "=== Thêm tất cả các file vào git ==="
git add .

echo "=== Tạo commit đầu tiên ==="
git commit -m "Initial commit - TimeSnap App"

echo "=== ĐÃ HOÀN TẤT CHUẨN BỊ LOCAL ==="
echo ""
echo "Để push lên GitHub và build APK (ví dụ bằng GitHub Actions):"
echo "1. Tạo một repository mới trên GitHub (trống, không tích chọn Add README)."
echo "2. Chạy lệnh sau (thay thế URL bằng link repository của bạn):"
echo "   git remote add origin https://github.com/USERNAME/REPOSITORY_NAME.git"
echo "   git branch -M main"
echo "   git push -u origin main"
