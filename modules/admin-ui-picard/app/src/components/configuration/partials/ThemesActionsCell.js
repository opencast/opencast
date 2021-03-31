import React, {useState} from 'react';
import {useTranslation} from "react-i18next";
import ConfirmModal from "../../shared/ConfirmModal";
import {connect} from "react-redux";
import {deleteTheme} from "../../../thunks/themeThunks";

/**
 * This component renders the action cells of themes in the table view
 */
const ThemesActionsCell = ({ row, deleteTheme }) => {
    const { t } = useTranslation();

    const [displayDeleteConfirmation, setDeleteConfirmation] = useState(false);

    const hideDeleteConfirmation = () => {
        setDeleteConfirmation(false);
    };

    const deletingTheme = id => {
        deleteTheme(id);
    };

    return (
        <>
            {/*TODO: When theme details are implemented, remove placeholder */}
            {/*TODO: with-Role */}
            <a onClick={() => onClickPlaceholder()}
               className="more"
               title={t('CONFIGURATION.THEMES.TABLE.TOOLTIP.DETAILS')}/>

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

//todo: remove if not needed anymore
const onClickPlaceholder = () => {
    console.log("In the Future here opens an other component, which is not implemented yet");
}

// Mapping actions to dispatch
const mapDispatchToProps = dispatch => ({
    deleteTheme: (id) => dispatch(deleteTheme(id)),
});

export default connect(null, mapDispatchToProps)(ThemesActionsCell);
