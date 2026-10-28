int analogPin = 9; // potentiometer wiper (middle terminal) connected to analog pin 3
                   
int val = 0;  
int moto_speed=0;  
void setup() {
  Serial.begin(9600);           //  setup serial
  pinMode(6, OUTPUT);
}

void loop() {
  analogWrite(analogPin,val);
  String input = Serial.readString();
  Serial.println( input.length() );
  if(input.startsWith("a")){
    Serial.println("input= a" );
    
    digitalWrite(6, LOW); 
    
    delay(1000);
  }else{
    Serial.println("input!= a" );
    digitalWrite(6, HIGH); 
  }
  Serial.println(input);
  val++;
  if(val > 255){
    val = 0;
  }
  delay(1000);
}
