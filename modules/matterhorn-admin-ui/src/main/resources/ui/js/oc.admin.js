/**
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 *
 * The Apereo Foundation licenses this file to you under the Educational
 * Community License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at:
 *
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

/* @namespace Holds functions and properites related to all Admin UIs. */
var ocAdmin = (function() {
  var admin = {};
  var DC_CATALOG_ROOT_NS    = 'http://www.opencastproject.org/xsd/1.0/dublincore/';
  var DC_CATALOG_ROOT_EL    = 'dublincore';
  var DUBLINCORE_NS_URI     = 'http://purl.org/dc/terms/';
  var DUBLINCORE_NS_PREFIX  = 'dcterms';
  var OC_NS_URI             = 'http://www.opencastproject.org/oc';
  var OC_NS_PREFIX          = 'oc';
  
  /* @class The Component class is a collection of form elements and associated functions for use
   * with the ocAdmin.Manager. It provides basic implementations for setting, getting, displaying,
   * and XMLifying the form elements.
   */
  admin.Component = function Component(fields, props, funcs) {
    this.fields = [];
    this.properties = [];
    this.required = false;
    this.value = null;
    this.key = null;
    this.nsPrefix = DUBLINCORE_NS_PREFIX;
    this.nsURI = DUBLINCORE_NS_URI;
    this.errors = { missingRequired: new ocAdmin.Error('missingRequired') };
    
    /* @lends ocAdmin.Component.prototype */
    /** 
     *  Sets the fields from an array of element ids.
     *  @param {String[]} Array of element ids
     */
    this.setFields = function(fields, append) {
      append = append || false;
      if(!append) {
        this.fields = [];
      }
      if(typeof fields == 'string') { //If a single field is specified, wrap in an array.
        fields = [fields];
      }
      for(var k in fields) {
        var e = $('#' + fields[k]);
        if(e[0]){
          this.fields[fields[k]] = e;
        }
      }
    };
    
    /** 
     *  Extends Component with additional methods and/or properties
     *  @param {Object} An object literal or instance with which to extend Component
     */
    this.setFunctions = function(funcs) {
      if(funcs && typeof funcs == 'object') {
        $.extend(this, funcs);
      }
    };
    
    /** 
     *  Sets the Component properties, arbitrary properties are added to properties array
     *  @param {Object} Key/Value pair of properties
     */
    this.setProperties = function(props) {
      if(typeof props == 'object') {
        for(var f in props) {
          switch(f) {
            case 'required':
              this.required = props[f];
              break;
            case 'key':
              this.key = props[f];
              break;
            case 'errors':
              this.errors = props[f];
              break;
            case 'nsPrefix':
              this.nsPrefix = props[f];
              break;
            case 'nsURI':
              this.nsURI = props[f];
              break;
            default:
              this.properties[f] = props[f];
          }
        }
      }
    };
    
    /** 
     *  Default getValue function
     *  @return A comma seperated string of all element values.
     *  @type String
     */
    this.getValue = function() {
      if(this.validate()) {
        var values = [];
        for(var el in this.fields) {
          var e = this.fields[el];
          if(e.length){
            switch(e[0].type) {
              case 'checkbox':
              case 'radio':
                if(e.is(":checked")) {
                  values.push('true');
                }else{
                  values.push('false');
                }
                break;
              case 'select-multiple':
                values.concat(e.val());
              default:
                values.push(e.val());
            }
          }
          this.value = values.join(',');
        } 
      }
      return this.value;
    };
    
    /** 
     *  Default setValue function
     *  Sets all elements to specified value
     *  @param {String}
     */
    this.setValue = function(val) {
      for(var el in this.fields) {
        if(this.fields[el].length) {
          switch(this.fields[el][0].type) {
            case 'checkbox':
            case 'radio':
              if(val === 'true' || val === true) {
                this.fields[el][0].checked = true;
              }
              break;
            case 'select-multiple':
              break;
            default:
              this.fields[el].val(val);
          }
        }
      }
    };
    
    /** 
     *  Add this because IE seems to use the default toString() of Object instead of the definition above (MH-5097)
     *  Default toString function
     *  @return A string of the Components value.
     *	@type String
     */
    this.asString = function() {
      return this.getValue();
    };
    
    /** 
     *  Default validation function, displays Component's error message
     *  @return True if Component is required and valid, otherwise false.
     *	@type Boolean
     */
    this.validate = function() {
      if(!this.required) {
        return []; //empty error array.
      } else {
        var oneIsValid = false;
        for(var e in this.fields) {
          if(this.fields[e][0].type == 'checkbox' || this.fields[e][0].type == 'radio') {
            if(this.fields[e][0].checked) {
              oneIsValid = true;
              break;
            }
          } else {
            if(this.fields[e].val()) {
              oneIsValid = true;
              break;
            }
          }
        }
        if(oneIsValid) {
          return [];
        }
      }
      return this.errors.missingRequired;
    }
    
    this.setFields(fields);
    this.setFunctions(funcs);
    this.setProperties(props);
  };
  /*
  TODO: Create a container for components to handle those components that can repeat
  
  ocAdmin.ComponentSet = function ComponentSet(){
  
  };
  
  $.extend(ocAdmin.ComponentSet.prototype, {
    components: []
  });
  
  */
  
  admin.Error = function Error(type, message) {
    this.type = type;
    this.message = message;
  }
  
  admin.Catalog = function Catalog(param) {
    this.name       = param.name;
    this.components = param.components || {};
    this.serializer = null;
    if(typeof param.serializer != 'undefined' && typeof param.serializer.serialize == 'function') {
      this.serializer = param.serializer;
    }
    this.serialize = function serialize() {
      if(this.serializer != null) {
        this.catalog = this.serializer.serialize(this.components);
        if(this.serializer.errors.length === 0) {
          return this.catalog;
        }
      }
      return false;
    };
    this.deserialize = function deserialize(catalogBody) {
      var catalog = this.serializer.deserialize(catalogBody);
      for(var i in catalog) {
        for(var j in this.components) {
          if(this.components[j].key === i){
            this.components[j].setValue(catalog[i]);
            break;
          }
        }
      }
    }
    this.getErrors = function() {
      return this.serializer.errors;
    };
  };
  
  admin.Serializer = function Serializer() {
    this.errors = [];
    this.serialize = function serialize(components) {
      var body = '';
      this.errors = [];
      for(i in components) {
        var comp = components[i];
        this.errors = this.errors.concat(comp.validate());
        body += comp.key + '=' + comp.getValue() + '\n';
      }
      return body;
    }
    this.deserialize = function deserialize(catalogBody) {
      var lines = catalogBody.split('\n');
      var catalog = {};
      for(var i in lines) {
        if(lines[i] != '' && lines[i].charAt(0) != '#') {
          var keyVal = lines[i].split('=');
          catalog[keyVal[0]] = keyVal[1];
        }
      }
      return catalog;
    }
  }
  
  admin.DublinCoreSerializer = function DublinCoreSerializer() {
    this.errors = [];
    this.namespaces = [];
    this.serialize = function serialize(components) {
      this.errors = [];
      var doc = ocUtils.createDoc(DC_CATALOG_ROOT_EL, DC_CATALOG_ROOT_NS);
      for(i in components){
        var comp = components[i];
        this.errors = this.errors.concat(comp.validate());
        this.addNS(comp.nsPrefix, comp.nsURI);
        var node = doc.createElement(comp.nsPrefix + ':' + comp.key);
        node.appendChild(doc.createTextNode(comp.getValue()));
        doc.documentElement.appendChild(node);
      }
      for(j in this.namespaces) {
        ns = doc.createAttribute('xmlns:' + j);
        ns.nodeValue = this.namespaces[j];
        doc.documentElement.setAttributeNode(ns);
      }
      return ocUtils.xmlToString(doc);
    };
    
    this.deserialize = function deserialze(catalogBody) {
      var catalog = {};
      if(typeof catalogBody[DUBLINCORE_NS_URI] != 'undefined') {
        for(i in catalogBody[DUBLINCORE_NS_URI]) {
          catalog[i] = catalogBody[DUBLINCORE_NS_URI][i][0].value;
        }
      }
      return catalog;
    }
    
    this.addNS = function addNS(nsPrefix, ns) {
      for(var i in this.namespaces) {
        if(i === nsPrefix || this.namespaces[i] === ns ) {
          return;
        }
      }
      this.namespaces[nsPrefix] = ns;
    }
  }
  
  admin.Error = function Error(name, label) {
    this.name = name || 'missingRequired';
    this.label = label || '';
    if(typeof this.label === 'string') {
      this.label = [this.label];
    }
  }
  
  return admin;
}());