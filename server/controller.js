const fs = require("fs");

function addMapping(router, mapping) {
    for (let url in mapping) {
        if (url.startsWith("POST ")) {
            let path = url.substring(5);
            router.post(path, mapping[url]);
        }
    }
}

function addControllers(router, dir) {
    fs.readdirSync(__dirname + "/" + dir).filter(f => {
        return f.endsWith(".js");
    }).forEach(f => {
        let mapping = require(__dirname + "/" + dir + "/" + f);
        addMapping(router, mapping);
    });
}

module.exports = () => {
    let router = require("koa-router")();
    addControllers(router, "controllers");
    return router.routes();
};
