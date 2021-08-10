import React, {useState} from 'react';
import {useTranslation} from "react-i18next";
import {connect} from "react-redux";
import ConfirmModal from "../../shared/ConfirmModal";
import {deleteAcl} from "../../../thunks/aclThunks";
import AclDetailsModal from "./modal/AclDetailsModal";

/**
 * This component renders the action cells of acls in the table view
 */
const AclsActionsCell = ({ row, deleteAcl }) => {
    const { t } = useTranslation();

    const [displayDeleteConfirmation, setDeleteConfirmation] = useState(false);
    const [displayAclDetails, setAclDetails] = useState(false);

    const hideDeleteConfirmation = () => {
        setDeleteConfirmation(false);
    };

    const deletingAcl = id => {
        deleteAcl(id);
    };

    const hideAclDetails = () => {
        setAclDetails(false);
    };

    const showAclDetails = async () => {
        // todo: fetch acl details

        setAclDetails(true);
    };

    return (
        <>
            {/*TODO: with-Role */}
            <a onClick={() => showAclDetails()}
               className="more"
               title={t('USERS.ACLS.TABLE.TOOLTIP.DETAILS')}/>

            {displayAclDetails && (
                <AclDetailsModal close={hideAclDetails} aclId={row.id}/>
            )}

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

const mapDispatchToProps = dispatch => ({
    deleteAcl: (id) => dispatch(deleteAcl(id))
})

export default connect(null, mapDispatchToProps)(AclsActionsCell);
