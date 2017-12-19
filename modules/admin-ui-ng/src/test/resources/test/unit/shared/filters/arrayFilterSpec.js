describe('adminNg.filters.joinBy', function () {
    var $filter, joinByFilter;

    beforeEach(module('adminNg'));

    beforeEach(module(function ($provide) {
        var service = {
            configureFromServer: function () {},
            getLanguageCode: function () { return 'en'; },
            formatDate: function (format, input) { return input + '-localized'; }
        };
        $provide.value('Language', service);
    }));

    beforeEach(inject(function (_$filter_, _joinByFilter_) {
        $filter = _$filter_;
        joinByFilter = _joinByFilter_;
    }));

    it('gets exposed', function () {
        expect($filter('joinBy')).not.toBeNull();
    });

    describe('without a separator', function () {

        it('joins elements using a comma', function () {
            expect(joinByFilter(['foo', 'bar'])).toEqual('foo,bar');
        });
    });

    describe('with a separator', function () {

        it('joins elements using the separator', function () {
            expect(joinByFilter(['foo', 'bar'], ' - ')).toEqual('foo - bar');
        });
    });
});
