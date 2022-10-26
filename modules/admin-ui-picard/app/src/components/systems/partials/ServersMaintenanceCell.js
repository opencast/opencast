import React from "react";
import { connect } from "react-redux";
import {
	fetchServers,
	setServerMaintenance,
} from "../../../thunks/serverThunks";
import { loadServersIntoTable } from "../../../thunks/tableThunks";
import { getUserInformation } from "../../../selectors/userInfoSelectors";
import { hasAccess } from "../../../utils/utils";

/**
 * This component renders the maintenance cells of servers in the table view
 */
const ServersMaintenanceCell = ({
	row,
	loadServers,
	loadServersIntoTable,
	user,
}) => {
	const onClickCheckbox = async (e) => {
		await setServerMaintenance(row.hostname, e.target.checked);
		await loadServers();
		loadServersIntoTable();
	};

	return (
		<>
			{hasAccess("ROLE_UI_SERVERS_MAINTENANCE_EDIT", user) && (
				<input
					type="checkbox"
					onChange={(e) => onClickCheckbox(e)}
					name="maintenanceStatus"
					checked={row.maintenance}
				/>
			)}
		</>
	);
};

// Getting state data out of redux store
const mapStateToProps = (state) => ({
	user: getUserInformation(state),
});

// mapping actions to dispatch
const mapDispatchToProps = (dispatch) => ({
	loadServers: () => dispatch(fetchServers()),
	loadServersIntoTable: () => dispatch(loadServersIntoTable()),
});

export default connect(
	mapStateToProps,
	mapDispatchToProps
)(ServersMaintenanceCell);
