/*global requirejs*/
requirejs.config({
  baseUrl: 'src/js/lib',
  paths: {
    require: 'require',
    jquery: 'jquery',
    underscore: 'underscore',
    backbone: 'backbone',
    engage: '../engage',
    plugins: '../engage/plugin/*/static'
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
var PLUGIN_MANAGER_PATH = '/engage/theodul/manager/list.json';
var PLUGIN_PATH = '/engage/theodul/plugin/';
