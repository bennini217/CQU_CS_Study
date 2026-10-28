

// the setup function runs once when you press reset or power the board
void setup() {
  Serial.begin(9600);
  // initialize digital pin LED_BUILTIN as an output.
  pinMode(LED_BUILTIN, OUTPUT);//信号灯
  pinMode(10, INPUT);//传感器
}

int last = 1;
int now = 1;

// the loop function runs over and over again forever
void loop() {
  int now = digitalRead(10);
  if(now != last){
    if(now == 0)
      digitalWrite(LED_BUILTIN, HIGH);
    else
      digitalWrite(LED_BUILTIN, LOW);
  }
  last = now;
}
