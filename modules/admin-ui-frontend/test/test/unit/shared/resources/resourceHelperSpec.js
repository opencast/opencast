describe('Resource Helper, a angular resource assisting class', function () {
    var ResourceHelper;

    beforeEach(module('adminNg'));

    beforeEach(inject(function (_ResourceHelper_) {
        ResourceHelper = _ResourceHelper_;
    }));

    describe('transformation with single result', function () {
        var json, result;

        json = JSON.stringify({
            'results': {'one': 'one', 'two': 'two', 'three': 'three', 'four': ['heinz', 'chrigi', 'xappi']},
            'total': 9,
            'offset': 1,
            'count': 1,
            'limit': 2
        });

        it('does not convert rows without given mapper', function () {

            result = ResourceHelper.parseResponse(json);

            expect(result.rows).toEqual([{one: 'one', two: 'two', three: 'three', four: ['heinz', 'chrigi', 'xappi']}]);
        });

        it('converts rows with given mapper', function () {

            var customMapper = function (data) {
                data.one = 'schnurr';
                return data;
            };

            result = ResourceHelper.parseResponse(json, customMapper);

            expect(result.rows[0].one).toEqual('schnurr');
        });

        it('sets the total', function () {

            result = ResourceHelper.parseResponse(json);

            expect(result.total).toEqual(9);
        });

        it('sets the offset', function () {

            result = ResourceHelper.parseResponse(json);

            expect(result.offset).toEqual(1);
        });

        it('sets the count', function () {

            result = ResourceHelper.parseResponse(json);

            expect(result.count).toEqual(1);
        });

        it('sets the limit', function () {

            result = ResourceHelper.parseResponse(json);

            expect(result.limit).toEqual(2);
        });
    });


    describe('transformation with array result', function () {
        var json, result;

        json = JSON.stringify({
            'results': [{'one': 'one', 'two': 'two', 'three': 'three', 'four': ['heinz', 'chrigi', 'xappi']}],
            'total': 9,
            'offset': 1,
            'count': 1,
            'limit': 2
        });

        it('does not convert rows without given mapper', function () {

            result = ResourceHelper.parseResponse(json);

            expect(result.rows).toEqual([{one: 'one', two: 'two', three: 'three', four: ['heinz', 'chrigi', 'xappi']}]);
        });

        it('converts rows with given mapper', function () {

            var customMapper = function (data) {
                data.one = 'schnurr';
                return data;
            };

            result = ResourceHelper.parseResponse(json, customMapper);

            expect(result.rows[0].one).toEqual('schnurr');
        });

        it('sets the total', function () {

            result = ResourceHelper.parseResponse(json);

            expect(result.total).toEqual(9);
        });

        it('sets the offset', function () {

            result = ResourceHelper.parseResponse(json);

            expect(result.offset).toEqual(1);
        });

        it('sets the count', function () {

            result = ResourceHelper.parseResponse(json);

            expect(result.count).toEqual(1);
        });

        it('sets the limit', function () {

            result = ResourceHelper.parseResponse(json);

            expect(result.limit).toEqual(2);
        });
    });
});