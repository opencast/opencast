module.exports = {
    'env': {
      'browser': true,
      'es6': true
    },
    'extends': 'eslint:recommended',
    'parserOptions': {
      'ecmaVersion': 2015
    },
    'globals': {
      'base': true,      
      '$': true,
      'jQuery': true,
      'paella': true
    },
    'rules': {
      'indent': [
        'error',
        2,
        { 'MemberExpression': 'off'}
      ],
      'linebreak-style': [
        'error',
        'unix'
      ],
      'quotes': [
        'error',
        'single'
      ],
      'semi': [
        'error',
        'always'
      ],
      'no-unused-vars': [
        'error',
        {
          'vars': 'local',
          'args': 'none',
          'ignoreRestSiblings': false 
        }
      ]
    }
  };