@echo off
set SPRING_APPLICATION_JSON={"spring":{"profiles":{"active":"dev"}}}
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5006"
