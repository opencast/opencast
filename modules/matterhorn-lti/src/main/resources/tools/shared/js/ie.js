(function ($) {
    "use strict";

   // Detecting IE
   var oldIE;
   if ($('html').is('.ie6, .ie7, .ie8, .ie9, .ie10')) {
        oldIE = true;
    }

    if (oldIE) {
        $("#submitButton").attr('disabled','disabled');
    } else {
        $("#ie_warning").hide();
    }
}(jQuery));
