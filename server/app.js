const Koa = require("koa");
const bodyParser = require("koa-bodyparser");

const model = require("./model");
const rest = require("./rest");
const controller = require("./controller");
const socket = require("./socket");

// init db
model.sync();

// init app
const app = new Koa();

// parse request body
app.use(bodyParser());

// prepare secure restful service
app.use(rest.restify());

// decrypt request
app.use(rest.decrypt());

// add controller
app.use(controller());

// run app
const server = app.listen(3000);

// init socket.io
const io = require("socket.io").listen(server);

// bind events to socket.io
socket(io);

console.log("app started at port 3000...");
