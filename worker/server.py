from flask import Flask
from cli import exec_command

app = Flask(__name__)

@app.route("/init")
def init(request):
  exec_command("init")
  return 'Command run!'

@app.route("/run")
def run(request):
  exec_command("run", request.args.get("stat", ""))
  return 'Command run!'

@app.route("/live")
def live(request):
  exec_command("live")
  return 'Command run!'

@app.route("/flush")
def flush(request):
  exec_command("flush")
  return 'Command run!'

@app.route("/refresh")
def refresh(request):
  exec_command("refresh")
  return 'Command run!'
