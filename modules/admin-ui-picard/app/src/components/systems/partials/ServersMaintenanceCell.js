import React from "react";
import {connect} from "react-redux";
import {fetchServers, setServerMaintenance} from "../../../thunks/serverThunks";
import {loadServersIntoTable} from "../../../thunks/tableThunks";

/**
 * This component renders the maintenance cells of servers in the table view
 */
const ServersMaintenanceCell = ({ row, loadServers, loadServersIntoTable }) => {

    const onClickCheckbox = async e => {
        await setServerMaintenance(row.hostname, e.target.checked);
        await loadServers();
        loadServersIntoTable();
    }

    return (
        <>
            {/*Todo: With role*/}
            <input type="checkbox"
                   onChange={e => onClickCheckbox(e)}
                   name="maintenanceStatus"
                   checked={row.maintenance}/>
        </>
    );
}

// mapping actions to dispatch
const mapDispatchToProps = dispatch => ({
    loadServers: () => dispatch(fetchServers()),
    loadServersIntoTable: () => dispatch(loadServersIntoTable())
});

export default connect(null, mapDispatchToProps)(ServersMaintenanceCell);
