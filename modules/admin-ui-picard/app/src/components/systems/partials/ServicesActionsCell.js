import React from "react";
import { useTranslation } from "react-i18next";
import { connect } from "react-redux";
import { fetchServices, restartService } from "../../../thunks/serviceThunks";
import { loadServicesIntoTable } from "../../../thunks/tableThunks";
import { getUserInformation } from "../../../selectors/userInfoSelectors";
import { hasAccess } from "../../../utils/utils";

/**
 * This component renders the action cells of services in the table view
 */
const ServicesActionCell = ({
	row,
	loadServices,
	loadServicesIntoTable,
	user,
}) => {
	const { t } = useTranslation();

	const onClickRestart = async () => {
		await restartService(row.hostname, row.name);
		await loadServices();
		loadServicesIntoTable();
	};

	return (
		row.status !== "SYSTEMS.SERVICES.STATUS.NORMAL" &&
		hasAccess("ROLE_UI_SERVICES_STATUS_EDIT", user) && (
			<a
				className="sanitize fa fa-undo"
				onClick={() => onClickRestart()}
				title={t("SYSTEMS.SERVICES.TABLE.SANITIZE")}
			/>
		)
	);
};

// Getting state data out of redux store
const mapStateToProps = (state) => ({
	user: getUserInformation(state),
});

// mapping actions to dispatch
const mapDispatchToProps = (dispatch) => ({
	loadServices: () => dispatch(fetchServices()),
	loadServicesIntoTable: () => dispatch(loadServicesIntoTable()),
});

export default connect(mapStateToProps, mapDispatchToProps)(ServicesActionCell);
