const path = require("path");
const express = require("express");
const router = express.Router();

const app = express();
const port = process.env.PORT || 5000;

//Todo: not all serve static links from admin-ui-frontend here
app.use(
    "/admin-ng",
    express.static(path.join(__dirname, "test/app/GET/admin-ng"))
);
app.use(
    "/acl-manager",
    express.static(path.join(__dirname, "test/app/GET/acl-manager"))
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
app.use("/app/styles",
    express.static(path.join(__dirname, "app/src/styles"))
);
app.use("/i18n",
    express.static(path.join(__dirname, "test/app/GET/i18n"))
);
app.use("/public",
    express.static(path.join(__dirname, "app/public"))
);
app.use("/staticfiles",
    express.static(path.join(__dirname, "test/app/POST"))
);
app.use("/modules",
    express.static(path.join(__dirname, "app/src/components"))
);
app.use("/shared",
    express.static(path.join(__dirname, "app/src/components"))
);
app.use("/img",
    express.static(path.join(__dirname, "app/src/img"))
);

app.post("/*", (req, res) => {
    let filePath = path.join(__dirname,'test/app/' + req.method + req.url);
    setTimeout(function () {
        res.status(201);
        res.sendFile(filePath);
    }, 1000);

});

app.listen(port, () => console.log(`Listing on port ${port}`));

