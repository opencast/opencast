import AclsActionsCell from "../../components/users/partials/AclsActionsCell";
/**
 * Config that contains the columns and further information regarding acls. These are the information that never or hardly changes.
 * That's why it is hard coded here and not fetched from server.
 * Information configured in this file:
 * - columns: names, labels, sortable, (template)
 * - caption for showing in table view
 * - resource type (here: acls)
 * - category type (here: users)
 * - is multi select possible?
 */
export const aclsTableConfig = {
	columns: [
		{
			name: "name",
			label: "USERS.ACLS.TABLE.NAME",
			sortable: true,
		},
		{
			template: "AclsActionsCell",
			name: "actions",
			label: "USERS.ACLS.TABLE.ACTION",
		},
	],
	caption: "USERS.ACLS.TABLE.CAPTION",
	resource: "acls",
	category: "users",
	multiSelect: false,
};

/**
 * This map contains the mapping between the template strings above and the corresponding react component.
 * This helps to render different templates of cells more dynamically. Even empty needed, because Table component
 * uses template map.
 */
export const aclsTemplateMap = {
	AclsActionsCell: AclsActionsCell,
};
