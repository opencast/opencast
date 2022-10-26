import React from "react";
import ReactDOM from "react-dom";
import "./index.css";
import App from "./App";
import * as serviceWorker from "./serviceWorker";

// redux imports
import { persistStore } from "redux-persist";
import { PersistGate } from "redux-persist/lib/integration/react";
import { Provider } from "react-redux";
import { configureStore } from "./store";

// import i18n (needs to be bundled)
import "./i18n/i18n";

// import css files for certain libraries
import "font-awesome/css/font-awesome.min.css";
import "react-datepicker/dist/react-datepicker.css";

// create redux store
const store = configureStore();
// todo: comment persistent stuff in, only out commented because for debugging purposes
//const persistor = persistStore(store);

ReactDOM.render(
	<React.StrictMode>
		<Provider store={store}>
			{/*<PersistGate*/}
			{/*    loading={<div>loading...</div>}*/}
			{/*    persistor={persistor} >*/}
			<App />
			{/*</PersistGate>*/}
		</Provider>
	</React.StrictMode>,
	document.getElementById("root")
);

// If you want your app to work offline and load faster, you can change
// unregister() to register() below. Note this comes with some pitfalls.
// Learn more about service workers: https://bit.ly/CRA-PWA
serviceWorker.unregister();
