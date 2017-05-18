const db = require("../db");

module.exports = db.defineModel("users", {
    email: {
        type: db.STRING,
        unique: true
    },
    password: db.STRING(60),
    publicKey: db.TEXT,
    privateKey: db.TEXT
});
