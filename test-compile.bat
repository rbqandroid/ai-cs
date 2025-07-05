@echo off
chcp 65001 >nul
echo ========================================
echo   测试项目编译
echo ========================================
echo.

echo 1. 检查Java版本...
java -version
echo.

echo 2. 检查Maven版本...
mvn -version
echo.

echo 3. 清理项目...
mvn clean
echo.

echo 4. 编译项目（详细输出）...
mvn compile -e -X > compile-log.txt 2>&1
if %ERRORLEVEL% EQU 0 (
    echo ✅ 编译成功！
    echo.
    echo 5. 运行测试...
    mvn test -q
    if %ERRORLEVEL% EQU 0 (
        echo ✅ 测试通过！
    ) else (
        echo ⚠️  测试失败，但编译成功
    )
) else (
    echo ❌ 编译失败
    echo 查看详细日志：compile-log.txt
    echo.
    echo 显示最后20行错误信息：
    powershell -Command "Get-Content compile-log.txt | Select-Object -Last 20"
)

echo.
echo 编译完成！
pause
