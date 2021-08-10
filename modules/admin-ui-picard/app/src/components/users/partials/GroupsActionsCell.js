import React, {useState} from 'react';
import {connect} from "react-redux";
import {useTranslation} from "react-i18next";
import ConfirmModal from "../../shared/ConfirmModal";
import {deleteGroup} from "../../../thunks/groupThunks";
import GroupDetailsModal from "./modal/GroupDetailsModal";

/**
 * This component renders the action cells of groups in the table view
 */
const GroupsActionsCell = ({ row, deleteGroup }) => {
    const { t } = useTranslation();

    const [displayDeleteConfirmation, setDeleteConfirmation] = useState(false);
    const [displayGroupDetails, setGroupDetails] = useState(false);

    const hideDeleteConfirmation = () => {
        setDeleteConfirmation(false);
    };

    const deletingGroup = id => {
        deleteGroup(id);
    };

    const hideGroupDetails = () => {
        setGroupDetails(false);
    };

    const showGroupDetails = async () => {
        // todo: fetch group details

        setGroupDetails(true);
    };

    return (
        <>
            {/*TODO: with-Role */}
            <a onClick={() => showGroupDetails()}
               className="more"
               title={t('USERS.GROUPS.TABLE.TOOLTIP.DETAILS')}/>

            {displayGroupDetails && (
                <GroupDetailsModal close={hideGroupDetails}
                                   groupname={row.name}/>
            )}

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

const mapDispatchToProps = dispatch => ({
    deleteGroup: (id) => dispatch(deleteGroup(id))
})

export default connect(null, mapDispatchToProps)(GroupsActionsCell);
