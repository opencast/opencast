import React, {useEffect} from 'react';
import {connect} from "react-redux";
import {HashRouter, Route, Switch} from "react-router-dom";
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
import {fetchUserInfo} from "./thunks/userInfoThunks";

function App({loadingUserInfo}) {
    useEffect(() => {
       // Load information about current user on mount
       loadingUserInfo();
    });
  return (
          <HashRouter>
              <Switch>
                  {/*Todo: When user is logged in then redirect to Events*/}
                  <Route exact path={"/"}>
                      <Events />
                  </Route>
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
    loadingUserInfo: () => dispatch(fetchUserInfo())
});

export default connect(null, mapDispatchToProps)(App);
