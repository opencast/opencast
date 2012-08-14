//
//  jquery.xmlns.js:  xml-namespace selector support for jQuery
//
//  This plugin modifies the jQuery tag and attribute selectors to
//  support optional namespace specifiers as defined in CSS 3:
//
//    $("elem")      - matches 'elem' nodes in the default namespace
//    $("|elem")     - matches 'elem' nodes that don't have a namespace
//    $("NS|elem")   - matches 'elem' nodes in declared namespace 'NS'
//    $("*|elem")    - matches 'elem' nodes in any namespace
//    $("NS|*")      - matches any nodes in declared namespace 'NS'
//
//  A similar synax is also supported for attribute selectors, but note
//  that the default namespace does *not* apply to attributes - a missing
//  or empty namespace selector selects only attributes with no namespace.
//
//  In a slight break from the W3C standards, and with a nod to ease of
//  implementation, the empty namespace URI is treated as equivalent to
//  an unspecified namespace.  Plenty of browsers seem to make the same
//  assumption...
//
//  Namespace declarations live in the $.xmlns object, which is a simple
//  mapping from namespace ids to namespace URIs.  The default namespace
//  is determined by the value associated with the empty string.
//
//    $.xmlns.D = "DAV:"
//    $.xmlns.FOO = "http://www.foo.com/xmlns/foobar"
//    $.xmlns[""] = "http://www.example.com/new/default/namespace/"
//
//  Unfortunately this is a global setting - I can't find a way to do
//  query-object-specific namespaces since the jQuery selector machinery
//  is stateless.  However, you can use the 'xmlns' function to push and
//  pop namespace delcarations with ease:
//
//    $().xmlns({D:"DAV:"})     // pushes the DAV: namespace
//    $().xmlns("DAV:")         // makes DAV: the default namespace
//    $().xmlns(false)          // pops the namespace we just pushed
//    $().xmlns(false)          // pops again, returning to defaults
//
//  To execute this as a kind of "transaction", pass a function as the
//  second argument.  It will be executed in the context of the current
//  jQuery object:
//
//    $().xmlns("DAV:",function() {
//      //  The default namespace is DAV: within this function,
//      //  but it is reset to the previous value on exit.
//      return this.find("response").each(...);
//    }).find("div")
//
//  If you pass a string as a function, it will be executed against the
//  current jQuery object using find(); i.e. the following will find all
//  "href" elements in the "DAV:" namespace:
//
//    $().xmlns("DAV:","href")
//
// 
//  And finally, the legal stuff:
//
//    Copyright (c) 2009, Ryan Kelly.
//    TAG and ATTR functions derived from jQuery's selector.js.
//    Dual licensed under the MIT and GPL licenses.
//    http://docs.jquery.com/License
//

