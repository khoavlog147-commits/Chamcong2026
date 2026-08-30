package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_config")
data class UserConfig(
    @PrimaryKey val userId: String,
    val luongCoBan: Double = 0.0, // Base Salary (LCB) in VND
    val luongDongBaoHiem: Double = 0.0, // Insurance Salary (LBH) in VND
    val tiLeDongBaoHiem: Double = 10.5, // Social insurance rate (%)
    val ngayChotLuong: Int = 1, // Salary cutoff day of month
    val doanPhiCongDoan: Double = 0.0, // Union fee in VND
    
    // OT multiplication coefficients
    val heSoOtNgayThuong: Double = 1.5,
    val heSoOtChuNhat: Double = 2.0,
    val heSoOtNgayLe: Double = 3.0,
    
    val tienChuyenCanGoc: Double = 0.0, // Chuyên cần gốc
    val soNgayPhepNam: Int = 0, // Annual leave quota
    val phepNamConLai: Int = 0, // Remaining annual leave days
    val lastAccumulatedMonth: String = "", // Last month with automatic +1 increment (format: yyyy-MM)
    
    // 12 Flat Allowances (Các khoản phụ cấp)
    val pcKyThuat: Double = 0.0,
    val pcTrachNhiem: Double = 0.0,
    val pcChucVu: Double = 0.0,
    val pcHieuSuat: Double = 0.0,
    val pcSanPham: Double = 0.0,
    val pcComCa: Double = 0.0, // Flat meal allowance (per shift rate)
    val pcComOt: Double = 0.0, // Extra overtime meal (per shift rate for OT >= 10h)
    val pcNhaO: Double = 0.0,
    val pcDocHai: Double = 0.0,
    val pcDtDoanhThu: Double = 0.0,
    val pcXangXe: Double = 0.0,
    val pcThamNien: Double = 0.0, // Thâm niên
    val pcKhac1: Double = 0.0,
    val pcCaDem: Double = 0.0,
    val allowanceCalcTypes: String = "", // Stores calc type overrides as "field1:type1;field2:type2..."
    val soGioNghiGiaiLao: Double = 1.5, // Break hours (unpaid hours) per shift
    val tinhKhauTruNghi: Boolean = false, // Enable break time deduction from hours worked
    
    // Editable User Profile details (so that they are saved & synced on SQLite/Cloud)
    val hoVaTen: String = "User Demo",
    val maNhanVien: String = "demo_9bcad9a7",
    val emailDangKy: String = "",
    val soDienThoai: String = "",
    val boPhan: String = "",
    val lichTrinh: String = "07:30 - 19:30",
    val ngayVaoLam: String = "", // Ngày vào làm/bắt đầu tính công (yyyy-MM-dd)
    
    // Multi-tenancy / Company configuration
    val companyId: String = "default_company",
    val companyName: String = "Công ty Mặc Định",
    val companyCode: String = "DEFAULT",
    val roleId: String = "",
    val roleName: String = "",
    
    // Retrocompatibility keys (to prevent DB compilation errors)
    val tienComMoiNgay: Double = 50000.0,
    val phuCap: Double = 1000000.0,
    val phuCapXangXe: Double = 500000.0,
    val phuCapDienThoai: Double = 300000.0,
    val phuCapNhaO: Double = 1000000.0,
    val phuCapChuyenCan: Double = 500000.0,
    val thuong: Double = 0.0,
    val heSoOtDem: Double = 1.75,
    val caDemStart: String = "22:00",
    val caDemEnd: String = "06:00",
    val isAdmin: Boolean = false
) {
    fun getCalcTypeFor(field: String): String {
        if (allowanceCalcTypes.isBlank()) {
            return getDefaultCalcType(field)
        }
        val map = allowanceCalcTypes.split(";").filter { it.contains(":") }.associate {
            val parts = it.split(":")
            parts[0] to parts[1]
        }
        return map[field] ?: getDefaultCalcType(field)
    }

    fun copyWithCalcType(field: String, calcType: String): UserConfig {
        val map = allowanceCalcTypes.split(";").filter { it.contains(":") }.associate {
            val parts = it.split(":")
            parts[0] to parts[1]
        }.toMutableMap()
        map[field] = calcType
        val newStr = map.entries.joinToString(";") { "${it.key}:${it.value}" }
        return this.copy(allowanceCalcTypes = newStr)
    }

    companion object {
        fun getDefaultCalcType(field: String): String {
            return when (field) {
                "pcComCa" -> "PER_WORK_DAY"
                "pcComOt" -> "OT_MEAL_GE_1H"
                "pcCaDem" -> "PER_NIGHT_SHIFT" // Phụ cấp ca đêm
                "pcThamNien" -> "MONTHLY_FLAT"
                "tienChuyenCanGoc" -> "MONTHLY_PRO_RATED"
                else -> "MONTHLY_PRO_RATED"
            }
        }
    }
}
