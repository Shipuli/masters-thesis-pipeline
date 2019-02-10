from Updater import Fetch
from db import DB

def update_dividends(range):
  print("Updating dividends...")
  Fetch.fetchField("dividends", __process_result, interval=range)
  print("Dividends updated!")  

def __process_result(results):
  for symbol in results:
    fetched = results[symbol]['dividends']
    if len(fetched) == 0:
      continue
    latest = list(DB.fetchDividends(symbol, limit=1))
    if len(fetched) == 1:
      fetched = fetched[0]
      if len(latest) > 0 and latest[0]['recordDate'] == fetched['recordDate']:
        continue
      fetched['symbol'] = symbol
      DB.insertDividend(fetched)
    else:
      if len(latest) > 0:
        fetched = list(filter(lambda x: x['recordDate'] > latest[0]['recordDate'], fetched))
        if len(fetched) == 0:
          continue
      for f in fetched:
        f['symbol'] = symbol
      DB.insertDividends(fetched)
