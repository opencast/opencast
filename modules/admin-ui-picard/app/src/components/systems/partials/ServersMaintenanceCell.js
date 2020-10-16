import React from "react";

/**
 * This component renders the maintenance cells of servers in the table view
 */
const ServersMaintenanceCell = ({ row }) => {
    return (
        <>
            {/*Todo: With role*/}
            {/*Todo: Implement OnChange (see old UI)*/}
            <input type="checkbox"
                   onChange={() => onClickPlaceholder()}
                   name="maintenanceStatus"
                   checked={row.maintenance}/>
        </>
    );
}

//todo: remove if not needed anymore
const onClickPlaceholder = () => {
    console.log("In the Future here happens something, which is not implemented yet");
}

export default ServersMaintenanceCell;
