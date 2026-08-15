@echo off
REM One-click launcher for Windows. Installs deps and starts the API in demo mode.
REM Just double-click this file (or run it from a terminal) inside the backend folder,
REM then open http://localhost:8000/docs and search any zipcode.

cd /d "%~dp0"

echo Installing dependencies (first run only, may take a minute)...
python -m pip install -q -r requirements.txt

echo.
echo ============================================================
echo  GroceryCompare backend starting in DEMO mode.
echo  Open your browser at:  http://localhost:8000/docs
echo  Try GET /search  with  q = Kartoffeln  and your zipcode.
echo  (Any zipcode works - sample offers auto-load for it.)
echo  Press CTRL+C here to stop the server.
echo ============================================================
echo.

set DEMO_MODE=1
python -m uvicorn app.main:app --port 8000
pause
