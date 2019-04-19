import pyrebase as pb
import serial

#set up configuration for the firebase app
config = {
  "apiKey": "AIzaSyAQ7U9x2dSvAJJ4fh7p9rO7KBO1qPp55KE",
  "authDomain": "mylitnesspal.firebaseapp.com",
  "databaseURL": "https://mylitnesspal.firebaseio.com/",
  "storageBucket": "mylitnesspal.appspot.com",
  "serviceAccount": "mylitnesspal-firebase-adminsdk-yrvkq-637f8b32bc.json"
}

#initialize firebase
firebase = pb.initialize_app(config)

#read data from serial port and write to text file


#read values from file and write to firebase database
#data = open("data.txt", "r")
#values = []
#for value in data:
#    value.replace('\n', '')
#    values.append(value)
db = firebase.database()
#db.child("Food weights").set(values)
lmao = db.child("Food weights").get().val()
print(lmao)
