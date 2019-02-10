import numpy as np
from datetime import datetime
from pytz import timezone
from calendar import monthrange
from .db import DB
from .Updater import Fetch


STD_TIMEZONE = timezone('EST')

class Price:

  def update(self):
    print("Updating prices...")
    Fetch.fetchField("quote", self.__process_result, fields="latestSource,latestTime,high,low")
    print("Prices updated!")

  def __transformDay(self, symbol, quote):
    quote['symbol'] = symbol
    quote['type'] = 'intraday'
    quote['datetime'] = datetime.now(STD_TIMEZONE)

  def __insertDay(self, symbol, quote):
    if not 'low' in quote or not 'high' in quote:
      return
    self.__transformDay(symbol, quote)
    DB.upsertQuote(quote)
    
  def __process_result(self, results):
    bulk = []
    for symbol in results:
      quote = results[symbol]['quote']
      if quote['latestSource'] == 'Close':
        self.__insertDay(symbol, quote)
        self.__end_day(symbol)
      elif quote['latestSource'] == 'Previous close':
        continue
      else:
        if 'low' in quote and 'high' in quote:
          bulk.append(self.__transformDay(quote))
    if len(bulk) > 0:
      DB.upsertQuotes(bulk)

  def __end_day(self, symbol):
    # Aggregate day
    self.__aggregate_result(symbol, 'day')

    now = datetime.now(STD_TIMEZONE)
    # Aggregate week
    if now.weekday() == 6:
      self.__aggregate_result(symbol, 'week')

    last_day = monthrange(now.year, now.month)[1]
    # Aggregate month
    if now.day == last_day:
      self.__aggregate_result(symbol, 'month')
      # Aggregate year
      if now.month == 12:
        self.__aggregate_result(symbol, 'year')

  def __aggregate_result(self, symbol, interval):
    today = DB.fetchQuotes(symbol, interval)
    mean_high = np.mean([x['high'] if 'high' in x and isinstance(x['high'], float) else 0 for x in today])
    mean_low = np.mean([x['low'] if 'low' in x and isinstance(x['low'], float) else 0 for x in today])
    quote = {
      "symbol": symbol,
      "high": mean_high,
      "low": mean_low,
      "type": interval,
      "datetime": datetime.now(STD_TIMEZONE).replace(hour=0, minute=0, second=0, microsecond=0)
    }
    DB.upsertQuote(quote)
    DB.clearQuotes(symbol, interval)

Price_updater = Price()
