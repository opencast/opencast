import React from "react";
import {useTranslation} from "react-i18next";
import ThemeDetails from "./ThemeDetails";

const ThemeDetailsModal = ({ handleClose, themeId }) => {
    const { t } = useTranslation();

    const close = () => {
        handleClose();
    }

    return (
        <>
            <div className="modal-animation modal-overlay"/>
            <section id="theme-details-modal" className="modal wizard modal-animation">
                <header>
                    <a className="fa fa-times close-modal" onClick={() => close()}/>
                    <h2>
                        {t('')}
                    </h2>
                </header>

                <ThemeDetails themeId={themeId}
                              close={close}/>
            </section>
        </>
    )
}

export default ThemeDetailsModal;
