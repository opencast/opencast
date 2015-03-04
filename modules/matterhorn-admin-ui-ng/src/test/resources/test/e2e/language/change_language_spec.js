// Protractor Jasmine end-to-end (e2e) test.
//
// See https://github.com/angular/protractor/blob/master/docs/getting-started.md
//

var EventsPage = require('./page_objects').EventsPage;
var page = new EventsPage();

describe('display language', function () {

    beforeEach(function () {
        page.get();
        page.languageIcon.click();
    });

    it('should allow the user to switch language', function () {
        expect(page.languageIcon.isPresent()).toBe(true);
    });

    it('should allow the user to choose a language', function () {
        expect(page.languageDropDown.getText()).toContain('English');
        expect(page.languageDropDown.getText()).toContain('Deutsch');
        expect(page.languageDropDown.getText()).toContain('日本語');
        expect(page.languageDropDown.getText()).toContain('Français');
        expect(page.languageDropDown.getText()).toContain('Norsk');
    });

    describe('changing the language', function () {

        describe('to English', function () {

            beforeEach(function () {
                page.languageItemEnglish.click();
            });

            it('displays English text', function () {
                expect(page.title.getText()).toEqual('Events');
            });

            it('reflects the selection in the language icon', function () {
                expect(page.languageIcon.getAttribute('class')).toContain('en');
                expect(page.languageIcon.getAttribute('class')).not.toContain('de');
            });
        });

        describe('to German', function () {

            beforeEach(function () {
                page.languageItemGerman.click();
            });

            it('displays German text', function () {
                expect(page.title.getText()).toEqual('Event Übersicht');
            });

            it('reflects the selection in the language icon', function () {
                expect(page.languageIcon.getAttribute('class')).toContain('de');
                expect(page.languageIcon.getAttribute('class')).not.toContain('en');
            });
        });
    });
});
