import React, {useState} from "react";
import {useTranslation} from "react-i18next";
import {Formik} from "formik";
import cn from "classnames";
import {EditUserSchema} from "../../../shared/wizard/validate";
import UserRolesTab from "../wizard/UserRolesTab";
import {connect} from "react-redux";
import {getUserDetails} from "../../../../selectors/userDetailsSelectors";
import EditUserGeneralTab from "../wizard/EditUserGeneralTab";
import UserEffectiveRolesTab from "../wizard/UserEffectiveRolesTab";
import {updateUserDetails} from "../../../../thunks/userDetailsThunks";

/**
 * This component manages the pages of the user details
 */
const UserDetails = ({close, userDetails, updateUserDetails }) => {
    const { t } = useTranslation();

    const [page, setPage] = useState(0);

    const initialValues = {
        ...userDetails,
        password: ''
    };

    // information about tabs
    const tabs = [
        {
            tabNameTranslation: 'USERS.USERS.DETAILS.TABS.USER',
            name: 'general'
        },
        {
            tabNameTranslation: 'USERS.USERS.DETAILS.TABS.ROLES',
            name: 'roles'
        },
        {
            tabNameTranslation: 'USERS.USERS.DETAILS.TABS.EFFECTIVEROLES',
            name: 'effectiveRoles'
        }
    ];

    const openTab = tabNr => {
        setPage(tabNr);
    };

    const handleSubmit = values => {
        updateUserDetails(values, userDetails.username);
        close();
    }

    return (
        <>
            {/* Navigation */}
            <nav className="modal-nav" id="modal-nav">
                <a className={cn({active: page === 0})}
                   onClick={() => openTab(0)}>
                    {t(tabs[0].tabNameTranslation)}
                </a>
                <a className={cn({active: page === 1})}
                   onClick={() => openTab(1)}>
                    {t(tabs[1].tabNameTranslation)}
                </a>
                <a className={cn({active: page === 2})}
                   onClick={() => openTab(2)}>
                    {t(tabs[2].tabNameTranslation)}
                </a>
            </nav>

            {/* formik form used in entire modal */}
            <Formik initialValues={initialValues}
                    validationSchema={EditUserSchema}
                    onSubmit={values => handleSubmit(values)}>
                {formik => (
                    <>
                        {page === 0 && (
                            <EditUserGeneralTab formik={formik}/>
                        )}
                        {page === 1 && (
                            <UserRolesTab formik={formik}/>
                        )}
                        {page === 2 && (
                            <UserEffectiveRolesTab formik={formik}/>
                        )}

                        {/* Navigation buttons and validation */}
                        {page !== 2 && (
                            <footer>
                                <button className={cn("submit", {
                                    active: (formik.dirty && formik.isValid),
                                    inactive: !(formik.dirty && formik.isValid)})
                                }
                                        disabled={!(formik.dirty && formik.isValid)}
                                        onClick={() => formik.handleSubmit()}
                                        type="submit">
                                    {t('SUBMIT')}
                                </button>
                                <button className="cancel"
                                        onClick={() => close()}>
                                    {t('CANCEL')}
                                </button>
                            </footer>
                        )}
                    </>
                )}
            </Formik>
        </>
    );
}

// Getting state data out of redux store
const mapStateToProps = state => ({
    userDetails: getUserDetails(state),
});

// Mapping actions to dispatch
const mapDispatchToProps = dispatch => ({
    updateUserDetails: (values, username) => dispatch(updateUserDetails(values, username))
});

export default connect(mapStateToProps, mapDispatchToProps)(UserDetails);
