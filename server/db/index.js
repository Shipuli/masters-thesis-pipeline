const mongoose = require('mongoose')
const Stock = require('./Stock')

class DBClient {
  constructor() {
    mongoose.connect('mongodb://127.0.0.1:27017/MasterPipeline', { useNewUrlParser: true })
  }

  fetchSymbols(symbol) {
    return Stock.find({ symbol }).exec()
  }
}

module.exports = DBClient