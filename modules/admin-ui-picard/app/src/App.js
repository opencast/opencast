import React, {useEffect} from 'react';
import {connect} from "react-redux";
import { HashRouter, Redirect, Route, Switch } from 'react-router-dom';
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
import { fetchFilters } from './thunks/tableFilterThunks';

function App({ loadingUserInfo, loadingOcVersion, loadingFilters }) {
    useEffect(() => {
       // Load information about current user on mount
       loadingUserInfo();
       // Load information about current opencast version on mount
       loadingOcVersion();
       // Load initial filters for event table view
       loadingFilters("events");
    }, []);

  return (
          <HashRouter>
              <Switch>
                  <Route exact
                         path={"/"}
                         render={() => <Redirect to={"/events/events"}/>}/>
                  <Route exact path={"/events/events"}>
                      <Events />
                  </Route>
                  <Route exact path={"/events/series"}>
                      <Series />
                  </Route>
                  <Route exact path={"/recordings/recordings"}>
                      <Recordings />
                  </Route>
                  <Route exact path={"/systems/jobs"}>
                      <Jobs />
                  </Route>
                  <Route exact path={"/systems/servers"}>
                      <Servers />
                  </Route>
                  <Route exact path={"/systems/services"}>
                      <Services />
                  </Route>
                  <Route exact path={"/users/users"}>
                      <Users />
                  </Route>
                  <Route exact path={"/users/groups"}>
                      <Groups />
                  </Route>
                  <Route exact path={"/users/acls"}>
                      <Acls />
                  </Route>
                  <Route exact path={"/configuration/themes"}>
                      <Themes />
                  </Route>
                  <Route exact path={"/statistics/organization"}>
                      <Statistics />
                  </Route>
              </Switch>
          </HashRouter>
  );
}

const mapDispatchToProps = dispatch => ({
    loadingUserInfo: () => dispatch(fetchUserInfo()),
    loadingOcVersion: () => dispatch(fetchOcVersion()),
    loadingFilters: resource => dispatch(fetchFilters(resource))
});

export default connect(null, mapDispatchToProps)(App);
