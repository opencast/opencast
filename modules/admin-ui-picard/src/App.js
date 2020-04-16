import React from 'react';
import './App.scss';
import Header from "./components/Header";
import Footer from "./components/Footer";

const version = {
  version: '8.03',
  buildNumber: '42'
};
const feedbackUrl = 'https://opencast.org/';

function App() {
  return (
      //todo: add Routing and all Pages
      <>
        <Header />
        <Footer version={version}  feedbackUrl={feedbackUrl}/>
      </>
  );
}

export default App;
