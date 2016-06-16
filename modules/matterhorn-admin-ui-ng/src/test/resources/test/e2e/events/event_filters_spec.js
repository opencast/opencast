// Protractor Jasmine end-to-end (e2e) test.
//
// See https://github.com/angular/protractor/blob/master/docs/getting-started.md
//

var EventsPage = require('./events_page').EventsPage,
    page = new EventsPage();

xdescribe('event filters', function () {

    beforeEach(function () {
        var width = 1200, height = 800;
        browser.driver.manage().window().setSize(width, height);
        page.get();
    });

    afterEach(function () {
        browser.executeScript('window.localStorage.clear();');
    });

    describe('filtering results', function () {

        beforeEach(function () {
            page.filterByText('Something');
            page.filterBy('Status', 'Ingesting');
            page.filterBy('Source', 'Twitter');
        });

        it('displays the selected filters', function () {
            expect(page.filter.field.getText()).toContain('Status: Ingesting');
            expect(page.filter.field.getText()).toContain('Source: Twitter');
        });

        it('clears the filter when asked to', function () {
            page.filter.clearButton.click();
            expect(page.filter.field.getText()).not.toContain('Status: Ingesting');
            expect(page.filter.field.getText()).not.toContain('Sources: Twitter');
        });

        it('restores filters when reloading the page', function () {
            page.get();
            expect(page.filter.field.getText()).toContain('Status: Ingesting');
            expect(page.filter.field.getText()).toContain('Source: Twitter');
            expect(page.filter.textFilter.getAttribute('value')).toContain('Something');
        });
    });

    describe('filter profiles', function () {
        beforeEach(function () {
            page.filterBy('Status', 'Ingesting');
            page.filterBy('Source', 'Twitter');

            page.profile.menuButton.click();
        });

        it('allows saving the current filters', function () {
            expect(page.profile.profiles.getText()).not.toContain('My Filter');

            expect(page.profile.nameField.isDisplayed()).toBe(false);
            page.profile.formButton.click();

            expect(page.profile.nameField.isDisplayed()).toBe(true);

            page.profile.nameField.sendKeys('My Filter');
            page.profile.saveButton.click();

            page.profile.menuButton.click();

            expect(page.profile.profiles.getText()).toContain('My Filter');
            expect(page.profile.activeProfile.getText()).toEqual('My Filter');
        });

        describe('with a previous filter', function () {
            beforeEach(function () {
                page.profile.formButton.click();
                page.profile.nameField.sendKeys('Twitter');
                page.profile.saveButton.click();

                page.filterBy('Status', 'Recording');
                page.filterBy('Source', 'Facebook');
            });

            it('saves a new profile', function () {
                page.profile.menuButton.click();
                page.profile.formButton.click();
                page.profile.nameField.sendKeys('Facebook');
                page.profile.saveButton.click();
                page.profile.menuButton.click();

                expect(page.profile.profiles.getText()).toContain('Twitter');
                expect(page.profile.profiles.getText()).toContain('Facebook');
                expect(page.profile.activeProfile.getText()).toEqual('Facebook');
            });
        });

        describe('loading a profile', function () {
            beforeEach(function () {
                page.profile.formButton.click();
                page.profile.nameField.sendKeys('Twitter');
                page.profile.saveButton.click();

                page.filterBy('Status', 'Recording');
                page.filterBy('Source', 'Facebook');

                page.profile.menuButton.click();
                page.profile.formButton.click();
                page.profile.nameField.sendKeys('Facebook');
                page.profile.saveButton.click();
                page.profile.menuButton.click();
            });

            it('restores saved filters', function () {
                expect(page.filter.field.getText()).toContain('Status: Recording');
                expect(page.filter.field.getText()).toContain('Source: Facebook');

                element(by.linkText('Twitter')).click();

                expect(page.filter.field.getText()).toContain('Status: Ingesting');
                expect(page.filter.field.getText()).toContain('Source: Twitter');
            });

            it('persists across page reload', function () {
                element(by.linkText('Twitter')).click();

                expect(page.filter.field.getText()).toContain('Status: Ingesting');
                expect(page.filter.field.getText()).toContain('Source: Twitter');

                page.get();

                expect(page.filter.field.getText()).toContain('Status: Ingesting');
                expect(page.filter.field.getText()).toContain('Source: Twitter');

                page.profile.menuButton.click();

                expect(page.profile.profiles.getText()).toContain('Twitter');
                expect(page.profile.profiles.getText()).toContain('Facebook');
            });
        });

        describe('editing a profile', function () {
            beforeEach(function () {
                page.profile.formButton.click();
                page.profile.nameField.sendKeys('Twitter');
                page.profile.saveButton.click();

                page.filterBy('Status', 'Recording');
                page.filterBy('Source', 'Facebook');

                page.profile.menuButton.click();
                page.profile.formButton.click();
                page.profile.nameField.sendKeys('Facebook');
                page.profile.saveButton.click();
                page.profile.menuButton.click();
            });

            it('renames', function () {
                page.profile.editButtons.get(1).click();
                page.profile.nameField.sendKeys(' Edited');
                page.profile.saveButton.click();
                page.profile.menuButton.click();

                expect(page.profile.profiles.getText()).toContain('Twitter');
                expect(page.profile.profiles.getText()).toContain('Facebook Edited');
            });
        });
    });
});
