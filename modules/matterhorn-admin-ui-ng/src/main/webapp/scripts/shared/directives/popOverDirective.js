angular.module("adminNg.directives")
.directive("popOver", ["$timeout", function ($timeout) {
    return {
        restrict: "A",
        link: function (scope, element) {
            var callToHide;

            element.mouseenter(function(){
                var popover = element.next(".js-popover");
                // make element visible in DOM,
                // otherwise positioning will not work
                popover.removeClass("is-hidden").removeClass("popover-left").removeClass("popover-right");

                // uses jquery-ui's #position
                popover.position({
                    my: "left+20 top-10",
                    at: "right",
                    of: element
                });

                // add modifier
                if (popover.position().left < 0) {
                    popover.addClass("popover-left");
                } else {
                    popover.addClass("popover-right");
                }

                popover.mouseenter(function(){
                    if(callToHide){
                        // prevent hiding the popover bubble
                        // because user has entered it
                        $timeout.cancel(callToHide);
                    }
                });

                popover.on("mouseleave click", function(){
                    popover.addClass("is-hidden");
                });
            });

            element.mouseleave(function(){
                // don't hide immediately to allow user
                // to reach the popover bubble
                callToHide = $timeout(function(){
                    var popover = element.next(".js-popover");
                    popover.addClass("is-hidden");
                }, 200);
            });

            element.click(function(){
                // no-op
                return false;
            });
        }
    };
}]);
