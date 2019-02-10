import requests, progressbar, time
from .db import DBClient

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
      fetched = False
      times = 0
      while(not fetched):
        try:
          r = requests.get(url)
          fetched = True
          if r.status_code == 200:
            result = r.json()
            callback(result)
          else:
            print(r.status_code)
        except:
          times += 1
          print("Connection cut for the " + str(times) + " time")
          time.sleep(2 ** times)
          if times > 4:
            break
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