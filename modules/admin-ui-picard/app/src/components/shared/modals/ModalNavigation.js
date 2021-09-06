import React from "react";
import {useTranslation} from "react-i18next";
import cn from 'classnames';

/**
 * This component renders the navigation in details modals
 */
const ModalNavigation = ({ tabInformation, page, openTab }) => {
    const { t } = useTranslation();

    return (
        <nav className="modal-nav" id="modal-nav">
            {tabInformation.map((tab, key) => (
                <a className={cn({active: page === key})}
                   onClick={() => openTab(key)}>
                    {t(tab.tabTranslation)}
                </a>
            ))}
        </nav>
    );
}

export default ModalNavigation;
