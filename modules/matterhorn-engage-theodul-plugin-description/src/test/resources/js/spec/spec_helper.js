/*global requirejs*/
requirejs.config({
  baseUrl: 'src/',
  paths: {
    main: 'main',
    require: 'test/resources/js/lib/require',
    jquery: 'test/resources/js/lib/jquery',
    underscore: 'test/resources/js/lib/underscore',
    backbone: 'test/resources/js/lib/backbone',
    engage: 'test/resources/js/engage'
  },
  shim: {
    'backbone': {
      //script dependencies
      deps: ['underscore', 'jquery'],
      //global variable
      exports: 'Backbone'
    },
    'underscore': {
      //global variable
      exports: '_'
    }
  }
});