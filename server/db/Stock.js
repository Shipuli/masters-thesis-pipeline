const { Schema } = require("mongoose");
const mongoose = require("mongoose");

const schema = new Schema({
  _id: {
    type: "ObjectId"
  },
  symbol: {
    type: "String"
  },
  date: {
    type: "Date"
  },
  iexId: {
    type: "Date"
  },
  isEnabled: {
    type: "Boolean"
  },
  name: {
    type: "String"
  },
  EBITDA: {
    type: "Number"
  },
  EPSSurpriseDollar: {
    type: "Mixed"
  },
  EPSSurprisePercent: {
    type: "Number"
  },
  beta: {
    type: "Number"
  },
  cash: {
    type: "Number"
  },
  companyName: {
    type: "String"
  },
  consensusEPS: {
    type: "Number"
  },
  day200MovingAvg: {
    type: "Number"
  },
  day30ChangePercent: {
    type: "Number"
  },
  day50MovingAvg: {
    type: "Number"
  },
  day5ChangePercent: {
    type: "Number"
  },
  debt: {
    type: "Number"
  },
  dividendRate: {
    type: "Number"
  },
  dividendYield: {
    type: "Number"
  },
  exDividendDate: {
    type: "Date"
  },
  float: {
    type: "Number"
  },
  grossProfit: {
    type: "Number"
  },
  insiderPercent: {
    type: "Mixed"
  },
  institutionPercent: {
    type: "Number"
  },
  latestEPS: {
    type: "Number"
  },
  latestEPSDate: {
    type: "Date"
  },
  marketcap: {
    type: "Number"
  },
  month1ChangePercent: {
    type: "Number"
  },
  month3ChangePercent: {
    type: "Number"
  },
  month6ChangePercent: {
    type: "Number"
  },
  numberOfEstimates: {
    type: "Number"
  },
  peRatioHigh: {
    type: "Number"
  },
  peRatioLow: {
    type: "Number"
  },
  priceToBook: {
    type: "Number"
  },
  priceToSales: {
    type: "Number"
  },
  profitMargin: {
    type: "Number"
  },
  returnOnAssets: {
    type: "Number"
  },
  returnOnCapital: {
    type: "Mixed"
  },
  returnOnEquity: {
    type: "Number"
  },
  revenue: {
    type: "Number"
  },
  revenuePerEmployee: {
    type: "Number"
  },
  revenuePerShare: {
    type: "Number"
  },
  sharesOutstanding: {
    type: "Number"
  },
  shortDate: {
    type: "Number"
  },
  shortInterest: {
    type: "Number"
  },
  shortRatio: {
    type: "Mixed"
  },
  ttmEPS: {
    type: "Number"
  },
  week52change: {
    type: "Number"
  },
  week52high: {
    type: "Number"
  },
  week52low: {
    type: "Number"
  },
  year1ChangePercent: {
    type: "Number"
  },
  year2ChangePercent: {
    type: "Number"
  },
  year5ChangePercent: {
    type: "Number"
  },
  ytdChangePercent: {
    type: "Number"
  }
});

module.exports = mongoose.model("Stock", schema);
