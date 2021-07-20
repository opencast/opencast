import React, {useState} from 'react';
import {connect} from "react-redux";
import {useTranslation} from "react-i18next";
import ConfirmModal from "../../shared/ConfirmModal";
import {deleteGroup} from "../../../thunks/groupThunks";

/**
 * This component renders the action cells of groups in the table view
 */
const GroupsActionsCell = ({ row, deleteGroup }) => {
    const { t } = useTranslation();

    const [displayDeleteConfirmation, setDeleteConfirmation] = useState(false);

    const hideDeleteConfirmation = () => {
        setDeleteConfirmation(false);
    };

    const deletingGroup = id => {
        deleteGroup(id);
    };

    return (
        <>
            {/*TODO: When group details are implemented, remove placeholder */}
            {/*TODO: with-Role */}
            <a onClick={() => onClickPlaceholder()}
               className="more"
               title={t('USERS.GROUPS.TABLE.TOOLTIP.DETAILS')}/>

            {/*// TODO: with-Role*/}
            <a onClick={() => setDeleteConfirmation(true)}
               className="remove"
               title={t('USERS.GROUPS.TABLE.TOOLTIP.DETAILS')}/>

            {/*Confirmation for deleting a group*/}
            {displayDeleteConfirmation && (
                <ConfirmModal close={hideDeleteConfirmation}
                              resourceId={row.id}
                              resourceName={row.name}
                              deleteMethod={deletingGroup}
                              resourceType="GROUP"/>
            )}

        </>
    );
}

//todo: remove if not needed anymore
const onClickPlaceholder = () => {
    console.log("In the Future here opens an other component, which is not implemented yet");
}

const mapDispatchToProps = dispatch => ({
    deleteGroup: (id) => dispatch(deleteGroup(id))
})

export default connect(null, mapDispatchToProps)(GroupsActionsCell);
