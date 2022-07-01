import React, {useEffect} from 'react';
import {connect} from "react-redux";
import { HashRouter, Navigate, Route, Routes } from 'react-router-dom';
import './App.scss';
import Events from "./components/events/Events";
import Recordings from "./components/recordings/Recordings";
import Jobs from "./components/systems/Jobs";
import Themes from "./components/configuration/Themes";
import Users from "./components/users/Users";
import Statistics from "./components/statistics/Statistics";
import Series from "./components/events/Series";
import Servers from "./components/systems/Servers";
import Services from "./components/systems/Services";
import Groups from "./components/users/Groups";
import Acls from "./components/users/Acls";
import {fetchOcVersion, fetchUserInfo} from "./thunks/userInfoThunks";

function App({ loadingUserInfo, loadingOcVersion }) {
    useEffect(() => {
       // Load information about current user on mount
       loadingUserInfo();
       // Load information about current opencast version on mount
       loadingOcVersion();
    }, [loadingOcVersion, loadingUserInfo]);

  return (
          <HashRouter>
              <Routes>
                  <Route exact path={"/"} element={<Events />} />

                  <Route exact path={"/events/events"} element={<Events />} />

                  <Route exact path={"/events/series"} element={<Series />} />

                  <Route exact path={"/recordings/recordings"} element={<Recordings />} />

                  <Route exact path={"/systems/jobs"} element={<Jobs />} />

                  <Route exact path={"/systems/servers"} element={<Servers />} />

                  <Route exact path={"/systems/services"} element={<Services />} />

                  <Route exact path={"/users/users"} element={<Users />} />

                  <Route exact path={"/users/groups"} element={<Groups />} />

                  <Route exact path={"/users/acls"} element={<Acls />} />

                  <Route exact path={"/configuration/themes"} element={<Themes />} />

                  <Route exact path={"/statistics/organization"} element={<Statistics />} />

                  <Route path={"*"}
                         render={() => <Navigate to={"/events/events"} replace />} />
              </Routes>
          </HashRouter>
  );
}

const mapDispatchToProps = dispatch => ({
    loadingUserInfo: () => dispatch(fetchUserInfo()),
    loadingOcVersion: () => dispatch(fetchOcVersion())
});

export default connect(null, mapDispatchToProps)(App);
