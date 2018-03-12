module.exports = function (grunt) {
  //to use via grunt, first load the task
  require('load-grunt-tasks')(grunt); // npm install --save-dev load-grunt-tasks

  grunt.initConfig({
    markdownlint: {
      full: {
        options: {
          config: { //configure the linting rules
            'default': true,
            'line-length': { "line_length": 120 }
          }
        },
        src: [
          './**/*.md'
        ]
      }
    }
  });

  grunt.registerTask('default', ['markdownlint']);

};
