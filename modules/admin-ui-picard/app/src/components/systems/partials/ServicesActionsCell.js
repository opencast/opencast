import React from "react";
import {useTranslation} from "react-i18next";
import {connect} from "react-redux";
import {fetchServices, restartService} from "../../../thunks/serviceThunks";
import {loadServicesIntoTable} from "../../../thunks/tableThunks";

/**
 * This component renders the action cells of services in the table view
 */
const ServicesActionCell = ({ row, loadServices, loadServicesIntoTable }) => {
    const { t } = useTranslation();

    const onClickRestart = async () => {
        await restartService(row.hostname, row.name);
        await loadServices();
        loadServicesIntoTable();
    }

    return (
        row.status !== 'SYSTEMS.SERVICES.STATUS.NORMAL' ? (
            // todo: with-role
            <a className="sanitize fa fa-undo"
               onClick={() => onClickRestart()}
               title={t('SYSTEMS.SERVICES.TABLE.SANITIZE')}/>
        ) : null
    );
}

// mapping actions to dispatch
const mapDispatchToProps = dispatch => ({
    loadServices: () => dispatch(fetchServices()),
    loadServicesIntoTable: () => dispatch(loadServicesIntoTable())
});

export default connect(null, mapDispatchToProps)(ServicesActionCell);
