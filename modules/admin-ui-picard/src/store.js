import {persistReducer} from "redux-persist";
import storage from 'redux-persist/lib/storage';
import autoMergeLevel2 from "redux-persist/lib";
import thunk from "redux-thunk";
import {composeWithDevTools} from "redux-devtools-extension";
import {applyMiddleware, combineReducers, createStore} from "redux";


const reducers = {

};

const persistConfig = {
    key: 'opencast',
    storage,
    stateReconciler: autoMergeLevel2
};

const rootReducer = combineReducers(reducers);
const persistedReducer = persistReducer(persistConfig, rootReducer);

export const configureStore = () => createStore(
    persistedReducer,
    composeWithDevTools(
        applyMiddleware(thunk)
    )
);
