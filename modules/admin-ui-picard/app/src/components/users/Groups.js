import React, {useEffect, useState} from "react";
import {useTranslation} from "react-i18next";
import MainNav from "../shared/MainNav";
import Link from "react-router-dom/Link";
import cn from "classnames";
import TableFilters from "../shared/TableFilters";
import Table from "../shared/Table";
import {fetchFilters} from "../../thunks/tableFilterThunks";
import {fetchUsers} from "../../thunks/userThunks";
import {loadGroupsIntoTable, loadUsersIntoTable} from "../../thunks/tableThunks";
import {withRouter} from "react-router-dom";
import {connect} from "react-redux";
import {getGroups} from "../../selectors/groupSelectors";
import {groupsTemplateMap} from "../../configs/tableConfigs/groupsTemplateMap";
import {fetchGroups} from "../../thunks/groupThunks";

const Groups = ({ loadingGroups, loadingGroupsIntoTable, groups, loadingFilters,
    loadingUsers, loadingUsersIntoTable }) => {
    const { t } = useTranslation();
    const [displayNavigation, setNavigation] = useState(false);

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

    const loadAccess = () => {
        console.log('To be implemented');
    }

    useEffect(() => {
        // Load jobs on mount
        loadGroups().then(r => console.log(r));

        // Load filters
        loadingFilters('groups');

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
                        <span>{t('USERS.ACTIONS.ADD_GROUP')}</span>
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
                          className={cn({active: true})}
                          onClick={() => loadGroups()}>
                        {t('USERS.NAVIGATION.GROUPS')}
                    </Link>
                    <Link to="/users/acls"
                          className={cn({active: false})}
                          onClick={() => loadAccess()}>
                        {t('USERS.NAVIGATION.PERMISSIONS')}
                    </Link>
                </nav>
            </section>

            <div className="main-view" style={displayNavigation ? styleNavOpen : styleNavClosed}>
                {/*Todo: What is data-admin-ng-notifications?*/}

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

const mapStateToProps = state => ({
    groups: getGroups(state)
});

const mapDispatchToProps = dispatch => ({
    loadingFilters: resource => dispatch(fetchFilters(resource)),
    loadingGroups: () => dispatch(fetchGroups()),
    loadingGroupsIntoTable: () => dispatch(loadGroupsIntoTable()),
    loadingUsers: () => dispatch(fetchUsers()),
    loadingUsersIntoTable: () => dispatch(loadUsersIntoTable())
});

export default withRouter(connect(mapStateToProps, mapDispatchToProps)(Groups));
