const opencast = require("eslint-config-opencast");
const babelParser = require("@babel/eslint-parser")

module.exports = [
    ...opencast,
    {
        "languageOptions": {
            "sourceType": "module",
            "parser": babelParser,
            "parserOptions": {
                "requireConfigFile": false
            },
            "globals": {
                "require": "writable",
                "cookieconsent": "writable"
            }
        }
    }
];
