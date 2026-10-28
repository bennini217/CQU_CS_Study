import serial
import threading

# 配置串口信息，确保 COM 口号和 Arduino 显示的一致 [cite: 287-289]
ser = serial.Serial("COM7", 9600, timeout=0.5) 

def loopCheckTask():
    while True:
        if ser.in_waiting > 0:
            # 读取下位机发来的信息并解码 [cite: 316-317]
            data = ser.readline().decode().rstrip()
            
            if data == 'LOW':          # 接收到障碍信号 [cite: 319]
                print("检测到障碍：发送 12 (绿灯亮)")
                ser.write("12".encode()) # 发送编码指令给下位机 [cite: 324]
            elif data == 'HIGH':       # 障碍消失信号 [cite: 322]
                print("障碍解除：发送 11 (红灯亮)")
                ser.write("11".encode()) # 发送编码指令给下位机 [cite: 321]

if __name__ == '__main__':
    # 开启线程持续监听串口 [cite: 300-301]
    thread = threading.Thread(target=loopCheckTask)
    thread.start()

