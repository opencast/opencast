exports.EventsPage = function () {
    this.get = function () {
        browser.get('#/events/events');
    };

    this.languageIcon = element(by.css('#lang-dd > .lang'));
    this.languageDropDown = element(by.css('#lang-dd.active > ul.dropdown-ul'));
    this.languageItemEnglish = element(by.css('#lang-dd > ul.dropdown-ul i.lang.en'));
    this.languageItemGerman  = element(by.css('#lang-dd > ul.dropdown-ul i.lang.de'));
    this.title = element(by.css('.main-view h1'));
};
