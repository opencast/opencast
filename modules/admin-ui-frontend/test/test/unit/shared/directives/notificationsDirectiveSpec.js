describe('adminNg.directives.adminNgNotifications', function () {
    var $compile, $rootScope, element, Notifications, $httpBackend;

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

    beforeEach(module(function ($provide) {
    var service = {
        getUser: function () {
            var user = {org: { properties: {
                "admin.notification.duration.error": -10,
                "admin.notification.duration.success": 50,
                "admin.notification.duration.warning": 50
            }}};
            return {$promise: {then: function (fn) { fn(user); return { catch: function() {} }; }}};
        }
    };
    $provide.value('AuthService', service);
    }));

    beforeEach(inject(function (_$httpBackend_, _$rootScope_, _$compile_, _Notifications_) {
        $httpBackend = _$httpBackend_;
        $compile = _$compile_;
        $rootScope = _$rootScope_;
        Notifications = _Notifications_;
    }));

    beforeEach(function () {
        $httpBackend.whenGET('modules/events/partials/index.html').respond('');
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
