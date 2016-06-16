describe('Message Step for the Email Template Wizard', function () {
    var EmailtemplateMessage, $httpBackend, $location, element;

    beforeEach(module('adminNg'));
    beforeEach(module(function ($provide) {
        $provide.value('Language', { configureFromServer: function () {} });
    }));

    beforeEach(inject(function (_EmailtemplateMessage_, _$httpBackend_, _$location_) {
        EmailtemplateMessage = _EmailtemplateMessage_;
        $httpBackend = _$httpBackend_;
        $location = _$location_;
    }));

    beforeEach(function () {
        jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';
        $httpBackend.whenGET('/admin-ng/resources/EMAIL_TEMPLATE_NAMES.json')
            .respond(JSON.stringify(getJSONFixture('admin-ng/resources/EMAIL_TEMPLATE_NAMES.json')));
        $httpBackend.whenGET('/email/variables.json')
            .respond(JSON.stringify(getJSONFixture('email/variables.json')));
    });

    it('fetches template names', function () {
        expect(EmailtemplateMessage.names.$resolved).toBe(false);
        $httpBackend.flush();
        expect(EmailtemplateMessage.names.$resolved).toBe(true);
    });

    describe('#isValid', function () {

        it('is valid when a unique name, a subject and a body is set', function () {
            spyOn($location, 'search').and.returnValue({ action: 'add' });
            expect(EmailtemplateMessage.isValid()).toBe(false);

            EmailtemplateMessage.ud.name = 'Template Name X';
            EmailtemplateMessage.ud.subject = 'My Subject';
            EmailtemplateMessage.ud.message = 'My Body';

            expect(EmailtemplateMessage.isValid()).toBe(false);

            $httpBackend.flush();

            EmailtemplateMessage.ud.name = 'Template Name 1';

            expect(EmailtemplateMessage.isValid()).toBe(false);

            EmailtemplateMessage.ud.name = 'Template Name X';

            expect(EmailtemplateMessage.isValid()).toBe(true);
        });
    });

    describe('#updatePreview', function () {
        beforeEach(function () {
            $httpBackend.whenPOST('/email/demotemplate')
                .respond(JSON.stringify({ body: 'Foobar' }));
            EmailtemplateMessage.isValid();
            $httpBackend.flush();
        });

        describe('with a valid form', function () {
            beforeEach(function () {
                EmailtemplateMessage.ud.subject = 'My Subject';
                EmailtemplateMessage.ud.message = 'My Body';
                EmailtemplateMessage.ud.name = 'Template Name X';
            });

            it('retrieves a preview for the message', function () {
                EmailtemplateMessage.updatePreview();
                $httpBackend.flush();

                expect(EmailtemplateMessage.preview.body).toContain('Foobar');
            });
        });

        describe('without a valid form', function () {

            it('retrieves a preview for the message', function () {
                EmailtemplateMessage.updatePreview();

                expect(EmailtemplateMessage.preview).toBeUndefined();
            });
        });
    });

    describe('#insertVariable', function () {
        beforeEach(function () {
            element = $('<textarea ng-model="wizard.step.ud.message"></textarea>');
            $('body').append(element);
        });

        afterEach(function () {
            element.remove();
        });

        describe('without a cursor position', function () {
            beforeEach(function () {
                EmailtemplateMessage.ud.message = 'string';
            });

            it('inserts text at the beginning', function () {
                EmailtemplateMessage.insertVariable('new ');

                expect(EmailtemplateMessage.ud.message).toEqual('new string');
            });
        });

        describe('without existing text', function () {

            it('creates text', function () {
                EmailtemplateMessage.insertVariable('new ');

                expect(EmailtemplateMessage.ud.message).toEqual('new ');
            });
        });

        describe('with a cursor position', function () {
            beforeEach(function () {
                EmailtemplateMessage.ud.message = 'string';
                element.val('string');
                element[0].setSelectionRange(6, 6);
            });

            it('inserts text at the current cursor position', function () {
                EmailtemplateMessage.insertVariable(' new');

                expect(EmailtemplateMessage.ud.message).toEqual('string new');
            });
        });
    });
});
