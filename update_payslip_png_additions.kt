    // 1. Lương cơ bản
    if (selectedTab == 1) {
        drawRow("Lương Cơ Bản Thỏa Thuận", "+${fmt.format(config.luongCoBan)}đ", isGreenVal = true)
        if (summary.standardWorkDays == 27) {
            drawRow("Bù công dôi dư tháng 31 ngày (1 ngày LCB)", "+${fmt.format(dailySalary)}đ", isGreenVal = true)
        }
    } else {
        drawRow("Lương Cơ Bản Tạm Tính", "+${fmt.format(summary.baseBasicSalary)}đ", isGreenVal = true)
        if (summary.standardWorkDays == 27) {
            drawRow("Bù công dôi dư tháng 31 ngày (1 ngày LCB)", "+${fmt.format(dailySalary)}đ", isGreenVal = true)
        }
    }
    
    // 2. Chuyên cần
    if (pcChuyenCanShowPNG > 0.0) {
        drawRow("Chuyên cần", "+${fmt.format(pcChuyenCanShowPNG)}đ", isGreenVal = true)
    }

    // 3. Trách nhiệm
    if (config.pcTrachNhiem > 0.0) {
        drawRow("Trách nhiệm", "+${fmt.format(pcTrachNhiemShowPNG)}đ", isGreenVal = true)
    }

    // 4. Kỹ thuật
    if (config.pcKyThuat > 0.0) {
        drawRow("Kỹ thuật", "+${fmt.format(pcKyThuatShowPNG)}đ", isGreenVal = true)
    }

    // 5. Hiệu suất
    if (config.pcHieuSuat > 0.0) {
        drawRow("Hiệu suất", "+${fmt.format(pcHieuSuatShowPNG)}đ", isGreenVal = true)
    }

    // 6. Sản phẩm
    if (config.pcSanPham > 0.0) {
        drawRow("Sản phẩm", "+${fmt.format(pcSanPhamShowPNG)}đ", isGreenVal = true)
    }

    // 7. Chức vụ
    if (config.pcChucVu > 0.0) {
        drawRow("Chức vụ", "+${fmt.format(pcChucVuShowPNG)}đ", isGreenVal = true)
    }

    // 8. Độc hại
    if (config.pcDocHai > 0.0) {
        drawRow("Độc hại", "+${fmt.format(pcDocHaiShowPNG)}đ", isGreenVal = true)
    }

    // 9. Doanh thu
    if (config.pcDtDoanhThu > 0.0) {
        drawRow("Doanh thu", "+${fmt.format(pcDtDoanhThuShowPNG)}đ", isGreenVal = true)
    }

    // 10. Thâm niên
    if (config.pcThamNien > 0.0) {
        drawRow("Thâm niên", "+${fmt.format(pcThamNienShowPNG)}đ", isGreenVal = true)
    }

    // 11. Cơm/ca
    if (pcComCaShowPNG > 0.0) {
        drawRow("Cơm/ ca", "+${fmt.format(pcComCaShowPNG)}đ", isGreenVal = true)
    }

    // 12. Cơm OT
    if (pcComOtShowPNG > 0.0) {
        drawRow("Cơm OT", "+${fmt.format(pcComOtShowPNG)}đ", isGreenVal = true)
    }

    // 13. OT 1.5
    if (summary.tienOtNgay > 0.0) {
        drawRow("OT 1.5 (${df.format(summary.otDayHours)}h)", "+${fmt.format(summary.tienOtNgay)}đ", isGreenVal = true)
    }
    if (selectedTab == 1 && customOt15DaysCount > 0.0) {
        drawRow("OT 1.5 (${df.format(customOt15DaysCount)} ngày)", "+${fmt.format(customOt15Pay)}đ", isGreenVal = true)
    }

    // 14. OT 2.0
    if (summary.tienChuNhat > 0.0) {
        drawRow("OT 2.0 (${df.format(summary.chuNhatHours)}h)", "+${fmt.format(summary.tienChuNhat)}đ", isGreenVal = true)
    }
    if (selectedTab == 1 && includeSundayInProjection && remainingSundays > 0) {
        drawRow("OT 2.0 ($remainingSundays)", "+${fmt.format(remainingSundays * dailySalary * config.heSoOtChuNhat)}đ", isGreenVal = true)
    }

    // 16. Phụ cấp đêm
    val finalPcCaDemCountPNG = if (selectedTab == 1 && selectedOt15Shift == "Đêm") summary.caDemCount + customOt15DaysCount.toInt() else summary.caDemCount
    val finalPcCaDemPNG = if (selectedTab == 1) (summary.pcCaDemVal + customNightAllowance) else summary.pcCaDemVal
    if (finalPcCaDemPNG > 0.0) {
        drawRow("Phụ cấp đêm ($finalPcCaDemCountPNG)", "+${fmt.format(finalPcCaDemPNG)}đ", isGreenVal = true)
    }

    // 17. Xăng xe
    if (config.pcXangXe > 0.0) {
        drawRow("Xăng xe", "+${fmt.format(pcXangXeShowPNG)}đ", isGreenVal = true)
    }

    // 18. Nhà ở
    if (config.pcNhaO > 0.0) {
        drawRow("Nhà ở", "+${fmt.format(pcNhaOShowPNG)}đ", isGreenVal = true)
    }

    // 19. Khác
    if (config.pcKhac > 0.0) {
        drawRow("Khác", "+${fmt.format(pcKhacShowPNG)}đ", isGreenVal = true)
    }

    // 20. Khác 1
    if (config.pcKhac1 > 0.0) {
        drawRow("Khác 1", "+${fmt.format(pcKhac1ShowPNG)}đ", isGreenVal = true)
    }
