import React, {useEffect, useState} from "react";
import {useTranslation} from "react-i18next";
import MainNav from "../shared/MainNav";
import Link from "react-router-dom/Link";
import cn from "classnames";
import TableFilters from "../shared/TableFilters";
import Table from "../shared/Table";
import {fetchFilters} from "../../thunks/tableFilterThunks";
import {fetchUsers} from "../../thunks/userThunks";
import {loadAclsIntoTable, loadGroupsIntoTable, loadUsersIntoTable} from "../../thunks/tableThunks";
import {withRouter} from "react-router-dom";
import {connect} from "react-redux";
import {getTotalGroups} from "../../selectors/groupSelectors";
import {groupsTemplateMap} from "../../configs/tableConfigs/groupsTableConfig";
import {fetchGroups} from "../../thunks/groupThunks";
import {fetchAcls} from "../../thunks/aclThunks";
import Notifications from "../shared/Notifications";
import NewResourceModal from "../shared/NewResourceModal";
import {editTextFilter} from "../../actions/tableFilterActions";

/**
 * This component renders the table view of groups
 */
const Groups = ({ loadingGroups, loadingGroupsIntoTable, groups, loadingFilters,
                    loadingUsers, loadingUsersIntoTable, loadingAcls,
                    loadingAclsIntoTable, resetTextFilter }) => {
    const { t } = useTranslation();
    const [displayNavigation, setNavigation] = useState(false);
    const [displayNewGroupModal, setNewGroupModal] = useState(false);

    const loadGroups = async () => {
        // Fetching groups from server
        await loadingGroups();

        // Load groups into table
        loadingGroupsIntoTable();
    }

    const loadUsers = () => {
        // Fetching users from server
        loadingUsers();

        // Load users into table
        loadingUsersIntoTable();
    }

    const loadAcls = () => {
        // Fetching acls from server
        loadingAcls();

        // Load acls into table
        loadingAclsIntoTable();
    }

    useEffect(() => {
        resetTextFilter();

        // Load groups on mount
        loadGroups().then(r => console.log(r));

        // Load filters
        loadingFilters('groups');

        // Fetch groups every minute
        let fetchGroupsInterval = setInterval(loadGroups, 100000);

        return () => clearInterval(fetchGroupsInterval);

    }, []);

    const toggleNavigation = () => {
        setNavigation(!displayNavigation);
    };

    const showNewGroupModal = () => {
        setNewGroupModal(true);
    };

    const hideNewGroupModal = () => {
        setNewGroupModal(false);
    };

    const styleNavOpen = {
        marginLeft: '130px',
    };
    const styleNavClosed = {
        marginLeft: '20px',
    };

    return (
        <>
            <section className="action-nav-bar">

                {/* Add group button */}
                <div className="btn-group">
                    {/*todo: implement onClick and with role*/}
                    <button className="add" onClick={() => showNewGroupModal()}>
                        <i className="fa fa-plus"/>
                        <span>{t('USERS.ACTIONS.ADD_GROUP')}</span>
                    </button>
                </div>

                {/* Display modal for new acl if add acl button is clicked */}
                <NewResourceModal showModal={displayNewGroupModal}
                                  handleClose={hideNewGroupModal}
                                  resource="group"/>

                {/* Include Burger-button menu*/}
                <MainNav isOpen={displayNavigation}
                         toggleMenu={toggleNavigation} />

                <nav>
                    {/*todo: with role*/}
                    <Link to="/users/users"
                          className={cn({active: false})}
                          onClick={() => loadUsers()}>
                        {t('USERS.NAVIGATION.USERS')}
                    </Link>
                    <Link to="/users/groups"
                          className={cn({active: true})}
                          onClick={() => loadGroups()}>
                        {t('USERS.NAVIGATION.GROUPS')}
                    </Link>
                    <Link to="/users/acls"
                          className={cn({active: false})}
                          onClick={() => loadAcls()}>
                        {t('USERS.NAVIGATION.PERMISSIONS')}
                    </Link>
                </nav>
            </section>

            <div className="main-view" style={displayNavigation ? styleNavOpen : styleNavClosed}>
                {/* Include notifications component */}
                <Notifications />

                <div  className="controls-container">
                    {/* Include filters component */}
                    <TableFilters loadResource={loadingGroups}
                                  loadResourceIntoTable={loadingGroupsIntoTable}
                                  resource={'groups'}/>
                    <h1>{t('USERS.GROUPS.TABLE.CAPTION')}</h1>
                    <h4>{t('TABLE_SUMMARY', { numberOfRows: groups.length})}</h4>
                </div>
                {/* Include table component */}
                <Table templateMap={groupsTemplateMap} />
            </div>
        </>
    );

};

// Getting state data out of redux store
const mapStateToProps = state => ({
    groups: getTotalGroups(state)
});

// Mapping actions to dispatch
const mapDispatchToProps = dispatch => ({
    loadingFilters: resource => dispatch(fetchFilters(resource)),
    loadingGroups: () => dispatch(fetchGroups()),
    loadingGroupsIntoTable: () => dispatch(loadGroupsIntoTable()),
    loadingUsers: () => dispatch(fetchUsers()),
    loadingUsersIntoTable: () => dispatch(loadUsersIntoTable()),
    loadingAcls: () => dispatch(fetchAcls()),
    loadingAclsIntoTable: () => dispatch(loadAclsIntoTable()),
    resetTextFilter: () => dispatch(editTextFilter(''))
});

export default withRouter(connect(mapStateToProps, mapDispatchToProps)(Groups));
