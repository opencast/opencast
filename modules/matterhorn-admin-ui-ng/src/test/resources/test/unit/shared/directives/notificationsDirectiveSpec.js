describe('adminNg.directives.adminNgNotifications', function () {
    var $compile, $rootScope, element, Notifications;

    beforeEach(module('adminNg'));
    beforeEach(module('shared/partials/notification.html'));
    beforeEach(module('shared/partials/notifications.html'));

    beforeEach(module(function ($provide) {
        var service = {
            configureFromServer: function () {},
            formatDate: function (val, date) { return date; },
            formatTime: function (val, date) { return date; },
            getLanguageCode: function () { return 'en'; }
        };
        $provide.value('Language', service);
    }));

    beforeEach(inject(function (_$rootScope_, _$compile_, _Notifications_) {
        $compile = _$compile_;
        $rootScope = _$rootScope_;
        Notifications = _Notifications_;
    }));

    beforeEach(function () {
        element = $compile('<div admin-ng-notifications=""></div>')($rootScope);
        $rootScope.$digest();
    });

    it('creates a notifications container', function () {
        expect(element).toBeMatchedBy('ul');
    });

    describe('when adding a notification', function () {
        beforeEach(function () {
            Notifications.add('success', 'LOGGED_IN');
            $rootScope.$digest();
        });

        it('fetches notifications', function () {
            expect(Object.keys(element.find('div').scope().notifications).length).toBe(1);
        });

        it('renders the notifications', function () {
            expect(element.find('li div')).toHaveClass('success');
        });
    });
});
