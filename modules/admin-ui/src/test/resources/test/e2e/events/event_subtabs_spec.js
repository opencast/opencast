// Protractor Jasmine end-to-end (e2e) test.
//
// See https://github.com/angular/protractor/blob/master/docs/getting-started.md
//

var page  = new (require('./events_page').EventsPage)(),
    mocks = require('./event_mocks');

describe('event sub-tabs', function () {

    beforeEach(function () {
        if (browser.useMocks) {
            browser.addMockModule('httpBackendMock', mocks.httpBackendMock);
        }
        page.getDetails('c3a4f68d-14d4-47e2-8981-8eb2fb300d3a');
        page.waitFor(page.modal.header);
    });

    describe('navigating to operation details', function () {
        beforeEach(function () {
            page.modal.workflowsTab.click();
        });

        it('displays breadcrumbs', function () {
            page.modal.firstDetailLink.click();
            expect(page.modal.breadcrumbs.getText()).toContain('Workflow Details');
            expect(page.modal.content.getText()).toContain('Workflow Details');

            page.modal.firstDetailLink.click();
            expect(page.modal.breadcrumbs.getText()).toContain('Workflow Details');
            expect(page.modal.breadcrumbs.getText()).toContain('Workflow Operations');
            expect(page.modal.content.getText()).toContain('Workflow Operations');

            page.modal.firstDetailLink.click();
            expect(page.modal.breadcrumbs.getText()).toContain('Workflow Details');
            expect(page.modal.breadcrumbs.getText()).toContain('Workflow Operations');
            expect(page.modal.breadcrumbs.getText()).toContain('Operation Details');
            expect(page.modal.content.getText()).toContain('Operation Details');
        });
    });

    describe('navigating to a previous sub tab', function () {
        beforeEach(function () {
            page.modal.workflowsTab.click();
            page.modal.firstDetailLink.click();
            page.modal.firstDetailLink.click();
            page.modal.firstDetailLink.click();
        });

        it('removes the lower breadcrumbs', function () {
            page.modal.breadcrumbs.all(by.css('a')).get(1).click();

            expect(page.modal.breadcrumbs.getText()).toContain('Workflow Details');
            expect(page.modal.breadcrumbs.getText()).toContain('Workflow Operations');
            expect(page.modal.breadcrumbs.getText()).not.toContain('Operation Details');
            expect(page.modal.content.getText()).toContain('Workflow Operations');
        });
    });
});
