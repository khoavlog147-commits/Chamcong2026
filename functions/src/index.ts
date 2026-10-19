import { onDocumentWritten } from "firebase-functions/v2/firestore";
import * as admin from "firebase-admin";

admin.initializeApp();

/**
 * Helper to parse Firestore Timestamp, numbers, or strings to a valid JavaScript Date.
 */
function parseToDate(val: any): Date | null {
  if (!val) return null;
  // Firestore Timestamp
  if (typeof val.toDate === "function") {
    return val.toDate();
  }
  // Epoch milliseconds
  if (typeof val === "number") {
    return new Date(val);
  }
  // String format
  if (typeof val === "string") {
    if (/^\d+$/.test(val)) {
      return new Date(parseInt(val, 10));
    }
    return new Date(val);
  }
  return null;
}

/**
 * Normalizes date strings (e.g. "dd/MM/yyyy" -> "yyyy-MM-dd") for database consistency.
 */
function formatToDocId(dateStr: string): string {
  if (!dateStr) return "";
  const parts = dateStr.split("/");
  if (parts.length === 3) {
    const dd = parts[0].padStart(2, "0");
    const mm = parts[1].padStart(2, "0");
    const yyyy = parts[2];
    return `${yyyy}-${mm}-${dd}`;
  }
  return dateStr.replace(/\//g, "-");
}

export const onAttendanceLogWritten = onDocumentWritten(
  "users/{uid}/attendance_logs/{logId}",
  async (event) => {
    const { uid, logId } = event.params;
    const snapshot = event.data?.after;

    // If the document is deleted, clean up or ignore
    if (!snapshot || !snapshot.exists) {
      const db = admin.firestore();
      const standardDocId = formatToDocId(logId);
      if (standardDocId) {
        await db
          .collection("users")
          .doc(uid)
          .collection("time_entries")
          .doc(standardDocId)
          .delete();
      }
      return;
    }

    const data = snapshot.data();
    if (!data) return;

    const clockInTimeVal = data.clockInTime;
    const clockOutTimeVal = data.clockOutTime;

    // 1. LẮNG NGHE: Nếu clockOutTime là null -> Bỏ qua
    if (!clockOutTimeVal) {
      return;
    }

    const clockInDate = parseToDate(clockInTimeVal);
    const clockOutDate = parseToDate(clockOutTimeVal);

    if (!clockInDate || !clockOutDate) {
      return;
    }

    // 2. LOGIC TÍNH TOÁN:
    // Mốc giờ chuẩn: 8:00 - 17:00, nghỉ trưa 12:00 - 13:00
    const year = clockInDate.getFullYear();
    const month = clockInDate.getMonth();
    const day = clockInDate.getDate();

    const stdStart = new Date(year, month, day, 8, 0, 0, 0);
    const stdEnd = new Date(year, month, day, 17, 0, 0, 0);
    const lunchStart = new Date(year, month, day, 12, 0, 0, 0);
    const lunchEnd = new Date(year, month, day, 13, 0, 0, 0);

    // lateMinutes: Phút đi trễ (sau 8:00)
    let lateMinutes = 0;
    if (clockInDate.getTime() > stdStart.getTime()) {
      lateMinutes = Math.max(0, Math.floor((clockInDate.getTime() - stdStart.getTime()) / 60000));
    }

    // earlyLeaveMinutes: Phút về sớm (trước 17:00)
    let earlyLeaveMinutes = 0;
    if (clockOutDate.getTime() < stdEnd.getTime()) {
      earlyLeaveMinutes = Math.max(0, Math.floor((stdEnd.getTime() - clockOutDate.getTime()) / 60000));
    }

    // Tính thời gian trùng với khoảng nghỉ trưa (12:00 - 13:00) để tự động trừ nghỉ trưa
    const overlapStart = Math.max(clockInDate.getTime(), lunchStart.getTime());
    const overlapEnd = Math.min(clockOutDate.getTime(), lunchEnd.getTime());
    const overlapMs = Math.max(0, overlapEnd - overlapStart);

    // totalHours: Tổng giờ làm thực tế (trừ 1h nghỉ trưa nếu đi qua ca)
    const totalMs = clockOutDate.getTime() - clockInDate.getTime();
    const actualMs = totalMs - overlapMs;
    const totalHours = Math.max(0, actualMs / 3600000.0);

    // workDay: >= 8h là 1.0 công; từ 4h đến dưới 8h là 0.5 công; dưới 4h là 0 công
    let workDay = 0.0;
    if (totalHours >= 8.0) {
      workDay = 1.0;
    } else if (totalHours >= 4.0) {
      workDay = 0.5;
    } else {
      workDay = 0.0;
    }

    // otHours: Tính phần vượt quá 8 tiếng với hệ số OT 1.5
    let otHours = 0.0;
    if (totalHours > 8.0) {
      otHours = (totalHours - 8.0) * 1.5;
    }

    // Làm tròn giá trị số cho gọn gàng
    const roundedTotalHours = Math.round(totalHours * 100) / 100;
    const roundedOtHours = Math.round(otHours * 100) / 100;

    // Chuẩn hóa dateString để lưu trữ theo ID
    const rawDateString = data.dateString || logId;
    const standardDocId = formatToDocId(rawDateString);

    if (!standardDocId) return;

    // 3. GHI DỮ LIỆU: Lưu kết quả vào path `/users/{uid}/time_entries/{dateString}`
    const db = admin.firestore();
    const timeEntryRef = db
      .collection("users")
      .doc(uid)
      .collection("time_entries")
      .doc(standardDocId);

    const payload = {
      userId: uid,
      date: standardDocId, // định dạng "yyyy-MM-dd"
      checkInTime: clockInTimeVal,
      checkOutTime: clockOutTimeVal,
      isWorking: false,
      dayType: data.status === "LEAVE" ? "PAID_LEAVE" : "NORMAL",
      isHourlyCalculated: true,
      note: data.notes || null,
      shiftId: "ca1",
      shiftType: "DAY",
      rawCheckIn: clockInTimeVal,
      rawCheckOut: clockOutTimeVal,
      normalizedCheckIn: clockInTimeVal,
      normalizedCheckOut: clockOutTimeVal,
      workDay: workDay,
      otHours: roundedOtHours,
      lateMinutes: lateMinutes,
      earlyLeaveMinutes: earlyLeaveMinutes,
      totalHours: roundedTotalHours,
      customBreakDeduction: overlapMs > 0,
      customBreakHours: Math.round((overlapMs / 3600000.0) * 100) / 100,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    };

    await timeEntryRef.set(payload, { merge: true });
  }
);
