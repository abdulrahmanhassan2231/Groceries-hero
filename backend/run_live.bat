@echo off
REM Launcher for LIVE Marktguru data (real prices, not demo).
REM 1) Copy .env.example to .env  (and optionally paste your Marktguru keys into it)
REM 2) Double-click this file, or run it from a terminal in the backend folder.
REM 3) Open http://localhost:8000/docs -> POST /admin/refresh with your zipcode -> Execute
REM    Then GET /search returns live offers.

cd /d "%~dp0"

if not exist ".env" (
    echo No .env found - creating one from .env.example ...
    copy /y ".env.example" ".env" >nul
    echo Created .env . You can edit it later to add your Marktguru keys.
    echo.
)

echo Installing dependencies (first run only)...
python -m pip install -q -r requirements.txt

echo.
echo ============================================================
echo  GroceryCompare backend starting in LIVE mode.
echo  1) Open:  http://localhost:8000/docs
echo  2) POST /admin/refresh  with your zipcode  -> Execute
echo     (check the response: "written" should be greater than 0)
echo  3) GET /search  with q and your zipcode
echo  If "written" is 0, see docs/live-marktguru-setup.md
echo  Press CTRL+C to stop.
echo ============================================================
echo.

set DEMO_MODE=0
python -m uvicorn app.main:app --port 8000
pause
