import React, {useEffect, useState} from "react";
import MainNav from "../shared/MainNav";
import {useTranslation} from "react-i18next";
import { Link } from "react-router-dom";
import {connect} from "react-redux";
import cn from 'classnames';
import TableFilters from "../shared/TableFilters";
import Table from "../shared/Table";
import Notifications from "../shared/Notifications";
import {recordingsTemplateMap} from "../../configs/tableConfigs/recordingsTableConfig";
import {getTotalRecordings} from "../../selectors/recordingSelectors";
import {fetchRecordings} from "../../thunks/recordingThunks";
import {loadRecordingsIntoTable} from "../../thunks/tableThunks";
import {fetchFilters} from "../../thunks/tableFilterThunks";
import {editTextFilter} from "../../actions/tableFilterActions";
import {styleNavClosed, styleNavOpen} from "../../utils/componentsUtils";
import {logger} from "../../utils/logger";
import Header from "../Header";
import Footer from "../Footer";
import {getUserInformation} from "../../selectors/userInfoSelectors";
import {hasAccess} from "../../utils/utils";

/**
 * This component renders the table view of recordings
 */
const Recordings = ({ loadingRecordings, loadingRecordingsIntoTable, recordings, loadingFilters, resetTextFilter, user }) => {
    const { t } = useTranslation();
    const [displayNavigation, setNavigation] = useState(false);

    const loadRecordings = async () => {
        // Fetching recordings from server
        await loadingRecordings();

        // Load recordings into table
        loadingRecordingsIntoTable();
    }

    useEffect(() => {
        resetTextFilter();

        // Load recordings on mount
        loadRecordings().then(r => logger.info(r));

    }, []);

    const toggleNavigation = () => {
        setNavigation(!displayNavigation);
    }

    return (
        <>
            <Header />
            <section className="action-nav-bar">

                {/* Include Burger-button menu*/}
                <MainNav isOpen={displayNavigation}
                         toggleMenu={toggleNavigation}/>

                <nav>
                    {hasAccess("ROLE_UI_LOCATIONS_VIEW", user) && (
                        <Link to="/recordings/recordings"
                              className={cn({active: true})}
                              onClick={() => {
                                  loadingFilters("recordings");
                                  loadRecordings().then();
                              }} >
                            {t('RECORDINGS.NAVIGATION.LOCATIONS')}
                        </Link>
                    )}
                </nav>
            </section>

            <div className="main-view" style={displayNavigation ? styleNavOpen : styleNavClosed}>
                {/* Include notifications component */}
                <Notifications />

                <div className="controls-container">
                    {/* Include filters component */}
                    <TableFilters loadResource={loadingRecordings}
                                  loadResourceIntoTable={loadingRecordingsIntoTable}
                                  resource={'recordings'}/>

                    <h1>{t('RECORDINGS.RECORDINGS.TABLE.CAPTION')}</h1>
                    <h4>{t('TABLE_SUMMARY', { numberOfRows: recordings })}</h4>
                </div>
                {/* Include table component */}
                <Table templateMap={recordingsTemplateMap} />
            </div>
            <Footer />
        </>
    )
}

// Getting state data out of redux store
const mapStateToProps = state => ({
    recordings: getTotalRecordings(state),
    user: getUserInformation(state)
});

// Mapping actions to dispatch
const mapDispatchToProps = dispatch => ({
    loadingRecordings: () => dispatch(fetchRecordings()),
    loadingRecordingsIntoTable: () => dispatch(loadRecordingsIntoTable()),
    loadingFilters: resource => dispatch(fetchFilters(resource)),
    resetTextFilter: () => dispatch(editTextFilter(''))
});

export default connect(mapStateToProps,mapDispatchToProps)(Recordings);
