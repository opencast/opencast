// Set up the Page Object
//
// See https://code.google.com/p/selenium/wiki/PageObjects
//
var behaviour = {};
behaviour.pressEnter = function () {
    // Press both enter key variants as different webdrivers provide
    // different key symbols.
    protractor.getInstance().actions().sendKeys(protractor.Key.RETURN).perform();
    protractor.getInstance().actions().sendKeys(protractor.Key.ENTER).perform();
};

exports.EventsPage = function () {
    this.get = function () {
        browser.get('#/events/events');
    };
    this.getDetails = function (id) {
        browser.get('#/events/events/?modal=event-details&resourceId=' + id);
    };

    this.eventsTableHeaders = element.all(by.css('th'));

    this.modal = {};
    this.modal.modal            = element(by.css('section.modal'));
    this.modal.header           = element(by.id('event-details-modal'));
    this.modal.content          = element(by.css('.modal-content.active'));
    this.modal.metadataTab      = element(by.linkText('Metadata'));
    this.modal.mediaTab         = element(by.linkText('Media'));
    this.modal.attachmentsTab   = element(by.linkText('Attachments'));
    this.modal.generalTab       = element(by.linkText('General'));
    this.modal.workflowsTab     = element(by.linkText('Workflows'));
    this.modal.commentsTab      = element(by.linkText('Comments'));
    this.modal.closeButton      = element(by.css('a.close-modal'));
    this.modal.previous         = element(by.css('i.fa-chevron-left'));
    this.modal.next             = element(by.css('i.fa-chevron-right'));
    this.modal.breadcrumbs      = element(by.id('breadcrumb'));
    this.modal.firstDetailLink  = element.all(by.linkText('Details')).first();

    this.metadata = {};
    this.metadata.editableCell    = element.all(by.css('.modal tr td.editable'));
    this.metadata.editableInput   = element.all(by.css('.modal tr td.editable input'));
    this.metadata.savedIcon       = element.all(by.css('td.editable i.saved'));
    this.metadata.presenterDelete = function (name) {
        return element(by.xpath('//span[contains(text(),"' + name + '")]/a'));
    };

    this.attachments = {};
    this.attachments.firstAttachmentTd = element(by.css('.modal-content.active td:nth-of-type(1)'));

    this.general = {};
    this.general.publications   = element.all(by.css('div[data-modal-tab-content="general"] ul li'));

    this.media = {};
    this.media.firstRow         = element(by.css('div[data-modal-tab-content="media"] table tbody tr'));
    this.media.firstTd          = element(by.css('div[data-modal-tab-content="media"] table tr:nth-of-type(1) td:first-of-type'));
    this.media.detailLinks      = element.all(by.linkText('Details'));
    this.media.detailsSubNavTab = element(by.linkText('Media Details'));

    this.comments = {};
    this.comments.list           = element(by.css('.comment-container'));
    this.comments.text           = element(by.model('myComment.text'));
    this.comments.button         = element(by.css('.comments button.save'));
    this.comments.replyLinks     = element.all(by.css('a.reply'));
    this.comments.reasonDropdown = element(by.css('.add-comment .chosen-container'));
    this.comments.reasons        = element.all(by.css('.add-comment .chosen-container li'));
    this.comments.resolvedFlag   = element(by.css('.comment span.resolve.resolved'));

    this.comments.reply = {};
    this.comments.reply.form     = element(by.css('form.add-comment.reply'));
    this.comments.reply.text     = element(by.css('form.add-comment.reply textarea'));
    this.comments.reply.resolved = element(by.model('myComment.resolved'));
    this.comments.reply.cancel   = element(by.css('form.add-comment.reply button.cancel'));
    this.comments.reply.submit   = element(by.css('form.add-comment.reply button.save'));
    this.comments.delete         = element.all(by.css('.comment-container a.delete')).first();

    this.tabs = {};
    this.tabs.events = element(by.linkText('Events'));
    this.tabs.series = element(by.linkText('Series'));

    this.moreButton  = element.all(by.css('a.more')).first();
    this.table       = element(by.css('table'));

    this.filter = {};
    this.filter.textFilter  = element(by.model('textFilter'));
    this.filter.addButton   = element(by.css('.table-filter .filters > i'));
    this.filter.field       = element(by.css('.table-filter .filters'));
    this.filter.clearButton = element(by.css('.table-filter i.clear'));

    this.profile = {};
    this.profile.menuButton    = element(by.css('.filter-settings-dd > i'));
    this.profile.formButton    = element(by.linkText('Save'));
    this.profile.nameField     = element(by.model('profile.name'));
    this.profile.saveButton    = element(by.linkText('Save'));
    this.profile.profiles      = element(by.css('.filters-list ul'));
    this.profile.editButtons   = element.all(by.css('.filters-list a.edit'));
    this.profile.activeProfile = element(by.css('.filters-list a.active'));

    this.currentUrl = function () {
        return protractor.getInstance().getCurrentUrl();
    };
    this.waitFor = function (element) {
        protractor.getInstance().wait(function () {
            return element.isDisplayed();
        });
    };
    this.filterBy = function (criterium, value) {
        this.filter.addButton.click();
        element(by.model('selectedFilter')).click();
        element(by.xpath('//option[contains(text(),"' + criterium + '")]')).click();
        element(by.model('selectedFilter')).click();

        element(by.model('filter.value')).click();
        element(by.xpath('//option[contains(text(),"' + value + '")]')).click();
    };
    this.filterByText = function (value) {
        this.filter.textFilter.sendKeys(value);
    };
    this.pressEnter = behaviour.pressEnter;
    this.pressEscape = function () {
        protractor.getInstance().actions().sendKeys(protractor.Key.ESCAPE).perform();
    };
};

exports.NewEventPage = function () {
    this.get = function () {
        browser.get('#/events/events');

    };
    this.waitFor = function (element) {
        protractor.getInstance().wait(function () {
            return element.isDisplayed();
        });
    };
    this.pressEnter             = behaviour.pressEnter;
    this.newEventButton         = element(by.buttonText('Add Event'));
    this.nextButton             = element(by.linkText('Next Step'));
    this.backButton             = element(by.linkText('Back'));
    this.metadata               = {};
    this.metadata.editableCell  = element.all(by.css('.modal tr td.editable'));
    this.metadata.editableInput = element.all(by.css('.modal tr td.editable input'));
};
