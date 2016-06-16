describe('Message Step in Bulk Message Wizard', function () {
    var BulkMessageMessage, BulkMessageRecipients, $httpBackend, element;

    beforeEach(module('adminNg'));
    beforeEach(module(function ($provide) {
        $provide.value('Language', {
            configureFromServer: function () {},
            formatDate:     function (val, time) { return time; },
        });
    }));

    beforeEach(inject(function (_BulkMessageMessage_, _BulkMessageRecipients_, _$httpBackend_) {
        BulkMessageMessage = _BulkMessageMessage_;
        BulkMessageRecipients = _BulkMessageRecipients_;
        $httpBackend = _$httpBackend_;
    }));

    beforeEach(function () {
        jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';
        $httpBackend.whenGET('/email/variables.json')
            .respond(JSON.stringify(getJSONFixture('email/variables.json')));
        $httpBackend.whenGET('/email/templates.json')
            .respond(JSON.stringify(getJSONFixture('email/templates.json')));
        $httpBackend.whenGET('/admin-ng/event/events/recipients?eventIds=')
            .respond(JSON.stringify(getJSONFixture('admin-ng/event/events/recipients')));
    });

    it('fetches templates', function () {
        expect(BulkMessageMessage.templates.$resolved).toBe(false);
        $httpBackend.flush();
        expect(BulkMessageMessage.templates.$resolved).toBe(true);
    });

    describe('#isValid', function () {

        it('is valid when a subject and a body is set', function () {
            expect(BulkMessageMessage.isValid()).toBe(false);

            BulkMessageMessage.ud.subject = 'My Subject';
            BulkMessageMessage.ud.message = 'My Body';

            expect(BulkMessageMessage.isValid()).toBe(true);
        });
    });

    describe('#applyTemplate', function () {
        beforeEach(function () {
            BulkMessageMessage.ud.email_template = { id: 1101 };
        });

        it('fetches the template and applies its values', function () {
            $httpBackend.expectGET('/email/template/1101')
                .respond(JSON.stringify(getJSONFixture('email/template/1101')));
            BulkMessageMessage.applyTemplate();
            $httpBackend.flush();

            expect(BulkMessageMessage.ud.subject).toEqual('Bar');
        });
    });

    describe('#updatePreview', function () {
        beforeEach(function () {
            $httpBackend.whenPOST('/email/preview/3812')
                .respond(JSON.stringify({ body: 'Foobar' }));
            BulkMessageMessage.isValid();
            $httpBackend.flush();
        });

        describe('with a valid form', function () {
            beforeEach(function () {
                BulkMessageMessage.ud.subject = 'My Subject';
                BulkMessageMessage.ud.message = 'My Body';
                BulkMessageMessage.ud.email_template = { id: 3812 };
            });

            it('renders a preview', function () {
                BulkMessageMessage.updatePreview();
                $httpBackend.flush();

                expect(BulkMessageMessage.preview.body).toContain('Foobar');
            });

            it('renders a preview with an audit trail flag', function () {
                BulkMessageRecipients.ud.audit_trail = true;
                BulkMessageMessage.updatePreview();
                $httpBackend.flush();

                expect(BulkMessageMessage.preview.body).toContain('Foobar');
            });
        });

        describe('without a valid form', function () {

            it('retrieves a preview for the message', function () {
                BulkMessageMessage.updatePreview();

                expect(BulkMessageMessage.preview).toBeUndefined();
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
                BulkMessageMessage.ud.message = 'string';
            });

            it('inserts text at the beginning', function () {
                BulkMessageMessage.insertVariable('new ');

                expect(BulkMessageMessage.ud.message).toEqual('new string');
            });
        });

        describe('without existing text', function () {

            it('creates text', function () {
                BulkMessageMessage.insertVariable('new ');

                expect(BulkMessageMessage.ud.message).toEqual('new ');
            });
        });

        describe('with a cursor position', function () {
            beforeEach(function () {
                BulkMessageMessage.ud.message = 'string';
                element.val('string');
                element[0].setSelectionRange(6, 6);
            });

            it('inserts text at the current cursor position', function () {
                BulkMessageMessage.insertVariable(' new');

                expect(BulkMessageMessage.ud.message).toEqual('string new');
            });
        });
    });
});
