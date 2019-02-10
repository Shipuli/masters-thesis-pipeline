const mongoose = require('mongoose')
const Stock = require('./Stock')
const Quote = require('./Quote')
const Dividend = require('./Dividend')
const Financial = require('./Financials')

class DBClient {
  constructor() {
    const host = process.env.MONGO_HOST || "127.0.0.1"
    mongoose.connect(`mongodb://${host}:27017/MasterPipeline`, { useNewUrlParser: true })
  }

  fetchSymbols(symbol) {
    return Stock.find({ symbol }).exec()
  }

  fetchQuotes(symbol, interval="") {
    if (interval.length > 0) {
      return Quote.find({ symbol, type: interval }).exec()
    } else {
      return Quote.find({ symbol }).exec()
    }
  }

  fetchDividends(symbol) {
    return Dividend.find({ symbol }).exec()
  }

  fetchFinancials(symbol) {
    return Financial.find({ symbol }).exec()
  }
}

module.exports = DBClient