module.exports = {
  'env': {
    'browser': true,
    'es6': true
  },
  'extends': 'eslint:recommended',
  'parserOptions': {
    'ecmaVersion': 2015
  },
  'plugins': [
    'header'
  ],
  'rules': {
    // enforce Opencast's license header at the beginning of JavaScript files
    'header/header': [
      2,
      '../../docs/checkstyle/opencast-header.txt'
    ],
    // enforce 2 spaces indentation
    'indent': [
      'error',
      2,
      {
        // indentation for variable specification:
        // var xy = 123,
        //     bla = 321,
        //     ...
        'VariableDeclarator': {
          'var': 2,
          'let': 2,
          'const': 3
        },
        // allow no indent for multi-line property chains:
        // foo
        // .bar
        // .baz();
        'MemberExpression': 'off'
      }
    ],
    // enforce \n for line breaks. No \r\n and no \n\r
    'linebreak-style': [
      'error',
      'unix'
    ],
    // no line ending with spaces:
    // var valid;|<--
    // var invalid;  |<--
    'no-trailing-spaces': [
      'error'
    ],
    // enforce one type of quotes:
    // var x = 'valid';
    // var y = "invalid";
    'quotes': [
      'error',
      'single'
    ],
    // enforce semicolons:
    // var valid = 123;
    // var invalid = 123
    'semi': [
      'error',
      'always'
    ],
    // enforce space around operators:
    // var valid = 1 + 2 * 3;
    // var invalid = 1+2*3;
    'space-infix-ops': [
      'error'
    ],
    // ensure a newline character at the end of a file
    'eol-last': [
      'error'
    ],
    // prevent comparisons to the same thing:
    // invalid === invalid;
    'no-self-compare': [
      'error'
    ],
    // Do not extend native JS objects since it leads to confusing results
    'no-extend-native': [
      'error'
    ],
    'no-global-assign': [
      'error'
    ],
    'no-return-assign': [
      'error'
    ],
    'no-shadow-restricted-names': [
      'error'
    ],
    'no-alert': [
      'error'
    ],
    'no-console': [
      'error'
    ],
    'no-useless-concat': [
      'error'
    ],
    'no-template-curly-in-string': [
      'error'
    ],
    'max-len': [
      'error',
      {
        'code': 120
      }
    ],
    'no-eval': [
      'error'
    ],
    'no-implied-eval': [
      'error'
    ],
    'no-loop-func': [
      'error'
    ]
  }
};
