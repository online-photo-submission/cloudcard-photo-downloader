@echo off
setlocal
rem Upgrades RemotePhoto Downloader from an older version without losing settings or photos.
rem Close the RemotePhoto Downloader app first. Keep this file in the same folder as the
rem new installer, then right-click it > Run as administrator.

set "MSI="
set "MSICOUNT=0"
set "APP=%ProgramFiles%\RemotePhoto Downloader\app"
set "BAK=%ProgramData%\RemotePhoto Downloader\upgrade-backup"
set "SVC=RemotePhoto-Downloader"
set "HASSVC="
set "WASRUNNING="

net session >nul 2>&1 || goto :notadmin
rem Use the one RemotePhoto Downloader installer next to this file, whatever its version.
for %%F in ("%~dp0RemotePhoto Downloader*.msi") do (set "MSI=%%~fF" & set /a MSICOUNT+=1)
if not "%MSICOUNT%"=="1" goto :nomsi
if not exist "%APP%\" goto :noapp
if exist "%BAK%\" goto :existingbackup

echo Stopping service...
sc query "%SVC%" >nul 2>&1 && set "HASSVC=1"
if defined HASSVC (
  sc query "%SVC%" | find "RUNNING" >nul && set "WASRUNNING=1"
  net stop "%SVC%" >nul 2>&1
  sc query "%SVC%" | find "STOPPED" >nul || goto :stopfail
)

echo Backing up settings and photos to %BAK%
mkdir "%BAK%" || goto :backupfail
if exist "%APP%\application.properties" (copy /y "%APP%\application.properties" "%BAK%\" >nul || goto :backupfail)
if exist "%APP%\downloader-service.json" (copy /y "%APP%\downloader-service.json" "%BAK%\" >nul || goto :backupfail)
if exist "%APP%\downloaded-photos\" (
  robocopy "%APP%\downloaded-photos" "%BAK%\downloaded-photos" /E /R:1 /W:1 /NP /NFL /NDL /NJH /NJS >nul
  if errorlevel 8 goto :backupfail
)

echo Installing new version. This can take a few minutes...
start "" /wait msiexec /i "%MSI%" /passive /norestart
set "MSICODE=%errorlevel%"
if not "%MSICODE%"=="0" if not "%MSICODE%"=="3010" goto :installfail

echo Restoring settings and photos...
if exist "%BAK%\application.properties" (copy /y "%BAK%\application.properties" "%APP%\" >nul || goto :restorefail)
if exist "%BAK%\downloader-service.json" (copy /y "%BAK%\downloader-service.json" "%APP%\" >nul || goto :restorefail)
if exist "%BAK%\downloaded-photos\" (
  robocopy "%BAK%\downloaded-photos" "%APP%\downloaded-photos" /E /R:1 /W:1 /NP /NFL /NDL /NJH /NJS >nul
  if errorlevel 8 goto :restorefail
)

rem Everything is back in place, so remove the temporary copy (it includes the API token).
rmdir /s /q "%BAK%" 2>nul
if exist "%BAK%\" echo Note: could not remove the temporary backup at %BAK% - please delete it.

if defined WASRUNNING (
  echo Starting service...
  net start "%SVC%" >nul 2>&1 || goto :startfail
)

echo.
echo Upgrade complete.
if "%MSICODE%"=="3010" echo Windows needs a restart to finish. Please restart when convenient.
pause
exit /b 0

:notadmin
echo Please right-click this file and choose "Run as administrator".
pause
exit /b 1

:nomsi
echo Keep exactly one RemotePhoto Downloader installer (.msi) in the same folder as this file, then run it again.
pause
exit /b 1

:noapp
echo RemotePhoto Downloader was not found at %APP%. Nothing was changed. Please contact RemotePhoto support.
pause
exit /b 1

:existingbackup
echo A backup from an earlier attempt already exists at %BAK%
echo To keep it safe, nothing was changed. Please contact RemotePhoto support.
pause
exit /b 1

:stopfail
echo Could not stop the RemotePhoto Downloader service. Nothing was changed.
echo Close the RemotePhoto Downloader app and try again, or contact RemotePhoto support.
pause
exit /b 1

:backupfail
if defined WASRUNNING net start "%SVC%" >nul 2>&1
echo Backup failed, so nothing was installed. Please contact RemotePhoto support.
pause
exit /b 1

:installfail
echo The installer did not finish (code %MSICODE%). Your files are saved in %BAK%
echo Please contact RemotePhoto support.
pause
exit /b 1

:restorefail
echo The new version is installed, but your files could not be copied back. They are saved in %BAK%
echo Please contact RemotePhoto support.
pause
exit /b 1

:startfail
echo The upgrade finished and your files were restored, but the service did not start.
echo Open RemotePhoto Downloader and click Start, or contact RemotePhoto support.
pause
exit /b 1
