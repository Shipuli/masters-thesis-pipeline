const express = require('express')
const app = express()
const DBClient = require('./server/db')
const db = new DBClient()

app.get("/", (req, res) => {
  res.send("<h1>Server is running</h1>")
})

app.get("/stocks/:symbol", async (req, res) => {
  res.json(await db.fetchSymbols(req.params.symbol))
})

app.get("/quotes/:symbol", async (req, res) => {
  res.json(await db.fetchQuotes(req.params.symbol, req.query.interval))
})

app.get("/dividends/:symbol", async (req, res) => {
  res.json(await db.fetchDividends(req.params.symbol))
})

app.get("/financials/:symbol", async (req, res) => {
  res.json(await db.fetchFinancials(req.params.symbol))
})

app.listen(6123, () => {
  console.log("Server is running on port: 6123")
})