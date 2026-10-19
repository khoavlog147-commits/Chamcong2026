package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_config")
data class UserConfig(
    @PrimaryKey val uid: String,
    val fullName: String,
    val hourlyRate: Double = 50000.0, // default hourly wage in VND
    val currency: String = "đ",
    val dailyTargetHours: Double = 8.0,
    val he_so_ot_dem: Double = 1.75,
    val thoi_gian_ca_dem: String = "22:00",
    
    // Core parameters
    val luong_co_ban: Double = 6000000.0,
    val luong_bao_hiem: Double = 5000000.0,
    val tile_bao_hiem: Double = 10.5,
    val doan_phi_40k: Double = 40000.0,
    val ngay_chot_luong: Int = 25,
    
    // OT parameters
    val he_so_ot_normal: Double = 1.5,
    val he_so_ot_sunday: Double = 2.0,
    val he_so_ot_holiday: Double = 3.0,
    
    // Active parameters
    val tien_chuyen_can: Double = 500000.0,
    val so_ngay_nghi_phep: Int = 12,
    
    // The 12 Allowances
    val phu_cap_ky_thuat: Double = 0.0,
    val phu_cap_trach_nhiem: Double = 0.0,
    val phu_cap_chuc_vu: Double = 0.0,
    val phu_cap_hieu_suat: Double = 0.0,
    val phu_cap_san_pham: Double = 0.0,
    val phu_cap_com_ca: Double = 30000.0,
    val phu_cap_com_ot: Double = 15000.0,
    val phu_cap_tham_nien: Double = 0.0,
    val phu_cap_nha_o: Double = 0.0,
    val phu_cap_doc_hai: Double = 0.0,
    val phu_cap_dien_thoai: Double = 0.0,
    val phu_cap_xang_xe: Double = 0.0,
    val phu_cap_khac: Double = 0.0,

    // Backward compatibility helpers
    val phu_cap: Double = 500000.0,
    val thuong: Double = 0.0,
    val tien_khau_tru_nghi: Double = 0.0,
    val tong_tien_com: Double = 600000.0,
    
    val last_accumulated_month: Int = -1
)
