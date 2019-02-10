from .Updater import Fetch
from .db import DB

def update_symbols():
  symbols = Fetch.fetchSymbols()
  DB.insertSymbols(symbols)

def update_stats():
  return Fetch.fetchField("stats", __process_result)

def __process_result(results):
  insertions = []
  for symbol in results:
    insertions.append(results[symbol]['stats'])
  DB.insertStats(insertions)
