var page = new (require('./series_creation_page').NewSeriesPage)(),
    mocks = require('./mocks');

describe('series creation', function () {

    var fillRequirementMetadata = function () {
            page.metadata.editableCell.get(0).click();
            page.metadata.editableInput.get(0).sendKeys('My New Series');
            page.pressEnter();
        },
        fillRequirementAccess = function () {
            page.chooseACL(0);
        };

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

    describe('showing first the metadata tab', function () {

        it('does not allow to go to the next step', function () {
            expect(page.wizard.nextButton.getAttribute('class')).toContain('inactive');
        });

        describe('filling the required metadata', function () {

            it('allows to go to the next step', function () {
                fillRequirementMetadata();
                expect(page.wizard.nextButton.getAttribute('class')).not.toContain('inactive');
            });
        });
    });

    describe('navigating to the access tab', function () {

        beforeEach(function () {
            fillRequirementMetadata();
            page.wizard.nextButton.click();
        });

        it('shows the access tab', function () {
            expect(page.wizard.activeStep.getText()).toEqual('Access');
            expect(page.wizard.nextButton.getAttribute('class')).toContain('inactive');
        });

        it('first does not allow to go to the next step', function () {
            expect(page.wizard.nextButton.getAttribute('class')).toContain('inactive');
        });

        describe('Selecting an access control list', function () {

            it('allows to go to the next step', function () {
                fillRequirementAccess();
                expect(page.wizard.nextButton.getAttribute('class')).not.toContain('inactive');
            });
        });
    });


    describe('navigating to the summary tab', function () {

        beforeEach(function () {
            fillRequirementMetadata();
            page.wizard.nextButton.click();
            fillRequirementAccess();
            page.wizard.nextButton.click();
        });

        it('shows the summary tab', function () {
            expect(page.wizard.activeStep.getText()).toEqual('Summary');
            expect(page.wizard.nextButton.getText()).toBe('Create');
        });

        it('shows the entered title', function(){
            expect( page.summary.tables.columns.get(1).getText()).toBe('My New Series');
        });

        it('shows the selected ACL', function(){
            expect( page.summary.tables.columns.get(3).getText()).toBe('1st ACL');
        });

    });

    describe('Submitting the data', function () {

        beforeEach(function () {
            fillRequirementMetadata();
            page.wizard.nextButton.click();
            fillRequirementAccess();
            page.wizard.nextButton.click();
        });

        it('shows a notification message', function () {
            page.wizard.nextButton.click();
            expect(page.summary.notification.isPresent()).toBe(true);
        });
    });


});
