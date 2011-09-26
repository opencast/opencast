module("Current Version");
test("Get current version", function(){
     stop(500);
     var version = matterhornVersion.getCurrent();
     start();
     ok(isNotEmptyString(version), "Version found");
});

function isNotEmptyString(s){
  return (typeof version == 'string' && version.length > 0);
}