import React from "react";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "react-router-dom";
import { connect } from "react-redux";
import {
	loadAclsIntoTable,
	loadEventsIntoTable,
	loadGroupsIntoTable,
	loadJobsIntoTable,
	loadRecordingsIntoTable,
	loadSeriesIntoTable,
	loadServersIntoTable,
	loadServicesIntoTable,
	loadThemesIntoTable,
	loadUsersIntoTable,
} from "../../thunks/tableThunks";
import { fetchEvents } from "../../thunks/eventThunks";
import { fetchRecordings } from "../../thunks/recordingThunks";
import { fetchJobs } from "../../thunks/jobThunks";
import { fetchUsers } from "../../thunks/userThunks";
import { fetchThemes } from "../../thunks/themeThunks";
import { fetchFilters, fetchStats } from "../../thunks/tableFilterThunks";
import { setOffset } from "../../actions/tableActions";
import { getUserInformation } from "../../selectors/userInfoSelectors";
import { hasAccess } from "../../utils/utils";
import { fetchSeries } from "../../thunks/seriesThunks";
import { fetchServers } from "../../thunks/serverThunks";
import { fetchServices } from "../../thunks/serviceThunks";
import { fetchGroups } from "../../thunks/groupThunks";
import { fetchAcls } from "../../thunks/aclThunks";
import { GlobalHotKeys } from "react-hotkeys";
import { availableHotkeys } from "../../configs/hotkeysConfig";

/**
 * This component renders the main navigation that opens when the burger button is clicked
 */
const MainNav = ({
	isOpen,
	toggleMenu,
	loadingEvents,
	loadingEventsIntoTable,
	loadingSeries,
	loadingSeriesIntoTable,
	loadingStats,
	loadingRecordings,
	loadingRecordingsIntoTable,
	loadingJobs,
	loadingJobsIntoTable,
	loadingServers,
	loadingServersIntoTable,
	loadingServices,
	loadingServicesIntoTable,
	loadingUsers,
	loadingUsersIntoTable,
	loadingGroups,
	loadingGroupsIntoTable,
	loadingAcls,
	loadingAclsIntoTable,
	loadingThemes,
	loadingThemesIntoTable,
	resetOffset,
	user,
	loadingFilters,
}) => {
	const { t } = useTranslation();
	let navigate = useNavigate();

	const loadEvents = () => {
		loadingFilters("events");

		// Reset the current page to first page
		resetOffset();

		// Fetching stats from server
		loadingStats();

		// Fetching events from server
		loadingEvents();

		// Load events into table
		loadingEventsIntoTable();
	};

	const loadSeries = () => {
		loadingFilters("series");

		// Reset the current page to first page
		resetOffset();

		// Fetching series from server
		loadingSeries();

		// Load series into table
		loadingSeriesIntoTable();
	};

	const loadRecordings = () => {
		loadingFilters("recordings");

		// Reset the current page to first page
		resetOffset();

		// Fetching recordings from server
		loadingRecordings();

		// Load recordings into table
		loadingRecordingsIntoTable();
	};

	const loadJobs = () => {
		loadingFilters("jobs");

		// Reset the current page to first page
		resetOffset();

		// Fetching jobs from server
		loadingJobs();

		// Load jobs into table
		loadingJobsIntoTable();
	};

	const loadServers = () => {
		loadingFilters("servers");

		// Reset the current page to first page
		resetOffset();

		// Fetching servers from server
		loadingServers();

		// Load servers into table
		loadingServersIntoTable();
	};

	const loadServices = () => {
		loadingFilters("services");

		// Reset the current page to first page
		resetOffset();

		// Fetching services from server
		loadingServices();

		// Load services into table
		loadingServicesIntoTable();
	};

	const loadUsers = () => {
		loadingFilters("users");

		// Reset the current page to first page
		resetOffset();

		// Fetching users from server
		loadingUsers();

		// Load users into table
		loadingUsersIntoTable();
	};

	const loadGroups = () => {
		loadingFilters("groups");

		// Reset the current page to first page
		resetOffset();

		// Fetching groups from server
		loadingGroups();

		// Load groups into table
		loadingGroupsIntoTable();
	};

	const loadAcls = () => {
		loadingFilters("acls");

		// Reset the current page to first page
		resetOffset();

		// Fetching acls from server
		loadingAcls();

		// Load acls into table
		loadingAclsIntoTable();
	};

	const loadThemes = () => {
		loadingFilters("themes");

		// Reset the current page to first page
		resetOffset();

		// Fetching themes from server
		loadingThemes();

		// Load themes into table
		loadingThemesIntoTable();
	};

	const hotkeyLoadEvents = () => {
		navigate("/events/events");
	};

	const hotkeyLoadSeries = () => {
		navigate("/events/series");
	};

	const hotKeyHandlers = {
		EVENT_VIEW: hotkeyLoadEvents,
		SERIES_VIEW: hotkeyLoadSeries,
		MAIN_MENU: toggleMenu,
	};
	return (
		<>
			<GlobalHotKeys
				keyMap={availableHotkeys.general}
				handlers={hotKeyHandlers}
			/>
			<div className="menu-top" onClick={() => toggleMenu()}>
				{isOpen && (
					<nav id="roll-up-menu">
						<div id="nav-container">
							{/* todo: more than one href? how? roles? (see MainNav admin-ui-frontend)*/}
							{hasAccess("ROLE_UI_NAV_RECORDINGS_VIEW", user) &&
								(hasAccess("ROLE_UI_EVENTS_VIEW", user) ? (
									<Link to="/events/events" onClick={() => loadEvents()}>
										<i className="events" title={t("NAV.EVENTS.TITLE")} />
									</Link>
								) : (
									hasAccess("ROLE_UI_SERIES_VIEW", user) && (
										<Link to="/events/series" onClick={() => loadSeries()}>
											<i className="events" title={t("NAV.EVENTS.TITLE")} />
										</Link>
									)
								))}
							{hasAccess("ROLE_UI_NAV_CAPTURE_VIEW", user) &&
								hasAccess("ROLE_UI_LOCATIONS_VIEW", user) && (
									<Link
										to="/recordings/recordings"
										onClick={() => loadRecordings()}
									>
										<i
											className="recordings"
											title={t("NAV.CAPTUREAGENTS.TITLE")}
										/>
									</Link>
								)}
							{hasAccess("ROLE_UI_NAV_SYSTEMS_VIEW", user) &&
								(hasAccess("ROLE_UI_JOBS_VIEW", user) ? (
									<Link to="/systems/jobs" onClick={() => loadJobs()}>
										<i className="systems" title={t("NAV.SYSTEMS.TITLE")} />
									</Link>
								) : hasAccess("ROLE_UI_SERVERS_VIEW", user) ? (
									<Link to="/systems/servers" onClick={() => loadServers()}>
										<i className="systems" title={t("NAV.SYSTEMS.TITLE")} />
									</Link>
								) : (
									hasAccess("ROLE_UI_SERVICES_VIEW", user) && (
										<Link to="/systems/services" onClick={() => loadServices()}>
											<i className="systems" title={t("NAV.SYSTEMS.TITLE")} />
										</Link>
									)
								))}
							{hasAccess("ROLE_UI_NAV_ORGANIZATION_VIEW", user) &&
								(hasAccess("ROLE_UI_USERS_VIEW", user) ? (
									<Link to="/users/users" onClick={() => loadUsers()}>
										<i className="users" title={t("NAV.USERS.TITLE")} />
									</Link>
								) : hasAccess("ROLE_UI_GROUPS_VIEW", user) ? (
									<Link to="/users/groups" onClick={() => loadGroups()}>
										<i className="users" title={t("NAV.USERS.TITLE")} />
									</Link>
								) : (
									hasAccess("ROLE_UI_ACLS_VIEW", user) && (
										<Link to="/users/acls" onClick={() => loadAcls()}>
											<i className="users" title={t("NAV.USERS.TITLE")} />
										</Link>
									)
								))}
							{hasAccess("ROLE_UI_NAV_CONFIGURATION_VIEW", user) &&
								hasAccess("ROLE_UI_THEMES_VIEW", user) && (
									<Link to="/configuration/themes" onClick={() => loadThemes()}>
										<i
											className="configuration"
											title={t("NAV.CONFIGURATION.TITLE")}
										/>
									</Link>
								)}
							{hasAccess("ROLE_UI_NAV_STATISTICS_VIEW", user) &&
								hasAccess("ROLE_UI_STATISTICS_ORGANIZATION_VIEW", user) && (
									<Link to="/statistics/organization">
										<i
											className="statistics"
											title={t("NAV.STATISTICS.TITLE")}
										/>
									</Link>
								)}
						</div>
					</nav>
				)}
			</div>
		</>
	);
};

