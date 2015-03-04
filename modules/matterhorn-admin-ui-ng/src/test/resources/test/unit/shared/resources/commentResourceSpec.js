describe('Comments API Resource', function () {
    var CommentResource, $httpBackend;

    beforeEach(module('adminNg.resources'));
    beforeEach(module('ngResource'));

    beforeEach(inject(function (_$httpBackend_, _CommentResource_) {
        $httpBackend  = _$httpBackend_;
        CommentResource = _CommentResource_;
    }));

    it('provides the comments resource', function () {
        expect(CommentResource.query).toBeDefined();
    });

    describe('#save', function () {

        it('saves the comment', function () {
            $httpBackend.expectPOST('/admin-ng/event/40518/comment/7').respond(200);
            CommentResource.save({
                resource: 'event',
                resourceId: '40518',
                type: 'comment',
                id: 7
            }, {
                text: 'my comment',
                reason: 'comment_reason_1'
            });
            $httpBackend.flush();
        });

        it('handles empty payloads gracefully', function () {
            $httpBackend.expectPOST('/admin-ng/event/40518/comment/7').respond(200);
            CommentResource.save({
                resource: 'event',
                resourceId: '40518',
                type: 'comment',
                id: 7
            }, undefined);
            $httpBackend.flush();
        });
    });
});
