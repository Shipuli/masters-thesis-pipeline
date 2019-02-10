const { Schema } = require("mongoose");
const mongoose = require("mongoose");

const schema = new Schema({
  _id: {
    type: "ObjectId"
  },
  exDate: {
    type: "Date"
  },
  paymentDate: {
    type: "Date"
  },
  recordDate: {
    type: "Date"
  },
  declaredDate: {
    type: "Date"
  },
  amount: {
    type: "Number"
  },
  flag: {
    type: "String"
  },
  type: {
    type: "String"
  },
  qualified: {
    type: "String"
  },
  indicated: {
    type: "String"
  },
  symbol: {
    type: "String"
  }
});

module.exports = mongoose.model("Dividend", schema);
