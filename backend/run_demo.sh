#!/usr/bin/env bash
# One-click launcher for macOS/Linux. Installs deps and starts the API in demo mode.
# Run:  bash run_demo.sh   (from inside the backend folder)
# Then open http://localhost:8000/docs and search any zipcode.
set -e
cd "$(dirname "$0")"

echo "Installing dependencies (first run only)..."
python3 -m pip install -q -r requirements.txt

echo
echo "============================================================"
echo " GroceryCompare backend starting in DEMO mode."
echo " Open your browser at:  http://localhost:8000/docs"
echo " Try GET /search with q=Kartoffeln and your zipcode."
echo " (Any zipcode works - sample offers auto-load for it.)"
echo " Press CTRL+C to stop."
echo "============================================================"
echo

export DEMO_MODE=1
python3 -m uvicorn app.main:app --port 8000
