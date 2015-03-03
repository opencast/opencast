/*global requirejs*/
requirejs.config({
    baseUrl: 'src/',
    paths: {
        require: 'test/resources/js/lib/require',
        jquery: 'test/resources/js/lib/jquery',
        underscore: 'test/resources/js/lib/underscore',
        backbone: 'test/resources/js/lib/backbone',
        bootbox: 'test/resources/js/lib/bootbox',
        basil: 'test/resources/js/lib/basil',
        bootstrap: 'test/resources/js/lib/bootstrap',
        bowser: 'test/resources/js/lib/bowser',
        jquery_mobile: 'test/resources/js/lib/jquery.mobile',
        moment: 'test/resources/js/lib/moment',
        mousetrap: 'test/resources/js/lib/mousetrap',
        engage: 'test/resources/js/engage'
    },
    shim: {
        "bootstrap": {
            deps: ["jquery"],
            exports: "Bootstrap"
        },
        "backbone": {
            deps: ["underscore", "jquery"],
            exports: "Backbone"
        },
        "underscore": {
            exports: "_"
        },
        "jquery.mobile": {
            deps: ["jquery"]
        },
        "mousetrap": {
            exports: "Mousetrap"
        },
        "moment": {
            exports: "Moment"
        },
        "basil": {
            exports: "Basil"
        },
        "bootbox": {
            exports: "Bootbox"
        }
    }
});

