// Protractor Jasmine end-to-end (e2e) test.
//
// See https://github.com/angular/protractor/blob/master/docs/getting-started.md
//

var EventsPage = require('./events_page').EventsPage;
var page = new EventsPage();

describe('event tabs', function () {

    beforeEach(function () {
        page.get();
    });

    it('activates the events tab by default', function () {
        expect(page.tabs.events.getAttribute('class')).toContain('active');
        expect(page.tabs.series.getAttribute('class')).not.toContain('active');
    });

    describe('navigating to series', function () {

        beforeEach(function () {
            page.tabs.series.click();
        });

        it('activates the series tab', function () {
            expect(page.tabs.events.getAttribute('class')).not.toContain('active');
            expect(page.tabs.series.getAttribute('class')).toContain('active');
        });
    });
});
