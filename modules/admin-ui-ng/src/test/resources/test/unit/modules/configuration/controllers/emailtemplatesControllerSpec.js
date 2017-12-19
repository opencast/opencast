describe('Email Templates controller', function () {
    var $scope, $httpBackend, EmailTemplatesResource, EmailTemplateResource, Table, Notifications;

    beforeEach(module('adminNg'));
    beforeEach(module(function ($provide) {
        $provide.value('Language', {
            configureFromServer: function () {},
            formatDate: function (val, date) { return date; },
            formatTime: function (val, date) { return date; }
        });
    }));

    beforeEach(inject(function ($rootScope, $controller, _$httpBackend_, _EmailTemplatesResource_, _EmailTemplateResource_, _Table_, _Notifications_) {
        $httpBackend = _$httpBackend_;
        EmailTemplatesResource = _EmailTemplatesResource_;
        EmailTemplateResource = _EmailTemplateResource_;
        Table = _Table_;
        Notifications = _Notifications_;

        $scope = $rootScope.$new();
        $controller('EmailtemplatesCtrl', {$scope: $scope});
    }));

    beforeEach(function () {
        jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';

        $httpBackend.whenGET('/admin-ng/resources/emailtemplates/filters.json')
            .respond(200);
    });

    it('instantiates', function () {
        expect($scope.table.resource).toEqual('emailtemplates');
    });

    describe('#delete', function () {
        var emailtemplate;

        beforeEach(function () {
            $httpBackend.whenGET('/email/template/1101')
                .respond(JSON.stringify(getJSONFixture('email/template/1101')));
            $httpBackend.whenGET('/email/templates.json?limit=10&offset=0')
                .respond(JSON.stringify(getJSONFixture('email/templates.json')));
            spyOn(Table, 'fetch');
            spyOn(Notifications, 'add');
            emailtemplate = EmailTemplateResource.get({ id: 1101 });
            $httpBackend.flush();
        });

        it('deletes the email template', function () {
            spyOn(EmailTemplateResource, 'delete');
            $scope.table.delete(emailtemplate);

            expect(EmailTemplateResource.delete).toHaveBeenCalled();
        });

        describe('on success', function () {
            beforeEach(function () {
                $httpBackend.whenDELETE('/email/template/1101').respond(201);
            });

            it('shows a notification and refreshes the table', function () {
                $scope.table.delete(emailtemplate);
                $httpBackend.flush();

                expect(Table.fetch).toHaveBeenCalled();
                expect(Notifications.add).toHaveBeenCalledWith('success', jasmine.any(String));
            });
        });

        describe('on error', function () {
            beforeEach(function () {
                $httpBackend.whenDELETE('/email/template/1101').respond(500);
            });

            it('shows a notification', function () {
                $scope.table.delete(emailtemplate);
                $httpBackend.flush();

                expect(Table.fetch).not.toHaveBeenCalled();
                expect(Notifications.add).toHaveBeenCalledWith('error', jasmine.any(String));
            });
        });
    });
});
