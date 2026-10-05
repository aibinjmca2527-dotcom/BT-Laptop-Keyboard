"""
Cross-Platform Companion & Testing Server for BT Laptop Keyboard App
Author: Aibin Joseph
Compatible with: Windows, macOS, Linux
Python 3.7+
"""

import socket
import json
import sys

def main():
    port = 8888
    # Create local UDP/TCP socket
    server = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    server.bind(("", port))
    
    # Get local IP
    hostname = socket.gethostname()
    try:
        local_ip = socket.gethostbyname(hostname)
    except Exception:
        local_ip = "127.0.0.1"

    print("=" * 60)
    print("   BT / Wi-Fi LAPTOP KEYBOARD RECEIVER - TEST SERVER")
    print(f"   Developer: Aibin Joseph")
    print(f"   Listening on IP: {local_ip}  Port: {port}")
    print("   Supported Systems: Windows, macOS, Linux")
    print("=" * 60)
    print("\nReady! Listening for keystroke packets...\n")

    try:
        while True:
            data, addr = server.recvfrom(1024)
            message = data.decode("utf-8", errors="ignore").strip()
            print(f"[Keystroke Received from {addr[0]}]: {message}")
    except KeyboardInterrupt:
        print("\nReceiver stopped by user.")
    finally:
        server.close()

if __name__ == "__main__":
    main()
