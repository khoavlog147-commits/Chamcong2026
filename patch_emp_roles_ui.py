import re

with open('app/src/main/java/com/example/ui/screens/AdminScreen.kt', 'r') as f:
    content = f.read()

# 1. UI Dropdown
target_ui = """                    Box(modifier = Modifier.weight(1f)) {
                        AdminInputField("Bộ phận", dept, onValueChange = { dept = it })
                    }
                }"""

replacement_ui = """                    Box(modifier = Modifier.weight(1f)) {
                        AdminInputField("Bộ phận", dept, onValueChange = { dept = it })
                    }
                }
                
                // Dropdown for Role
                if (roles.isNotEmpty()) {
                    var roleDropdownExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        OutlinedTextField(
                            value = selectedRole?.roleName ?: "Không chọn (Tùy chỉnh)",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Chức vụ (Tự động điền mức lương)") },
                            modifier = Modifier.fillMaxWidth().clickable { roleDropdownExpanded = true },
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = White,
                                disabledBorderColor = NeonBlue,
                                disabledLabelColor = NeonBlue
                            ),
                            enabled = false
                        )
                        DropdownMenu(
                            expanded = roleDropdownExpanded,
                            onDismissRequest = { roleDropdownExpanded = false },
                            modifier = Modifier.background(DarkContainer)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Không chọn", color = White) },
                                onClick = {
                                    selectedRole = null
                                    roleDropdownExpanded = false
                                }
                            )
                            roles.forEach { r ->
                                DropdownMenuItem(
                                    text = { Text(r.roleName, color = White) },
                                    onClick = {
                                        selectedRole = r
                                        roleDropdownExpanded = false
                                        // Auto-fill values
                                        lcb = formatCurrency(r.luongCoBan)
                                        pcChucVu = formatCurrency(r.pcChucVu)
                                        pcTrachNhiem = formatCurrency(r.pcTrachNhiem)
                                        pcKyThuat = formatCurrency(r.pcKyThuat)
                                        pcKhac1 = formatCurrency(r.pcKhac1)
                                        pcSanPham = formatCurrency(r.pcSanPham)
                                        pcComCa = formatCurrency(r.pcComCa)
                                        pcComOt = formatCurrency(r.pcComOt)
                                        pcNhaO = formatCurrency(r.pcNhaO)
                                        pcDocHai = formatCurrency(r.pcDocHai)
                                        pcDtDoanhThu = formatCurrency(r.pcDtDoanhThu)
                                        pcXangXe = formatCurrency(r.pcXangXe)
                                        pcThamNien = formatCurrency(r.pcThamNien)
                                        pcCaDem = formatCurrency(r.pcCaDem)
                                        chuyenCan = formatCurrency(r.tienChuyenCanGoc)
                                    }
                                )
                            }
                        }
                    }
                }"""

# Insert only inside EmployeeConfigEdit
start_idx = content.find("fun EmployeeConfigEdit(")
end_idx = content.find("fun ConfigSection", start_idx)

sub = content[start_idx:end_idx]
sub = sub.replace(target_ui, replacement_ui)

# 2. Save action
target_save = """                        boPhan = dept,
                        lichTrinh = schedule,"""
replacement_save = """                        boPhan = dept,
                        lichTrinh = schedule,
                        roleId = selectedRole?.roleId ?: "",
                        roleName = selectedRole?.roleName ?: "","""

sub = sub.replace(target_save, replacement_save)

content = content[:start_idx] + sub + content[end_idx:]

with open('app/src/main/java/com/example/ui/screens/AdminScreen.kt', 'w') as f:
    f.write(content)
