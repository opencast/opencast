define(['engage/engage_mobile_view'], function (EngageMobileView) {
  describe("EngageMobileView", function () {
    var engageMobileView;

    beforeEach(function () {
      engageMobileView = EngageMobileView;
    });

    it("should have a initView function", function () {
      expect(engageMobileView.initView).toBeDefined();
    });

    it("should have a insertPlugin function", function () {
      expect(engageMobileView.insertPlugin).toBeDefined();
    });

    it("should have a allPluginsLoaded function", function () {
      expect(engageMobileView.allPluginsLoaded).toBeDefined();
    });

  });
});