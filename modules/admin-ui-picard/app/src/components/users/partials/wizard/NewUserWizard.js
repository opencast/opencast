import React, { useEffect, useState } from 'react';
import {connect} from "react-redux";
import {Formik} from "formik";
import {useTranslation} from "react-i18next";
import cn from 'classnames';
import NewUserGeneralTab from "./NewUserGeneralTab";
import UserRolesTab from "./UserRolesTab";
import {initialFormValuesNewUser} from "../../../../configs/modalConfig";
import {getUsernames} from "../../../../selectors/userSelectors";
import {postNewUser} from "../../../../thunks/userThunks";
import {NewUserSchema} from "../../../../utils/validate";
import {logger} from "../../../../utils/logger";

/**
 * This component renders the new user wizard
 */
const NewUserWizard = ({ close, usernames, postNewUser }) => {
    const { t } = useTranslation();

    const navStyle = {
        left: '0px',
        top: 'auto',
        position: 'initial'
    };

    const [tab, setTab] = useState(0);

    const openTab = tabNr=> {
        setTab(tabNr);
    };

    const handleSubmit = values => {
        const response = postNewUser(values);
        logger.info(response);
        close();
    }

    return (
        <>
            {/*Head navigation*/}
            <nav className="modal-nav" id="modal-nav" style={navStyle}>
                <a className={cn("wider", {active: tab === 0})}
                   onClick={() => openTab(0)}>
                    {t('USERS.USERS.DETAILS.TABS.USER')}
                </a>
                <a className={cn("wider", {active: tab === 1})}
                   onClick={() => openTab(1)}
                   title={t('USERS.USERS.DETAILS.DESCRIPTION.ROLES')}>
                    {t('USERS.USERS.DETAILS.TABS.ROLES')}
                </a>
            </nav>

            {/* Initialize overall form */}
            <Formik initialValues={initialFormValuesNewUser}
                    validationSchema={NewUserSchema(usernames)}
                    onSubmit={values => handleSubmit(values)}>

                {/* Render wizard tabs depending on current value of tab variable */}
                {formik => {

                    // eslint-disable-next-line react-hooks/rules-of-hooks
                    useEffect(() => {
                        formik.validateForm();
                    }, [tab]);

                    return (
                      <>
                          {tab === 0 && (
                            <NewUserGeneralTab formik={formik}/>
                          )}
                          {tab === 1 && (
                            <UserRolesTab formik={formik}/>
                          )}

                          {/* Navigation buttons and validation */}
                          <footer>
                              <button className={cn("submit", {
                                  active: (formik.dirty && formik.isValid),
                                  inactive: !(formik.dirty && formik.isValid)})}
                                      disabled={!(formik.dirty && formik.isValid)}
                                      onClick={() => formik.handleSubmit()}>
                                  {t('SUBMIT')}
                              </button>
                              <button className="cancel" onClick={() => close()}>
                                  {t('CANCEL')}
                              </button>
                          </footer>
                      </>
                    );
                }}
            </Formik>
        </>
    )
};

// Getting state data out of redux store
const mapStateToProps = state => ({
    usernames: getUsernames(state)
});

// Mapping actions to dispatch
const mapDispatchToProps = dispatch => ({
    postNewUser: values => dispatch(postNewUser(values))
});

export default connect(mapStateToProps, mapDispatchToProps)(NewUserWizard);
