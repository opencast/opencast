import {persistReducer} from "redux-persist";
import storage from 'redux-persist/lib/storage';
import autoMergeLevel2 from "redux-persist/lib";
import thunk from "redux-thunk";
import {composeWithDevTools} from "redux-devtools-extension";
import {applyMiddleware, combineReducers, createStore} from "redux";
import tableFilters from './reducers/tableFilterReducers';
import tableFilterProfiles from './reducers/tableFilterProfilesReducer';
import events from './reducers/eventReducers';
import table from './reducers/tableReducers';
import series from "./reducers/seriesReducer";
import recordings from "./reducers/recordingReducer";
import jobs from "./reducers/jobReducer";
import servers from "./reducers/serverReducer";
import services from "./reducers/serviceReducer";
import users from "./reducers/userReducers";
import groups from "./reducers/groupReducers";
import acls from "./reducers/aclReducers";
import themes from "./reducers/themeReducers";
import health from "./reducers/healthReducers";
import {notifications} from "./reducers/notificationReducers";
import workflows from "./reducers/workflowReducers";
import eventDetails from "./reducers/eventDeatilsReducers";
import recordingDetails from "./reducers/recordingDetailsReducer";

/**
 * This File contains the configuration for the store used by the reducers all over the app
 */

// form reducer and all other reducers used in this app
const reducers = {
    tableFilters,
    tableFilterProfiles,
    events,
    series,
    table,
    recordings,
    jobs,
    servers,
    services,
    users,
    groups,
    acls,
    themes,
    health,
    notifications,
    workflows,
    eventDetails,
    recordingDetails
};

// Configuration for persisting store
const persistConfig = {
    key: 'opencast',
    storage,
    stateReconciler: autoMergeLevel2
};

const rootReducer = combineReducers(reducers);
const persistedReducer = persistReducer(persistConfig, rootReducer);

// Store configuration, store holds the current state of the app
// Todo: Change rootReducer to persistedReducer for actually saving state even if reloads occur. At the moment it is
//  commented out because of debugging purposes.
export const configureStore = () => createStore(
    rootReducer,
    composeWithDevTools(
        applyMiddleware(thunk)
    )
);
