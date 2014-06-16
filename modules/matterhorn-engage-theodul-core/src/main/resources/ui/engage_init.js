/*global requirejs*/
requirejs.config({
  baseUrl: 'js/lib',
  paths: {
    engage: '../engage',
    plugins: '/engage/plugin/*/static'
  },
  shim: {
    'bootstrap': {
      //script dependencies
      deps: ['jquery'],
      //global variable
      exports: 'Bootstrap'
    },
    'backbone': {
      //script dependencies
      deps: ['underscore', 'jquery'],
      //global variable
      exports: 'Backbone'
    },
    'underscore': {
      //global variable
      exports: '_'
    },
    'jquery.mobile': {
      //script dependencies
      deps: ['jquery'],
    },
    'mousetrap': {
      exports: 'Mousetrap'
    }
  }
});
var PLUGIN_MANAGER_PATH = '/engage/theodul/manager/list.json';
var PLUGIN_PATH = '/engage/theodul/plugin/';
//start core logic
require(["engage/engage_core"]);