import React, { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { useHistory } from "react-router-dom";
import { Field, Formik } from "formik";
import languages from "../i18n/languages";
import i18n from "../i18n/i18n";
import cn from "classnames";
import axios from "axios";
import { logger } from "../utils/logger";

//Get code, flag and name of the current language
let currentLang = languages.find(({ code }) => code === i18n.language);
if (typeof currentLang === "undefined") {
	currentLang = languages.find(({ code }) => code === "en-GB");
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
	const [isError, setError] = useState(false);

	let history = useHistory();

	let initialValues = {
		j_username: "",
		j_password: "",
		_spring_security_remember_me: true,
	};

	useEffect(() => {
		// Function for handling clicks outside of an open dropdown menu
		const handleClickOutside = (e) => {
			if (containerLang.current && !containerLang.current.contains(e.target)) {
				setMenuLang(false);
			}
		};

		window.addEventListener("mousedown", handleClickOutside);

		return () => {
			window.removeEventListener("mousedown", handleClickOutside);
		};
	}, []);

	// Handle submission of login data to backend
	const handleSubmit = (values) => {
		console.log(values);
		let data = new URLSearchParams();
		data.append("j_username", values.j_username);
		data.append("j_password", values.j_password);
		data.append(
			"_spring_security_remember_me",
			values._spring_security_remember_me
		);

		axios
			.post("/admin-ng/j_spring_security_check", data)
			.then((response) => {
				logger.info(response);
				history.push("/events/events");
			})
			.catch((response) => {
				logger.error(response);
				setError(true);
			});
	};

	return (
		<>
			{/*Todo: find equivalent to ng-cloak*/}
			<div className="login-body">
				<section className="login-container">
					<div className="login-form">
						<div className="form-container">
							{/*Login form*/}
							<Formik
								onSubmit={(values) => handleSubmit(values)}
								initialValues={initialValues}
							>
								{(formik) => (
									<div className="formik-container">
										<div className="row">
											<p>
												<span>{t("LOGIN.WELCOME")}</span>
												<br />
											</p>
										</div>

										{/*Only show if error occurs on login*/}
										{isError && (
											<div className="error-container">{t("LOGIN.ERROR")}</div>
										)}
										<div className="row">
											<Field
												name="j_username"
												type="text"
												id="email"
												placeholder={t("LOGIN.USERNAME")}
												className={cn("login-input", {
													error:
														(formik.touched.j_username &&
															formik.errors.j_username) ||
														isError,
												})}
												autoFocus="autoFocus"
											/>
										</div>
										<div className="row">
											<Field
												name="j_password"
												type="password"
												id="password"
												placeholder={t("LOGIN.PASSWORD")}
												className={cn("login-input", {
													error:
														(formik.touched.j_password &&
															formik.errors.j_password) ||
														isError,
												})}
											/>
										</div>

										<div className="row remember-me">
											<Field
												type="checkbox"
												id="remember"
												name="_spring_security_remember_me"
											/>
											<label htmlFor="remember">{t("LOGIN.REMEMBER")}</label>
										</div>
										<div className="row">
											<button
												className={cn("submit", {
													active: formik.dirty && formik.isValid,
													inactive: !(formik.dirty && formik.isValid),
												})}
												disabled={!(formik.dirty && formik.isValid)}
												onClick={() => formik.handleSubmit()}
												type="submit"
											>
												{t("LOGIN.LOGIN")}
											</button>
										</div>
									</div>
								)}
							</Formik>
						</div>

						{/*Language dropdown menu*/}
						<nav className="login-nav nav-dd-container" id="nav-dd-container">
							<div
								className="nav-dd lang-dd"
								id="lang-dd"
								ref={containerLang}
								onClick={() => setMenuLang(!displayMenuLang)}
							>
								<img
									className="lang-flag"
									src={currentLanguage.flag}
									alt={currentLanguage.code}
								/>
								<span>{currentLanguage.long}</span>
								{displayMenuLang && (
									<ul className="dropdown-ul">
										{languages.map((language, key) => (
											<li key={key}>
												<a
													href="#"
													onClick={() => changeLanguage(language.code)}
												>
													<img
														className="lang-flag"
														src={language.flag}
														alt={language.code}
													/>
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
			</div>
		</>
	);
};

export default Login;
