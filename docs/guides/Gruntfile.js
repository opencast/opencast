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
              indent: 4 // python-markdown requires 4 spaces of indentation. see https://python-markdown.github.io/#differences
            },
            MD009: true, // prevent trailing spaces
            MD010: true, // prevent the usage of tabs
            MD013: { // line-length
              line_length: 120,
              code_blocks: false,
              tables: false
            },
            MD018: true, // require space after hash on atx style header
            MD019: true, // prohibit multiple spaces after hash on atx style header
            MD025: true, // prevent multiple top level headers in the same document
            MD042: true, // no empty links
            MD045: true, // accessibility
          }
        },
        src: ["./admin/**/*.md", "./developer/**/*.md", "./user/**/*.md", "../../*.md"]
      }
    }
  });

  grunt.registerTask("default", ["markdownlint"]);
};
