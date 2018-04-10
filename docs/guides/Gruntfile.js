module.exports = function(grunt) {
  //to use via grunt, first load the task
  require("load-grunt-tasks")(grunt); // npm install --save-dev load-grunt-tasks

  grunt.initConfig({
    markdownlint: {
      full: {
        options: {
          config: {
            // configure the linting rules
            default: false, // in the beginning disable all rules and enable them on a one-by-one basis
            MD004: { // unordered list style
              style: "consistent"
            },
            MD005: true, // inconsistent indentation for list items at the same level
            MD006: true, // consider starting bulleted lists at the beginning of the line
            MD007: { // unordered list indentation
              indent: 2
            },
            MD013: { // line-length
              line_length: 120,
              code_blocks: false,
              tables: false
            },
            MD045: true, // accessibility
          }
        },
        src: ["./admin/**/*.md", "./developer/**/*.md", "./user/**/*.md"]
      }
    }
  });

  grunt.registerTask("default", ["markdownlint"]);
};
