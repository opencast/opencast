import React from "react";

/**
 * This component renders the roles cells of users in the table view
 */
const UsersRolesCell = ({ row }) => {
	const getRoleString = () => {
		let roleString = "";

		row.roles.forEach((role) => {
			roleString = roleString.concat(role.name + ", ");
		});

		return roleString;
	};

	return <span>{getRoleString()}</span>;
};

export default UsersRolesCell;
