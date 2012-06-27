<div>
  <table>
    <tr>
      <td class="td-key">Series Title:</td>
      <td class="td-value"><%= (data[j].dc.title) ? data[j].dc.title : '' %></td>
    </tr>
    <tr>
      <td class="td-key">Organizer:</td>
      <td class="td-value"><%= (data[j].dc.creator) ? data[j].dc.creator : '' %></td>
    </tr>
    <tr>
      <td class="td-key">Contributor:</td>
      <td class="td-value"><%= (data[j].dc.contributor) ? data[j].dc.contributor : '' %></td>
    </tr>
    <tr>
      <td class="td-key">Subject:</td>
      <td class="td-value"><%= (data[j].dc.subject) ? data[j].dc.subject : '' %></td>
    </tr>
    <tr>
      <td class="td-key">Language:</td>
      <td class="td-value"><%= (data[j].dc.language) ? data[j].dc.language : '' %></td>
    </tr>
    <tr>
      <td class="td-key">License:</td>
      <td class="td-value"><%= (data[j].dc.license) ? data[j].dc.license : '' %></td>
    </tr>
    <tr>
      <td class="td-key">Description:</td>
      <td class="td-value"><%= (data[j].dc.description) ? data[j].dc.description : '' %></td>
    </tr>
  </table>
</div>