int last = HIGH; 
int now = HIGH;

void setup() {
  Serial.begin(9600);           // 开启串口通信 [cite: 182]
  pinMode(10, INPUT);            // 10号引脚连接传感器 [cite: 330]
  pinMode(8, OUTPUT);           // 8号引脚连接红灯 [cite: 265]
  pinMode(13, OUTPUT);          // 13号引脚连接绿灯 [cite: 266]
}

void loop() {
  // --- 部分 A：采集数据并发送给上位机 ---
  now = digitalRead(10);         // 读取传感器信号 [cite: 330]
  if(now != last) {             // 只有状态改变时才发送，减少串口拥堵 [cite: 331]
    if(now == HIGH) {
      Serial.println("HIGH");   // 无障碍物，发送字符串 "HIGH" [cite: 333]
    } else {
      Serial.println("LOW");    // 检测到障碍物，发送字符串 "LOW" [cite: 336]
    }
    last = now;                 // 更新状态 [cite: 339]
  }

  // --- 部分 B：接收并解析上位机的控制指令 ---
  if (Serial.available() > 0) {
    String cmd = Serial.readString(); // 读取上位机传来的编码 [cite: 263]
    
    // 解析编码：第一个字符控制灯，第二个控制颜色 [cite: 277]
    if(cmd[0] == '1' && cmd[1] == '1') { // 收到 "11"
      digitalWrite(8, HIGH);    // 红灯亮 [cite: 265]
      digitalWrite(13, LOW);    // 绿灯灭 [cite: 266]
    }
    if(cmd[0] == '1' && cmd[1] == '2') { // 收到 "12"
      digitalWrite(8, LOW);     // 红灯灭 [cite: 269]
      digitalWrite(13, HIGH);   // 绿灯亮 [cite: 270]
    }
  }
}
