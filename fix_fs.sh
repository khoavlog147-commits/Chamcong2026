sed -i 's/roleId = getString("roleId") ?: "",//g' app/src/main/java/com/example/data/FirestoreService.kt
sed -i 's/roleName = getString("roleName") ?: "",//g' app/src/main/java/com/example/data/FirestoreService.kt

sed -i '/fun DocumentSnapshot.toUserConfig(): UserConfig {/,/val id = getString("userId") ?: this.id/!b;//!d'
# Let's just use Python or patch
