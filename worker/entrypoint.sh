#!/bin/bash
set -e

python cli.py init
exec "$@"
