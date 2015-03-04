module.exports = function (grunt) {

    // Project configuration.
    grunt.initConfig({

        /**===================================
         * Configuration variables
         ====================================*/

        pkg: grunt.file.readJSON("package.json"),

        /** JSHint properties */
        jshintProperties: grunt.file.readJSON("jshint.json"),

        /** The path to the app sources */
        baseDir: "src/main/webapp",
        testDirBase: "src/test/resources/app",
        i18nDir: "src/main/resources/public",
        unitTestDir: "src/test/resources/test/unit",
        mockDir: "src/test/resources/mock",

        /** The current target file for the watch tasks */
        currentWatchFile: "",

        /** Local directory for the dev server */
        serverDir: "target/grunt/webapp",

        /** Paths for the different types of ressource */
        srcPath: {
            js   : "<%= baseDir %>/**/*.js",
            less : "<%= baseDir %>/css/**/*.less",
            html : "<%= baseDir %>/**/*.html",
            mocks: "<%= testDirBase %>/**/*",
            unit : "<%= unitTestDir %>/**/*.js",
            i18n: "<%= i18nDir %>/**/*.json"
        },

        /** The profile being used currently */
        currentProfile: undefined,

        /**===================================
         * Tasks
         ====================================*/

        /** Linter task, use the jshint.json file for the option */
        jshint: {
            one     : "<%= baseDir %>/<%= currentWatchFile %>",
            all     : ["<%= srcPath.js %>", "<%= srcPath.unit %>"],
            options : "<%= jshintProperties %>"
        },

        /** Protractor options */
        protractor: {
            options: {
                configFile: "src/test/resources/protractor.conf.js",
                keepAlive: true,
                noColor: false
            },
            dev: {
                options: {
                }
            },
            continuous: {
                options: {
                    configFile: "src/test/resources/protractor.phantomjs.conf.js"
                }
            },
            mh: {
                options: {
                    configFile: "src/test/resources/protractor.mh.conf.js"
                }
            },
            debug: {
                options: {
                    debug: true
                }
            }
        },

        /** Task to watch src files and process them */
        watch: {
            options: {
                nospawn: true
            },
            // Watch Javascript file
            js: {
                files: ["<%= srcPath.js %>"],
                tasks: ["jshint:one", "copy:one"]
            },
            html: {
                files: ["<%= srcPath.html %>"],
                tasks: ["copy:one"]
            },
            mocks: {
                files: ["<%= srcPath.mocks %>"],
                tasks: ["copy:oneTest"]
            },
            // Watch less file
            less: {
                files: ["<%= srcPath.less %>"],
                tasks: ["less:dev"]
            },
            i18n: {
                files: ["<%= srcPath.i18n %>"],
                tasks: ["copy:i18n"]
            },
            // Watch file on web server for live reload
            www: {
                options: {
                    livereload : true
                },
                files: ["<%= serverDir %>/**/*", "<%= serverDir %>"]
            },
            grunt: {
                files: ["Gruntfile.js"]
            }
        },

        /** Compile the less files into a CSS file */
        less: {
            dev: {
                options: {
                    concat: false,
                    compress: false,
                    paths: ["<%= baseDir %>/css/"]
                },
                files: {
                    "<%= serverDir %>/css/main.css": "<%= baseDir %>/css/less/override.less"
                }
            },
            production: {
                options: {
                    syncImport    : true,
                    strictImports : true,
                    concat        : true,
                    compress      : true,
                    paths         : [ "<%= baseDir %>/css/" ]
                },
                files: {
                    "<%= serverDir %>/css/main.css": "<%= baseDir %>/css/less/override.less"
                }
            }
        },

        /** Copy .. */
        copy: {
            // ... a single file locally
            one: {
                cwd     : "<%= baseDir %>",
                expand  : true,
                src     : "<%= currentWatchFile %>",
                dest    : "<%= serverDir %>",
                filter  : "isFile"
            },
            oneTest: {
                cwd     : "<%= testDirBase %>/GET",
                expand  : true,
                src     : "<%= currentWatchFile %>",
                dest    : "<%= serverDir %>",
                filter  : "isFile"
            },
            i18n: {
                cwd     : "<%= i18nDir %>",
                expand  : true,
                src     : ["org/**/*", "!./.*"],
                dest    : "<%= serverDir %>/public"
            },
            // ... the stylesheet locally
            style: {
                expand: true,
                cwd  : "<%= baseDir %>",
                src  : ["css/*.css"],
                dest : "<%= serverDir %>/"
            },
         // ... all assets
            prod: {
                files   : [{
                    cwd     : "<%= baseDir %>",
                    expand  : true,
                    src     : ["{img,modules,shared,lib,org}/**/*", "!./.*"],
                    dest    : "<%= serverDir %>"
                }, {
                    cwd     : "<%= baseDir %>",
                    expand  : true,
                    src     : ["*.html", "*.js", "!./.*"],
                    dest    : "<%= serverDir %>"
                }, {
                    cwd     : "<%= baseDir %>",
                    expand  : true,
                    src     : ["css/**/*.css", "!./.*"],
                    dest    : "<%= serverDir %>"
                }, {
                    cwd     : "<%= i18nDir %>",
                    expand  : true,
                    src     : ["org/**/*", "!./.*"],
                    dest    : "<%= serverDir %>/public"
                }]
            },
            // ... all assets
            all: {
                files   : [{
                    cwd     : "<%= baseDir %>",
                    expand  : true,
                    // remove videos as soon as tools section has been implemented
                    src     : ["{img,videos,modules,shared,lib,org}/**/*", "!./.*"],
                    dest    : "<%= serverDir %>"
                }, {
                    cwd     : "<%= baseDir %>",
                    expand  : true,
                    src     : ["*.html", "*.js", "!./.*"],
                    dest    : "<%= serverDir %>"
                }, {
                    cwd     : "<%= baseDir %>",
                    expand  : true,
                    src     : ["css/**/*.css", "!./.*"],
                    dest    : "<%= serverDir %>"
                }, {
                    cwd     : "<%= testDirBase %>/GET",
                    expand  : true,
                    src     : ["**/*", "!./.*"],
                    dest    : "<%= serverDir %>"
                }, {
                    cwd     : "<%= i18nDir %>",
                    expand  : true,
                    src     : ["org/**/*", "!./.*"],
                    dest    : "<%= serverDir %>/public"
                }]
            }
        },

        clean: ["<%= serverDir %>"],

        /** Task to run tasks in parrallel */
        concurrent: {
            dev: {
                tasks: ["watch:js", "watch:i18n", "watch:less", "watch:html", "watch:mocks", "watch:www", "watch:grunt", "connect:server", "karma:devCoverage"],
                options: {
                    logConcurrentOutput: true,
                    limit: 9
                }
            }
        },

        /** Web server */
        connect: {
            server: {
                options: {
                    port       : 9001,
                    base       : "<%= serverDir %>",
                    keepalive  : true,
                    livereload : false,
                    debug      : false,
                    hostname   : "*",
                    middleware: function(connect, options, middlwares) {

                        var responseHeaders = {
                            "POST": {
                                "/staticfiles": {
                                    "Location": "http://localhost:9001/staticfiles/uuid1"
                                }
                            }
                        }, resolveHeaders = function(method, url){
                            var headers = {}, object;

                            if(typeof responseHeaders[method] !== "undefined" && typeof responseHeaders[method][url] !== "undefined"){
                                // if there are configured response headers in the responseHeaders map
                                object = responseHeaders[method][url];
                                for(var key in object){
                                    // add all to the resulting headers
                                    if(object.hasOwnProperty(key)){
                                        headers[key] = object[key];
                                    }
                                }
                            }
                            return headers;
                        };

                        return [
                            connect.static(options.base[0]),

                            /*
                             * This function serves POST / PUT / DELETE mock requests by getting the file content from src/test/resources/app/<method>/<url>.
                             */
                            function (req, res, next) {

                                var path = "src/test/resources/app/" + req.method + req.url;

                                if ((req.method === "POST" || req.method==="PUT" || req.method==="DELETE") &&
                                        grunt.file.exists(path)) {
                                        setTimeout(function () {
                                            if (req.method === "POST") {
                                                res.writeHead(201, resolveHeaders(req.method, req.url));
                                            }
                                            res.end(grunt.file.read(path));
                                        }, 1000);
                                } else {
                                    next();
                                }
                            }
                        ];
                    }
                }
            },
            test: {
                options: {
                    port       : 9007,
                    base       : "<%= serverDir %>",
                    keepalive  : false,
                    livereload : false,
                    debug      : false,
                    hostname   : "*"
                }
            }
        },

        /** Test runner */
        karma: {
            options: {
                configFile : "src/test/resources/karma.conf.js",
                runnerPort : 9999
            },
            continuous: {
                singleRun : true,
                browsers  : ["PhantomJS"]
            },
            dev: {
                reporters : ["dots"],
                browsers  : ["Chrome"]
            },
            coverage: {
                singleRun : true,
                reporters : ["dots", "coverage"],
                browsers  : ["PhantomJS"]
            },
            devCoverage: {
                reporters : ["dots", "coverage"],
                browsers  : ["PhantomJS"]
            }
        },

        /** Documentation generator */
        docular: {
            groups: [{
                groupTitle: "Admin NG",
                groupId:    "adminNg",
                showSource: true,
                sections: [{
                    id:      "main",
                    title:   "Main",
                    scripts: ["<%= baseDir %>/js"]
                }]
            }],
            showDocularDocs: false,
            showAngularDocs: true
        }
    });

    // Load the plugin that provides the "uglify" task.
    grunt.loadNpmTasks("assemble-less");
    grunt.loadNpmTasks("grunt-concurrent");
    grunt.loadNpmTasks("grunt-contrib-jshint");
    grunt.loadNpmTasks("grunt-contrib-watch");
    grunt.loadNpmTasks("grunt-contrib-clean");
    grunt.loadNpmTasks("grunt-contrib-copy");
    grunt.loadNpmTasks("grunt-contrib-connect");
    grunt.loadNpmTasks("grunt-docular");
    grunt.loadNpmTasks("grunt-karma");
    grunt.loadNpmTasks("grunt-protractor-runner");

    // Base task for the development
    grunt.registerTask("dev", ["clean", "less:dev", "jshint:all", "copy:all", "concurrent:dev"]);

    // Base task for production
    var buildWithoutTests = ["clean", "less:production", "copy:prod" ] ;
    var buildWithTests = ["jshint:all", "karma:continuous", "clean", "less:production", "copy:prod" ];
    grunt.registerTask("build", grunt.option('skipTests') ? buildWithoutTests : buildWithTests);

    // Deployment task
    grunt.registerTask("deploy", ["jshint:all", "karma:continuous", "build:prod"]);

    // Provide protractor with an independent web server
    grunt.registerTask("e2e:prepare", ["clean", "less:production", "copy:all", "connect:test"]);
    grunt.registerTask("e2e:continuous", ["e2e:prepare", "protractor:continuous"]);
    grunt.registerTask("e2e:dev", ["e2e:prepare", "protractor:dev"]);
    grunt.registerTask("e2e:debug", ["e2e:prepare", "protractor:debug"]);

    // Task for continuous integration
    grunt.registerTask("ci", ["jshint:all", "karma:continuous"]);

    // The default task when run without arguments
    grunt.registerTask("default", ["dev"]);

    // on watch events configure jshint:all to only run on changed file
    grunt.event.on("watch", function (action, filepath, target) {
        if (target === "mocks") {
            grunt.config.set("currentWatchFile", [filepath.replace(grunt.config.get("testDirBase") + "/", "")]);
        }
        else {
            grunt.config.set("currentWatchFile", [filepath.replace(grunt.config.get("baseDir") + "/", "")]);
        }
    });


    // Create documentation
    grunt.registerTask("doc", ["docular", "docular-server"]);
};
