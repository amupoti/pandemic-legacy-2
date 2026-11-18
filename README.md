# Pandemic legacy season 2 - Infection deck predictor

Infection deck predictor is a tool to obtain the next cards that will appear in the infection deck, taking into account the cards that already appeared in previous epidemics
The tool is used for pandemic legacy season 2, but can be used for any other version on the game 

The tool is based on:
* A google sheets file to keep track of the infected cities in each epidemic
* A local server which will read from the google sheets file and provide a list and order of cards that will appear 

## How to run it ##

* Clone the project
* Run the application using `mvn spring-boot:run` (or from your IDE) and go to http://localhost:8080/

The campaign is saved in `campaign.json` (set `pandemic.campaign-file` in `application.properties` to change it).
It is written after every action, so you can keep a copy of it as a backup.

## Playing ##

* **Infection step**: tap each card you draw from the infection pile. It moves to the discard pile.
* **Epidemic**: press "Epidèmia!" and tap the card you drew from the bottom of the pile. The discard pile and that
  card go back on top, and a new epidemic starts.
* **Mistakes**: "Desfer" undoes the last draw or epidemic, as many times as needed.
* **Cards and legacy** ("Cartes i llegat"): each card is in one state, *A la xarxa*, *Fora de la xarxa*,
  *Pila final partida* or *Caixa 6*, and can move to any other state at any time. Only cards in the network are part
  of the infection deck. You can also rename cards, change labels and colours, add several cards of a city at once
  (they go to the bottom of the pile) and delete a card entered by mistake. Every change is written in the legacy log.
* **New game**: at the end of a game, "Començar partida nova" puts every card back in the deck and starts the initial
  infection. Cards, states and labels are kept.
* **Correction grid** ("Graella"): the same table as the old Google sheet, one row per card and one column per
  infection period, to fix mistakes that undo cannot reach.
* **Backups**: the campaign is saved in `campaign.json` after every change, with one copy per day (the last 30) in
  `campaign-backups/`. The campaign page can download the campaign as JSON and restore it from a file.

* **Starting**: the first time, the app offers three ways to start: import the Google sheet (only when
  `credentials.json` is present), load a campaign file exported before, or start from scratch and add the cards by hand.
* **Phone**: the app listens on the local network. "Cartes i llegat" shows the address to open from a phone on the
  same wifi (for instance `http://192.168.1.140:8080/`). On phones the cards are shown in two columns.

## Safety net for changes ##

`GoldenPageTest` imports a saved copy of the Google sheet (`src/test/resources/golden/sheet-2026-11-18.json`),
renders the page and compares everything it shows (piles, cards, labels, appearances, percentages, next epidemic,
status) with a reviewed snapshot (`expected-page-2026-11-18.json`). If a change is meant to alter the page, regenerate
the snapshot with `./mvnw test -Dtest=GoldenPageTest -Dgolden.update=true` and review the diff.

## Importing from Google Sheets ##

The Google sheet is optional. If you have one, the app can import its current state once. After that, the sheet is no longer used
(you can re-import it from the "Cartes i llegat" page, which replaces the whole campaign).

* Follow the instructions in https://developers.google.com/sheets/api/quickstart/java and put your service account
  credentials in `src/main/resources/credentials.json`
* Create a Spreadsheet file with the following format
  * First line is the header
  * Columns: city names, label (optional), card is in the network (yes/no), destroyed (yes/no), in box 6 (yes/no), Initial infection, Epidemic 1, Epidemic 2, ..., Padding column (needs to have some dummy value on it)
  * Mark infection appearances as '1' to indicate the card has appeared
  * See an example in https://docs.google.com/spreadsheets/d/1y3HicO9FcthZwH_DOC1J315JG3gcJrvUF4ssqZRyvhQ/edit#gid=0
* Set `pandemic.spreadsheet-id` in `application.properties` to the id of your spreadsheet
