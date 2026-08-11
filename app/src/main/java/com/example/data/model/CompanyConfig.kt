package com.example.data.model

data class CompanyConfig(
    val companyId: String = "default_company",
    val companyName: String = "Công ty Mặc Định",
    val companyCode: String = "DEFAULT",
    val description: String = "",
    val address: String = "",
    
    // Core Salary & Allowance Defaults for this Company
    val luongCoBan: Double = 6000000.0,
    val luongDongBaoHiem: Double = 5000000.0,
    val tiLeDongBaoHiem: Double = 10.5,
    val ngayChotLuong: Int = 1,
    val doanPhiCongDoan: Double = 40000.0,
    
    val heSoOtNgayThuong: Double = 1.5,
    val heSoOtChuNhat: Double = 2.0,
    val heSoOtNgayLe: Double = 3.0,
    val heSoOtDem: Double = 1.75,
    val caDemStart: String = "22:00",
    val caDemEnd: String = "06:00",
    
    val tienChuyenCanGoc: Double = 500000.0,
    val soNgayPhepNam: Int = 12,
    
    // 14 Flat Allowances
    val pcKyThuat: Double = 0.0,
    val pcTrachNhiem: Double = 0.0,
    val pcChucVu: Double = 0.0,
    val pcHieuSuat: Double = 0.0,
    val pcSanPham: Double = 0.0,
    val pcComCa: Double = 30000.0,
    val pcComOt: Double = 15000.0,
    val pcNhaO: Double = 0.0,
    val pcDocHai: Double = 0.0,
    val pcDtDoanhThu: Double = 0.0,
    val pcXangXe: Double = 0.0,
    val pcThamNien: Double = 0.0,
    val pcKhac1: Double = 0.0,
    val pcCaDem: Double = 0.0,
    val allowanceCalcTypes: String = "",
    val soGioNghiGiaiLao: Double = 1.5,
    val tinhKhauTruNghi: Boolean = false,
    val lichTrinh: String = "08:00 - 17:00",
    val rolesData: String = "",
    
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun getCalcTypeFor(field: String): String {
        if (allowanceCalcTypes.isBlank()) {
            return UserConfig.getDefaultCalcType(field)
        }
        val map = allowanceCalcTypes.split(";").filter { it.contains(":") }.associate {
            val parts = it.split(":")
            parts[0] to parts[1]
        }
        return map[field] ?: UserConfig.getDefaultCalcType(field)
    }

    fun copyWithCalcType(field: String, calcType: String): CompanyConfig {
        val map = allowanceCalcTypes.split(";").filter { it.contains(":") }.associate {
            val parts = it.split(":")
            parts[0] to parts[1]
        }.toMutableMap()
        map[field] = calcType
        val newStr = map.entries.joinToString(";") { "${it.key}:${it.value}" }
        return this.copy(allowanceCalcTypes = newStr, updatedAt = System.currentTimeMillis())
    }

    fun applyToUserConfig(user: UserConfig, overwriteCustomBaseSalary: Boolean = false): UserConfig {
        val baseConfig = user.copy(
            companyId = this.companyId,
            companyName = this.companyName,
            companyCode = this.companyCode,
            luongCoBan = if (!overwriteCustomBaseSalary && user.luongCoBan > 0.0) user.luongCoBan else this.luongCoBan,
            luongDongBaoHiem = if (!overwriteCustomBaseSalary && user.luongDongBaoHiem > 0.0) user.luongDongBaoHiem else this.luongDongBaoHiem,
            tiLeDongBaoHiem = this.tiLeDongBaoHiem,
            ngayChotLuong = this.ngayChotLuong,
            doanPhiCongDoan = this.doanPhiCongDoan,
            heSoOtNgayThuong = this.heSoOtNgayThuong,
            heSoOtChuNhat = this.heSoOtChuNhat,
            heSoOtNgayLe = this.heSoOtNgayLe,
            heSoOtDem = this.heSoOtDem,
            caDemStart = this.caDemStart,
            caDemEnd = this.caDemEnd,
            tienChuyenCanGoc = this.tienChuyenCanGoc,
            soNgayPhepNam = this.soNgayPhepNam,
            pcKyThuat = this.pcKyThuat,
            pcTrachNhiem = this.pcTrachNhiem,
            pcChucVu = this.pcChucVu,
            pcHieuSuat = this.pcHieuSuat,
            pcSanPham = this.pcSanPham,
            pcComCa = this.pcComCa,
            pcComOt = this.pcComOt,
            pcNhaO = this.pcNhaO,
            pcDocHai = this.pcDocHai,
            pcDtDoanhThu = this.pcDtDoanhThu,
            pcXangXe = this.pcXangXe,
            pcThamNien = this.pcThamNien,
            pcKhac1 = this.pcKhac1,
            pcCaDem = this.pcCaDem,
            allowanceCalcTypes = if (this.allowanceCalcTypes.isNotBlank()) this.allowanceCalcTypes else user.allowanceCalcTypes,
            soGioNghiGiaiLao = this.soGioNghiGiaiLao,
            tinhKhauTruNghi = this.tinhKhauTruNghi,
            lichTrinh = this.lichTrinh
        )

        val role = if (user.roleId.isNotBlank()) getRoles().find { it.roleId == user.roleId } else null
        return if (role != null) {
            baseConfig.copy(
                roleName = role.roleName,
                luongCoBan = if (!overwriteCustomBaseSalary && user.luongCoBan > 0.0) user.luongCoBan else (if (role.luongCoBan > 0.0) role.luongCoBan else baseConfig.luongCoBan),
                pcKyThuat = if (role.pcKyThuat > 0.0) role.pcKyThuat else baseConfig.pcKyThuat,
                pcTrachNhiem = if (role.pcTrachNhiem > 0.0) role.pcTrachNhiem else baseConfig.pcTrachNhiem,
                pcChucVu = if (role.pcChucVu > 0.0) role.pcChucVu else baseConfig.pcChucVu,
                pcHieuSuat = if (role.pcHieuSuat > 0.0) role.pcHieuSuat else baseConfig.pcHieuSuat,
                pcSanPham = if (role.pcSanPham > 0.0) role.pcSanPham else baseConfig.pcSanPham,
                pcComCa = if (role.pcComCa > 0.0) role.pcComCa else baseConfig.pcComCa,
                pcComOt = if (role.pcComOt > 0.0) role.pcComOt else baseConfig.pcComOt,
                pcNhaO = if (role.pcNhaO > 0.0) role.pcNhaO else baseConfig.pcNhaO,
                pcDocHai = if (role.pcDocHai > 0.0) role.pcDocHai else baseConfig.pcDocHai,
                pcDtDoanhThu = if (role.pcDtDoanhThu > 0.0) role.pcDtDoanhThu else baseConfig.pcDtDoanhThu,
                pcXangXe = if (role.pcXangXe > 0.0) role.pcXangXe else baseConfig.pcXangXe,
                pcThamNien = if (role.pcThamNien > 0.0) role.pcThamNien else baseConfig.pcThamNien,
                pcKhac1 = if (role.pcKhac1 > 0.0) role.pcKhac1 else baseConfig.pcKhac1,
                pcCaDem = if (role.pcCaDem > 0.0) role.pcCaDem else baseConfig.pcCaDem,
                tienChuyenCanGoc = if (role.tienChuyenCanGoc > 0.0) role.tienChuyenCanGoc else baseConfig.tienChuyenCanGoc,
                tinhKhauTruNghi = role.tinhKhauTruNghi,
                soGioNghiGiaiLao = role.soGioNghiGiaiLao
            )
        } else {
            baseConfig
        }
    }

    companion object {
        val DEFAULT_COMPANY = CompanyConfig(
            companyId = "default_company",
            companyName = "Công ty Mặc Định",
            companyCode = "DEFAULT",
            description = "Cấu hình chung mặc định cho toàn bộ người dùng",
            luongCoBan = 6000000.0,
            luongDongBaoHiem = 5000000.0,
            tiLeDongBaoHiem = 10.5,
            ngayChotLuong = 1,
            doanPhiCongDoan = 40000.0,
            heSoOtNgayThuong = 1.5,
            heSoOtChuNhat = 2.0,
            heSoOtNgayLe = 3.0,
            heSoOtDem = 1.75,
            tienChuyenCanGoc = 500000.0,
            pcComCa = 30000.0,
            pcComOt = 15000.0
        )
    }
}

