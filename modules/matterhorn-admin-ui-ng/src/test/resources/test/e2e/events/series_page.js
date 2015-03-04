// Set up the Page Object
//
// See https://code.google.com/p/selenium/wiki/PageObjects
//
exports.SeriesPage = function () {
    this.get = function () {
        browser.get('#/events/series');
    };
    this.table = element(by.css('table'));
    this.seriesTableHeaders = element.all(by.css('th'));
};
