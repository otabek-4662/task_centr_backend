import requests

BASE_URL = "https://task-centr-backend.onrender.com/api"
import sys

def main():
    # 1. Register a new user
    print("Registering new user...")
    register_data = {
        "name": "bot_tester_106",
        "password": "Password123!"
    }
    r1 = requests.post(f"{BASE_URL}/auth/register", json=register_data)
    print("Register response:", r1.status_code, r1.text)

    # 2. Login to get token
    print("\nLogging in...")
    login_data = {
        "name": "bot_tester_106",
        "password": "Password123!"
    }
    r2 = requests.post(f"{BASE_URL}/auth/login", json=login_data)
    print("Login response:", r2.status_code, r2.text)
    
    if r2.status_code != 200:
        print("Login failed!")
        return

    # Extract token
    res_json = r2.json()
    # ApiResponse format -> res_json["data"]["accessToken"]
    token = res_json.get("data", {}).get("accessToken")
    
    if not token:
        print("Failed to parse token!")
        return

    headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json"
    }

    # 3. Create a Workspace
    print("\nCreating workspace...")
    ws_data = {
        "title": "Avtomatik Sinov Loyihasi",
        "description": "Takliflarni tekshirish uchun",
        "bgColor": "#1e1e1e"
    }
    r3 = requests.post(f"{BASE_URL}/workspaces", json=ws_data, headers=headers)
    print("Workspace response:", r3.status_code, r3.text)
    
    if r3.status_code != 201 and r3.status_code != 200:
        print("Failed to create workspace!")
        return
        
    workspace_id = r3.json().get("data", {}).get("id")

    # 4. Invite user
    print(f"\nInviting user to workspace {workspace_id}...")
    invite_data = {
        "usernameOrEmail": "otabeksotimov9@gmail.com",
        "role": "MEMBER"
    }
    r4 = requests.post(f"{BASE_URL}/workspaces/{workspace_id}/invites", json=invite_data, headers=headers)
    print("Invite response:", r4.status_code, r4.text)

    print("\nALL DONE!")

if __name__ == "__main__":
    main()
