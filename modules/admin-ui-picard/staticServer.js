const path = require("path");
const express = require("express");
const bodyParser = require('body-parser');

const app = express();
const port = process.env.PORT || 5000;

//Todo: not all serve static links from admin-ui-frontend here
app.use(
    "/admin-ng",
    express.static(path.join(__dirname, "test/app/GET/admin-ng"))
);
app.use(
    "/blacklist",
    express.static(path.join(__dirname, "test/app/GET/blacklist"))
);
app.use(
    "/capture-agents",
    express.static(path.join(__dirname, "test/app/GET/capture-agents"))
);
app.use(
    "/email",
    express.static(path.join(__dirname, "test/app/GET/email"))
);
app.use(
    "/groups",
    express.static(path.join(__dirname, "test/app/GET/groups"))
);
app.use(
    "/info",
    express.static(path.join(__dirname, "test/app/GET/info"))
);
app.use(
    "/roles",
    express.static(path.join(__dirname, "test/app/GET/roles"))
);
app.use(
    "/services",
    express.static(path.join(__dirname, "test/app/GET/services"))
);
app.use(
    "/sysinfo",
    express.static(path.join(__dirname, "test/app/GET/sysinfo"))
);
app.use(
    "/workflow",
    express.static(path.join(__dirname, "test/app/GET/workflow"))
);

app.listen(port, () => console.log(`Listing on port ${port}`));

