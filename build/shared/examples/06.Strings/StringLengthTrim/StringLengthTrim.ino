/*
  String length() and trim()
  Examples of how to use length() and trim() in a String
 
  created 27 July 2010
  modified 2 Apr 2012
  by Tom Igoe
  
  Hardware Required:
  * MSP-EXP430G2 LaunchPad
 
  This example code is in the public domain.
*/

void setup() {
  // Open serial communications and wait for port to open:
  Serial.begin(9600);

  // send an intro:
  Serial.println("\n\nString  length() and trim():");
  Serial.println();
}

void loop() {
  // here's a String with empty spaces at the end (called white space):
  String stringOne = "Hello!       ";
  Serial.print(stringOne);
  Serial.print("<--- end of string. Length: ");
  Serial.println(stringOne.length());

  // trim the white space off the string:
  stringOne.trim();
  Serial.print(stringOne);
  Serial.print("<--- end of trimmed string. Length: ");
  Serial.println(stringOne.length());

  // do nothing while true:
  while(true);
}
