//The build will inline common dependencies into this file.

//For any third party dependencies, like jQuery, place them in the lib folder.

//Configure loading modules from the lib directory,
//except for 'app' ones, which are in a sibling
//directory.
requirejs.config({
  baseUrl: 'js/lib',
  paths: {
      app: '../app'
  },
  shim: {
    'bootstrap' : {
      deps: ['jquery']
    },
    'bootstrap-accessibility': {
      deps: ['bootstrap']
    },
    'jquery.liveSearch': {
      deps: ['jquery']
    },
    'jquery.utils': {
      deps: ['jquery']
    },
    'bootbox': {
      deps: ['bootstrap', 'jquery'],
      exports: 'bootbox'
    },
    'dropdowns-enhancement' : {
      deps: ['bootstrap']
    },
    'redirect' : {
      deps: ['jquery']
    },
    'underscore' : {
      exports: '_'
    }
  }
});
