const fs = require("fs");
const NodeRSA = require("node-rsa");

const keyPEM = fs.readFileSync("./security/server_private.pem", "utf-8");
const key = new NodeRSA(keyPEM);
key.setOptions({ encryptionScheme: "pkcs1" });

module.exports = {
    decrypt: encrypted => key.decrypt(encrypted, "utf-8")
};
