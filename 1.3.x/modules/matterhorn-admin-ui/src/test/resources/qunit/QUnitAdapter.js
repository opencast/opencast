/*
QUnitAdapter
Version: 1.0.3

Run qunit tests using JS Test Driver

This provides almost the same api as qunit.

Tests must run sychronously, which means no use of stop and start methods.
You can use jsUnit Clock object to deal with timeouts and intervals:
http://googletesting.blogspot.com/2007/03/javascript-simulating-time-in-jsunit.html

The qunit #main DOM element is not included. If you need to do any DOM manipulation
you need to set it up and tear it down in each test.

*/
(function() {
   
    window.module = function(name, lifecycle) {
        QUnitTestCase = TestCase(name);
		QUnitTestCase.tearDown = function() {};

        if (lifecycle) {
            QUnitTestCase.prototype.setUp = lifecycle.setup || function() {};
            QUnitTestCase.tearDown = lifecycle.teardown || function() {};
        }
    };
    
    window.test = function(name, test) {
		var tearDown = QUnitTestCase.tearDown;
        QUnitTestCase.prototype['test ' + name] = function() {
			try {
				test();
			} catch(ex) {
				throw(ex);
			} finally {
				tearDown();
			}
		};
    };
    
    window.expect = function(count) {
        expectAsserts(count);
    };
    
    window.ok = function(actual, msg) {
        assertTrue(msg ? msg : '', !!actual);
    };
    
    window.equals = function(a, b, msg) {
        assertEquals(msg ? msg : '', b, a);
    };
    
    window.start = window.stop = function() {
        fail('start and stop methods are not available when using JS Test Driver.\n' +
            'Use jsUnit Clock object to deal with timeouts and intervals:\n' + 
            'http://googletesting.blogspot.com/2007/03/javascript-simulating-time-in-jsunit.html.');
    };
    
    window.same = function(a, b, msg) {
        assertTrue(msg ? msg : '', window.equiv(b, a));
    };
    
    window.reset = function() {
    	fail('reset method is not available when using JS Test Driver');
    };

    window.isLocal = function() {
    	return false;
    };
    
    window.QUnit = {
    	equiv: window.equiv,
    	ok: window.ok
    };

	module('Default Module');

})();