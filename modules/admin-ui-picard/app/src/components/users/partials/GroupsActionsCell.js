import React, {useState} from 'react';
import {connect} from "react-redux";
import {useTranslation} from "react-i18next";
import ConfirmModal from "../../shared/ConfirmModal";
import {deleteGroup} from "../../../thunks/groupThunks";
import GroupDetailsModal from "./modal/GroupDetailsModal";
import {fetchGroupDetails} from "../../../thunks/groupDetailsThunks";

/**
 * This component renders the action cells of groups in the table view
 */
const GroupsActionsCell = ({ row, deleteGroup, fetchGroupDetails }) => {
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
        await fetchGroupDetails(row.id);

        setGroupDetails(true);
    };

    return (
        <>
            {/*TODO: with-Role */}
            <a onClick={() => showGroupDetails()}
               className="more"
               title={t('USERS.GROUPS.TABLE.TOOLTIP.DETAILS')}/>

            {/*modal displaying details about group*/}
            {displayGroupDetails && (
                <GroupDetailsModal close={hideGroupDetails}
                                   groupName={row.name} />
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
    deleteGroup: (id) => dispatch(deleteGroup(id)),
    fetchGroupDetails: groupName => dispatch(fetchGroupDetails(groupName))
});

export default connect(null, mapDispatchToProps)(GroupsActionsCell);