(function($) {

//  Some common default namespaces, that are treated specially by browsers.
//
var default_xmlns = {
    "xml": "http://www.w3.org/XML/1998/namespace",
    "xmlns": "http://www.w3.org/2000/xmlns/",
    "html": "http://www.w3.org/1999/xhtml/"
};
 

//  A reverse mapping for common namespace prefixes.
//
var default_xmlns_rev = {}
for(var k in default_xmlns) {
    default_xmlns_rev[default_xmlns[k]] = k;
}


//  $.xmlns is a mapping from namespace identifiers to namespace URIs.
//  The default default-namespace is "*", and we provide some additional
//  defaults that are specified in the XML Namespaces standard.
//
$.extend({xmlns: $.extend({},default_xmlns,{"":"*"})});


//  jQuery method to push/pop namespace declarations.
//
//  If a single argument is specified:
//    * if it's a mapping, push those namespaces onto the stack
//    * if it's a string, push that as the default namespace
//    * if it evaluates to false, pop the latest namespace
//
//  If two arguments are specified, the second is executed "transactionally"
//  using the namespace declarations found in the first.  It can be either a
//  a selector string (in which case it is passed to this.find()) or a function
//  (in which case it is called in the context of the current jQuery object).
//  The given namespace mapping is automatically pushed before executing and
//  popped afterwards.
//
var xmlns_stack = [];
$.fn.extend({xmlns: function(nsmap,func) {
    if(typeof nsmap == "string") {
        nsmap = {"":nsmap};
    }
    if(nsmap) {
        xmlns_stack.push($.xmlns);
        $.xmlns = $.extend({},$.xmlns,nsmap);
        if(func !== undefined) {
            if(typeof func == "string") {
                return this.find(func).xmlns(undefined)
            } else {
                var self = this;
                try {
                    self = func.call(this);
                    if(!self) {
                        self = this;
                    }
                } finally {
                    self.xmlns(undefined);
                }
                return self
            }
        } else {
            return this;
        }
    } else {
        $.xmlns = (xmlns_stack ? xmlns_stack.pop() : {});
        return this;
    }
}});


//  Convert a namespace prefix into a namespace URI, based
//  on the delcarations made in $.xmlns.
//
var getNamespaceURI = function(id) {
    // No namespace id, use the default.
    if(!id) {
        return $.xmlns[""];
    }
    // Strip the pipe character from the specifier
    id = id.substr(0,id.length-1);
    // Certain special namespaces aren't mapped to a URI
    if(id == "" || id == "*") {
        return id;
    }
    var ns = $.xmlns[id];
    if(typeof(ns) == "undefined") {
        throw "Syntax error, undefined namespace prefix '" + id + "'";
    }
    return ns;
};


//  Update the regex used by $.expr to parse selector components for a
//  particular type of selector (e.g. "TAG" or "ATTR").
//
//  This logic is taken straight from the jQuery/Sizzle sources.
//
var setExprMatchRegex = function(type,regex) {
  $.expr.match[type] = new RegExp(regex.source + /(?![^\[]*\])(?![^\(]*\))/.source);
  if($.expr.leftMatch) {
      $.expr.leftMatch[type] = new RegExp(/(^(?:.|\r|\n)*?)/.source + $.expr.match[type].source.replace(/\\(\d+)/g, function(all, num){
          return "\\" + (num - 0 + 1);
      }));
  }
}



//  Modify the TAG match regexp to include optional namespace selector.
//  This is basically (namespace|)?(tagname).
//
setExprMatchRegex("TAG",/^((?:((?:[\w\u00c0-\uFFFF\*_-]*\|)?)((?:[\w\u00c0-\uFFFF\*_-]|\\.)+)))/);


//  Perform some capability-testing.
//
var div = document.createElement("div");

//  Sometimes getElementsByTagName("*") will return comment nodes,
//  which we will have to remove from the results.
//
var gebtn_yields_comments = false;
div.appendChild(document.createComment(""));
if(div.getElementsByTagName("*").length > 0) {
    gebtn_yields_comments = true;
}

//  Some browsers return node.localName in upper case, some in lower case.
//
var localname_is_uppercase = true;
if(div.localName && div.localName == "div") {
    localname_is_uppercase = false;
}

//  Allow the testing div to be garbage-collected.
//
div = null;


//  Modify the TAG find function to account for a namespace selector.
//
$.expr.find.TAG = function(match,context,isXML) {
    var ns = getNamespaceURI(match[2]);
    var ln = match[3];
    var res;
    if(typeof context.getElementsByTagNameNS != "undefined") {
        //  Easy case - we have getElementsByTagNameNS
        res = context.getElementsByTagNameNS(ns,ln);
    } else if(typeof context.selectNodes != "undefined") {
        //  Use xpath if possible (not available on HTML DOM nodes in IE)
        if(context.ownerDocument) {
            context.ownerDocument.setProperty("SelectionLanguage","XPath");
        } else {
            context.setProperty("SelectionLanguage","XPath");
        }
        var predicate = "";
        if(ns != "*") {
            if(ln != "*") {
                predicate="namespace-uri()='"+ns+"' and local-name()='"+ln+"'";
            } else {
                predicate="namespace-uri()='"+ns+"'";
            }
        } else {
            if(ln != "*") {
                predicate="local-name()='"+ln+"'";
            }
        }
        if(predicate) {
            res = context.selectNodes("descendant-or-self::*["+predicate+"]");
        } else {
            res = context.selectNodes("descendant-or-self::*");
        }
    } else {
        //  Otherwise, we need to simulate using getElementsByTagName
        res = context.getElementsByTagName(ln); 
        if(gebtn_yields_comments && ln == "*") {
            var tmp = [];
            for(var i=0; res[i]; i++) {
                if(res[i].nodeType == 1) {
                    tmp.push(res[i]);
                }
            }
            res = tmp;
        }
        if(res && ns != "*") {
            var tmp = [];
            for(var i=0; res[i]; i++) {
               if(res[i].namespaceURI == ns || res[i].tagUrn == ns) {
                   tmp.push(res[i]);
               }
            }
            res = tmp;
        }
    }
    return res;
};


//  Check whether a node is part of an XML document.
//  Copied verbatim from jQuery sources, needed in TAG preFilter below.
//
var isXML = function(elem){
    return elem.nodeType === 9 && elem.documentElement.nodeName !== "HTML" ||
            !!elem.ownerDocument && elem.ownerDocument.documentElement.nodeName !== "HTML";
};


//  Modify the TAG preFilter function to work with modified match regexp.
//  This normalises case of the tag name if we're in a HTML document.
//
$.expr.preFilter.TAG = function(match, curLoop, inplace, result, not, isXML) {
  var ln = match[3];
  if(!isXML) {
      if(localname_is_uppercase) {
          ln = ln.toUpperCase();
      } else {
          ln = ln.toLowerCase();
      }
  }
  return [match[0],getNamespaceURI(match[2]),ln];
};


//  Modify the TAG filter function to account for a namespace selector.
//
$.expr.filter.TAG = function(elem,match) {
    var ns = match[1];
    var ln = match[2];
    var e_ns = elem.namespaceURI ? elem.namespaceURI : elem.tagUrn;
    var e_ln = elem.localName ? elem.localName : elem.tagName;
    if(ns == "*" || e_ns == ns || (ns == "" && !e_ns)) {
        return ((ln == "*" && elem.nodeType == 1)  || e_ln == ln);
    }
    return false;
};


//  Modify the ATTR match regexp to extract a namespace selector.
//  This is basically ([namespace|])(attrname)(op)(quote)(pattern)(quote)
//
setExprMatchRegex("ATTR",/\[\s*((?:((?:[\w\u00c0-\uFFFF\*_-]*\|)?)((?:[\w\u00c0-\uFFFF_-]|\\.)+)))\s*(?:(\S?=)\s*(['"]*)(.*?)\5|)\s*\]/);


//  Modify the ATTR preFilter function to account for new regexp match groups,
//  and normalise the namespace URI.
//
$.expr.preFilter.ATTR = function(match, curLoop, inplace, result, not, isXML) {
    var name = match[3].replace(/\\/g, "");
    if(!isXML && $.expr.attrMap[name]) {
        match[3] = $.expr.attrMap[name];
    }
    if( match[4] == "~=" ) {
        match[6] = " " + match[6] + " ";
    }
    if(!match[2] || match[2] == "|") {
        match[2] = "";
    } else {
        match[2] = getNamespaceURI(match[2]);
    }
    return match;
};


//  Modify the ATTR filter function to account for namespace selector.
//  Unfortunately this means factoring out the attribute-checking code
//  into a separate function, since it might be called multiple times.
//
var filter_attr = function(result,type,check) {
    var value = result + "";
    return result == null ?
                type === "!=" :
                type === "=" ?
                value === check :
                type === "*=" ?
                value.indexOf(check) >= 0 :
                type === "~=" ?
                (" " + value + " ").indexOf(check) >= 0 :
                !check ?
                value && result !== false :
                type === "!=" ?
                value != check :
                type === "^=" ?
                value.indexOf(check) === 0 :
                type === "$=" ?
                value.substr(value.length - check.length) === check :
                type === "|=" ?
                value === check || value.substr(0,check.length+1)===check+"-" :
                false;
}


$.expr.filter.ATTR = function(elem, match) {
    var ns = match[2];
    var name = match[3];
    var type = match[4];
    var check = match[6];
    var result;
    //  No namespace, just use ordinary attribute lookup.
    if(ns == "") {
        result = $.expr.attrHandle[name] ?
                     $.expr.attrHandle[name](elem) :
                     elem[name] != null ?
                         elem[name] :
                         elem.getAttribute(name);
        return filter_attr(result,type,check);
    }
    //  Directly use getAttributeNS if applicable and available
    if(ns != "*" && typeof elem.getAttributeNS != "undefined") {
        return filter_attr(elem.getAttributeNS(ns,name),type,check);
    }
    //  Need to iterate over all attributes, either because we couldn't
    //  look it up or because we need to match all namespaces.
    var attrs = elem.attributes;
    for(var i=0; attrs[i]; i++) {
        var ln = attrs[i].localName;
        if(!ln) {
            ln = attrs[i].nodeName
            var idx = ln.indexOf(":");
            if(idx >= 0) {
                ln = ln.substr(idx+1);
            }
        }
        if(ln == name) {
            result = attrs[i].nodeValue;
            if(ns == "*" || attrs[i].namespaceURI == ns) {
                if(filter_attr(result,type,check)) {
                    return true;
                }
            }
            if(attrs[i].namespaceURI === "" && attrs[i].prefix) {
                if(attrs[i].prefix == default_xmlns_rev[ns]) {
                    if(filter_attr(result,type,check)) {
                        return true;
                    }
                }
            }
        }
    }
    return false;
};


})(jQuery);

