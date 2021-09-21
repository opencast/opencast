import React, {useState} from 'react';
import {useTranslation} from "react-i18next";
import {connect} from "react-redux";
import ConfirmModal from "../../shared/ConfirmModal";
import {deleteUser} from "../../../thunks/userThunks";
import UserDetailsModal from "./modal/UserDetailsModal";
import {fetchUserDetails} from "../../../thunks/userDetailsThunks";

/**
 * This component renders the action cells of users in the table view
 */
const UsersActionCell = ({ row, deleteUser, fetchUserDetails }) => {
    const { t } = useTranslation();

    const [displayDeleteConfirmation, setDeleteConfirmation] = useState(false);
    const [displayUserDetails, setUserDetails] = useState(false);

    const hideDeleteConfirmation = () => {
        setDeleteConfirmation(false);
    };

    const deletingUser = id => {
        deleteUser(id);
    };

    const showUserDetails = async () => {
        await fetchUserDetails(row.username);

        setUserDetails(true);
    };

    const hideUserDetails = () => {
      setUserDetails(false);
    };

    return (
        <>
            {/*TODO: with-Role */}
            <a onClick={() => showUserDetails()}
               className="more"
               title={t('USERS.USERS.TABLE.TOOLTIP.DETAILS')}/>

            {displayUserDetails && (
                <UserDetailsModal close={hideUserDetails}
                                  username={row.username} />
            )}

            {row.manageable ? (
                // TODO: with-Role
                <>
                    <a onClick={() => setDeleteConfirmation(true)}
                       className="remove"
                       title={t('USERS.USERS.TABLE.TOOLTIP.DETAILS')}/>

                    {/* Confirmation for deleting a user */}
                    {displayDeleteConfirmation && (
                        <ConfirmModal close={hideDeleteConfirmation}
                                      resourceName={row.name}
                                      resourceId={row.username}
                                      resourceType="USER"
                                      deleteMethod={deletingUser}/>
                    )}
                </>
            ) : null }
        </>
    );
}

// Mapping actions to dispatch
const mapDispatchToProps = dispatch => ({
    deleteUser: (id) => dispatch(deleteUser(id)),
    fetchUserDetails: username => dispatch(fetchUserDetails(username))
});

export default connect(null, mapDispatchToProps)(UsersActionCell);
