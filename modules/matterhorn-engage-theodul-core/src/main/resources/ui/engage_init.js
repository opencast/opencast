/*global requirejs*/
requirejs.config({
  baseUrl: 'js/lib',
  paths: {
    engage: '../engage',
    plugins: '/engage/plugin/*/static'
  },
  shim: {
    'bootstrap' : {
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
    }
  }
});
var PLUGIN_MANAGER_PATH = '/engage/theodul/manager/list.json';
var PLUGIN_PATH = '/engage/theodul/plugin/';
//start core logic
require(["engage/engage_core"]);