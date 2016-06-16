// Protractor Jasmine end-to-end (e2e) test.
//
// See https://github.com/angular/protractor/blob/master/docs/getting-started.md
//

var page  = new (require('./events_page').EventsPage)(),
    mocks = require('./mocks');

describe('event metadata', function () {

    beforeEach(function () {
        if (browser.useMocks) {
            browser.addMockModule('httpBackendMock', mocks.httpBackendMock);
        }
        page.getDetails('40518');
        page.waitFor(page.modal.header);
        page.modal.metadataTab.click();
    });

    it('has a title', function () {
        expect(page.metadata.editableCell.get(0).getText()).toEqual('•mock• Test Title');
    });

    it('has presenters', function () {
        expect(page.metadata.editableCell.get(1).getText()).toContain('Matt Smith');
        expect(page.metadata.editableCell.get(1).getText()).toContain('Chuck Norris');
    });

    describe('editing a text field', function () {

        beforeEach(function () {
            page.metadata.editableCell.get(0).click();
        });

        it('becomes editable when clicked', function () {
            expect(page.metadata.savedIcon.get(0).getAttribute('class')).not.toContain('active');
            expect(page.metadata.editableCell.get(0).getText()).toEqual('');

            expect(page.metadata.editableInput.get(0).isDisplayed()).toBeTruthy();
        });

        it('saves changes on blur', function () {
            page.metadata.editableInput.get(0).sendKeys(' Edited');
            page.modal.header.click();

            expect(page.metadata.savedIcon.get(0).getAttribute('class')).toContain('active');
            expect(page.metadata.editableCell.get(0).getText()).toEqual('•mock• Test Title Edited');
        });

        it('saves changes on submit', function () {
            page.metadata.editableInput.get(0).sendKeys(' Edited');
            page.pressEnter();

            expect(page.metadata.savedIcon.get(0).getAttribute('class')).toContain('active');
            expect(page.metadata.editableCell.get(0).getText()).toEqual('•mock• Test Title Edited');
        });

        it('restores changes on ESC', function () {
            page.metadata.editableInput.get(0).sendKeys(' Edited');
            page.pressEscape();

            expect(page.metadata.savedIcon.get(0).getAttribute('class')).not.toContain('active');
            expect(page.metadata.editableCell.get(0).getText()).toEqual('•mock• Test Title');
        });
    });

    describe('editing a multi select field', function () {

        beforeEach(function () {
            page.metadata.editableCell.get(1).click();
        });

        it('becomes editable when clicked', function () {
            expect(page.metadata.savedIcon.get(1).getAttribute('class')).not.toContain('active');
            expect(page.metadata.editableCell.get(1).getText()).not.toContain('matt.smith');

            expect(page.metadata.editableInput.get(1).isDisplayed()).toBeTruthy();
        });

        it('shows current values', function () {
            expect(page.metadata.editableCell.get(1).getText()).toContain('Matt Smith');
            expect(page.metadata.editableCell.get(1).getText()).toContain('Chuck Norris');
        });

        it('saves changes', function () {
            page.metadata.editableInput.get(1).sendKeys('franz.kafka');
            page.pressEnter();

            var values = page.metadata.editableCell.get(1).getText();
            expect(values).toContain('Matt Smith');
            expect(values).toContain('Chuck Norris');
            expect(values).toContain('Franz Kafka');

            expect(page.metadata.savedIcon.get(1).getAttribute('class')).toContain('active');
        });

        it('discards changes when pressing ESC', function () {
            page.metadata.editableInput.get(1).sendKeys('franz.kafka');
            page.pressEscape();

            expect(page.metadata.savedIcon.get(1).getAttribute('class')).not.toContain('active');

            var values = page.metadata.editableCell.get(1).getText();
            expect(values).toContain('Matt Smith');
            expect(values).toContain('Chuck Norris');
            expect(values).not.toContain('Franz Kafka');
        });

        it('allows removal of a value', function () {
            expect(page.metadata.editableCell.get(1).getText()).toContain('Chuck Norris');
            page.metadata.presenterDelete('Chuck Norris').click();
            expect(page.metadata.editableCell.get(1).getText()).toContain('Matt Smith');
            expect(page.metadata.editableCell.get(1).getText()).not.toContain('Chuck Norris');
        });
    });
});
