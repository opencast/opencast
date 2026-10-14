import React from "react";
import { LuLoaderCircle } from "react-icons/lu";
import * as i18next from "i18next";

export const Loading: React.FC<{ t: i18next.TFunction }> = ({ t }) => <div>
    <LuLoaderCircle className="spin" />
    <span>{t("LTI.LOADING")}</span>
</div>;
