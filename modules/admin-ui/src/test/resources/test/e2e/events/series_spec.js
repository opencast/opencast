// Protractor Jasmine end-to-end (e2e) test.
//
// See https://github.com/angular/protractor/blob/master/docs/getting-started.md
//

var SeriesPage = require('./series_page.js').SeriesPage,
    page = new SeriesPage();

describe('events', function () {

    beforeEach(function () {
        page.get();
    });

    afterEach(function () {
        browser.executeScript('window.localStorage.clear();');
    });

    it('shows the series table', function () {
        expect(page.table.isDisplayed()).toBe(true);
    });

    describe('sorting', function () {
        var header;

        beforeEach(function () {
            header = page.seriesTableHeaders.get(2);
            // Sorts by third column ascending
            header.click();
        });

        it('sorts the series table descendingly', function () {
            expect(header.element(by.css('i.sort.desc')).isDisplayed()).toBeTruthy();
        });

        it('sorts the series table ascendingly', function () {
            expect(header.element(by.css('i.sort.desc')).isDisplayed()).toBeTruthy();
            header.click();
            expect(header.element(by.css('i.sort.asc')).isDisplayed()).toBeTruthy();
        });

        it('restores sortering when reloading the page', function () {
            expect(header.element(by.css('i.sort.desc')).isDisplayed()).toBeTruthy();
            page.get();
            header = page.seriesTableHeaders.get(2);
            expect(header.element(by.css('i.sort.desc')).isDisplayed()).toBeTruthy();
        });
    });


    xdescribe('filtering results', function () {

        beforeEach(function () {
            page.selectStatus('Ingesting');
            page.selectSource('Twitter');
            page.filterBy('Something');
        });

        it('displays the selected filters', function () {
            expect(page.filterStatus.getText()).toContain('Status: Ingesting');
            expect(page.filterStatus.getText()).toContain('Sources: Twitter');
            expect(page.filterStatus.getText()).toContain('textSearch: Something');
        });

        describe('clearing the filters', function () {

            beforeEach(function () {
                page.clearAllButton.click();
            });

            it('clears the filter when asked to', function () {
                expect(page.filterStatus.getText()).not.toContain('Status: Ingesting');
                expect(page.filterStatus.getText()).not.toContain('Sources: Twitter');
                expect(page.filterStatus.getText()).not.toContain('textSearch: Something');
            });
        });
    });
});
