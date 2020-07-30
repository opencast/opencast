import React from "react";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faSpinner } from "@fortawesome/free-solid-svg-icons";
import i18next from "i18next";

export const Loading: React.SFC<{ t: i18next.TFunction }> = ({ t }) => <div>
    <FontAwesomeIcon icon={faSpinner} spin />
    <span>{t("LTI.LOADING")}</span>
</div>;