data class RoleConfig(
    val roleId: String = java.util.UUID.randomUUID().toString(),
    val roleName: String = "",
    val luongCoBan: Double = 0.0,
    val pcKyThuat: Double = 0.0,
    val pcTrachNhiem: Double = 0.0,
    val pcChucVu: Double = 0.0,
    val pcHieuSuat: Double = 0.0,
    val pcSanPham: Double = 0.0,
    val pcComCa: Double = 0.0,
    val pcComOt: Double = 0.0,
    val pcNhaO: Double = 0.0,
    val pcDocHai: Double = 0.0,
    val pcDtDoanhThu: Double = 0.0,
    val pcXangXe: Double = 0.0,
    val pcThamNien: Double = 0.0,
    val pcKhac1: Double = 0.0,
    val pcCaDem: Double = 0.0,
    val tienChuyenCanGoc: Double = 0.0,
    val allowanceCalcTypes: String = "",
    val tinhKhauTruNghi: Boolean = false,
    val soGioNghiGiaiLao: Double = 1.5
) {
    fun getCalcTypeFor(field: String): String {
        if (allowanceCalcTypes.isBlank()) {
            return UserConfig.getDefaultCalcType(field)
        }
        val map = allowanceCalcTypes.split(";").filter { it.contains(":") }.associate {
            val parts = it.split(":")
            parts[0] to parts[1]
        }
        return map[field] ?: UserConfig.getDefaultCalcType(field)
    }

    fun copyWithCalcType(field: String, calcType: String): RoleConfig {
        val map = allowanceCalcTypes.split(";").filter { it.contains(":") }.associate {
            val parts = it.split(":")
            parts[0] to parts[1]
        }.toMutableMap()
        map[field] = calcType
        val newStr = map.entries.joinToString(";") { "${it.key}:${it.value}" }
        return this.copy(allowanceCalcTypes = newStr)
    }

    fun toCsv(): String {
        return listOf(roleId, roleName, luongCoBan, pcKyThuat, pcTrachNhiem, pcChucVu, pcHieuSuat, pcSanPham, pcComCa, pcComOt, pcNhaO, pcDocHai, pcDtDoanhThu, pcXangXe, pcThamNien, pcKhac1, pcCaDem, tienChuyenCanGoc, allowanceCalcTypes, tinhKhauTruNghi, soGioNghiGiaiLao).joinToString("||")
    }
    companion object {
        fun fromCsv(csv: String): RoleConfig? {
            val parts = csv.split("||")
            if (parts.size < 18) return null
            return try {
                RoleConfig(
                    roleId = parts[0],
                    roleName = parts[1],
                    luongCoBan = parts[2].toDoubleOrNull() ?: 0.0,
                    pcKyThuat = parts[3].toDoubleOrNull() ?: 0.0,
                    pcTrachNhiem = parts[4].toDoubleOrNull() ?: 0.0,
                    pcChucVu = parts[5].toDoubleOrNull() ?: 0.0,
                    pcHieuSuat = parts[6].toDoubleOrNull() ?: 0.0,
                    pcSanPham = parts[7].toDoubleOrNull() ?: 0.0,
                    pcComCa = parts[8].toDoubleOrNull() ?: 0.0,
                    pcComOt = parts[9].toDoubleOrNull() ?: 0.0,
                    pcNhaO = parts[10].toDoubleOrNull() ?: 0.0,
                    pcDocHai = parts[11].toDoubleOrNull() ?: 0.0,
                    pcDtDoanhThu = parts[12].toDoubleOrNull() ?: 0.0,
                    pcXangXe = parts[13].toDoubleOrNull() ?: 0.0,
                    pcThamNien = parts[14].toDoubleOrNull() ?: 0.0,
                    pcKhac1 = parts[15].toDoubleOrNull() ?: 0.0,
                    pcCaDem = parts[16].toDoubleOrNull() ?: 0.0,
                    tienChuyenCanGoc = parts[17].toDoubleOrNull() ?: 0.0,
                    allowanceCalcTypes = if (parts.size > 18) parts[18] else "",
                    tinhKhauTruNghi = if (parts.size > 19) parts[19].toBooleanStrictOrNull() ?: false else false,
                    soGioNghiGiaiLao = if (parts.size > 20) parts[20].toDoubleOrNull() ?: 1.5 else 1.5
                )
            } catch (e: Exception) { null }
        }
    }
}

fun CompanyConfig.getRoles(): List<RoleConfig> {
    if (this.rolesData.isBlank()) return emptyList()
    return this.rolesData.split(";;").mapNotNull { RoleConfig.fromCsv(it) }
}

fun CompanyConfig.updateRoles(roles: List<RoleConfig>): CompanyConfig {
    return this.copy(rolesData = roles.joinToString(";;") { it.toCsv() }, updatedAt = System.currentTimeMillis())
}
