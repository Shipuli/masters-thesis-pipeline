from .Updater import Fetch
from .db import DB

def update_financials(annual):
  print("Updating financials...")
  if annual:
    Fetch.fetchField("financials", __process_result)
  else:
    Fetch.fetchField("financials", __process_result, period=annual)
  print("Financials updated!")

def __process_result(results):
  for symbol in results:
    if 'financials' in results[symbol]['financials']:
      previous = list(DB.fetchFinancials(symbol))
      fetched = results[symbol]['financials']['financials']
      new = list(filter(lambda x: len(list(filter(lambda y: y['reportDate'] == x['reportDate'], previous))) == 0, fetched))
      if len(new) == 0:
        continue
      for fina in new:
        fina['symbol'] = symbol
      DB.insertFinancials(new)
