void setup() {
  // 1. 开启串口通信，必须与串口监视器设置的波特率一致 
  Serial.begin(9600); 

  // 2. 配置信号灯引脚为输出模式 [cite: 176]
  // 根据练习要求，通常 8 号接红灯，13 号接绿灯（或板载灯） [cite: 265, 266]
  pinMode(8, OUTPUT);  
  pinMode(13, OUTPUT); 
}

void loop() {
  // 检查串口缓冲区是否有数据进来
  if (Serial.available() > 0) {
    // 读取上位机发送的整个字符串 [cite: 202, 263]
    String now = Serial.readString(); 

    // 逻辑解析：根据编码规则控制灯 
    // 如果收到 "11"：第一个 '1' 代表方向，第二个 '1' 代表红灯亮 [cite: 278]
    if(now[0] == '1' && now[1] == '1') {
        digitalWrite(8, HIGH); // 8号红灯亮 
        digitalWrite(13, LOW); // 13号绿灯灭 [cite: 266]
    }
    
    // 如果收到 "12"：第一个 '1' 代表方向，第二个 '2' 代表绿灯亮 [cite: 278]
    if(now[0] == '1' && now[1] == '2') {
        digitalWrite(8, LOW);   // 8号红灯灭 [cite: 269]
        digitalWrite(13, HIGH); // 13号绿灯亮 [cite: 270]
    }
  }
}
