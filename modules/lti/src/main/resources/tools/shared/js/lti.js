(function() {
  function getURLParameter(name) {
      return decodeURIComponent((new RegExp('[?|&]' + name + '=' + '([^&;]+?)(&|#|;|$)').exec(location.search)||[,""])[1].replace(/\+/g, '%20')) || '';
  }

 function xhr(params, cb, fail, always) {
  if (!params.url) {
    if (fail && typeof fail == 'function') {
      fail(new Error('no URL provided'));
    }
    return;
  }
  params.type = params.type ? params.type.toUpperCase() : 'GET';
  params.data = params.data || null;
  var request = new XMLHttpRequest();
  request.open(params.type, params.url, true);
  request.onload = function() {
    if (request.status < 300) {
      if (cb && typeof cb == 'function') {
        if (params.responseType) {
          switch(params.responseType) {
            case 'json':
              try {
                cb(JSON.parse(request.responseText));
              } catch(e) {
                if (fail && typeof fail == 'function') {
                }
              }
              break;
            case 'document':
              cb(request.response);
              break;
            default:
              cb(request);
              break;
          }
        }
        else cb(request);
      }
    }
    else if (request.stats > 399) {
      if (fail && typeof fail == 'function') {
        fail(request);
      }
    }
  };
  request.onerror = function() {
  }
  request.send(params.data);
}

var courseID = getURLParameter("sid");
var uploadSetting = getURLParameter('upload') || false;
var isAdmin;

function setRoles(roles, accessTypes) {
  isAdmin = roles.indexOf('ROLE_ADMIN') > -1;
  if (!isAdmin) {
    var adminRoles = accessTypes                           //contains all the 'write' roles
                       .filter(function(accessType) {
                         return accessType.allow && accessType.action == 'write';
                       })
                       .map(function(accessType) {
                         return accessType.role;
                       });

    roles.some(function(role) {
      isAdmin = adminRoles.indexOf(role) > -1;
      return isAdmin;
    });
  }

  if (isAdmin) {
    document.querySelector('.lti_links')
      .classList.remove('student');

    Array.prototype.slice.call(document.querySelectorAll('.lti_links li a, #manageNotificationModal a, #refreshModal a'))
      .forEach(function(anchor) {
        anchor.href = anchor.href.replace('%courseID%', courseID).replace('%upload%', uploadSetting);
      });

    

    if ($('#manageNotificationModal').length > 0) {
      var eventList = '/admin-ng/event/events.json?filter=series:' + courseID;
      xhr({url: eventList, responseType: 'json'},
        function(res) {
          if (res.total === 0) {
            var notify = true;
            try {
              var noRemind = localStorage.getItem('manageNotify');
              if (noRemind && noRemind === 'true') {
                notify = false;
              }
            } catch(e) {
            }
            if (notify) {
              $('#manageNotificationModal').addClass('in');
            }
          }
        }
      );
    }
  }
}

var downloadButton = document.querySelector('.button[data-icon=download]');
if (downloadButton) {
  downloadButton.href = downloadButton.href.replace('%courseID%', courseID).replace('%upload%', uploadSetting);
}

xhr({url: "/info/me.json", responseType: 'json'},
  function(response) {
    xhr({url: '/series/' + courseID + '/acl.json', responseType: 'json'}, function(acl) {
      try {
        setRoles(response.roles, acl.acl.ace || []);
      } catch(all) { 
        console.log(all);
      }
    }, function(aclErr) {
      console.log('acl err');
    });
  },
  function(err) {
      console.log('info err');
  }
);


})();
