import React, {useEffect, useState} from "react";
import {useTranslation} from "react-i18next";
import MainNav from "../shared/MainNav";
import Link from "react-router-dom/Link";
import cn from 'classnames';
import TableFilters from "../shared/TableFilters";
import Table from "../shared/Table";
import {fetchFilters} from "../../thunks/tableFilterThunks";
import {withRouter} from "react-router-dom";
import {connect} from "react-redux";
import {usersTemplateMap} from "../../configs/tableConfigs/usersTableConfig";
import {getUsers} from "../../selectors/userSelectors";
import {fetchUsers} from "../../thunks/userThunks";
import {loadAclsIntoTable, loadGroupsIntoTable, loadUsersIntoTable} from "../../thunks/tableThunks";
import {fetchGroups} from "../../thunks/groupThunks";
import {fetchAcls} from "../../thunks/aclThunks";
import Notifications from "../shared/Notifications";

/**
 * This component renders the table view of users
 */
const Users = ({ loadingUsers, loadingUsersIntoTable, users, loadingFilters,
                   loadingGroups, loadingGroupsIntoTable, loadingAcls,
                   loadingAclsIntoTable }) => {
    const { t } = useTranslation();
    const [displayNavigation, setNavigation] = useState(false);

    const loadUsers = async () => {
        // Fetching users from server
        await loadingUsers();

        // Load users into table
        loadingUsersIntoTable();


    }

    const loadGroups = () => {
        // Fetching groups from server
        loadingGroups();

        // Load groups into table
        loadingGroupsIntoTable();
    }

    const loadAcls = () => {
        // Fetching acls from server
        loadingAcls();

        // Load acls into table
        loadingAclsIntoTable();
    }

    useEffect(() => {
        // Load users on mount
        loadUsers().then(r => console.log(r));

        // Load filters
        loadingFilters('users');

        // Fetch users every minute
        let fetchUsersInterval = setInterval(loadUsers, 100000);

        return () => clearInterval(fetchUsersInterval);

    }, []);

    const toggleNavigation = () => {
        setNavigation(!displayNavigation);
    }

    const placeholder = () => {
        console.log("To be implemented");
    }

    const styleNavOpen = {
        marginLeft: '130px',
    };
    const styleNavClosed = {
        marginLeft: '20px',
    };

    return (
        <>
            <section className="action-nav-bar">

                {/* Add user button */}
                <div className="btn-group">
                    {/*todo: implement onClick and with role*/}
                    <button className="add" onClick={() => placeholder()}>
                        <i className="fa fa-plus"/>
                        <span>{t('USERS.ACTIONS.ADD_USER')}</span>
                    </button>
                </div>

                {/* Include Burger-button menu*/}
                <MainNav isOpen={displayNavigation}
                         toggleMenu={toggleNavigation} />

                <nav>
                    {/*todo: with role*/}
                    <Link to="/users/users"
                          className={cn({active: true})}
                          onClick={() => loadUsers()}>
                        {t('USERS.NAVIGATION.USERS')}
                    </Link>
                    <Link to="/users/groups"
                          className={cn({active: false})}
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
                    <TableFilters loadResource={loadingUsers}
                                  loadResourceIntoTable={loadingUsersIntoTable}
                                  resource={'users'}/>
                    <h1>{t('USERS.USERS.TABLE.CAPTION')}</h1>
                    <h4>{t('TABLE_SUMMARY', { numberOfRows: users.length})}</h4>
                </div>
                {/* Include table component */}
                <Table templateMap={usersTemplateMap} />
            </div>
        </>
    )
}

// Getting state data out of redux store
const mapStateToProps = state => ({
    users: getUsers(state)
});

// Mapping actions to dispatch
const mapDispatchToProps = dispatch => ({
    loadingFilters: resource => dispatch(fetchFilters(resource)),
    loadingUsers: () => dispatch(fetchUsers()),
    loadingUsersIntoTable: () => dispatch(loadUsersIntoTable()),
    loadingGroups: () => dispatch(fetchGroups()),
    loadingGroupsIntoTable: () => dispatch(loadGroupsIntoTable()),
    loadingAcls: () => dispatch(fetchAcls()),
    loadingAclsIntoTable: () => dispatch(loadAclsIntoTable())
});

export default withRouter(connect(mapStateToProps, mapDispatchToProps)(Users));
