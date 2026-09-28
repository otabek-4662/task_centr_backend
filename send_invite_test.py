import requests
import json
import time

BASE_URL = "https://task-centr-backend.onrender.com/api"

# 1. Register a new user
print("Registering new user...")
register_data = {
    "name": "Bot Tester",
    "email": "bot_tester_99@test.com",
    "password": "Password123!"
}
r1 = requests.post(f"{BASE_URL}/auth/register", json=register_data)
print("Register response:", r1.status_code, r1.text)

# 2. Login to get token (just in case register doesn't return it directly)
print("Logging in...")
login_data = {
    "usernameOrEmail": "bot_tester_99@test.com",
    "password": "Password123!"
}
r2 = requests.post(f"{BASE_URL}/auth/login", json=login_data)
print("Login response:", r2.status_code, r2.text)
token = r2.json().get("data", {}).get("accessToken")
if not token:
    # If auth format is different, fallback to root
    token = r2.json().get("accessToken")

headers = {
    "Authorization": f"Bearer {token}",
    "Content-Type": "application/json"
}

# 3. Create a Workspace
print("Creating workspace...")
ws_data = {
    "title": "Ajoyib Test Loyiha",
    "description": "Bu loyiha avtomatik test orqali yaratildi",
    "bgColor": "#ff0000"
}
r3 = requests.post(f"{BASE_URL}/workspaces", json=ws_data, headers=headers)
print("Workspace response:", r3.status_code, r3.text)
workspace_id = r3.json().get("data", {}).get("id")

if not workspace_id:
    print("Could not get workspace ID")
    exit(1)

# 4. Invite user
print(f"Inviting user to workspace {workspace_id}...")
invite_data = {
    "usernameOrEmail": "otabeksotimov9@gmail.com",
    "role": "MEMBER"
}
r4 = requests.post(f"{BASE_URL}/workspaces/{workspace_id}/invites", json=invite_data, headers=headers)
print("Invite response:", r4.status_code, r4.text)

print("ALL DONE!")
