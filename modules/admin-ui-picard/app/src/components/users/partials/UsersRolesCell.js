import React from "react";

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
