import React, {useState} from 'react';
import {useTranslation} from "react-i18next";
import {connect} from "react-redux";
import ConfirmModal from "../../shared/ConfirmModal";
import {deleteAcl} from "../../../thunks/aclThunks";

/**
 * This component renders the action cells of acls in the table view
 */
const AclsActionsCell = ({ row, deleteAcl }) => {
    const { t } = useTranslation();

    const [displayDeleteConfirmation, setDeleteConfirmation] = useState(false);

    const hideDeleteConfirmation = () => {
        setDeleteConfirmation(false);
    };

    const deletingAcl = id => {
        deleteAcl(id);
    };

    return (
        <>
            {/*TODO: When acl details are implemented, remove placeholder */}
            {/*TODO: with-Role */}
            <a onClick={() => onClickPlaceholder()}
               className="more"
               title={t('USERS.ACLS.TABLE.TOOLTIP.DETAILS')}/>

            {/*// TODO: with-Role*/}
            <a onClick={() => setDeleteConfirmation(true)}
               className="remove"
               title={t('USERS.ACLS.TABLE.TOOLTIP.DETAILS')}/>

            {/* Confirmation for deleting an ACL */}
            {displayDeleteConfirmation && (
                <ConfirmModal close={hideDeleteConfirmation}
                              resourceName={row.name}
                              resourceId={row.id}
                              resourceType="ACL"
                              deleteMethod={deletingAcl}/>
            )}

        </>
    );
};

//todo: remove if not needed anymore
const onClickPlaceholder = () => {
    console.log("In the Future here opens an other component, which is not implemented yet");
}

const mapDispatchToProps = dispatch => ({
    deleteAcl: (id) => dispatch(deleteAcl(id))
})

export default connect(null, mapDispatchToProps)(AclsActionsCell);
