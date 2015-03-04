describe('Items Step in User Blacklist Wizard', function () {
    var NewUserblacklistItems, UsersResource, $httpBackend;

    beforeEach(module('ngResource'));
    beforeEach(module('adminNg'));

    beforeEach(module(function ($provide) {
        $provide.value('Language', {
            configureFromServer: function () {}
        });
    }));

    beforeEach(inject(function (_NewUserblacklistItems_, _UsersResource_, _$httpBackend_) {
        NewUserblacklistItems = _NewUserblacklistItems_;
        UsersResource = _UsersResource_;
        $httpBackend = _$httpBackend_;
    }));

    beforeEach(function () {
        $httpBackend.whenGET('/admin-ng/users/users.json').respond('{"results":[]}');
    });

    describe('#isValid', function () {

        it('is valid when users are set', function () {
            expect(NewUserblacklistItems.isValid()).toBe(false);
            NewUserblacklistItems.ud.items = [{ id: 81294 }];
            expect(NewUserblacklistItems.isValid()).toBe(true);
        });
    });

    describe('#isEditing', function () {

        it('returns the editing status', function () {
            expect(NewUserblacklistItems.isEditing()).toBe(false);
        });
    });

    describe('#addUser', function () {
        beforeEach(function () {
            NewUserblacklistItems.ud.items.push({ id: 81294 });
        });

        describe('with a new user', function () {

            it('selects the user', function () {
                expect(NewUserblacklistItems.ud.items.length).toBe(1);

                NewUserblacklistItems.ud.userToAdd = { id: 7231 };
                NewUserblacklistItems.addUser();

                expect(NewUserblacklistItems.ud.items).toContain({ id: 7231 });
                expect(NewUserblacklistItems.ud.items.length).toBe(2);
            });
        });

        describe('with an already selected user', function () {
            beforeEach(function () {
                NewUserblacklistItems.ud.items.push({ id: 7231 });
            });

            it('does not select the user twice', function () {
                expect(NewUserblacklistItems.ud.items.length).toBe(2);

                NewUserblacklistItems.ud.userToAdd = { id: 7231 };
                NewUserblacklistItems.addUser();

                expect(NewUserblacklistItems.ud.items.length).toBe(2);
            });
        });
    });
});
