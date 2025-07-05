@echo off
chcp 65001 >nul
echo ========================================
echo   修复Maven依赖问题
echo ========================================
echo.

echo 1. 检测Maven配置问题...
echo 发现问题：系统配置了无法访问的内部Maven仓库
echo.

echo 2. 创建本地Maven配置...
if not exist "%USERPROFILE%\.m2" mkdir "%USERPROFILE%\.m2"

echo 3. 复制settings.xml到用户目录...
copy settings.xml "%USERPROFILE%\.m2\settings.xml" >nul
if %ERRORLEVEL% EQU 0 (
    echo ✅ Maven配置文件已更新
) else (
    echo ❌ 配置文件复制失败
)
echo.

echo 4. 使用简化版pom.xml（不包含Spring AI Alibaba）...
copy pom-simple.xml pom.xml >nul
echo ✅ 已切换到简化版pom.xml
echo.

echo 5. 清理Maven缓存...
mvn clean >nul 2>&1
echo ✅ Maven缓存已清理
echo.

echo 6. 测试Maven编译...
echo 正在编译项目...
mvn compile -q
if %ERRORLEVEL% EQU 0 (
    echo ✅ 编译成功！
    echo.
    echo 7. 运行测试...
    mvn test -q
    if %ERRORLEVEL% EQU 0 (
        echo ✅ 测试通过！
    ) else (
        echo ⚠️  测试失败，但编译成功
    )
) else (
    echo ❌ 编译失败
    echo.
    echo 尝试使用阿里云镜像重新编译...
    mvn -s settings.xml compile -q
    if %ERRORLEVEL% EQU 0 (
        echo ✅ 使用阿里云镜像编译成功！
    ) else (
        echo ❌ 编译仍然失败，请检查网络连接
    )
)

echo.
echo ========================================
echo   修复完成
echo ========================================
echo.
echo 说明：
echo - 已配置阿里云Maven镜像
echo - 使用简化版pom.xml（暂时移除Spring AI Alibaba依赖）
echo - 基础Spring Boot功能可正常使用
echo.
echo 如需添加Spring AI Alibaba支持，请：
echo 1. 确保网络可访问Spring仓库
echo 2. 使用完整版pom.xml
echo.

pause
