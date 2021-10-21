module.exports = {
    "extends": "../../docs/checkstyle/eslintrc.js",
    "parser": "@babel/eslint-parser",
    "parserOptions": {
        "sourceType": "module",
        "ecmaVersion": 2017,
        "requireConfigFile": false
    },
    "globals": {
        "require": true,
    }
};
