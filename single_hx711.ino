#include "HX711.h"

HX711 scale; //HX711 scale(6, 5);

float calibration_factor = -417;
float units;
float ounces;

void setup()
{
  Serial.begin(9600);
  Serial.println("HX711 weighing");
  scale.begin(3,2);
  scale.set_scale(calibration_factor);
  scale.tare();
  Serial.println("Readings:");
}

void loop()
{
  Serial.print("Reading:");
  units = scale.get_units(),10;
  if (units < 0)
  {
    units = 0.00;
  }
  ounces = units * 0.035274;
  Serial.print(units);
  Serial.println(" grams");
  delay(1000);
}
