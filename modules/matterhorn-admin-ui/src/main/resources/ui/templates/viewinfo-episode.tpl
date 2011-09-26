<div>
  <table>
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
      <td class="td-key">Description:</td>
      <td class="td-value"><%= (data[j].dc.description) ? data[j].dc.description : '' %></td>
    </tr>
  </table>
</div>