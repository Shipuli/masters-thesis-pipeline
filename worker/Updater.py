import requests, progressbar, time
from db import DBClient

def chunks(l, n):
    """Yield successive n-sized chunks from l."""
    for i in range(0, len(l), n):
        yield l[i:i + n]

class Fetcher:
  def __init__(self):
    self.baseUrl = 'https://api.iextrading.com/1.0'
    self.db = DBClient() 

  def fetchField(self, types, callback, fields="", interval="", period=""):
    symbols = self.db.fetchSymbols()
    symbols = list(chunks(symbols, 100))
    bar = progressbar.ProgressBar(maxval=len(symbols), \
    widgets=[progressbar.Bar('=', '[', ']'), ' ', progressbar.Percentage()])
    bar.start()
    i = 0
    for batch in symbols:
      start = time.time()
      url = self.baseUrl + '/stock/market/batch?symbols=' + ",".join(batch).lower() + "&types=" + types
      if len(fields) > 0:
        url += "&filter=" + fields
      if len(interval) > 0:
        url += "&range=" + interval
      if len(period) > 0:
        url += "&period=" + period 
      r = requests.get(url)
      if r.status_code == 200:
        fetch_time = time.time() - start
        print("Fetch took: " + str(round(fetch_time)) + "s")
        start = time.time()
        result = r.json()
        unpacking_time = time.time() - start
        print("Unpacking took: " + str(round(unpacking_time)) + "s")
        start = time.time()
        callback(result)
        process_time = time.time() - start
        print("Processing took: " + str(round(process_time)) + "s")
      else:
        print(r.status_code)
      i += 1
      bar.update(i)
    bar.finish()

  def fetchSymbols(self):
    r = requests.get(self.baseUrl + '/ref-data/symbols')
    if r.status_code == 200:
      result = r.json()
      symbols = list(filter(lambda x: x['type'] == 'cs', result))
      for sym in symbols:
        sym.pop('type', None)
      return symbols

Fetch = Fetcher()