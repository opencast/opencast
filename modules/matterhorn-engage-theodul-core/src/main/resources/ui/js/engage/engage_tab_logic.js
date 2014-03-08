/**
 * Copyright 2009-2011 The Regents of the University of California Licensed
 * under the Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 * 
 * http://www.osedu.org/licenses/ECL-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 */
/*jslint browser: true, nomen: true*/
/*global define, CustomEvent*/
define(['jquery', 'bootstrap'], function ($, Bootstrap) {
  //
  'use strict'; // strict mode in all our application
  //
  // Sorts bootstrap tabs and stores the current state
  // * The ul tag must have an id.
  // * id's of the li tags are overridden with the tab_id_prefix and a tab count
  function bootstrapTabLogic(tab_id_prefix, selector) {
    var TAB_IDS_PREFIX = tab_id_prefix;

    var extractIdFromHref = function (a_node){
      var regex_href_id = /#(\w+)$/i;
      return a_node.href.match(regex_href_id)[1];
    };

    // Based on http://stackoverflow.com/questions/11344531/pure-javascript-store-object-in-cookie
    var readCookie = function(name) {
      var result = null;
      if (navigator.cookieEnabled) {
        result = document.cookie.match(new RegExp(name + '=([^;]+)'));
        result && (result = JSON.parse(result[1]));
      }
      else {
        console.log('Cookies disabled. Can not read from cookie.'); 
      }
      return result;
    };

    var addIdsToBootstrapTabs = function(selector) {
      // Give each li an id
      $('#' + selector + ' li').each(function(index) {
        $(this).attr('id', TAB_IDS_PREFIX + index);
      });
    };

    var sortBootstrapTabsAlphabetically = function(selector) {
      // Get tabs
      var $tabs = [];
      console.log('Sorting tabs alphabetically');
      $.each($('#' + selector +' a'), function(index, value) {
        $tabs[index] = $(this).detach();
      });

      // Sort tabs by tab text
      $tabs.sort( function(a, b) {
        if (a[0].id > b[0].id)
          return 1;
        if (a[0].id < b[0].id)
          return -1;
        // a must be equal to b
        return 0;
      });

      // Sort tabs
      $.each($tabs, function(index, value) {
        $('#' + TAB_IDS_PREFIX + index).append($tabs[index]);
      });
    };

    // Based on an example from javascript documentation
    var addDraggingToBootstrapTabs = function (selector) {
      var drag = null;
      console.log('Activating dragging of tabs');
      $.each($('#' + selector + ' a'), function(index, value) {
        $(this).addClass('draggable');
        $(this).addClass('dropzone');
      });

      // events fired on the draggable target
      document.addEventListener('drag', function(event) {
      }, false);

      document.addEventListener('dragstart', function(event) {
        // store a ref. on the dragged elem
        if ($(event.target).hasClass('draggable')) {
          drag = $(event.target);
          // make it half transparent
          drag.css('opacity', 0.5);
        } else {
          drag = null;
        }
      }, false);

      document.addEventListener('dragend', function(event) {
        // reset the transparency
        $(drag).css('opacity', 1.0);
      }, false);

      // events fired on the drop targets
      document.addEventListener('dragover', function(event) {
        // prevent default to allow drop
        event.preventDefault();
      }, false);

      document.addEventListener('dragenter', function(event) {
        // highlight potential drop target when the draggable element enters it
        var dragenter = $(event.target);
        if ( dragenter.hasClass('dropzone')) {
          dragenter.css('opacity', 0.5);
        }
      }, false);

      document.addEventListener('dragleave', function(event) {
        // reset background of potential drop target when the draggable element leaves it
        var dragleave = $(event.target);
        if ( dragleave.hasClass('dropzone')) {
          dragleave.css('opacity', 1.0);
        }
      }, false);

      document.addEventListener('drop', function(event) {
        // prevent default action (open as link for some elements)
        event.preventDefault();
        // move dragged elem to the selected drop target
        var drop = $(event.target);
        if ( drag !== null && drop.hasClass('dropzone')) {
          drop.css('opacity',1.0);
          var active_class = 'active';
          var drop_parent = drop.parent();
          var drag_parent = drag.parent();

          // Switch nodes
          var drag_detached = drag.detach();
          drag_parent.append(drop.detach());
          drop_parent.append(drag);

          // Active the dragged tab
          if (drag_parent.hasClass(active_class)) {
            drag_parent.removeClass(active_class);
            drop_parent.addClass(active_class);
            saveBootstrapActiveTab(drop_parent);
          }
          else if (drop_parent.hasClass(active_class)) {
            drop_parent.removeClass(active_class);
            drag_parent.addClass(active_class);
            saveBootstrapActiveTab(drag_parent);
          }
          saveBootstrapTabSortingAsCookie(selector);
        }
      }, false);
    };

    var addBootstrapTabClickBehaviour = function(selector) {
      // click listener to change tab
      console.log('Adding click behaviour to tabs');
      $('#' + selector + ' a').click(function (e) {
        e.preventDefault();
        $(this).tab('show');
        saveBootstrapActiveTab($(this).parent());
      });
    };

    var saveBootstrapActiveTab = function(tab) {
      if (navigator.cookieEnabled) {
        console.log('Saving active tab');
        document.cookie= TAB_IDS_PREFIX + '_active=' + JSON.stringify(tab.attr('id'));
      }
      else {
        console.log('Cookies disabled. Active tab not saved.');
      }
    };

    var saveBootstrapTabSortingAsCookie = function(selector) {
      var ahref = null;
      // save tabs in cookie
      if (navigator.cookieEnabled) {
        console.log('Saving tabs positions to cookie');
        $.each($('#' + selector + ' a'), function (index, value) {
          ahref = extractIdFromHref(this);
          document.cookie = $(this).parent().attr('id') + '=' + JSON.stringify(ahref);
        });
      }
      else {
        console.log('Cookies disabled. Tabs positions not saved.');
      }
    };

    var activateBootstrapTabOnPosition = function(selector, tabnr) {
      var ahref = null;
      var activation_class = 'active';
      var activated = $('#' + selector + ' li:nth-child(' + tabnr + ')');
      if( activated.length > 0 ) {
        console.log('Activating tab with number ' + tabnr);
        activated.addClass(activation_class);
        ahref = extractIdFromHref(activated[0].firstElementChild);
        $('#' + ahref).addClass(activation_class);
      }
      else {
        console.log('Activating first tab');
        activated = $('#' + selector + ' li:first').addClass(activation_class);
        ahref = extractIdFromHref(activated[0].firstElementChild);
        $('#' + ahref).addClass(activation_class);
      }
    };

    var restoreActiveBootstrapTabFromCookie = function(selector) {
      if (navigator.cookieEnabled) {
        var active_tab = readCookie(TAB_IDS_PREFIX + '_active');
        console.log('Reactiving last selected tab from cookie');
        if (active_tab !== undefined && active_tab !== null) {
          // Reduce to tab number and add one for the child selector
          active_tab = active_tab.replace(TAB_IDS_PREFIX,'');
        }
        else {
          // Select first tab as failback
          active_tab = 0;
        }
        activateBootstrapTabOnPosition(selector, parseInt(active_tab, 10)+1);
      }
      else {
        console.log('Cookies not available. Last active tab not restored.'); 
      }
    };

    var restoreBootstrapTabsSortingFromCookie = function(selector) {
      var left_tabs = [];
      var restore_tab_positions = [];
      var detached_tabs = {};
      if (navigator.cookieEnabled) {
        var cookies = readCookie(TAB_IDS_PREFIX + '1');
        console.log('Restoring tab positions from cookie');
        // Try to find tab links in html from saved data in cookie by href
        $.each($('#' + selector + ' li'), function (index, value) {
          var tab_link_id = readCookie(TAB_IDS_PREFIX + index);
          // No tab content id for tab position found
          if (tab_link_id === undefined || tab_link_id === null) {
            console.log('No tab content saved for ' + value.id + '.');
          }
          else {
            // Search link in html and store on last/saved postition in array
            var found = false;
            $('#' + selector + ' a').each( function(index_a) {
              var href_id = new RegExp('#' + tab_link_id,'i');
              if( this.href.match(href_id)) {
                restore_tab_positions[index] = $(this).detach();
                found = true;
              }
            });
            // No node for saved tab content id found
            if (!found) {
              restore_tab_positions[index] = null;
              console.log('No tab content found for tab with id ' + tab_link_id + '.');
            }
          }
        });
        // Detach any left tab link (may be new ones)
        $.each($('#' + selector + ' a'), function(index,value) {
          left_tabs[index] = $(value);
        });

        var restored_tabs = restore_tab_positions.concat(left_tabs);
        // Remove empty array elements like "",null, undefined and 0;
        restored_tabs = restored_tabs.filter(function(e){
          return e;
        });

        // Attach link to tabs
        $.each($('#' + selector + ' li'), function (index, value) {
          $(this).append(restored_tabs[index]);
        });
      }
    };

    addIdsToBootstrapTabs(selector);
    sortBootstrapTabsAlphabetically(selector);
    restoreBootstrapTabsSortingFromCookie(selector);
    restoreActiveBootstrapTabFromCookie(selector);
    addBootstrapTabClickBehaviour(selector);
    addDraggingToBootstrapTabs(selector);
  }

  return bootstrapTabLogic;
});
