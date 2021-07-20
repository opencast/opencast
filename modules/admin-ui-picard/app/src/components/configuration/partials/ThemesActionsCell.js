import React, {useState} from 'react';
import {useTranslation} from "react-i18next";
import {connect} from "react-redux";
import ConfirmModal from "../../shared/ConfirmModal";
import ThemeDetailsModal from "./wizard/ThemeDetailsModal";
import {deleteTheme} from "../../../thunks/themeThunks";
import {fetchThemeDetails, fetchUsage} from "../../../thunks/themeDetailsThunks";


/**
 * This component renders the action cells of themes in the table view
 */
const ThemesActionsCell = ({ row, deleteTheme, fetchThemeDetails, fetchUsage }) => {
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
    }

    const deletingTheme = id => {
        deleteTheme(id);
    };

    return (
        <>
            {/*TODO: with-Role */}
            <a onClick={() => showThemeDetails()}
               className="more"
               title={t('CONFIGURATION.THEMES.TABLE.TOOLTIP.DETAILS')}/>

            {displayThemeDetails && (
                <ThemeDetailsModal handleClose={hideThemeDetails}
                                   themeId={row.id}/>
            )}

            {/*// TODO: with-Role*/}
            <a onClick={() => setDeleteConfirmation(true)}
               className="remove ng-scope ng-isolate-scope"
               title={t('CONFIGURATION.THEMES.TABLE.TOOLTIP.DELETE')}/>

            {displayDeleteConfirmation && (
                <ConfirmModal close={hideDeleteConfirmation}
                              resourceName={row.name}
                              resourceId={row.id}
                              deleteMethod={deletingTheme}
                              resourceType="THEME"/>
            )}

        </>
    );
};

// Mapping actions to dispatch
const mapDispatchToProps = dispatch => ({
    deleteTheme: id => dispatch(deleteTheme(id)),
    fetchThemeDetails: id => dispatch(fetchThemeDetails(id)),
    fetchUsage: id => dispatch(fetchUsage(id))
});

export default connect(null, mapDispatchToProps)(ThemesActionsCell);
