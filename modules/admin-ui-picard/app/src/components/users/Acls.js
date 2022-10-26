import React, { useEffect, useState } from "react";
import { connect } from "react-redux";
import { useTranslation } from "react-i18next";
import MainNav from "../shared/MainNav";
import { Link } from "react-router-dom";
import cn from "classnames";
import TableFilters from "../shared/TableFilters";
import Table from "../shared/Table";
import Notifications from "../shared/Notifications";
import NewResourceModal from "../shared/NewResourceModal";
import { aclsTemplateMap } from "../../configs/tableConfigs/aclsTableConfig";
import { fetchFilters } from "../../thunks/tableFilterThunks";
import { fetchUsers } from "../../thunks/userThunks";
import {
	loadAclsIntoTable,
	loadGroupsIntoTable,
	loadUsersIntoTable,
} from "../../thunks/tableThunks";
import { fetchGroups } from "../../thunks/groupThunks";
import { fetchAcls } from "../../thunks/aclThunks";
import { getTotalAcls } from "../../selectors/aclSelectors";
import { editTextFilter } from "../../actions/tableFilterActions";
import { setOffset } from "../../actions/tableActions";
import { styleNavClosed, styleNavOpen } from "../../utils/componentsUtils";
import { logger } from "../../utils/logger";
import Header from "../Header";
import Footer from "../Footer";
import { hasAccess } from "../../utils/utils";
import { getUserInformation } from "../../selectors/userInfoSelectors";
import { getCurrentFilterResource } from "../../selectors/tableFilterSelectors";

/**
 * This component renders the table view of acls
 */
const Acls = ({
	loadingAcls,
	loadingAclsIntoTable,
	acls,
	loadingFilters,
	loadingUsers,
	loadingUsersIntoTable,
	loadingGroups,
	loadingGroupsIntoTable,
	resetTextFilter,
	resetOffset,
	user,
	currentFilterType,
}) => {
	const { t } = useTranslation();
	const [displayNavigation, setNavigation] = useState(false);
	const [displayNewAclModal, setNewAclModal] = useState(false);

	const loadAcls = async () => {
		// Fetching acls from server
		await loadingAcls();

		// Load acls into table
		loadingAclsIntoTable();
	};

	const loadUsers = () => {
		// Reset the current page to first page
		resetOffset();

		// Fetching users from server
		loadingUsers();

		// Load users into table
		loadingUsersIntoTable();
	};

	const loadGroups = () => {
		// Reset the current page to first page
		resetOffset();

		// Fetching groups from server
		loadingGroups();

		// Load groups into table
		loadingGroupsIntoTable();
	};

	useEffect(() => {
		if ("acls" !== currentFilterType) {
			loadingFilters("acls");
		}

		resetTextFilter();

		// Load acls on mount
		loadAcls().then((r) => logger.info(r));

		// Fetch ACLs every minute
		let fetchAclInterval = setInterval(loadAcls, 5000);

		return () => clearInterval(fetchAclInterval);
	}, []);

	const toggleNavigation = () => {
		setNavigation(!displayNavigation);
	};

	const showNewAclModal = () => {
		setNewAclModal(true);
	};

	const hideNewAclModal = () => {
		setNewAclModal(false);
	};

	return (
		<>
			<Header />
			<section className="action-nav-bar">
				{/* Add acl button */}
				<div className="btn-group">
					{hasAccess("ROLE_UI_ACLS_CREATE", user) && (
						<button className="add" onClick={() => showNewAclModal()}>
							<i className="fa fa-plus" />
							<span>{t("USERS.ACTIONS.ADD_ACL")}</span>
						</button>
					)}
				</div>

				{/* Display modal for new acl if add acl button is clicked */}
				<NewResourceModal
					showModal={displayNewAclModal}
					handleClose={hideNewAclModal}
					resource="acl"
				/>

				{/* Include Burger-button menu*/}
				<MainNav isOpen={displayNavigation} toggleMenu={toggleNavigation} />

				<nav>
					{hasAccess("ROLE_UI_USERS_VIEW", user) && (
						<Link
							to="/users/users"
							className={cn({ active: false })}
							onClick={() => loadUsers()}
						>
							{t("USERS.NAVIGATION.USERS")}
						</Link>
					)}
					{hasAccess("ROLE_UI_GROUPS_VIEW", user) && (
						<Link
							to="/users/groups"
							className={cn({ active: false })}
							onClick={() => loadGroups()}
						>
							{t("USERS.NAVIGATION.GROUPS")}
						</Link>
					)}
					{hasAccess("ROLE_UI_ACLS_VIEW", user) && (
						<Link
							to="/users/acls"
							className={cn({ active: true })}
							onClick={() => loadAcls()}
						>
							{t("USERS.NAVIGATION.PERMISSIONS")}
						</Link>
					)}
				</nav>
			</section>

			<div
				className="main-view"
				style={displayNavigation ? styleNavOpen : styleNavClosed}
			>
				{/* Include notifications component */}
				<Notifications />

				<div className="controls-container">
					{/* Include filters component */}
					<TableFilters
						loadResource={loadingAcls}
						loadResourceIntoTable={loadingAclsIntoTable}
						resource={"acls"}
					/>
					<h1>{t("USERS.ACLS.TABLE.CAPTION")}</h1>
					<h4>{t("TABLE_SUMMARY", { numberOfRows: acls })}</h4>
				</div>
				{/* Include table component */}
				<Table templateMap={aclsTemplateMap} />
			</div>
			<Footer />
		</>
	);
};

// Getting state data out of redux store
const mapStateToProps = (state) => ({
	acls: getTotalAcls(state),
	user: getUserInformation(state),
	currentFilterType: getCurrentFilterResource(state),
});

// Mapping actions to dispatch
const mapDispatchToProps = (dispatch) => ({
	loadingFilters: (resource) => dispatch(fetchFilters(resource)),
	loadingAcls: () => dispatch(fetchAcls()),
	loadingAclsIntoTable: () => dispatch(loadAclsIntoTable()),
	loadingUsers: () => dispatch(fetchUsers()),
	loadingUsersIntoTable: () => dispatch(loadUsersIntoTable()),
	loadingGroups: () => dispatch(fetchGroups()),
	loadingGroupsIntoTable: () => dispatch(loadGroupsIntoTable()),
	resetTextFilter: () => dispatch(editTextFilter("")),
	resetOffset: () => dispatch(setOffset(0)),
});

export default connect(mapStateToProps, mapDispatchToProps)(Acls);
