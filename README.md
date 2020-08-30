# Pandemic legacy season 2 - Infection deck predictor

Infection deck predictor is a tool to obtain the next cards that will appear in the infection deck, taking into account the cards that already appeared in previous epidemics
The tool is used for pandemic legacy season 2, but can be used for any other version on the game 

The tool is based on:
* A google sheets file to keep track of the infected cities in each epidemic
* A local server which will read from the google sheets file and provide a list and order of cards that will appear 

## How to run it ##

* Clone the project
* Follow the instructions in https://developers.google.com/sheets/api/quickstart/java and replace the `credentials.json` file with your own credentials
* Create a Spreadsheet file with the following format
  * First line is the header
  * Columns: city names, label (optional), card is in the game (use yes/no),ignore column, ignore column, Initial infection, Epidemic 1, Epidemic2, ..., Padding column (neet to have some dummy value on it)
  * Marks infection appearances as '1' to indicate the card has appeared
  * See an example in https://docs.google.com/spreadsheets/d/1y3HicO9FcthZwH_DOC1J315JG3gcJrvUF4ssqZRyvhQ/edit#gid=0  
* Change the SPREADSHEET_ID to the Id of your spreadsheet file in the Controller
* Run the application using `./mvnw spring-boot:run`
* Go to http://localhost:8080/predict.html. After that, check the application logs and grant access to the application to get a token to access the google sheet
* After that, the deck of cards will show up in the browser
