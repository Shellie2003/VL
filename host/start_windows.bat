@echo off
cd /d "%~dp0"
where py >nul 2>nul && (set PY=py -3) || (set PY=python)
%PY% -m pip install --disable-pip-version-check -q -r requirements.txt
%PY% volavo_host.py %*
if errorlevel 1 pause
