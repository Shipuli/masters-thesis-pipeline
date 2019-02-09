const { Schema } = require("mongoose");
const mongoose = require("mongoose");

const schema = new Schema({
  _id: {
    type: "String"
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
  chart: {
    type: ["Mixed"]
  },
  dividends: {
    type: ["Mixed"]
  },
  financials: {
    type: ["Mixed"]
  },
  stats: {
    marketcap: {
      type: "Number"
    },
    beta: {
      type: "Number"
    },
    week52high: {
      type: "Number"
    },
    week52low: {
      type: "Number"
    },
    week52change: {
      type: "Number"
    },
    shortInterest: {
      type: "Number"
    },
    shortDate: {
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
    latestEPS: {
      type: "Number"
    },
    latestEPSDate: {
      type: "Date"
    },
    sharesOutstanding: {
      type: "Number"
    },
    float: {
      type: "Number"
    },
    returnOnEquity: {
      type: "Number"
    },
    consensusEPS: {
      type: "Number"
    },
    numberOfEstimates: {
      type: "Number"
    },
    EPSSurpriseDollar: {
      type: "Mixed"
    },
    EPSSurprisePercent: {
      type: "Number"
    },
    EBITDA: {
      type: "Number"
    },
    revenue: {
      type: "Number"
    },
    grossProfit: {
      type: "Number"
    },
    cash: {
      type: "Number"
    },
    debt: {
      type: "Number"
    },
    ttmEPS: {
      type: "Number"
    },
    revenuePerShare: {
      type: "Number"
    },
    revenuePerEmployee: {
      type: "Number"
    },
    peRatioHigh: {
      type: "Number"
    },
    peRatioLow: {
      type: "Number"
    },
    returnOnAssets: {
      type: "Number"
    },
    returnOnCapital: {
      type: "Mixed"
    },
    profitMargin: {
      type: "Number"
    },
    priceToSales: {
      type: "Number"
    },
    priceToBook: {
      type: "Number"
    },
    day200MovingAvg: {
      type: "Number"
    },
    day50MovingAvg: {
      type: "Number"
    },
    institutionPercent: {
      type: "Number"
    },
    insiderPercent: {
      type: "Mixed"
    },
    shortRatio: {
      type: "Mixed"
    },
    year5ChangePercent: {
      type: "Number"
    },
    year2ChangePercent: {
      type: "Number"
    },
    year1ChangePercent: {
      type: "Number"
    },
    ytdChangePercent: {
      type: "Number"
    },
    month6ChangePercent: {
      type: "Number"
    },
    month3ChangePercent: {
      type: "Number"
    },
    month1ChangePercent: {
      type: "Number"
    },
    day5ChangePercent: {
      type: "Number"
    },
    day30ChangePercent: {
      type: "Number"
    }
  }
});

module.exports = mongoose.model("Stock", schema);
