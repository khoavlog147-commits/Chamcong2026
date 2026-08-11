import re

with open('app/src/main/java/com/example/ui/screens/AdminScreen.kt', 'r') as f:
    content = f.read()

# Find the start of EmployeeConfigEdit
start_idx = content.find("fun EmployeeConfigEdit(")
if start_idx == -1:
    print("Not found")
    exit(1)

# Find var dept
dept_idx = content.find("var dept by remember", start_idx)

# Find Basic Info section
basic_info_idx = content.find('Text("Thông tin chung",', dept_idx)

insert_code = """
    val companies by adminViewModel.companies.collectAsStateWithLifecycle()
    val myCompany = companies.find { it.companyId == employee.companyId }
    val roles = myCompany?.getRoles() ?: emptyList()
    var selectedRole by remember { mutableStateOf(roles.find { it.roleId == employee.roleId }) }
"""

# Insert inside the function block
content = content[:dept_idx] + insert_code + content[dept_idx:]

with open('app/src/main/java/com/example/ui/screens/AdminScreen.kt', 'w') as f:
    f.write(content)
