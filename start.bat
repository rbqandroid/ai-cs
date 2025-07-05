@echo off
echo 正在启动Spring Alibaba AI智能客服系统...
echo.

REM 检查Java版本
java -version
echo.

REM 检查Maven版本
mvn -version
echo.

REM 编译项目
echo 正在编译项目...
mvn clean compile
if %ERRORLEVEL% NEQ 0 (
    echo 编译失败！
    pause
    exit /b 1
)

echo 编译成功！
echo.

REM 运行应用
echo 正在启动应用...
mvn spring-boot:run

pause
