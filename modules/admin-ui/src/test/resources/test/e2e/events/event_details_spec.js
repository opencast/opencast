// Protractor Jasmine end-to-end (e2e) test.
//
// See https://github.com/angular/protractor/blob/master/docs/getting-started.md
//

var page  = new (require('./events_page').EventsPage)(),
    mocks = require('./mocks');

describe('event details', function () {

    beforeEach(function () {
        page.get();
    });

    it('shows the event details button', function () {
        expect(page.moreButton.isDisplayed()).toBe(true);
    });

    describe('opening details', function () {

        beforeEach(function () {
            page.moreButton.click();
            page.waitFor(page.modal.header);
        });

        it('displays event details', function () {
            expect(page.modal.header.getText()).toContain('Event Details c3a4f68d-14d4-47e2-8981-8eb2fb300d3a');
            expect(page.modal.content.getText()).toContain('Publication Status');
        });

        it('changes the URL', function () {
            expect(page.currentUrl()).toContain('c3a4f68d-14d4-47e2-8981-8eb2fb300d3a');
        });

        describe('navigating modal tabs', function () {

            beforeEach(function () {
                page.modal.metadataTab.click();
            });

            it('shows the content of the tab', function () {
                expect(page.modal.content.getText()).toContain('Event Details');
                expect(page.modal.content.getText()).not.toContain('Publication Status');
            });

            it('is able to navigate back', function () {
                page.modal.publicationsTab.click();
                expect(page.modal.content.getText()).toContain('Publication Status');
                expect(page.modal.content.getText()).not.toContain('Event Details');
            });

            it('changes the URL', function () {
                expect(page.currentUrl()).toContain('metadata');
            });
        });

        describe('and closing the modal', function () {

            it('removes the details', function () {
                expect(page.modal.modal.isPresent()).toBe(true);
                page.modal.closeButton.click();
                expect(page.modal.modal.isPresent()).toBe(false);
            });

            it('changes the URL', function () {
                page.modal.closeButton.click();
                expect(page.currentUrl()).not.toContain('c3a4f68d-14d4-47e2-8981-8eb2fb300d3a');
            });
        });
    });

    describe('direct access via URL', function () {

        beforeEach(function () {
            page.getDetails('c3a4f68d-14d4-47e2-8981-8eb2fb300d3a');
            page.waitFor(page.modal.header);
        });

        it('displays event details', function () {
            expect(page.modal.header.getText()).toContain('Event Details c3a4f68d-14d4-47e2-8981-8eb2fb300d3a');
            expect(page.modal.content.getText()).toContain('Publication Status');
        });
    });
});
