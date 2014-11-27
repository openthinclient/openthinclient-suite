@ECHO OFF
REM # usage: deplyoment.bat <environment> <goal>

CD groovy

ECHO [Batch] Execute: java -cp .;groovy-all.jar;mysql-connector-java.jar;liquibase-core.jar;commons-io.jar;commons-lang3.jar com.t_systems_mms.health.deployment.GroovyRun %1 %2
CALL java -cp .;groovy-all.jar;mysql-connector-java.jar;liquibase-core.jar;commons-io.jar;commons-lang3.jar com.t_systems_mms.health.deployment.GroovyRun %1 %2
SET RESULT=%ERRORLEVEL%

CD ..

ECHO [Batch] ErrorLevel: %RESULT%
IF NOT %RESULT% == 0 exit /b %RESULT% 