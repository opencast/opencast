import GroupsActionsCell from "../../components/users/partials/GroupsActionsCell";

/**
 * Config that contains the columns and further information regarding groups. These are the information that never or hardly changes.
 * That's why it is hard coded here and not fetched from server.
 * Information configured in this file:
 * - columns: names, labels, sortable, (template)
 * - caption for showing in table view
 * - resource type (here: groups)
 * - category type (here: users)
 * - is multi select possible?
 */
export const groupsTableConfig = {
	columns: [
		{
			name: "name",
			label: "USERS.GROUPS.TABLE.NAME",
			sortable: true,
		},
		{
			name: "description",
			label: "USERS.GROUPS.TABLE.DESCRIPTION",
			sortable: true,
		},
		{
			name: "role",
			label: "USERS.GROUPS.TABLE.ROLE",
			sortable: true,
		},
		{
			template: "GroupsActionsCell",
			name: "actions",
			label: "USERS.USERS.TABLE.ACTION",
		},
	],
	caption: "USERS.GROUPS.TABLE.CAPTION",
	resource: "groups",
	category: "users",
	multiSelect: false,
};

/**
 * This map contains the mapping between the template strings above and the corresponding react component.
 * This helps to render different templates of cells more dynamically. Even empty needed, because Table component
 * uses template map.
 */
export const groupsTemplateMap = {
	GroupsActionsCell: GroupsActionsCell,
};
