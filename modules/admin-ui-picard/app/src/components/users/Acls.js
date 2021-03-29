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
import {fetchGroups} from "../../thunks/groupThunks";
import {withRouter} from "react-router-dom";
import {connect} from "react-redux";
import {aclsTemplateMap} from "../../configs/tableConfigs/aclsTableConfig";
import {fetchAcls} from "../../thunks/aclThunks";
import {getAcls} from "../../selectors/aclSelectors";
import Notifications from "../shared/Notifications";

/**
 * This component renders the table view of acls
 */
const Acls = ({ loadingAcls, loadingAclsIntoTable, acls, loadingFilters,
                    loadingUsers, loadingUsersIntoTable, loadingGroups,
                    loadingGroupsIntoTable }) => {
    const { t } = useTranslation();
    const [displayNavigation, setNavigation] = useState(false);

    const loadAcls = async () => {
        // Fetching acls from server
        await loadingAcls();

        // Load acls into table
        loadingAclsIntoTable();
    };

    const loadUsers = () => {
        // Fetching users from server
        loadingUsers();

        // Load users into table
        loadingUsersIntoTable();
    };

    const loadGroups = () => {
        // Fetching groups from server
        loadingGroups();

        // Load groups into table
        loadingGroupsIntoTable();
    };

    useEffect(() => {
        // Load acls on mount
        loadAcls().then(r => console.log(r));

        // Load filters
        loadingFilters('acls');

        // Fetch ACLs every minute
        let fetchAclInterval = setInterval(loadAcls, 100000);

        return () => clearInterval(fetchAclInterval);

    }, []);

    const toggleNavigation = () => {
        setNavigation(!displayNavigation);
    };

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

                {/* Add acl button */}
                <div className="btn-group">
                    {/*todo: implement onClick and with role*/}
                    <button className="add" onClick={() => placeholder()}>
                        <i className="fa fa-plus"/>
                        <span>{t('USERS.ACTIONS.ADD_ACL')}</span>
                    </button>
                </div>

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
                          className={cn({active: false})}
                          onClick={() => loadGroups()}>
                        {t('USERS.NAVIGATION.GROUPS')}
                    </Link>
                    <Link to="/users/acls"
                          className={cn({active: true})}
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
                    <TableFilters loadResource={loadingAcls}
                                  loadResourceIntoTable={loadingAclsIntoTable}
                                  resource={'acls'}/>
                    <h1>{t('USERS.ACLS.TABLE.CAPTION')}</h1>
                    <h4>{t('TABLE_SUMMARY', { numberOfRows: acls.length})}</h4>
                </div>
                {/* Include table component */}
                <Table templateMap={aclsTemplateMap} />
            </div>
        </>
    );
};

// Getting state data out of redux store
const mapStateToProps = state => ({
    acls: getAcls(state)
});

// Mapping actions to dispatch
const mapDispatchToProps = dispatch => ({
    loadingFilters: resource => dispatch(fetchFilters(resource)),
    loadingAcls: () => dispatch(fetchAcls()),
    loadingAclsIntoTable: () => dispatch(loadAclsIntoTable()),
    loadingUsers: () => dispatch(fetchUsers()),
    loadingUsersIntoTable: () => dispatch(loadUsersIntoTable()),
    loadingGroups: () => dispatch(fetchGroups()),
    loadingGroupsIntoTable: () => dispatch(loadGroupsIntoTable()),
});

export default withRouter(connect(mapStateToProps, mapDispatchToProps)(Acls));
