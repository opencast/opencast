import React from "react";
import { createRoot } from "react-dom/client";
import { GraphiQL } from "graphiql";
import { explorerPlugin } from "@graphiql/plugin-explorer";
import { createLocalStorage, Fetcher } from "@graphiql/toolkit";
import "graphiql/graphiql.css";
import "@graphiql/plugin-explorer/dist/style.css";

import "./index.css";

const App: React.FC = () => {
  const fetcher: Fetcher = async graphQLParams => {
    const data = await fetch(
      "/graphql",
      {
        method: "POST",
        headers: {
          Accept: "application/json",
          "Content-Type": "application/json",
        },
        body: JSON.stringify(graphQLParams),
        credentials: "same-origin",
      },
    );
    return data.json().catch(() => data.text());
  };

  return (
    <GraphiQL
      plugins={[explorerPlugin({})]}
      fetcher={fetcher}
      storage={createLocalStorage({ namespace: "opencast" })}
      shouldPersistHeaders></GraphiQL>
  );
};

const rootElement = document.getElementById("root");
if (rootElement == null) {
  throw new Error("No root element found");
}

const root = createRoot(rootElement);
root.render(<App />);
