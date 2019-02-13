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
    quote['source'] = 'IEX'
    quote['datetime'] = datetime.now(STD_TIMEZONE)
    
  def __process_result(self, results):
    bulk = []
    for symbol in results:
      quote = results[symbol]['quote']
      if quote['latestSource'] == 'Previous close':
        continue
      else:
        if 'low' in quote and 'high' in quote:
          bulk.append(self.__transformDay(quote))
    if len(bulk) > 0:
      DB.upsertQuotes(bulk)

Price_updater = Price()
