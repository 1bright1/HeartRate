import socket
import time

def send_ecg_data(client_socket):
    with open('ECG_data_set.csv', 'r') as file:
        for line in file:
            print(f"Sending: {line.strip()}")
            client_socket.sendall(line.encode())
            time.sleep(0.05)  # Simulate real-time data sending

HOST = '10.219.21.230'  # Standard loopback interface address (localhost)
PORT = 65432      # Port to listen on (non-privileged ports are > 1023)

with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
    s.bind((HOST, PORT))
    s.listen()
    print(f"Server listening on {PORT}")
    conn, addr = s.accept()
    with conn:
        print('Connected by', addr)
        send_ecg_data(conn)

