module('DublinCore XML creation', {
  setup: function() {
    this.metadata = {
      'title':'A pirates life',
      'creator':'Long John Silva'
    };
    this.doc = ocIngest.createDublinCoreCatalog(this.metadata);
  },
  teardown: function(){ }
});

test('DublinCore XML document created', function(){
  ok((this.doc.documentElement != null), "document element exists");
  var title = this.doc.documentElement.getElementsByTagName('dcterms:title')[0];
  ok((title != null), "title element exists");
  ok((title.childNodes[0].data == 'A pirates life'), "title element has the right content");
  var creator = this.doc.documentElement.getElementsByTagName('dcterms:creator')[0];
  ok((creator != null), "creator element exists");
  ok((creator.childNodes[0].data == 'Long John Silva'), "creator element has the right content");
});
