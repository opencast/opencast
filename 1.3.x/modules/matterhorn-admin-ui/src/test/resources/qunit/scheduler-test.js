module( "FormField", {
  setup: function(){ 
    var doc = $(document.body);
    doc.append('<input type="text" id="textbox" value="some text here" />');
    doc.append('<input type="checkbox" id="input1" value="input1" />');
    doc.append('<input type="checkbox" id="input2" value="input2" />');
    doc.append('<input type="checkbox" id="input3" value="input3" />');
    doc.append('<select id="attendees"><option value="agent1">agent1</option></select>');
    doc.append('<input type="text" id="recurDurationHour" value="1" />');
    doc.append('<input type="text" id="recurDurationMin" value="5" />');
  },
  teardown: function(){
    $(document.body).empty();
  }
});

test("FormField creation", function(){
     var field = new FormField();
     ok((field && typeof field === 'object' && field instanceof FormField), "Instantiate empty FormField");
     field.setFormFields('textbox');
     ok((field.fields.textbox && field.fields.textbox[0].type === 'text'), "Add Field");
     field.setFormFieldOpts({required: true, foo: true});
     ok((field.required && field.foo), "FormField Opts set");
});

test("FormField get/set/disp/check", function(){
     var field = new FormField('textbox');
     same(field.getValue(), 'some text here' , "Run getValue");
     field.setValue('test');
     same(field.getValue(), 'test', "Run setValue");
     same(field.dispValue(), 'test', "Run dispValue");
});


test("FormField custom functions get/set/disp/check", function(){
     var field = new FormField('textbox');
     field.setFormFieldOpts({
                            getValue: function(){ return this.fields.textbox.val(); },
                            setValue: function(val){ this.fields.textbox.val(val); },
                            dispValue: function(){ return this.fields.textbox.val() }
                            });
     field.setValue('test');
     same(field.fields.textbox.val(), 'test', "Run custom setValue");
     same(field.getValue(), 'test', "Run custom getValue");
     same(field.dispValue(), 'test', "Run custom dispValue");
});

test("FormField agent get/set/disp/check", function(){
     var field = new FormField({getValue: getAgent, setValue: setAgent, checkValue: checkAgent});
     field.setFormFields('attendees');
     field.setFormFieldOpts({getValue: getAgent, setValue: setAgent, checkValue: checkAgent});
     field.setValue('test');
     ok(field.checkValue(), "Run agent checkValue");
     same(field.fields.attendees.val(), 'test', "Run agent setValue");
     field.setValue('agent1');
     same(field.getValue(), 'agent1', "Run agent getValue");
     same(field.dispValue(), 'agent1', "Run agent dispValue");
});

test("FormField duration get/set/disp/check", function(){
     var field = new FormField();
     field.setFormFields(['recurDurationHour','recurDurationMin']);
     field.setFormFieldOpts({getValue: getDuration, setValue: setDuration, checkValue: checkDuration, dispValue: getDurationDisplay});
     field.setValue('3660000');
     same(field.fields.recurDurationHour.val(), '1', "Run duration hour setValue");
     same(field.fields.recurDurationMin.val(), '1', "Run duration min setValue");
     ok(field.checkValue(), "Run duration checkValue");
     same(field.getValue(), 3660000 , "Run duration getValue");
     same(field.dispValue(), '1 hours, 1 minutes', "Run duration dispValue");
});

test("FormField input get/set/disp/check", function(){
     var field = new FormField();
     field.setFormFields(['input1', 'input2', 'input3']);
     field.setFormFieldOpts({getValue: getInputs, setValue: setInputs, checkValue: checkInputs});
     ok(!field.checkValue(), "Run input checkValue false");
     field.setValue('input1,input2,input3');
     ok(field.checkValue(), "Run input checkValue true");
     same(field.fields.input1.val(), 'input1', "Run input1 setValue");
     same(field.fields.input2.val(), 'input2', "Run input2 setValue");
     same(field.fields.input3.val(), 'input3', "Run input3 setValue");
     same(field.getValue(), 'input1,input2,input3', "Run input getValue");
});

test("FormField startdate get/set/disp/check", function(){
     ok(true, "TODO: startdate tests");
});