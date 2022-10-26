import React from "react";
import { useTranslation } from "react-i18next";
import cn from "classnames";
import { getUserInformation } from "../../../selectors/userInfoSelectors";
import { connect } from "react-redux";
import { hasAccess } from "../../../utils/utils";

/**
 * This component renders the navigation in details modals
 */
const ModalNavigation = ({ tabInformation, page, openTab, user }) => {
	const { t } = useTranslation();

	return (
		<nav className="modal-nav" id="modal-nav">
			{tabInformation.map(
				(tab, key) =>
					hasAccess(tab.accessRole, user) && (
						<a
							key={key}
							className={cn({ active: page === key })}
							onClick={() => openTab(key)}
						>
							{t(tab.tabTranslation)}
						</a>
					)
			)}
		</nav>
	);
};

// Getting state data out of redux store
const mapStateToProps = (state) => ({
	user: getUserInformation(state),
});

export default connect(mapStateToProps)(ModalNavigation);
