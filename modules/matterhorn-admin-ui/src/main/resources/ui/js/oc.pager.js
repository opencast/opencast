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
var ocPager = ocPager || {};

ocPager.pageSize = 20;
ocPager.currentPageIdx = 0;

ocPager.init = function() {
  // Event: change of pagesize selector
  $('.paging-nav-pagesize-selector').change(function() {
    var val = $(this).val();
    $('.paging-nav-pagesize-selector').val(val);
    ocPager.update(val, ocPager.currentPageIdx);
    ocRecordings.displayRecordings(ocRecordings.currentState, true);
  });

  // Event: text entered
  $('.paging-nav-goto').keyup(function(event) {
    if (event.keyCode == 13) {
      var val = $(this).val();
      if ((val !== '') && (!isNaN(val))) {
        ocPager.update(ocPager.pageSize, val-1);
        ocRecordings.displayRecordings(ocRecordings.currentState, true);
        $(this).val('');
      }
    }
  });

  // Event: pager nav next clicked
  $('.paging-nav-go-next').click(function() {
    ocPager.update(ocPager.pageSize, ocPager.currentPageIdx+1);
    ocRecordings.displayRecordings(ocRecordings.currentState, true);
  });

  // Event: pager nav previous clicked
  $('.paging-nav-go-previous').click(function() {
    ocPager.update(ocPager.pageSize, ocPager.currentPageIdx-1);
    ocRecordings.displayRecordings(ocRecordings.currentState, true);
  });

  $('.paging-nav-pagesize-selector').each( function() {
    $(this).val(ocPager.pageSize);
  })
}

ocPager.update = function(size, current) {
  ocPager.pageSize = size;
  var numPages = Math.ceil(ocRecordings.lastCount/size);     // number of pages
  
  if (current >= numPages) {
    current = numPages - 1;
  }
  if (current < 0) {
    current = 0;
  }
  ocPager.currentPageIdx = current;

  // take care for prev and next links
  if (ocPager.currentPageIdx == 0) {
    $('.paging-mocklink-prev').css('display', 'inline');
    $('.paging-nav-go-previous').css('display', 'none');
  } else {
    $('.paging-mocklink-prev').css('display', 'none');
    $('.paging-nav-go-previous').css('display', 'inline');
  }
  if (ocPager.currentPageIdx >= numPages-1) {
    $('.paging-mocklink-next').css('display','inline');
    $('.paging-nav-go-next').css('display','none');
  } else {
    $('.paging-mocklink-next').css('display','none');
    $('.paging-nav-go-next').css('display','inline');
  }

  // populate UI fields
  $('.paging-nav-current').each(function() {
    $(this).text(ocPager.currentPageIdx+1);
  });
  $('.paging-nav-total').each(function() {
    $(this).text(numPages);
  });
  //alert("pageSize: " + ocPager.pageSize + "\ncurrent: " + ocPager.currentPageIdx + "\nitems: " + ocRecordings.lastCount + "\npages: " + numPages + "\nstate: " + ocRecordings.currentState);
}

