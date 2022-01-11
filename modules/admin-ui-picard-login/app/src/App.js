import React from 'react';
import logo from './logo.svg';
import './App.scss';
import Login from "./components/Login";
import {HashRouter, Navigate, Route, Routes} from "react-router-dom";

function App() {
  return (
      <HashRouter>
        <Routes>
            <Route path={"/"} element={<Login/>} />
        </Routes>
      </HashRouter>
  );
}

export default App;
