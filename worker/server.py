from flask import Flask, request
from cli import exec_command

app = Flask(__name__)

@app.route("/init")
def init():
  exec_command("init")
  return 'Command run!'

@app.route("/run")
def run():
  exec_command("run", request.args.get("stat", ""))
  return 'Command run!'

@app.route("/live")
def live():
  exec_command("live")
  return 'Command run!'

@app.route("/flush")
def flush():
  exec_command("flush")
  return 'Command run!'

@app.route("/refresh")
def refresh():
  exec_command("refresh")
  return 'Command run!'

if __name__ == "__main__":
  app.run(host='0.0.0.0')