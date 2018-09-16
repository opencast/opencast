module.exports = {
  'env': {
    'browser': true,
    'es6': true
  },
  'extends': 'eslint:recommended',
  'parserOptions': {
    'ecmaVersion': 2015
  },
  "plugins": [
    "header"
  ],
  'rules': {
    'header/header': [
      2,
      '../../docs/checkstyle/opencast-header.txt'
    ],
    'indent': [
      'error',
      2,
      {
        "VariableDeclarator": {
          "var": 2,
          "let": 2,
          "const": 3
        }
      }
    ],
    'linebreak-style': [
      'error',
      'unix'
    ],
    'no-trailing-spaces': [
      'error'
    ],
    'quotes': [
      'error',
      'single'
    ],
    'semi': [
      'error',
      'always'
    ],
    'space-infix-ops': [
      'error'
    ]
  }
};
