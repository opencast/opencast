describe('Email API Resource', function () {
    var EmailResource, $httpBackend;

    beforeEach(module('adminNg'));

    beforeEach(module(function ($provide) {
        $provide.value('Language', {
            configureFromServer: function () {}
        });
    }));

    beforeEach(inject(function (_$httpBackend_, _EmailResource_) {
        $httpBackend  = _$httpBackend_;
        EmailResource = _EmailResource_;
    }));

    describe('#save', function () {

        it('calls the email service', function () {
            $httpBackend.expectPOST('/email/send/231', function (data) {
                var request = $.deparam(data);
                expect(request.eventIds).toEqual('92,23');
                expect(request.personIds).toEqual('44,49');
                expect(request.signature).toEqual('false');
                expect(request.store).toEqual('false');
                expect(request.body).toEqual('my message.');
                expect(request.subject).toEqual('subject A');
                return true;
            }).respond(200);
            EmailResource.save({ templateId: 231 }, {
                message: {
                    name: 'test',
                    subject: 'subject A',
                    message: 'my message.'
                },
                recipients: {
                    items: {
                        recordings: [{ id: 92 }, { id: 23 }],
                        recipients: [{ id: 44 }, { id: 49 }]
                    }
                }
            });
            $httpBackend.flush();
        });

        it('handles empty payloads gracefully', function () {
            $httpBackend.expectPOST('/email/send/231').respond(200);
            EmailResource.save({ templateId: 231 }, {});
            $httpBackend.flush();
        });

        describe('with the signature and audit trail flags set', function () {

            it('saves the flags accordingly', function () {
                $httpBackend.expectPOST('/email/send/231', function (data) {
                    var request = $.deparam(data);
                    expect(request.signature).toEqual('true');
                    expect(request.store).toEqual('true');
                    return true;
                }).respond(200);
                EmailResource.save({ templateId: 231 }, {
                    message: {
                        include_signature: true
                    },
                    recipients: {
                        audit_trail: true,
                        items: {
                            recordings: [],
                            recipients: []
                        }
                    }
                });
                $httpBackend.flush();
            });
        });
    });
});
