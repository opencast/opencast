import React from "react";

/**
 * This component renders the roles cells of users in the table view
 */
const UsersRolesCell = ({ row }) => {
    return (
        row.roles.map((role, key) => (
            <span key={key}>
                {role.name},
            </span>
        ))
    )
}

export default UsersRolesCell;
