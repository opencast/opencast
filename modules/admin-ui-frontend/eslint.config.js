const opencast = require('eslint-config-opencast');

module.exports = [
  ...opencast,
  {
    'languageOptions': {
      'globals': {
        'angular': 'writable',
        'moment': 'writable',
        '$': 'writable',
        '_': 'writable'
      }
    }
  },
  {
    'ignores': ['app/scripts/lib/**'],
  }
];
