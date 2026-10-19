                if (finalOtHours > 0.0) {
                    if (e.dayType == "HOLIDAY") {
                        totalOtLeHours += finalOtHours
                        otLePay += finalOtHours * (hourlySalary * config.heSoOtNgayLe)
                    } else {
                        totalOtDayHours += finalOtHours
                        otDayPay += finalOtHours * (hourlySalary * config.heSoOtNgayThuong)
                    }
                }
            }
        }
