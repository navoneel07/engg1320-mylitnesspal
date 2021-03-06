import pyrebase as pb
import serial
import re
from clarifai.rest import ClarifaiApp
from cImage import *
import math

#dictionary containing caloric information on some foods (per 100g)
foods = {"banana":89, "apple":52, "broccoli":34, "carrot":41, "bread":77, "toast":77}

#setup clarifai api with its food model
app = ClarifaiApp(api_key='528dcf0eeb5d4168b4a0a43831fc080c')
model = app.models.get('food-items-v1.0')

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
    arduino = serial.Serial('com3', 9600)
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

def getCalories(food, weight):
    kcal = 90;
    for key, value in foods.items():
        if key == food:
            kcal = math.floor((weight * value) / 100)
    return kcal;

def processImage(message):
    if message["data"] != None:
        #an image has been uploaded, process it using Clarifai Api
        response = model.predict_by_url(message["data"])
        
        foodName1 = response["outputs"][0]["data"]["concepts"][0]["name"]
        foodName2 = response["outputs"][0]["data"]["concepts"][1]["name"]
        foodName3 = response["outputs"][0]["data"]["concepts"][2]["name"]
        
        db.child("Ingredient").child("names").child("name1").set(foodName1)
        db.child("Ingredient").child("names").child("name2").set(foodName2)
        db.child("Ingredient").child("names").child("name3").set(foodName3)
                
    else:
        print("no image uploaded")

def processName(message):
    if message["data"] != None:
        db.child("Ingredient").child("kCal").set(str(getCalories(message["data"], getWeight())))

imageStream = db.child("Ingredient").child("Photo").stream(processImage);
nameStream = db.child("Ingredient").child("Name").stream(processName)
    
