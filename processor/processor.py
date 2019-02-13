from flask import Flask, request
from src.Aggregator import Aggregator

aggregator = Aggregator()
app = Flask(__name__)

@app.route("/aggregate")
def init():
  print(request.args.get("interval", ""))
  aggregator.aggregate(request.args.get("interval", ""))
  return 'Command run!'

if __name__ == "__main__":
  app.run(host='0.0.0.0')