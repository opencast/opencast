import React from 'react';

const TableFilterContext = React.createContext({});

export const TableFilterProvider = TableFilterContext.Provider;
export const TableFilterConsumer = TableFilterContext.Consumer;
