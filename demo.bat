@echo off
chcp 65001 >nul
echo ========================================
echo   Spring Alibaba AI 智能客服系统演示
echo ========================================
echo.

echo 1. 检查系统环境...
java -version 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Java未安装或未配置环境变量
    echo 请安装Java 17或更高版本
    pause
    exit /b 1
)

mvn -version 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Maven未安装或未配置环境变量
    echo 请安装Maven 3.6或更高版本
    pause
    exit /b 1
)

echo ✅ 环境检查通过
echo.

echo 2. 编译项目...
mvn clean compile -q
if %ERRORLEVEL% NEQ 0 (
    echo ❌ 编译失败，请检查代码
    pause
    exit /b 1
)
echo ✅ 编译成功
echo.

echo 3. 运行测试...
mvn test -q
if %ERRORLEVEL% NEQ 0 (
    echo ⚠️  测试失败，但可以继续运行
) else (
    echo ✅ 测试通过
)
echo.

echo 4. 系统功能说明：
echo    📱 Web聊天界面: http://localhost:8080
echo    📊 系统监控: http://localhost:8080/api/chat/health
echo    💾 数据库控制台: http://localhost:8080/h2-console
echo    📖 关于页面: http://localhost:8080/about
echo.

echo 5. 重要提醒：
echo    🔑 请确保已配置通义千问API密钥
echo    📝 可在application.yml中配置或设置环境变量TONGYI_API_KEY
echo.

echo 6. 启动应用...
echo    按Ctrl+C可停止应用
echo.

mvn spring-boot:run
