module.exports = function (config) {
    config.set({
        basePath : './',

        files : [
            '../../main/webapp/scripts/lib/jquery-1.10.2.js',
            '../../main/webapp/scripts/lib/jquery-ui.js',
            // bower:js
            '../../../bower_components/angular/angular.js',
            '../../../bower_components/angular-md5/angular-md5.js',
            '../../../bower_components/angular-translate/angular-translate.js',
            '../../../bower_components/angular-route/angular-route.js',
            '../../../bower_components/angular-resource/angular-resource.js',
            '../../../bower_components/angular-animate/angular-animate.js',
            '../../../bower_components/angular-messages/angular-messages.js',
            '../../../bower_components/angular-wizard/dist/angular-wizard.js',
            '../../../bower_components/angular-hotkeys/build/hotkeys.css',
            '../../../bower_components/angular-hotkeys/build/hotkeys.js',
            // endbower
            '../../main/webapp/scripts/lib/angular/angular-translate-loader-static-files.js',
            '../../main/webapp/scripts/lib/angular/angular-local-storage.js',
            '../../main/webapp/scripts/lib/angular-file-upload/angular-file-upload.js',
            '../../main/webapp/scripts/lib/chosen.jquery.js',
            '../../main/webapp/scripts/lib/angular-chosen.js',
            '../../main/webapp/scripts/lib/angular/angular-*.js',
            '../../main/webapp/scripts/lib/angular-sortable.js',
            '../../main/webapp/scripts/lib/underscore-1.5.2.js',
            '../../main/webapp/scripts/lib/video-js/video.js',
            '../../main/webapp/scripts/lib/moment.min.js',
            '../../main/webapp/scripts/lib/javascript-md5/js/md5.min.js',
            '../../main/webapp/scripts/lib/timepicker/jquery-ui-timepicker-addon.js',

            '../../main/webapp/scripts/app.js',
            '../../main/webapp/scripts/shared/filters/filters.js',
            '../../main/webapp/scripts/shared/resources/resources.js',
            '../../main/webapp/scripts/shared/directives/directives.js',
            '../../main/webapp/scripts/shared/controllers/controllers.js',
            '../../main/webapp/scripts/shared/services/services.js',
            '../../main/webapp/scripts/modules/**/*.js',
            '../../main/webapp/scripts/shared/**/*.js',
            '../../main/webapp/**/*.html',

            'test/lib/jasmine-jquery.js',
            'test/lib/jquery-deparam.js',

            {pattern: '../../main/webapp/scripts/lib/angular/angular-*.map', watched: false, served: true, included: false},
            // fixtures
            {pattern: '../../main/resources/public/**/*.json', watched: true, served: true, included: false},
            {pattern: 'test/unit/fixtures/**/*.json', watched: true, served: true, included: false},
            {pattern: 'app/GET/**/*', watched: true, served: true, included: false},

            'test/unit/**/*Spec.js'
        ],

        exclude : [
            '../../main/webapp/scripts/lib/angular/angular-loader.js',
            '../../main/webapp/scripts/lib/angular/*.min.js',
            '../../main/webapp/scripts/lib/angular/angular-scenario.js'
        ],

        autoWatch : true,

        frameworks: ['jasmine'],

        browsers : ['PhantomJS'],

        plugins : [
            'karma-chrome-launcher',
            'karma-phantomjs-launcher',
            'karma-coverage',
            'karma-firefox-launcher',
            'karma-jasmine',
            'karma-ng-html2js-preprocessor'
        ],

        captureTimeout: 60000,
        browserDisconnectTimeout : 10000,
        browserDisconnectTolerance : 1,
        browserNoActivityTimeout : 60000, // by default 10000

        preprocessors: {
            '../../main/webapp/scripts/shared/**/*.js': ['coverage'],
            '../../main/webapp/scripts/modules/**/*.js': ['coverage'],
            '../../main/webapp/**/*.html': ['ng-html2js']
        },

        reporters: ['progress'],

        coverageReporter: {
            type : 'html',
            dir : '../../../coverage/'
        },

        ngHtml2JsPreprocessor: {
            cacheIdFromPath: function (filepath) {
                var match = filepath.match(/.*src\/main\/webapp\/scripts\/(.*)/);
                if (match !== null) {
                    return match[1];
                } else {
                    console.log('no match');
                }
            }
        }
    });
};
