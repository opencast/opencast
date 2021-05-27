import React from "react";
import "./App.css";
import { Series } from "./components/Series";
import { Upload } from "./components/Upload";
import { Deeplink } from "./components/Deeplink";
import { Welcome } from "./components/Welcome";
import "./i18n";
import { parsedQueryString } from "./utils";

function App() {
    const qsParsed = parsedQueryString();
    if (qsParsed.subtool === "series")
        return <div className="container"><Series /></div>;
    if (qsParsed.subtool === "upload")
        return <div className="container"><Upload /></div>;
    if (qsParsed.subtool === "deeplink")
        return <div className="container"><Deeplink /></div>;
    return <div className="container"><Welcome /></div>;
}

export default App;
