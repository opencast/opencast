var page = new (require('./event_creation_page').NewEventPage)(),
    mocks = require('./mocks');

describe('event creation', function () {

    beforeEach(function () {
        if (browser.useMocks) {
            browser.addMockModule('httpBackendMock', mocks.httpBackendMock);
        }
        page.get();
        page.newEventButton.click();
        page.waitFor(page.wizard.header);
    });

    it('starts with the metadata tab', function () {
        expect(page.wizard.activeStep.getText()).toEqual('Metadata');
        expect(page.wizard.nextButton.getAttribute('class')).toContain('inactive');
    });

    describe('setting the required meta data', function () {

        beforeEach(function () {
            page.metadata.editableCell.get(0).click();
            page.metadata.editableInput.get(0).sendKeys('My New Event');
            page.pressEnter();

            page.metadata.editableCell.get(1).click();
            page.metadata.editableInput.get(1).sendKeys('matt.smith');
            page.pressEnter();

            page.metadata.editableCell.get(4).click();
            page.metadata.editableInput.get(3).sendKeys('About Stuff');
            page.pressEnter();
        });

        it('allows to go to the next step', function () {
            expect(page.wizard.nextButton.getAttribute('class')).not.toContain('inactive');
        });

        describe('navigating to the source tab', function () {

            beforeEach(function () {
                page.wizard.nextButton.click();
            });

            it('shows the source tab', function () {
                expect(page.wizard.activeStep.getText()).toEqual('Source');
                expect(page.wizard.nextButton.getAttribute('class')).toContain('inactive');
            });
        });
    });
});
