angular.module('adminNg.directives')
.directive('adminNgNav', function () {
    return {
        restrict: 'E',
        replace: true,
        templateUrl: 'shared/partials/mainNav.html',
        link: function (scope, element) {
            // Menu roll up
            var menu = element.find('#roll-up-menu'),
                marginTop = element.height() + 1,
                isMenuOpen = false;

            scope.toggleMenu = function () {
                var menuItems = element.find('#nav-container'),
                    mainView = angular.element('.main-view'),
                    mainViewLeft = '130px';
                if (isMenuOpen) {
                    menuItems.animate({opacity: 0}, 50, function () {
                        $(this).css('display', 'none');
                        menu.animate({opacity: 0}, 50, function () {
                            $(this).css('overflow', 'visible');
                            mainView.animate({marginLeft: '20px'}, 100);
                        });
                        isMenuOpen = false;
                    });
                } else {
                    mainView.animate({marginLeft: mainViewLeft}, 100, function () {
                        menu.animate({marginTop: marginTop, opacity: 1}, 50, function () {
                            $(this).css('overflow', 'visible');
                            menuItems.animate({opacity: 1}, 50, function () {
                                $(this).css('display', 'block');
                                menu.css('margin-right', '20px');
                            });
                            isMenuOpen = true;
                        });
                    });
                }
            };
        }
    };
});
