module.exports = function (config) {
    config.set({
        basePath : './',

        files : [
            '../../main/webapp/lib/jquery-1.10.2.js',
            '../../main/webapp/lib/jquery-ui.js',
            '../../main/webapp/lib/angular/angular.js',
            '../../main/webapp/lib/angular/angular-translate.js',
            '../../main/webapp/lib/angular/angular-translate-loader-static-files.js',
            '../../main/webapp/lib/angular/angular-route.js',
            '../../main/webapp/lib/angular/angular-resource.js',
            '../../main/webapp/lib/angular/angular-local-storage.js',
            '../../main/webapp/lib/angular-file-upload/angular-file-upload.js',
            '../../main/webapp/lib/angular/angular-*.js',
            {pattern: '../../main/webapp/lib/angular/angular-*.map', watched: false, served: true, included: false},
            '../../main/webapp/lib/chosen.jquery.min.js',
            '../../main/webapp/lib/chosen.js',
            '../../main/webapp/lib/angular-sortable.js',
            '../../main/webapp/lib/underscore-1.5.2.js',
            '../../main/webapp/lib/video-js/video.js',

            '../../main/webapp/app.js',
            '../../main/webapp/shared/filters/filters.js',
            '../../main/webapp/shared/resources/resources.js',
            '../../main/webapp/shared/directives/directives.js',
            '../../main/webapp/shared/controllers/controllers.js',
            '../../main/webapp/shared/services/services.js',
            '../../main/webapp/modules/**/*.js',
            '../../main/webapp/shared/**/*.js',
            '../../main/webapp/**/*.html',

            'test/lib/jasmine-jquery.js',
            'test/lib/jquery-deparam.js',

            // fixtures
            {pattern: '../../main/resources/public/**/*.json', watched: true, served: true, included: false},
            {pattern: 'test/unit/fixtures/**/*.json', watched: true, served: true, included: false},
            {pattern: 'app/GET/**/*', watched: true, served: true, included: false},

            'test/unit/**/*Spec.js'
        ],

        exclude : [
            '../../main/webapp/lib/angular/angular-loader.js',
            '../../main/webapp/lib/angular/*.min.js',
            '../../main/webapp/lib/angular/angular-scenario.js'
        ],

        autoWatch : true,

        frameworks: ['jasmine'],

        browsers : ['Chrome'],

        plugins : [
            'karma-chrome-launcher',
            'karma-phantomjs-launcher',
            'karma-coverage',
            'karma-firefox-launcher',
            'karma-jasmine',
            'karma-ng-html2js-preprocessor'
        ],

        preprocessors: {
            '../../main/webapp/shared/**/*.js': ['coverage'],
            '../../main/webapp/modules/**/*.js': ['coverage'],
            '../../main/webapp/**/*.html': ['ng-html2js']
        },

        reporters: ['progress'],

        coverageReporter: {
            type : 'html',
            dir : '../../../coverage/'
        },

        ngHtml2JsPreprocessor: {
            cacheIdFromPath: function (filepath) {
                return filepath.match(/.*src\/main\/webapp\/(.*)/)[1];
            }
        }
    });
};
