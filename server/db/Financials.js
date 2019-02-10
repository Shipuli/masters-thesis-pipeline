const { Schema } = require("mongoose");
const mongoose = require("mongoose");

const schema = new Schema({
  _id: {
    type: "ObjectId"
  },
  reportDate: {
    type: "Date"
  },
  grossProfit: {
    type: "Number"
  },
  costOfRevenue: {
    type: "Number"
  },
  operatingRevenue: {
    type: "Number"
  },
  totalRevenue: {
    type: "Number"
  },
  operatingIncome: {
    type: "Number"
  },
  netIncome: {
    type: "Number"
  },
  researchAndDevelopment: {
    type: "Number"
  },
  operatingExpense: {
    type: "Number"
  },
  currentAssets: {
    type: "Number"
  },
  totalAssets: {
    type: "Number"
  },
  totalLiabilities: {
    type: "Number"
  },
  currentCash: {
    type: "Number"
  },
  currentDebt: {
    type: "Number"
  },
  totalCash: {
    type: "Number"
  },
  totalDebt: {
    type: "Number"
  },
  shareholderEquity: {
    type: "Number"
  },
  cashChange: {
    type: "Number"
  },
  cashFlow: {
    type: "Number"
  },
  operatingGainsLosses: {
    type: "Mixed"
  },
  symbol: {
    type: "String"
  }
});

module.exports = mongoose.model("Financial", schema);
