describe('Notifications', function () {
    var $httpBackend, Notifications;

    beforeEach(module('adminNg.services'));

    beforeEach(inject(function (_$httpBackend_, _Notifications_) {
        $httpBackend = _$httpBackend_;
        $httpBackend.whenGET('/resources/components.json').respond(JSON.stringify({
            'keyA': 'Value A'
        }));
        Notifications = _Notifications_;
    }));

    it('provides a constructor', function () {
        expect(Notifications.get).toBeDefined();
    });

    describe('#get', function () {

        describe('without a context', function () {
            beforeEach(function () {
                Notifications.add('success', 'TEST');
            });

            it('returns global notifications', function () {
                expect(Object.keys(Notifications.get()).length).toBe(1);
                expect(Notifications.get()[1].type).toEqual('success');
                expect(Notifications.get()[1].message).toEqual('NOTIFICATIONS.TEST');
            });
        });

        describe('with a context', function () {
            beforeEach(function () {
                Notifications.add('success', 'TEST', 'not-global');
                Notifications.add('success', 'TEST2', 'not-global');
            });

            it('returns global notifications', function () {
                expect(Object.keys(Notifications.get('not-global')).length).toBe(2);
                expect(Notifications.get('not-global')[1].type).toEqual('success');
                expect(Notifications.get('not-global')[1].message).toEqual('NOTIFICATIONS.TEST');
            });
        });
    });
});
