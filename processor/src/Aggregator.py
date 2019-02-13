from .db import DB

class Aggregator:
  def __aggregate_result(self, symbol, interval):
    today = DB.fetchQuotes(symbol, interval)
    mean_high = np.mean([x['high'] if 'high' in x and isinstance(x['high'], float) else 0 for x in today])
    mean_low = np.mean([x['low'] if 'low' in x and isinstance(x['low'], float) else 0 for x in today])
    quote = {
      "symbol": symbol,
      "high": mean_high,
      "low": mean_low,
      "type": interval,
      "source": "Aggregate",
      "datetime": datetime.now(STD_TIMEZONE).replace(hour=0, minute=0, second=0, microsecond=0)
    }
    return quote
    # DB.clearQuotes(symbol, interval)
  
  def aggregate(self, interval):
    symbols = DB.fetchSymbols()
    results = []
    for symbol in symbols:
      quote = self.__aggregate_result(symbol, interval)
    if len(results) > 0:
      DB.upsertQuotes(results)
