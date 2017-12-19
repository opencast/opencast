// Protractor Jasmine end-to-end (e2e) test.
//
// See https://github.com/angular/protractor/blob/master/docs/getting-started.md
//

var EventsPage = require('./events_page').EventsPage,
    page = new EventsPage();

describe('events', function () {

    beforeEach(function () {
        page.get();
    });

    afterEach(function () {
        browser.executeScript('window.localStorage.clear();');
    });

    it('shows the events table', function () {
        expect(page.table.isDisplayed()).toBe(true);
    });

    describe('sorting', function () {
        var header, sortingIcon;

        beforeEach(function () {
            header = page.eventsTableHeaders.get(1);
            // Sorts by second column ascending
            header.click();
        });

        it('sorts the events table descendingly', function () {
            sortingIcon = header.element(by.css('i.sort.desc'));
            // the arrow down icon is visible
            expect(sortingIcon.isDisplayed()).toBeTruthy();
        });

        it('sorts the events table ascendingly', function () {
            header.click();
            sortingIcon = header.element(by.css('i.sort.asc'));
            // the arrow up icon is visible
            expect(sortingIcon.isDisplayed()).toBeTruthy();
        });

        it('restores sortering when reloading the page', function () {
            page.get();
            header = page.eventsTableHeaders.get(1);
            sortingIcon = header.element(by.css('i.sort.desc'));
            expect(sortingIcon.isDisplayed()).toBeTruthy();
        });
    });
});
