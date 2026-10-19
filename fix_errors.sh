sed -i '1221d' app/src/main/java/com/example/data/FirestoreService.kt
sed -i '/import com.example.data.model.UserConfig/a import com.example.data.model.RoleConfig\nimport com.example.data.model.getRoles\nimport com.example.data.model.updateRoles' app/src/main/java/com/example/ui/screens/AdminScreen.kt
