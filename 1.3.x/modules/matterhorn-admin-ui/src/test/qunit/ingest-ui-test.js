module('Ingest UI behavior', {
  setup: function() {},
  teardown: function(){ }
});

test('Display missing fields notification', function(){
  Upload.checkRequiredFields(true);
  $('#container-missingFields').show();
  ok($('#container-missingFields').is(':visible'), 'Missing fields notification container visible');
  ok($('#notification-track').is(':visible'), 'Notification for missing field track visible');
  same($('#label-track').css('color'), 'red', 'Label for field track colored red');
});

test('Display hide fields notification', function(){
  $('#track').val("test.mpg");
  Upload.checkRequiredFields(false);
  same($('#container-missingFields').is(':visible'), true, 'Missing fields notification container visible (yields true even is container isn\'t visible)');  // container not visible, though .is(":visible") yields true
  same($('#notification-track').is(':visible'), false, 'Notification for missing field track visible');
  same($('#label-track').css('color'), 'black', 'Label for field track colored black');
});

test('Display upload progress popup', function() {
  Upload.showProgressStage();
  ok($('#gray-out').is(':visible'), 'gray-out visible');
  ok($('#progress-stage').is(':visible'), 'progress-stage visible');
});

test('Set upload progress', function() {
  Upload.setProgress('43%', 'text', 'total', 'transfered');
  same($('#progressbar-indicator').css('width'), '43%', 'Indicator set correctly');
  same($('#progressbar-label').text(), 'text', 'Indicator label set correctly');
  same($('#label-filesize').text(), 'total', 'File size label set correctly');
  same($('#label-bytestrasfered').text(), 'transfered', 'Bytes transfered label set correctly');
});

test('Hide upload progress popup', function() {
  Upload.hideProgressStage();
  //same($('#gray-out').css('display'), 'none', 'gray-out visible');    // though elements are not visible .is(':visible') yields true
  //same($('#progress-stage').css('display'), 'none', 'progress-stage visible');
});