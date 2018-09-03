// Protractor Jasmine end-to-end (e2e) test.
//
// See https://github.com/angular/protractor/blob/master/docs/getting-started.md
//

var page  = new (require('./events_page').EventsPage)(),
    mocks = require('./mocks');

describe('event details', function () {
    beforeEach(function () {
        if (browser.useMocks) {
            browser.addMockModule('httpBackendMock', mocks.httpBackendMock);
        }
    });

    describe('opening comments', function () {

        beforeEach(function () {
            page.get();
            page.moreButton.click();
            page.waitFor(page.modal.header);
            page.modal.commentsTab.click();
        });

        it('displays comments and changes the URL', function () {
            expect(page.modal.header.getText()).toContain('Event Details c3a4f68d-14d4-47e2-8981-8eb2fb300d3a');
            expect(page.modal.content.getText()).toContain('Existing comment');
            expect(page.currentUrl()).toContain('c3a4f68d-14d4-47e2-8981-8eb2fb300d3a');
            expect(page.currentUrl()).toContain('comments');
        });
    });

    describe('comment', function () {
        beforeEach(function () {
            page.getDetails('c3a4f68d-14d4-47e2-8981-8eb2fb300d3a');
            page.waitFor(page.modal.header);
            page.modal.commentsTab.click();
        });

        describe('creation', function () {

            beforeEach(function () {
                browser.executeScript('window.scrollTo(0,document.body.scrollHeight);');
                page.comments.text.sendKeys('Dynamically added comment');

                page.comments.reasonDropdown.click();
                page.waitFor(page.comments.reasons.first());
                page.comments.reasons.first().click();

                page.comments.button.click();
            });

            it('displays regular comment form, not reply one', function () {
                expect(page.comments.reply.form.isPresent()).toBeFalsy();
            });

            it('has a reason choice', function () {
                expect(page.comments.reasonDropdown.isPresent()).toBeTruthy();
                page.comments.reasonDropdown.click().then(function () {
                    expect(element.all(by.css('ul.chosen-results li')).count()).toEqual(3);
                });
            });

            it('adds the new comment', function () {
                expect(page.comments.list.getText()).toContain('Dynamically added comment');
                expect(page.comments.list.getText()).toContain('Existing comment');
            });
        });

        describe('deletion', function () {
            beforeEach(function () {
                page.comments.delete.click();
            });

            it('deleted the specified comment', function () {
                expect(page.comments.list.getText()).not.toContain('Existing comment');
            });
        });

        describe('replies', function () {
            beforeEach(function () {
                page.comments.replyLinks.get(0).click();
            });

            it('form is rendered correctly', function () {
                expect(page.comments.reply.form.isDisplayed()).toBeTruthy();
                expect(page.comments.reply.text.getAttribute('placeholder')).toEqual('Write to @The Test Suite');
                expect(page.comments.reply.resolved.isDisplayed()).toBeTruthy();
                expect(page.comments.reply.resolved.getAttribute('value')).toEqual('on');
            });

            it('cancel reply button is functional', function () {
                expect(page.comments.reply.cancel.isDisplayed()).toBeTruthy();
                page.comments.reply.cancel.click();
                expect(page.comments.reply.form.isPresent()).toBeFalsy();
            });

            it('does not resolve the replied to comment if asked to', function () {
                expect(page.comments.resolvedFlag.isPresent()).toBeFalsy();

                page.comments.reply.resolved.click();
                page.comments.reply.text.sendKeys('not resolved');

                page.comments.reply.submit.click();

                expect(page.comments.resolvedFlag.isPresent()).toBeFalsy();
                expect(page.comments.reply.form.isPresent()).toBeFalsy();
            });

            it('replies to a comment, resolving it', function () {
                expect(page.comments.resolvedFlag.isPresent()).toBeFalsy();

                page.comments.reply.text.sendKeys('resolved');
                page.comments.reply.submit.click();

                expect(page.comments.resolvedFlag.isDisplayed()).toBeTruthy();
                expect(page.comments.reply.form.isPresent()).toBeFalsy();
            });
        });
    });
});
