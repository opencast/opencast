describe('Email Template API Resource', function () {
    var EmailTemplatesResource, EmailTemplateResource, $httpBackend;

    beforeEach(module('adminNg'));

    beforeEach(module(function ($provide) {
        $provide.value('Language', {
            configureFromServer: function () {},
            formatDate: function (val, date) { return date; }
        });
    }));

    beforeEach(inject(function (_$httpBackend_, _EmailTemplatesResource_, _EmailTemplateResource_) {
        $httpBackend  = _$httpBackend_;
        EmailTemplateResource = _EmailTemplateResource_;
        EmailTemplatesResource = _EmailTemplatesResource_;
    }));

    beforeEach(function () {
        jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';
    });

    describe('#query', function () {

        it('calls the email template service', function () {
            $httpBackend.expectGET('/email/templates.json')
                .respond(JSON.stringify(getJSONFixture('email/templates.json')));
            EmailTemplatesResource.query();
            $httpBackend.flush();
        });

        it('flattens the JSON data received', function () {
            $httpBackend.whenGET('/email/templates.json')
                .respond(JSON.stringify(getJSONFixture('email/templates.json')));
            var data = EmailTemplatesResource.query();
            $httpBackend.flush();

            expect(data.rows.length).toBe(1);
            expect(data.rows[0].name).toEqual('•mock• New Template');
        });
    });

    describe('#get', function () {

        it('calls the email template service', function () {
            $httpBackend.expectGET('/email/template/819').respond(200, '{}');
            EmailTemplateResource.get({ id: 819 });
            $httpBackend.flush();
        });

        it('maps the retrieved values', function () {
            $httpBackend.whenGET('/email/template/819')
                .respond(JSON.stringify({ name: 'My Template' }));
            var data = EmailTemplateResource.get({ id: 819 });
            $httpBackend.flush();

            expect(data.name).toEqual('My Template');
        });
    });

    describe('#save', function () {

        it('calls the email template service', function () {
            $httpBackend.expectPOST('/email/template').respond(200);
            EmailTemplateResource.save({}, { message: {
                name: 'test',
                subject: 'subject A',
                message: 'my message.'
            }});
            $httpBackend.flush();
        });

        it('handles empty payloads gracefully', function () {
            $httpBackend.expectPOST('/email/template').respond(200);
            EmailTemplateResource.save();
            $httpBackend.flush();
        });
    });

    describe('#update', function () {

        it('calls the email template service', function () {
            $httpBackend.expectPUT('/email/template/631').respond(200);
            EmailTemplateResource.update({ id: 631 }, { message: {
                name: 'test',
                subject: 'subject A',
                message: 'my message.'
            }});
            $httpBackend.flush();
        });

        it('handles empty payloads gracefully', function () {
            $httpBackend.expectPUT('/email/template/631').respond(200);
            EmailTemplateResource.update({ id: 631 }, {});
            $httpBackend.flush();
        });
    });
});
