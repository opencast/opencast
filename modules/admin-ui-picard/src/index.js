import React from 'react';
import ReactDOM from 'react-dom';
import './index.css';
import App from './App';
import * as serviceWorker from './serviceWorker';

// import i18n (needs to be bundled)
import './i18n/i18n';

import 'font-awesome/css/font-awesome.min.css';
import "react-datepicker/dist/react-datepicker.css";

import 'react-dates/initialize';
import 'react-dates/lib/css/_datepicker.css';
ReactDOM.render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
  document.getElementById('root')
);

// If you want your app to work offline and load faster, you can change
// unregister() to register() below. Note this comes with some pitfalls.
// Learn more about service workers: https://bit.ly/CRA-PWA
serviceWorker.unregister();
