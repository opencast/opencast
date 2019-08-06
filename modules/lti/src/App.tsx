import React from "react";
import "./App.css";
import { Series } from "./components/Series";
import { Upload } from "./components/Upload";
import { Welcome } from "./components/Welcome";
import "./i18n";
import { parsedQueryString } from "./utils";

function App() {
    const qsParsed = parsedQueryString();
    if (qsParsed.tool === "series")
        return <div className="container"><Series /></div>;
    if (qsParsed.tool === "upload")
        return <div className="container"><Upload /></div>;
    return <div className="container"><Welcome /></div>;
}

export default App;
