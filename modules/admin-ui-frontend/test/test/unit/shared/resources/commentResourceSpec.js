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
            $httpBackend.expectPOST('/admin-ng/event/c3a4f68d-14d4-47e2-8981-8eb2fb300d3a/comment/7').respond(200);
            CommentResource.save({
                resource: 'event',
                resourceId: 'c3a4f68d-14d4-47e2-8981-8eb2fb300d3a',
                type: 'comment',
                id: 7
            }, {
                text: 'my comment',
                reason: 'comment_reason_1'
            });
            $httpBackend.flush();
        });

        it('handles empty payloads gracefully', function () {
            $httpBackend.expectPOST('/admin-ng/event/c3a4f68d-14d4-47e2-8981-8eb2fb300d3a/comment/7').respond(200);
            CommentResource.save({
                resource: 'event',
                resourceId: 'c3a4f68d-14d4-47e2-8981-8eb2fb300d3a',
                type: 'comment',
                id: 7
            }, undefined);
            $httpBackend.flush();
        });
    });
});
