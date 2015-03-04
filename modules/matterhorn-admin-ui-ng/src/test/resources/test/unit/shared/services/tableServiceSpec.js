describe('Table', function () {
    var $httpBackend, $location, Table, Storage, UsersResource;

    beforeEach(module('adminNg'));

    beforeEach(module(function ($provide) {
        var service = {
            configureFromServer: function () {},
            formatDate:     function (val, date) { return date; },
            formatDateTime: function (val, date) { return date; },
            formatTime:     function (val, date) { return date; }
        };
        $provide.value('Language', service);
    }));

    beforeEach(inject(function (_$httpBackend_, _$location_, _Table_, _Storage_, _UsersResource_) {
        $httpBackend = _$httpBackend_;
        $location = _$location_;
        Table = _Table_;
        Storage = _Storage_;
        UsersResource = _UsersResource_;
    }));

    beforeEach(function () {
        jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';
    });

    it('provides a constructor', function () {
        expect(Table.configure).toBeDefined();
    });

    describe('#configure', function () {
        var params = {
            columns: [{
                name: 'name',
                label: 'NAME'
            }],
            caption: 'CAPTION',
            resource: 'tables',
            category: 'furniture',
            apiService: {
                query: function () {
                    var rows = [];
                    rows.push({test: ''});


                    return {
                        $promise: {
                            then: function (fn) {
                                fn(rows);
                            }
                        }
                    };
                }
            }
        };

        it('defines a Table object', function () {
            Table.configure(params);
            expect(Table.resource).toEqual('tables');
            expect(Table.caption).toEqual('CAPTION');
        });

        it('sets default sort parameters', function () {
            Table.configure(params);
            expect(Table.predicate).toEqual('');
            expect(Table.reverse).toBe(false);
        });

        describe('with stored sort parameters', function () {
            beforeEach(function () {
                Storage.put('sorter', 'tables', 'color', {
                    name     : 'color',
                    priority : 0,
                    order    : 'DESC'
                });
                Table.configure(params);
            });

            it('restores the sort parameters', function () {
                expect(Table.sorters.length).toEqual(1);
                expect(Table.predicate).toEqual('color');
                expect(Table.reverse).toBe(true);
            });
        });
    });

    describe('#fetch', function () {
        beforeEach(function () {
            Table.resource = 'users';
            Table.apiService = UsersResource;
        });

        it('updates the table data from the API', function () {
            $httpBackend.expectGET('/admin-ng/users/users.json?limit=10&offset=0')
                .respond(JSON.stringify(getJSONFixture('admin-ng/users/users.json')));
            Table.fetch();
            $httpBackend.flush();
            expect(Table.rows.length).toBe(6);
        });

        describe('with a table filter', function () {
            beforeEach(function () {
                Storage.put('filter', Table.resource, 'username', 'admin');
            });

            afterEach(function () {
                Storage.remove('filter');
            });

            it('constructs a valid query string', function () {
                $httpBackend.expectGET('/admin-ng/users/users.json?filter=username:admin&limit=10&offset=0')
                    .respond(JSON.stringify(getJSONFixture('admin-ng/users/users.json')));
                Table.fetch();
                $httpBackend.flush();
            });
        });
    });

    describe('#sortBy', function () {
        beforeEach(function () {
            Table.reverse = true;
            Table.resource = 'users';
            Table.apiService = UsersResource;

            $httpBackend.whenGET('/admin-ng/users/users.json?limit=10&offset=0')
                .respond(JSON.stringify(getJSONFixture('admin-ng/users/users.json')));
            Table.fetch();
            $httpBackend.flush();
        });

        it('sets the sort params', function () {
            $httpBackend.whenGET('/admin-ng/users/users.json?limit=10&offset=0&sort=email:ASC')
                .respond(JSON.stringify(getJSONFixture('admin-ng/users/users.json')));
            Table.sortBy({ name: 'email' });
            $httpBackend.flush();
            expect(Table.predicate).toEqual('email');
            expect(Table.reverse).toBe(false);
        });

        it('stores the filters', function () {
            spyOn(Storage, 'put');
            $httpBackend.whenGET('/admin-ng/users/users.json?limit=10&offset=0&sort=email:DESC')
                .respond(JSON.stringify(getJSONFixture('admin-ng/users/users.json')));
            Table.sortBy({ name: 'email' });
            $httpBackend.flush();
            expect(Storage.put).toHaveBeenCalledWith('sorter', 'users', 'email',  { name : 'email', priority : 0, order : 'DESC' });
        });

        xit('applies the datatable filter', function () {
            expect(Table.rows[0].email).toEqual('xavier.butty@example.com');
            $httpBackend.whenGET('/admin-ng/users/users.json?limit=10&offset=0')
            .respond(JSON.stringify(getJSONFixture('admin-ng/users/users.json')));
            Table.sortBy({ name: 'email' });
            $httpBackend.flush();
            expect(Table.rows[0].email).toEqual('admin@example.com');
        });
    });

    describe('Pagination', function () {
        var page, mockPages;
        page = function (pageNumber, label, isActive) {
            var active = isActive === 'active';
            return {
                number: pageNumber,
                label: label,
                active: active
            };
        };

        mockPages = function (params) {
            var pages = [], i, numberOfPages = params.totalItems / params.limit;

            for (i = 0; i < numberOfPages || (i === 0 && numberOfPages === 0); i++) {
                pages.push(page(i, (i + 1).toString(), i === params.activePage ? 'active': 'inactive'));
            }

            return {
                totalItems  :         params.totalItems,
                pages       :         pages,
                limit       :         params.limit,
                offset      :         params.activePage,
                directAccessibleNo :  3,
                fastNavigationSize :  params.fastNavigationSize
            };
        };

        describe('#getDirectAccessiblePages', function () {

            it('returns [(0),1,2,3,4,5,..,7] when index is 0', function () {
                Table.pagination = mockPages({totalItems: 39, limit: 5, activePage: 0});

                expect(Table.getDirectAccessiblePages())
                    .toEqual([page(0, '1', 'active'), page(1, '2'), page(2, '3'), page(3, '4'), page(4, '5'), page(5, '..'), page(7, '8')]);
            });


            it('returns [0,..,3,(4),5,6,7] when index is 4', function () {
                Table.pagination = mockPages({totalItems: 39, limit: 5, activePage: 4});

                expect(Table.getDirectAccessiblePages())
                    .toEqual([page(0, '1'), page(2, '..'), page(3, '4'), page(4, '5', 'active'), page(5, '6'), page(6, '7'), page(7, '8')]);
            });

            it('returns [0,..,3,4,5,6,(7)] when index is 4', function () {
                Table.pagination = mockPages({totalItems: 39, limit: 5, activePage: 7});

                expect(Table.getDirectAccessiblePages())
                    .toEqual([page(0, '1'), page(2, '..'), page(3, '4'), page(4, '5'), page(5, '6'), page(6, '7'), page(7, '8', 'active')]);
            });

            it('returns [1] when total items are 0', function () {
                Table.pagination = mockPages({totalItems: 0, limit: 5, activePage: 0});

                expect(Table.getDirectAccessiblePages())
                    .toEqual([page(0, '1', 'active')]);
            });

        });

        describe('#goToPage', function () {
            beforeEach(function () {
                spyOn(Table, 'fetch').and.returnValue('');
            });

            it('correctly switches the page', function () {
                Table.pagination = mockPages({totalItems: 39, limit: 5, activePage: 0});
                Table.goToPage(2);
                expect(Table.getDirectAccessiblePages()).toEqual([page(0, '1'), page(1, '2'), page(2, '3', 'active'), page(3, '4'),
                    page(4, '5'), page(5, '..'), page(7, '8')]);
            });
        });
    });

});