// Getting state data out of redux store
const mapStateToProps = (state) => ({
	user: getUserInformation(state),
});

// Mapping actions to dispatch
const mapDispatchToProps = (dispatch) => ({
	loadingEvents: () => dispatch(fetchEvents()),
	loadingEventsIntoTable: () => dispatch(loadEventsIntoTable()),
	loadingSeries: () => dispatch(fetchSeries()),
	loadingSeriesIntoTable: () => dispatch(loadSeriesIntoTable()),
	loadingStats: () => dispatch(fetchStats()),
	loadingRecordings: () => dispatch(fetchRecordings()),
	loadingRecordingsIntoTable: () => dispatch(loadRecordingsIntoTable()),
	loadingJobs: () => dispatch(fetchJobs()),
	loadingJobsIntoTable: () => dispatch(loadJobsIntoTable()),
	loadingServers: () => dispatch(fetchServers()),
	loadingServersIntoTable: () => dispatch(loadServersIntoTable()),
	loadingServices: () => dispatch(fetchServices()),
	loadingServicesIntoTable: () => dispatch(loadServicesIntoTable()),
	loadingUsers: () => dispatch(fetchUsers()),
	loadingUsersIntoTable: () => dispatch(loadUsersIntoTable()),
	loadingGroups: () => dispatch(fetchGroups()),
	loadingGroupsIntoTable: () => dispatch(loadGroupsIntoTable()),
	loadingAcls: () => dispatch(fetchAcls()),
	loadingAclsIntoTable: () => dispatch(loadAclsIntoTable()),
	loadingThemes: () => dispatch(fetchThemes()),
	loadingThemesIntoTable: () => dispatch(loadThemesIntoTable()),
	resetOffset: () => dispatch(setOffset(0)),
	loadingFilters: (resource) => dispatch(fetchFilters(resource)),
});

export default connect(mapStateToProps, mapDispatchToProps)(MainNav);
