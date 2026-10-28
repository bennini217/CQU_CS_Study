// 实验一：练习1 - 传感器控制信号灯 [cite: 248]
int last = 1; // 记录上一次传感器的状态
int now = 1;  // 记录当前传感器的状态

void setup() {
  // 必须要有 setup 函数来做初始化 [cite: 156, 174]
  Serial.begin(9600);           // 开启串口通信，波特率 9600 [cite: 182, 220]
  pinMode(LED_BUILTIN, OUTPUT); // 设置板载 LED（13号引脚）为输出 [cite: 158, 222]
  pinMode(10, INPUT);           // 设置 10 号引脚为传感器输入 [cite: 223]
}

void loop() {
  // 必须要有 loop 函数来处理循环逻辑 [cite: 160, 175]
  now = digitalRead(10); // 读取 10 号引脚的传感器信号 
  
  if(now != last) { // 如果状态发生了变化 [cite: 253]
    if(now == 0) { // 传感器检测到障碍物（通常输出低电平 0） [cite: 254]
      digitalWrite(LED_BUILTIN, HIGH); // 点亮 LED [cite: 162, 255]
      Serial.println("Obstacle Detected!"); // 上位机打印信息
    } else { // 障碍物消失 [cite: 256]
      digitalWrite(LED_BUILTIN, LOW); // 熄灭 LED [cite: 164, 257]
      Serial.println("Clear");
    }
  }
  last = now; // 更新状态 [cite: 259]
}
