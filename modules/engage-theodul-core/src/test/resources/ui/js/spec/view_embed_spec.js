define(['engage/views/embed'], function (EngageEmbedView) {
  describe("EngageEmbedView", function () {
    var engageEmbedView;

    beforeEach(function () {
      engageEmbedView = EngageEmbedView;
    });

    it("should have a initView function", function () {
      expect(engageEmbedView.initView).toBeDefined();
    });

    it("should have a insertPlugin function", function () {
      expect(engageEmbedView.insertPlugin).toBeDefined();
    });

    it("should have a allPluginsLoaded function", function () {
      expect(engageEmbedView.allPluginsLoaded).toBeDefined();
    });

  });
});
