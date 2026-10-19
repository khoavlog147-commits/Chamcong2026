import re

with open("app/src/main/java/com/example/data/FirestoreService.kt", "r") as f:
    content = f.read()
    
# look for sendAdminNotification
print("--- sendAdminNotification ---")
send_match = re.search(r"suspend fun sendAdminNotification.*?\{.*?(?=\nsuspend fun)", content, re.DOTALL)
if send_match:
    print(send_match.group(0))

