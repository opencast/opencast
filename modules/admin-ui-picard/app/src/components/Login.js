import React, {useEffect, useState} from 'react';
import {useTranslation} from "react-i18next";
import {Formik, Field} from "formik";
import languages from "../i18n/languages";
import i18n from "../i18n/i18n";
import cn from 'classnames';

//Get code, flag and name of the current language
let currentLang = languages.find(({ code }) => code === i18n.language);
if (typeof currentLang === 'undefined') {
    currentLang = languages.find(({ code }) => code === 'en-GB');
}
const currentLanguage = currentLang;

// References for detecting a click outside of the container of the dropdown menu of languages
const containerLang = React.createRef();

function changeLanguage(code) {
    // Load json-file of the language with provided code
    i18n.changeLanguage(code);
    // Reload window for updating the flag of the language dropdown menu
    window.location.reload();
}

/**
 * This component renders the login page
 */
const Login = () => {
    const { t } = useTranslation();
    // State for opening (true) and closing (false) the dropdown menus for language
    const [displayMenuLang, setMenuLang] = useState(false);

    // TODO: This is only a placeholder. Implement actual error handling of login
    let isError = true;

    useEffect(() => {
        // Function for handling clicks outside of an open dropdown menu
        const handleClickOutside = e => {
            if (containerLang.current && !containerLang.current.contains(e.target)) {
                setMenuLang(false);
            }
        }

        window.addEventListener('mousedown', handleClickOutside);

        return () => {
            window.removeEventListener('mousedown', handleClickOutside);
        }
    }, []);

    const handleChange = e => {
        const itemName = e.target.name;
        const itemValue = e.target.value;
    }

    // Handle submission of login data to backend
    const handleSubmit = e => {
        e.preventDefault();
        const data = new FormData(e.target);

        fetch('/admin-ng/j_spring_security_check', {
            method: 'POST',
            body: data
        }).then(r => console.log(r))
    }

    return (
        <>
            <head>
                <meta charSet="UTF-8"/>
                <title>Opencast</title>
            </head>

            {/*Todo: find equivalent to ng-cloak*/}
            <body className="login-body">
                <section className="login-container">
                    <div className="login-form">
                        <div className="form-container">
                            {/*Login form*/}
                                <Formik>
                                    {formik => (
                                        <>
                                            <div className="row" >
                                                <p>
                                                    <span>{t('LOGIN.WELCOME')}</span><br />
                                                </p>
                                            </div>

                                            {/*Only show if error occurs on login*/}
                                            {isError && (
                                                <div className="error-container">{t('LOGIN.ERROR')}</div>
                                            )}
                                            <div className="row">
                                                <Field name="j_username"
                                                       type="text"
                                                       placeholder={t('LOGIN.USERNAME')}
                                                       className={cn("login-input", {error: isError})}
                                                       autoFocus="autoFocus"/>
                                            </div>
                                            <div className="row">
                                                <Field name="j_password"
                                                       type="password"
                                                       placeholder={t('LOGIN.PASSWORD')}
                                                       className={cn("login-input", {error: isError})}/>
                                            </div>

                                            <div className="row remember-me">
                                                <Field type="checkbox" id="remember" name="_spring_security_remember_me"
                                                       checked/>
                                                <label htmlFor="remember">{t('LOGIN.REMEMBER')}</label>
                                            </div>
                                            <div className="row">
                                                <button className="submit">{t('LOGIN.LOGIN')}</button>
                                            </div>
                                        </>
                                    )}
                                </Formik>
                        </div>

                        {/*Language dropdown menu*/}
                        <nav className="login-nav nav-dd-container" id="nav-dd-container">
                            <div className="nav-dd lang-dd" id="lang-dd"
                                 ref={containerLang}
                                 onClick={() => setMenuLang(!displayMenuLang)}>
                                <img className="lang-flag" src={currentLanguage.flag} alt={currentLanguage.code} />
                                <span>{currentLanguage.long}</span>
                                { displayMenuLang && (
                                    <ul className="dropdown-ul">
                                        {languages.map((language, key) => (
                                            <li key={key}>
                                                <a href="#" onClick={() => changeLanguage(language.code)}>
                                                    <img className="lang-flag"
                                                         src={language.flag}
                                                         alt={language.code}/>
                                                    {language.long}
                                                </a>
                                            </li>
                                        ))}
                                    </ul>
                                )}
                            </div>
                        </nav>
                    </div>
                </section>
            </body>
        </>
    )
};

export default Login;
