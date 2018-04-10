module.exports = function (grunt) {
  //to use via grunt, first load the task
  require('load-grunt-tasks')(grunt); // npm install --save-dev load-grunt-tasks

  grunt.initConfig({
    markdownlint: {
      full: {
        options: {
          config: { // configure the linting rules
            'default': false, // in the beginning disable all rules and enable them on a one-by-one basis
            'line-length': { // MD013
              "line_length": 120,
              code_blocks: false,
              tables: false,
            },
            'accessibility': true,
          }
        },
        src: [
          './admin/**/*.md',
          './developer/**/*.md',
          './user/**/*.md',
        ]
      }
    }
  });

  grunt.registerTask('default', ['markdownlint']);

};
