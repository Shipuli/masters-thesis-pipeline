import sys, traceback, time
from datetime import datetime
from db import DB
from Updater import Fetch
from pytz import timezone
from calendar import monthrange

from Price import Price_updater
from Dividend import update_dividends
from Financials import update_financials
from Stats import update_stats, update_symbols
from History import update_history

SECONDS_IN_HOUR = 60 * 60
SECONDS_IN_DAY = SECONDS_IN_HOUR * 24
SECONDS_IN_WEEK = SECONDS_IN_DAY * 7
SECONDS_IN_MONTH = SECONDS_IN_DAY * 30
STD_TIMEZONE = timezone('US/Pacific')

def general_task(task, interval, updateFunction, *args):
  prev = DB.fetchLatestTask(task)
  if prev.count() > 0:
    prev = prev.next()
    now = datetime.now()
    if (now - prev['date']).total_seconds() < interval and prev['status'] == 'Success':
      return
  try:
    start = time.time()
    print("Updating " + task + "...")
    updateFunction(*args)
    print(task.capitalize() + " updated!")
    update_time = time.time() - start
    print("This took: " + str(round(update_time)) + "s")
    DB.insertTask(task, datetime.now(), "Success")
    return True
  except Exception as e:
    print("Task " + task + " failed!")
    traceback.print_exc(file=sys.stdout)
    DB.insertTask(task, datetime.now(), "Failed", str(e))
    return False

def init():
  # update_dividends("5y")
  # update_financials(annual=True)
  # update_stats()
  update_history()
  
def main():
  argv = sys.argv
  if len(argv) > 1 and argv[1] == "init":
    if general_task("symbols", 0, update_symbols):
      general_task("init", sys.maxsize, init)
  elif len(argv) > 1 and argv[1] == "refresh":
    general_task("stats", SECONDS_IN_WEEK, update_stats)
    general_task("financials", 3 * SECONDS_IN_MONTH, update_financials)
    general_task("dividends", SECONDS_IN_MONTH, update_dividends)
  elif len(argv) > 1 and argv[1] == "live":
    general_task("quote", 0, Price_updater.update)
  elif len(argv) > 1 and argv[1] == "flush":
    DB.flush()
  elif len(argv) > 2 and argv[1] == "run":
    if argv[2] == 'financials':
      general_task("financials", 0, update_financials)
    elif argv[2] == 'dividends':
      general_task("dividends", 0, update_dividends, "1m")
    elif argv[2] == 'stats':
      general_task("stats", 0, update_stats)
    elif argv[2] == 'symbols':
      general_task("symbols", 0, update_symbols)

main()