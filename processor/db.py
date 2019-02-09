import progressbar
from datetime import datetime, timedelta
from pymongo import MongoClient, UpdateOne
from pytz import timezone

STD_TIMEZONE = timezone('US/Pacific')

class DBClient:
  def __init__(self):
    self.client = MongoClient('mongodb://127.0.0.1:27017')
    self.db = self.client['MasterPipeline']
    self.db.stocks.create_index('symbol')

  def updateStocks(self, symbols):
    operations = []
    for symbol in symbols:
      operations.append(UpdateOne({ "symbol": symbol['symbol']}, { "$set": symbol}, upsert=True))
    self.db.stocks.bulk_write(operations)

  def insertStats(self, stats):
    self.updateStocks(stats)

  def insertSymbols(self, symbols):
    self.updateStocks(symbols)

  def fetchSymbols(self):
    return list(map(lambda x: x['symbol'], self.db.stocks.find({}, { "symbol": 1 })))

  # Task db logic
  def fetchLatestTask(self, task):
    return self.db.tasks.find({ 'type': task }).sort([('date',-1)]).limit(1)
    
  def insertTask(self, task, date, status, error=""):
    if len(error) != 0:
      self.db.tasks.insert_one({ 'type': task, 'date': date, 'status': status, 'error': error })
    else:
      self.db.tasks.insert_one({ 'type': task, 'date': date, 'status': status })

  # Quote db logic
  def fetchQuotes(self, symbol, interval):
    today = datetime.now(STD_TIMEZONE).replace(hour=0, minute=0, second=0, microsecond=0)
    if interval == 'day':
      return self.db.quotes.find({ "symbol": symbol, "type": "intraday", "datetime": { "$gt": today }})
    elif interval == 'week':
      return self.db.quotes.find({ "symbol": symbol, "type": "day", "datetime": {"$gte": today - timedelta(days=7)}})
    elif interval == 'month':
      return self.db.quotes.find({ "symbol": symbol, "type": "day", "datetime": {"$gte": today.replace(day=1)}})
    elif interval == 'year':
      return self.db.quotes.find({ "symbol": symbol, "type": "month", "datetime": {"$gte": today.replace(day=1, month=1) }})
  
  def upsertQuote(self, quote):
    self.db.quotes.update({"symbol": quote["symbol"], "datetime": quote["datetime"], "type": quote["type"]}, quote, upsert=True)

  def insertQuote(self, quote):
    self.db.quotes.insert_one(quote)

  def insertQuotes(self, quotes):
    self.db.quotes.insert_many(quotes)
  
  def clearQuotes(self, symbol, interval):
    today = datetime.now(STD_TIMEZONE).replace(hour=0, minute=0, second=0, microsecond=0)
    if interval == 'day':
      self.db.quotes.delete_many({ "symbol": symbol, "type": "intraday", "datetime": { "$lt": today - timedelta(days=3) }})
      return self.db.quotes.delete_many({ "symbol": symbol, "type": "day", "datetime": {"$lt": today - timedelta(days=36)}})
    elif interval == 'week':
      return self.db.quotes.delete_many({ "symbol": symbol, "type": "week", "datetime": {"$lt": today - timedelta(days=7)}})
    elif interval == 'month':
      return self.db.quotes.delete_many({ "symbol": symbol, "type": "month", "datetime": {"$lt": today - timedelta(months=36)}})    

  # Financial db logic
  def fetchFinancials(self, symbol, limit=0):
    if limit > 0:
      return self.db.financials.find({ "symbol": symbol }).sort([("reportDate", -1)]).limit(limit)
    else:
      return self.db.financials.find({ "symbol": symbol}).sort([("reportDate", -1)])

  def insertFinancial(self, financial):
    return self.db.financial.insert_one(financial)

  def insertFinancials(self, financials):
    return self.db.financial.insert_many(financials)

  # Dividend db logic
  def fetchDividends(self, symbol, limit=0):
    if limit > 0:
      return self.db.dividends.find({ "symbol": symbol }).sort([("recordDate", -1)]).limit(limit)
    else:
      return self.db.dividends.find({ "symbol": symbol}).sort([("recordDate", -1)])

  def insertDividend(self, dividend):
    return self.db.dividends.insert_one(dividend)
  
  def insertDividends(self, dividends):
    return self.db.dividends.insert_many(dividends)  

  def flush(self):
    self.db.stocks.delete_many({})
    self.db.quotes.delete_many({})
    self.db.dividends.delete_many({})
    self.db.financials.delete_many({})

DB = DBClient()
