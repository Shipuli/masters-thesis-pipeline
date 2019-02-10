import numpy as np
from datetime import datetime
from pytz import timezone
from calendar import monthrange
from .Updater import Fetch
from .db import DB

STD_TIMEZONE = timezone('EST')

def update_history():
  print("Updating history...")
  Fetch.fetchField("chart", parse_history, interval="5y")
  print("History updated!")

def parse_history(results):
  for symbol in results:
    chart = results[symbol]['chart']
    if len(chart) == 0:
      continue
    years = aggregate_years(symbol, chart)
    months = aggregate_months(symbol, chart)
    days = filter_days(symbol, chart)
    DB.insertQuotes(years + months + days)

def filter_days(symbol, arr):
  today = datetime.now()
  result = []
  for item in arr:
    date = datetime.strptime(item['date'], "%Y-%m-%d")
    delta = today - date
    if delta.days < 36 and 'low' in item and 'high' in item:
      result.append({
        "symbol": symbol,
        "low": item["low"],
        "high": item["high"],
        "type": "day",
        "datetime": date.replace(hour=0, minute=0, second=0, microsecond=0)
      })
  return result

def group_by_year(arr):
  grouped = {}
  for item in arr:
    year = datetime.strptime(item['date'], "%Y-%m-%d").year
    if year in grouped:
      grouped[year].append(item)
    else:
      grouped[year] = [item]
  return grouped

def group_by_month(arr):
  grouped = {}
  for item in arr:
    month = datetime.strptime(item['date'], "%Y-%m-%d").month
    if month in grouped:
      grouped[month].append(item)
    else:
      grouped[month] = [item]
  return grouped

def aggregate_years(symbol, arr):
  grouped = group_by_year(arr)
  results = []
  for year in grouped:
    results.append({
      "symbol": symbol,
      "datetime": datetime.now().replace(year=year, month=12, day=31, hour=0, minute=0, second=0, microsecond=0),
      "type": "year",
      "low": np.mean(list(map(lambda x: x['low'], list(filter(lambda y: 'low' in y, grouped[year]))))),
      "high": np.mean(list(map(lambda x: x['high'], list(filter(lambda y: 'high' in y, grouped[year])))))
    })
  return results

def aggregate_months(symbol, arr):
  all_years = group_by_year(arr)
  this_year = datetime.now(STD_TIMEZONE).year
  grouped = {}
  for year in all_years:
    if year >= this_year - 2:
      grouped[year] = all_years[year]
  results = []
  for year in grouped:
    months = group_by_month(grouped[year])
    for month in months:
      last_day = monthrange(year, month)[1]
      results.append({
        "symbol": symbol,
        "datetime": datetime.now().replace(year=year, month=month, day=last_day, hour=0, minute=0, second=0, microsecond=0),
        "type": "month",
        "low": np.mean(list(map(lambda x: x['low'], list(filter(lambda y: 'low' in y, months[month]))))),
        "high": np.mean(list(map(lambda x: x['high'], list(filter(lambda y: 'high' in y, months[month])))))
      })
  return results