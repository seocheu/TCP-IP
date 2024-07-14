#include "DHT.h"

enum CONTROL_PINS {
  TEMPER_HUMID = A0,
  RED_LED = 8U,
  GREEN_LED,
  BLUE_LED,
  BUZZER = 30U
};
class DHT dht(TEMPER_HUMID, 11);

void setup() {
  // put your setup code here, to run once:
  Serial.begin(115200UL);
  Serial1.begin(9600UL);  // BLUETOOTH
  pinMode(TEMPER_HUMID, INPUT);
  pinMode(RED_LED, OUTPUT);
  pinMode(GREEN_LED, OUTPUT);
  dht.begin();
}

void loop() {
  // put your main code here, to run repeatedly:
  if(dht.read()) {
    const float temperature {dht.readTemperature()};
    const float humidity = {dht.readHumidity()};

    const String sending_data {String(temperature) + ',' + String(humidity)};
    Serial.println(sending_data);

    for(int i = 0; i < 50; i++) {
      if(Serial.available()) {
        String in_comming_string {Serial.readStringUntil('\n')};
        in_comming_string.trim(); // \r, \n을 기준으로 잘라내기
        int index_led_end = in_comming_string.lastIndexOf('_') + 1; 
        if(in_comming_string.substring(0, index_led_end).equals("RED_LED_")) {
          analogWrite(RED_LED, in_comming_string.substring(index_led_end, in_comming_string.length()).toInt());
        } else if(in_comming_string.substring(0, index_led_end).equals("GREEN_LED_")) {
          analogWrite(GREEN_LED, in_comming_string.substring(index_led_end, in_comming_string.length()).toInt());
        } else if(in_comming_string.substring(0, index_led_end).equals("BLUE_LED_")) {
          analogWrite(BLUE_LED, in_comming_string.substring(index_led_end, in_comming_string.length()).toInt());
        } else if(in_comming_string.substring(0, index_led_end).equals("BUZZER_")) {
          tone(BUZZER, in_comming_string.substring(index_led_end, in_comming_string.length()).toInt());
        } else if(in_comming_string.substring(0, index_led_end).equals("BUZZER_STOP_")) {
          noTone(BUZZER);
        } else {}
      }
      delay(20UL);
    }
  }
}
