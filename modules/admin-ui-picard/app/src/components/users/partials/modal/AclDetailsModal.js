import React from "react";
import { useTranslation } from "react-i18next";
import AclDetails from "./AclDetails";

/**
 * This component renders the modal for displaying acl details
 */
const AclDetailsModal = ({ close, aclName }) => {
	const { t } = useTranslation();

	const handleClose = () => {
		close();
	};

	const modalStyle = {
		fontSize: "14px",
		color: "#666666",
	};

	return (
		// todo: add hotkeys
		<>
			<div className="modal-animation modal-overlay" />
			<section
				className="modal wizard modal-animation"
				id="acl-details-modal"
				style={modalStyle}
			>
				<header>
					<a
						className="fa fa-times close-modal"
						onClick={() => handleClose()}
					/>
					<h2>{t("USERS.ACLS.DETAILS.HEADER", { name: aclName })}</h2>
				</header>

				{/* component that manages tabs of acl details modal*/}
				<AclDetails close={close} />
			</section>
		</>
	);
};

export default AclDetailsModal;
