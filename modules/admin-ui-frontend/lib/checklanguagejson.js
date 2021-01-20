'use strict';
const testFolder = 'resources/public/org/opencastproject/adminui/languages';
const fs = require('fs');

let languages = []
let files = fs.readdirSync(testFolder);
files.forEach(file => {
  languages.push(file.substr(5,5));
});

let languages_json = JSON.parse(fs.readFileSync('resources/public/languages.json'));
let availablelanguages = [] ;
languages_json.availableLanguages.forEach(langcode => {
    availablelanguages.push(langcode.code);
});

let allAvailable = (languages.length === availablelanguages.length);
if (allAvailable){
  for(var i = 0; i < languages.length; i++){
    allAvailable = allAvailable && ( availablelanguages[i] ==  languages[i]);
    if(availablelanguages[i] !=  languages[i]){
      console.log("Missing language: " + languages[i]);
    }
  }
}
if(!allAvailable){
  process.exit(1);
}
