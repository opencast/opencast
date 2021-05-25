import React, {useState} from 'react';
import {useTranslation} from "react-i18next";
import {connect} from "react-redux";
import ConfirmModal from "../../shared/ConfirmModal";
import {deleteUser} from "../../../thunks/userThunks";

/**
 * This component renders the action cells of users in the table view
 */
const UsersActionCell = ({ row, deleteUser }) => {
    const { t } = useTranslation();

    const [displayDeleteConfirmation, setDeleteConfirmation] = useState(false);

    const hideDeleteConfirmation = () => {
        setDeleteConfirmation(false);
    };

    const deletingUser = id => {
        deleteUser(id);
    };

    return (
        <>
            {/*TODO: When user details are implemented, remove placeholder */}
            {/*TODO: with-Role */}
            <a onClick={() => onClickPlaceholder()}
               className="more"
               title={t('USERS.USERS.TABLE.TOOLTIP.DETAILS')}/>

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

//todo: remove if not needed anymore
const onClickPlaceholder = () => {
    console.log("In the Future here opens an other component, which is not implemented yet");
}

// Mapping actions to dispatch
const mapDispatchToProps = dispatch => ({
    deleteUser: (id) => dispatch(deleteUser(id))
});

export default connect(null, mapDispatchToProps)(UsersActionCell);
