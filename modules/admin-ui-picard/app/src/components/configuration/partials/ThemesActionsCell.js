import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { connect } from "react-redux";
import ConfirmModal from "../../shared/ConfirmModal";
import ThemeDetailsModal from "./wizard/ThemeDetailsModal";
import { deleteTheme } from "../../../thunks/themeThunks";
import {
	fetchThemeDetails,
	fetchUsage,
} from "../../../thunks/themeDetailsThunks";
import { getUserInformation } from "../../../selectors/userInfoSelectors";
import { hasAccess } from "../../../utils/utils";

/**
 * This component renders the action cells of themes in the table view
 */
const ThemesActionsCell = ({
	row,
	deleteTheme,
	fetchThemeDetails,
	fetchUsage,
	user,
}) => {
	const { t } = useTranslation();

	const [displayDeleteConfirmation, setDeleteConfirmation] = useState(false);
	const [displayThemeDetails, setThemeDetails] = useState(false);

	const hideDeleteConfirmation = () => {
		setDeleteConfirmation(false);
	};

	const hideThemeDetails = () => {
		setThemeDetails(false);
	};

	const showThemeDetails = async () => {
		await fetchThemeDetails(row.id);
		await fetchUsage(row.id);

		setThemeDetails(true);
	};

	const deletingTheme = (id) => {
		deleteTheme(id);
	};

	return (
		<>
			{/* edit themes */}
			{hasAccess("ROLE_UI_THEMES_EDIT", user) && (
				<a
					onClick={() => showThemeDetails()}
					className="more"
					title={t("CONFIGURATION.THEMES.TABLE.TOOLTIP.DETAILS")}
				/>
			)}

			{displayThemeDetails && (
				<ThemeDetailsModal
					handleClose={hideThemeDetails}
					themeId={row.id}
					themeName={row.name}
				/>
			)}

			{/* delete themes */}
			{hasAccess("ROLE_UI_THEMES_DELETE", user) && (
				<a
					onClick={() => setDeleteConfirmation(true)}
					className="remove ng-scope ng-isolate-scope"
					title={t("CONFIGURATION.THEMES.TABLE.TOOLTIP.DELETE")}
				/>
			)}

			{displayDeleteConfirmation && (
				<ConfirmModal
					close={hideDeleteConfirmation}
					resourceName={row.name}
					resourceId={row.id}
					deleteMethod={deletingTheme}
					resourceType="THEME"
				/>
			)}
		</>
	);
};

// Getting state data out of redux store
const mapStateToProps = (state) => ({
	user: getUserInformation(state),
});

// Mapping actions to dispatch
const mapDispatchToProps = (dispatch) => ({
	deleteTheme: (id) => dispatch(deleteTheme(id)),
	fetchThemeDetails: (id) => dispatch(fetchThemeDetails(id)),
	fetchUsage: (id) => dispatch(fetchUsage(id)),
});

export default connect(mapStateToProps, mapDispatchToProps)(ThemesActionsCell);
