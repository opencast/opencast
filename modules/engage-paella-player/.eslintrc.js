module.exports = {
    'globals': {
      'base': true,
      '$': true,
      'jQuery': true,
      'paella': true
    },
	  'rules': {
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
