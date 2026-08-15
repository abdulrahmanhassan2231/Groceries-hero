@echo off
REM Launcher that binds to ALL interfaces so your PHONE can reach the backend.
REM run_demo.bat / run_live.bat bind to localhost only - the phone cannot see those.

cd /d "%~dp0"

if not exist ".env" (
    if exist ".env.example" copy /y ".env.example" ".env" >nul
)

echo Installing dependencies (first run only)...
python -m pip install -q -r requirements.txt

echo.
echo ============================================================
echo  GroceryCompare backend - LAN mode (reachable from phone)
echo.
echo  On this PC:   http://localhost:8000/docs
echo  On phone:     http://192.168.178.176:8000/health
echo.
echo  If the phone URL does not load, it is the Windows firewall.
echo  Run once in an ADMIN PowerShell:
echo    netsh advfirewall firewall add rule name="GroceryCompare 8000" dir=in action=allow protocol=TCP localport=8000
echo.
echo  Press CTRL+C to stop.
echo ============================================================
echo.

set DEMO_MODE=1
python -m uvicorn app.main:app --host 0.0.0.0 --port 8000
pause
