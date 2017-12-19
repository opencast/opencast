/*global requirejs*/
requirejs.config({
  baseUrl: 'src/',
  paths: {
    require: 'test/resources/js/lib/require',
    jquery: 'test/resources/js/lib/jquery',
    underscore: 'test/resources/js/lib/underscore',
    backbone: 'test/resources/js/lib/backbone',
    basil: 'test/resources/js/lib/basil',
    bootstrap: 'test/resources/js/lib/bootstrap',
    bowser: 'test/resources/js/lib/bowser',
    jquery_mobile: 'test/resources/js/lib/jquery.mobile',
    moment: 'test/resources/js/lib/moment',
    mousetrap: 'test/resources/js/lib/mousetrap',
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