// Protractor Jasmine end-to-end (e2e) test.
//
// See https://github.com/angular/protractor/blob/master/docs/getting-started.md
//

var EventsPage = require('./events_page').EventsPage,
    page = new EventsPage();

describe('event navigation', function () {

    beforeEach(function () {
        page.get();
        var width = 1200, height = 800;
        browser.driver.manage().window().setSize(width, height);
        page.moreButton.click();
        page.waitFor(page.modal.header);
    });

    it('displays event details', function () {
        expect(page.modal.header.getText()).toContain('Event Details c3a4f68d-14d4-47e2-8981-8eb2fb300d3a');
    });

    describe('navigating to the previous record', function () {

        beforeEach(function () {
            page.modal.previous.click();
        });

        it('stays on the current record', function () {
            expect(page.modal.header.getText()).toContain('Event Details c3a4f68d-14d4-47e2-8981-8eb2fb300d3a');
        });
    });

    describe('navigating to the next record', function () {

        beforeEach(function () {
            page.modal.next.click();
        });

        it('loads on the next record', function () {
            expect(page.modal.header.getText()).toContain('Event Details 30112');
        });

        describe('and then back again', function () {

            beforeEach(function () {
                page.modal.previous.click();
            });

            it('loads the original record', function () {
                expect(page.modal.header.getText()).toContain('Event Details c3a4f68d-14d4-47e2-8981-8eb2fb300d3a');
            });
        });
    });
});
