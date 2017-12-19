describe('Recipients Step for the Bulk Message Wizard', function () {
    var BulkMessageRecipients, RecipientsResource, Table, $httpBackend;

    beforeEach(module('adminNg'));

    beforeEach(module(function ($provide) {
        $provide.value('Language', {
            configureFromServer: function () {}
        });
    }));

    beforeEach(inject(function (_BulkMessageRecipients_, _RecipientsResource_, _Table_, _$httpBackend_) {
        Table = _Table_;
        BulkMessageRecipients = _BulkMessageRecipients_;
        RecipientsResource = _RecipientsResource_;
        $httpBackend = _$httpBackend_;
    }));

    beforeEach(function () {
        $httpBackend.whenGET('/admin-ng/capture-agents/agents.json')
            .respond('{"results":[]}');
    });

    describe('retrieving recipients', function () {

        describe('for events', function () {
            beforeEach(function () {
                Table.rows = [
                    { id: 5, selected: true },
                    { id: 6, selected: false },
                    { id: 9, selected: true }
                ];
                $httpBackend.expectGET('/admin-ng/event/events/recipients?eventIds=')
                    .respond('{}');
                $httpBackend.flush();
            });

            it('fetches recipients for selected table rows', function () {
                BulkMessageRecipients.reset();
                $httpBackend.expectGET('/admin-ng/event/events/recipients?eventIds=5,9')
                    .respond(JSON.stringify(getJSONFixture('admin-ng/event/events/recipients')));
                $httpBackend.flush();

                expect(BulkMessageRecipients.ud.items.recipients.length).toBe(2);
            });
        });

        describe('for series', function () {
            beforeEach(function () {
                Table.resource = 'series';
                Table.rows = [
                    { id: 5, selected: true },
                    { id: 6, selected: false },
                    { id: 9, selected: true }
                ];
                $httpBackend.expectGET('/admin-ng/event/events/recipients?eventIds=')
                    .respond('{}');
                $httpBackend.flush();
            });

            it('fetches recipients for selected table rows', function () {
                BulkMessageRecipients.reset();
                $httpBackend.expectGET('/admin-ng/series/series/recipients?seriesIds=5,9')
                    .respond(JSON.stringify(getJSONFixture('admin-ng/event/events/recipients')));
                $httpBackend.flush();

                expect(BulkMessageRecipients.ud.items.recipients.length).toBe(2);
            });
        });
    });

    describe('#isValid', function () {

        it('is valid when recipients are set', function () {
            expect(BulkMessageRecipients.isValid()).toBe(false);
            BulkMessageRecipients.ud.items.recipients = [{ id: 81294 }];
            expect(BulkMessageRecipients.isValid()).toBe(true);
        });
    });
});
