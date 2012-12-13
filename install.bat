@ECHO OFF
echo Installing HTS Wowza application...
xcopy "conf\Application.xml" "%WMSAPP_HOME%\conf\wowza-hts\" /s /Y
xcopy "vod\Application.xml" "%WMSAPP_HOME%\conf\vod\" /s /Y

IF NOT EXIST "%WMSAPP_HOME%\applications\wowza-hts" mkdir "%WMSAPP_HOME%\applications\wowza-hts"
IF NOT EXIST "%WMSAPP_HOME%\applications\vod" mkdir "%WMSAPP_HOME%\applications\vod"
rem IF NOT "%1" == "all" pause
