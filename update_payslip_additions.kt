                        // 1. Lương cơ bản
                        if (selectedTab == 1) {
                            PayslipMoneyRow(label = "Lương Cơ Bản Thỏa Thuận", value = c.luongCoBan, isAddition = true)
                            if (s.standardWorkDays == 27) {
                                PayslipMoneyRow(label = "Bù công dôi dư tháng 31 ngày (1 ngày LCB)", value = dailySalary, isAddition = true)
                            }
                        } else {
                            PayslipMoneyRow(label = "Lương Cơ Bản Tạm Tính", value = s.baseBasicSalary, isAddition = true)
                            if (s.standardWorkDays == 27) {
                                PayslipMoneyRow(label = "Bù công dôi dư tháng 31 ngày (1 ngày LCB)", value = dailySalary, isAddition = true)
                            }
                        }
                        
                        // 2. Chuyên cần
                        if (pcChuyenCanShow > 0.0) {
                            PayslipMoneyRow(label = "Chuyên cần", value = pcChuyenCanShow, isAddition = true)
                        }

                        // 3. Trách nhiệm
                        if (c.pcTrachNhiem > 0.0) {
                            PayslipMoneyRow(label = "Trách nhiệm", value = pcTrachNhiemShow, isAddition = true)
                        }

                        // 4. Kỹ thuật
                        if (c.pcKyThuat > 0.0) {
                            PayslipMoneyRow(label = "Kỹ thuật", value = pcKyThuatShow, isAddition = true)
                        }

                        // 5. Hiệu suất
                        if (c.pcHieuSuat > 0.0) {
                            PayslipMoneyRow(label = "Hiệu suất", value = pcHieuSuatShow, isAddition = true)
                        }

                        // 6. Sản phẩm
                        if (c.pcSanPham > 0.0) {
                            PayslipMoneyRow(label = "Sản phẩm", value = pcSanPhamShow, isAddition = true)
                        }

                        // 7. Chức vụ
                        if (c.pcChucVu > 0.0) {
                            PayslipMoneyRow(label = "Chức vụ", value = pcChucVuShow, isAddition = true)
                        }

                        // 8. Độc hại
                        if (c.pcDocHai > 0.0) {
                            PayslipMoneyRow(label = "Độc hại", value = pcDocHaiShow, isAddition = true)
                        }

                        // 9. Doanh thu
                        if (c.pcDtDoanhThu > 0.0) {
                            PayslipMoneyRow(label = "Doanh thu", value = pcDtDoanhThuShow, isAddition = true)
                        }

                        // 10. Thâm niên
                        if (c.pcThamNien > 0.0) {
                            PayslipMoneyRow(label = "Thâm niên", value = pcThamNienShow, isAddition = true)
                        }

                        // 11. Cơm/ca
                        val finalPcComCa = if (selectedTab == 1) (s.pcComCaVal + (remainingWeekdays * c.pcComCa)) else s.pcComCaVal
                        if (finalPcComCa > 0.0) {
                            PayslipMoneyRow(label = "Cơm/ ca", value = finalPcComCa, isAddition = true)
                        }

                        // 12. Cơm OT
                        val finalPcComOt = if (selectedTab == 1) (pcComOtShow + otMealAllowance) else pcComOtShow
                        if (finalPcComOt > 0.0) {
                            PayslipMoneyRow(label = "Cơm OT", value = finalPcComOt, isAddition = true)
                        }

                        // 13. OT 1.5
                        if (s.tienOtNgay > 0.0) {
                            PayslipMoneyRow(label = "OT 1.5 (${df.format(s.otDayHours)}h)", value = s.tienOtNgay, isAddition = true, isAccent = true)
                        }
                        if (selectedTab == 1 && customOt15DaysCount > 0.0) {
                            PayslipMoneyRow(label = "OT 1.5 (${df.format(customOt15DaysCount)} ngày)", value = customOt15Pay, isAddition = true, isAccent = true)
                        }

                        // 14. OT 2.0
                        if (s.tienChuNhat > 0.0) {
                            PayslipMoneyRow(label = "OT 2.0 (${df.format(s.chuNhatHours)}h)", value = s.tienChuNhat, isAddition = true, isAccent = true)
                        }
                        if (selectedTab == 1 && isCurrentSelectedMonth && includeSundayInProjection && remainingSundays > 0) {
                            PayslipMoneyRow(label = "OT 2.0 ($remainingSundays)", value = additionalSundaysPay, isAddition = true, isAccent = true)
                        }

                        // 16. Phụ cấp đêm
                        val finalPcCaDemCount = if (selectedTab == 1 && selectedOt15Shift == "Đêm") s.caDemCount + customOt15DaysCount.toInt() else s.caDemCount
                        val finalPcCaDem = if (selectedTab == 1) (s.pcCaDemVal + customNightAllowance) else s.pcCaDemVal
                        if (finalPcCaDem > 0.0) {
                            PayslipMoneyRow(label = "Phụ cấp đêm ($finalPcCaDemCount)", value = finalPcCaDem, isAddition = true)
                        }

                        // 17. Xăng xe
                        if (c.pcXangXe > 0.0) {
                            PayslipMoneyRow(label = "Xăng xe", value = pcXangXeShow, isAddition = true)
                        }

                        // 18. Nhà ở
                        if (c.pcNhaO > 0.0) {
                            PayslipMoneyRow(label = "Nhà ở", value = pcNhaOShow, isAddition = true)
                        }

                        // 19. Khác
                        if (c.pcKhac > 0.0) {
                            PayslipMoneyRow(label = "Khác", value = pcKhacShow, isAddition = true)
                        }

                        // 20. Khác 1
                        if (c.pcKhac1 > 0.0) {
                            PayslipMoneyRow(label = "Khác 1", value = pcKhac1Show, isAddition = true)
                        }
