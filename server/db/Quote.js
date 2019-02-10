const { Schema } = require("mongoose");
const mongoose = require("mongoose");

const schema = new Schema({
  _id: {
    type: "ObjectId"
  },
  symbol: {
    type: "String"
  },
  datetime: {
    type: "Date"
  },
  type: {
    type: "String"
  },
  low: {
    type: "Number"
  },
  high: {
    type: "Number"
  }
});

module.exports = mongoose.model("Quote", schema);
