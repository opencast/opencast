describe('Signature API Resource', function () {
    var SignatureResource, $httpBackend;

    beforeEach(module('adminNg.resources'));
    beforeEach(module('ngResource'));

    beforeEach(inject(function (_$httpBackend_, _SignatureResource_) {
        $httpBackend  = _$httpBackend_;
        SignatureResource = _SignatureResource_;
    }));

    beforeEach(function () {
        jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';
    });

    describe('#get', function () {
        it('calls the series service', function () {
            $httpBackend.expectGET('/admin-ng/user-settings/signature')
                .respond(JSON.stringify(getJSONFixture('admin-ng/user-settings/signature')));
            SignatureResource.get();
            $httpBackend.flush();
        });

        it('flattens the JSON data received', function () {
            $httpBackend.whenGET('/admin-ng/user-settings/signature')
                .respond(JSON.stringify(getJSONFixture('admin-ng/user-settings/signature')));
            var data = SignatureResource.get();
            $httpBackend.flush();
            expect(data.name).toEqual('admin');
            expect(data.existsOnServer).toBeTruthy();
            expect(data.sender.address).toEqual('adi@panter.ch');
            expect(data.sender.name).toEqual('test');
            expect(data.signature).toEqual('test');
        });

        it('returns empty object when 404 is encountered', function () {
            $httpBackend.whenGET('/admin-ng/user-settings/signature')
                // This should be .respond(404, '');
                // But then data will never be resolved.
                .respond('not available');
            var data = SignatureResource.get();
            $httpBackend.flush();
            expect(data.existsOnServer).toBeFalsy();
            expect(data.replyTo).toExist();
        });
    });

    describe('#create', function () {

        it('sends new signature data', function () {
            $httpBackend.expectPOST('/admin-ng/user-settings/signature', function (data) {
                var payload = angular.fromJson($.deparam(data));
                expect(payload.username).toEqual('admin');
                expect(payload.reply_name).toEqual('adi');
                expect(payload.from_name).toEqual('adi');
                expect(payload.from_address).toEqual('adi@panter.ch');
                expect(payload.text).toEqual('test signature');
                return true;
            }).respond(200);
            SignatureResource.save({
                sender: {
                    address: 'adi@panter.ch'
                },
                username: 'admin',
                signature: 'test signature',
                name: 'adi'
            });
            $httpBackend.flush();
        });
    });


    describe('#update', function () {

        it('updates an existing record', function () {
            $httpBackend.expectPUT('/admin-ng/user-settings/signature/1501', function (data) {
                var payload = angular.fromJson($.deparam(data));
                expect(payload.username).toEqual('admin');
                expect(payload.reply_name).toEqual('adi');
                expect(payload.from_name).toEqual('adi');
                expect(payload.from_address).toEqual('adi@panter.ch');
                expect(payload.text).toEqual('test signature');
                return true;
            }).respond(200);
            SignatureResource.update({
                id: 1501,
                sender: {
                    address: 'adi@panter.ch'
                },
                username: 'admin',
                signature: 'test signature',
                name: 'adi'
            });
            $httpBackend.flush();
        });
    });
});
