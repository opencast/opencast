// Generated on 2016-03-07 using generator-angular 0.15.1
'use strict';

// # Globbing
// for performance reasons we're only matching one level down:
// 'test/spec/{,*/}*.js'
// use this if you want to recursively match all subfolders:
// 'test/spec/**/*.js'
var proxyMiddleware = require('./lib/proxyMiddleware');
var staticMiddleware = require('./lib/staticMiddleware');

module.exports = function (grunt) {

  // Time how long tasks take. Can help when optimizing build times
  require('time-grunt')(grunt);

  // Require sass implementation
  const sass = require('node-sass');

  // Automatically load required Grunt tasks
  require('jit-grunt')(grunt, {
    useminPrepare: 'grunt-usemin',
    setupProxies: 'grunt-middleware-proxy'
  });

  // Configurable paths for the application
  var appConfig = {
    app: require('./bower.json').appPath,
    dist: 'target/grunt/webapp',
    staging: 'target/grunt/.tmp'
  };

  // Define the configuration for all the tasks
  grunt.initConfig({

    // Project settings
    yeoman: appConfig,

    proxyPort: 9009,

    // Watches files for changes and runs tasks based on the changed files
    watch: {
      bower: {
        files: ['bower.json'],
        tasks: ['wiredep']
      },
      js: {
        files: ['<%= yeoman.app %>/scripts/{,*/}*.js'],
        tasks: ['newer:jshint:all', 'newer:jscs:all'],
        options: {
          livereload: '<%= connect.options.livereload %>'
        }
      },
      jsTest: {
        files: ['test/spec/{,*/}*.js'],
        tasks: ['newer:jshint:test', 'newer:jscs:test', 'karma']
      },
      sass: {
        files: ['<%= yeoman.app %>/styles/**/*.{scss,sass}'],
        tasks: ['sass:server', 'postcss']
      },
      gruntfile: {
        files: ['Gruntfile.js']
      },
      livereload: {
        options: {
          livereload: '<%= connect.options.livereload %>'
        },
        files: [
          '<%= yeoman.app %>/{,*/}*.html',
          '<%= yeoman.staging %>/styles/{,*/}*.css',
          '<%= yeoman.app %>/images/{,*/}*.{png,jpg,jpeg,gif,webp,svg}'
        ]
      }
    },

    // The actual grunt server settings
    connect: {
      options: {
        port: 9000,
        // Change this to '0.0.0.0' to access the server from outside.
        hostname: 'localhost',
        livereload: 35729
      },
      livereload: {
        options: {
          open: true,
          middleware: staticMiddleware(grunt, appConfig.app)
        }
      },
      proxy: {
        options: {
          keepalive: true,
          livereload: true,
          debug: false,
          proxyPort: '<%= proxyPort %>',
          middleware: proxyMiddleware(grunt, appConfig.app)
        },
        proxies: [{
          context: '/admin-ng',
          host: 'localhost',
          port: '<%= proxyPort %>',
          https: false
        }, {
          context: '/acl-manager',
          host: 'localhost',
          port: '<%= proxyPort %>',
          https: false
        }, {
          context: '/i18n',
          host: 'localhost',
          port: '<%= proxyPort %>',
          https: false
        }, {
          context: '/broker',
          host: 'localhost',
          port: '<%= proxyPort %>',
          https: false
        }, {
          context: '/services',
          host: 'localhost',
          port: '<%= proxyPort %>',
          https: false
        }]
      },
      test: {
        options: {
          port: 9001,
          middleware: staticMiddleware(grunt, appConfig.app)
        }
      },
      dist: {
        options: {
          open: true,
          base: '<%= yeoman.dist %>',
          middleware: staticMiddleware(grunt, appConfig.dist)
        }
      }
    },

    // Make sure there are no obvious mistakes
    jshint: {
      options: {
        jshintrc: '.jshintrc',
        reporter: require('jshint-stylish')
      },
      all: {
        src: [
          'Gruntfile.js',
          '<%= yeoman.app %>/scripts/{,*/}*.js'
        ]
      },
      test: {
        options: {
          jshintrc: 'test/.jshintrc'
        },
        src: ['test/spec/{,*/}*.js']
      }
    },

    // Make sure code styles are up to par
    jscs: {
      options: {
        config: '.jscsrc',
        fix: true, // Autofix code style violations when possible.
        excludeFiles: ['src/main/webapp/scripts/lib/**']
      },
      all: {
        src: [
          'Gruntfile.js',
          ['<%= yeoman.app %>/scripts/{,*/}*.js', '!<%= yeoman.app %>/scripts/lib/{,*/}*.js']
        ]
      },
      test: {
        src: ['test/spec/{,*/}*.js']
      }
    },

    // Empties folders to start fresh
    clean: {
      dist: {
        files: [{
          dot: true,
          src: [
            '<%= yeoman.staging %>',
            '<%= yeoman.dist %>/{,*/}*',
            '!<%= yeoman.dist %>/.git{,*/}*'
          ]
        }]
      },
      server: '<%= yeoman.staging %>'
    },

    // Add vendor prefixed styles
    postcss: {
      options: {
        processors: [
          require('autoprefixer')({browsers: ['last 1 version']})
        ]
      },
      server: {
        options: {
          map: true
        },
        files: [{
          expand: true,
          cwd: '<%= yeoman.staging %>/styles/',
          src: '{,*/}*.css',
          dest: '<%= yeoman.staging %>/styles/'
        }]
      },
      dist: {
        files: [{
          expand: true,
          cwd: '<%= yeoman.staging %>/styles/',
          src: '{,*/}*.css',
          dest: '<%= yeoman.staging %>/styles/'
        }]
      }
    },

    // Automatically inject Bower components into the app
    wiredep: {
      app: {
        src: ['<%= yeoman.app %>/index.html', '<%= yeoman.app %>/login.html'],
        ignorePath:  /\.\.\//
      },
      test: {
        devDependencies: true,
        src: '<%= karma.options.configFile %>',
        ignorePath:  /\.\.\//,
        fileTypes: {
          js: {
            block: /(([\s\t]*)\/{2}\s*?bower:\s*?(\S*))(\n|\r|.)*?(\/{2}\s*endbower)/gi,
            detect: {
              js: /'(.*\.js)'/gi
            },
            replace: {
              js: '\'../{{filePath}}\','
            }
          }
        }
      }
    },

    // Compiles Sass to CSS and generates necessary files if requested
    sass: {
      options: {
        implementation: sass,
        includePaths: [
          'bower_components'
        ]
      },
      dist: {
        files: [{
          expand: true,
          cwd: '<%= yeoman.app %>/styles',
          src: ['*.scss'],
          dest: '<%= yeoman.staging %>/styles',
          ext: '.css'
        }]
      },
      server: {
        files: [{
          expand: true,
          cwd: '<%= yeoman.app %>/styles',
          src: ['*.scss'],
          dest: '<%= yeoman.staging %>/styles',
          ext: '.css'
        }]
      }
    },

    // Renames files for browser caching purposes
    filerev: {
      dist: {
        src: [
          '<%= yeoman.dist %>/scripts/{,*/}*.js',
          '<%= yeoman.dist %>/styles/{,*/}*.css',
          '<%= yeoman.dist %>/images/{,*/}*.{png,jpg,jpeg,gif,webp,svg}',
          '<%= yeoman.dist %>/styles/fonts/*'
        ]
      }
    },

    // Reads HTML for usemin blocks to enable smart builds that automatically
    // concat, minify and revision files. Creates configurations in memory so
    // additional tasks can operate on them
    useminPrepare: {
      html: ['<%= yeoman.app %>/index.html', '<%= yeoman.app %>/index.html'],
      options: {
        staging: '<%= yeoman.staging %>',
        dest: '<%= yeoman.dist %>',
        flow: {
          html: {
            steps: {
              js: ['concat', 'uglifyjs'],
              css: ['cssmin']
            },
            post: {}
          }
        }
      }
    },

    // Performs rewrites based on filerev and the useminPrepare configuration
    usemin: {
      html: ['<%= yeoman.dist %>/{,*/}*.html'],
      css: ['<%= yeoman.dist %>/styles/{,*/}*.css'],
      js: ['<%= yeoman.dist %>/scripts/{,*/}*.js'],
      options: {
        assetsDirs: [
          '<%= yeoman.dist %>',
          '<%= yeoman.dist %>/images',
          '<%= yeoman.dist %>/styles'
        ],
        patterns: {
          js: [[/(images\/[^''""]*\.(png|jpg|jpeg|gif|webp|svg))/g, 'Replacing references to images']]
        }
      }
    },

    htmlmin: {
      dist: {
        options: {
          collapseWhitespace: true,
          conservativeCollapse: true,
          collapseBooleanAttributes: true,
          removeCommentsFromCDATA: true
        },
        files: [{
          expand: true,
          cwd: '<%= yeoman.dist %>',
          src: ['*.html'],
          dest: '<%= yeoman.dist %>'
        }]
      }
    },

    concat: {
      options: {
        sourceMap: true,
        sourceMapStyle: 'inline'
      }
    },

    // ng-annotate tries to make the code safe for minification automatically
    // by using the Angular long form for dependency injection.
    ngAnnotate: {
      options: {
        sourceMap: true
      },
      dist: {
        files: [{
          expand: true,
          cwd: '<%= yeoman.staging %>/concat/scripts',
          src: '*.js',
          dest: '<%= yeoman.staging %>/concat/scripts'
        }]
      }
    },

    // The sourcemaps embedded by ngAnnotate are sometimes missing
    // a trailing newline, which is expected by extract_sourcemap
    endline: {
      ngAnnotate: {
        files: [{
          src: '<%= yeoman.staging %>/concat/scripts/*.js',
          dest: '.'
        }]
      }
    },

    // Uglify generate bogus sourcemaps when trying to take into account
    // a preexisting inline sourcemap, so we extract the inline sourcemaps
    // that ngAnnotate generates into their own files.
    // Note that ngAnnotate **can** actually output external source maps, too,
    // but in that mode, it does not in turn take into account any preexisting sourcemap.

    // jscs:disable requireCamelCaseOrUpperCaseIdentifiers
    extract_sourcemap: {
    // jscs:disable requireCamelCaseOrUpperCaseIdentifiers
      ngAnnotate: {
        files: [{
            src: '<%= yeoman.staging %>/concat/scripts/*.js',
            dest: '<%= yeoman.staging %>/concat/scripts'
        }]
      }
    },

    uglify: {
      options: {
        sourceMap: {
          includeSources: true
        },
        sourceMapIn: function (file) {
          return file + '.map';
        }
      }
    },

    // Copies remaining files to places other tasks can use
    copy: {
      dist: {
        files: [{
          expand: true,
          dot: true,
          cwd: '<%= yeoman.app %>',
          dest: '<%= yeoman.dist %>',
          src: [
            '*.{ico,png,txt}',
            '*.html',
            'images/{,*/}*.{webp}',
            'fonts/{,*/}*.*',
            'img/{,*/}*.*'
          ]
        }, {
          expand: true,
          cwd: '<%= yeoman.staging %>/images',
          dest: '<%= yeoman.dist %>/images',
          src: ['generated/*']
        }, {
          expand: true,
          cwd: '<%= yeoman.app %>/scripts/lib',
          dest: '<%= yeoman.dist %>/lib',
          src: [
            '**'
          ]
        }, {
          expand: true,
          cwd: 'src/main/resources/public',
          dest: '<%= yeoman.dist %>/public',
          src: [
            '**'
          ]
        }, {
          expand: true,
          cwd: '<%= yeoman.app %>/scripts',
          dest: '<%= yeoman.dist %>',
          src: ['**/*.html']
        }]
      },
      styles: {
        expand: true,
        cwd: '<%= yeoman.app %>/styles',
        dest: '<%= yeoman.staging %>/styles/',
        src: '{,*/}*.css'
      }
    },

    // Run some tasks in parallel to speed up the build process
    concurrent: {
      server: [
        'sass:server',
        'copy:styles'
      ],
      test: [
        'sass',
        'copy:styles'
      ],
      dist: [
        'sass',
        'copy:styles'
      ]
    },

    // Test settings
    karma: {
      options: {
        configFile: 'src/test/resources/karma.conf.js'
      },
      unit: {
        singleRun: true
      },
      coverage: {
        singleRun : true,
        reporters : ['dots', 'coverage'],
        browsers  : ['PhantomJS']
      }
    }
  });


  grunt.registerTask('serve', 'Compile then start a connect web server', function (target) {
    if (target === 'dist') {
      grunt.task.run(['build', 'connect:dist:keepalive']);
      return;
    }

    grunt.task.run([
      'clean:server',
      'wiredep',
      'concurrent:server',
      'postcss:server',
      'connect:livereload',
      'watch'
    ]);
  });

  grunt.registerTask('server', 'DEPRECATED TASK. Use the "serve" task instead', function (target) {
    grunt.log.warn('The `server` task has been deprecated. Use `grunt serve` to start a server.');
    grunt.task.run(['serve:' + target]);
  });

  grunt.registerTask('test', [
    'clean:server',
    'wiredep',
    'concurrent:test',
    'postcss',
    'connect:test',
    'karma',
    'newer:jshint',
    'newer:jscs'
  ]);

  grunt.registerTask('build', function() {
    var skipTests = grunt.option('skipTests');
    if (skipTests !== true) {
      grunt.task.run(['test']);
    }
    grunt.task.run([
      'clean:dist',
      'wiredep',
      'useminPrepare',
      'concurrent:dist',
      'postcss',
      'concat',
      'ngAnnotate',
      'endline',
      'extract_sourcemap',
      'copy:dist',
      'cssmin',
      'uglify',
      'filerev',
      'usemin',
      'htmlmin'
    ]);
  });

  grunt.registerTask('default', [
    'serve'
  ]);

  // Base task for the development with proxy to a real backend
  grunt.registerTask('proxy', [
    'clean:server',
    'wiredep',
    'concurrent:server',
    'postcss:server',
    'setupProxies:proxy',
    'connect:proxy',
    'watch'
  ]);
};
