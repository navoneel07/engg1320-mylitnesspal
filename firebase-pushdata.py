import pyrebase as pb
import serial
import re

# for the firebase database

config = {
  "apiKey": "AIzaSyAQ7U9x2dSvAJJ4fh7p9rO7KBO1qPp55KE",
  "authDomain": "mylitnesspal.firebaseapp.com",
  "databaseURL": "https://mylitnesspal.firebaseio.com/",
  "storageBucket": "mylitnesspal.appspot.com",
  "serviceAccount": "mylitnesspal-firebase-adminsdk-yrvkq-637f8b32bc.json"
}

firebase = pb.initialize_app(config)
db = firebase.database()

def getWeight():
    arduino = serial.Serial("/dev/cu.usbmodem14101", 9600)
    rawdata = []
    count = 0
    while count<10:
        print(count)
        rawdata.append(str(arduino.readline()))
        count+=1
    newraw = []
    for line in rawdata:
        newraw.append(re.findall('\d+\.\d+', line))
    sum = 0
    for i in range(5,10):
        sum+=float(newraw[i][0])
    return round(sum/5, 2)

def addIngredient():
    newIng = {"Photo" : '', "Name" : '', "Weight" : getWeight()}
    db.child("Ingredient").set(newIng)
    print("Node added.")

addIngredient()
