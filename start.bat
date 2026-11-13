@echo off
chcp 65001 >nul
title SmartSlot 一键启动

rem 项目根目录（bat 所在位置，兼容路径中的空格）
cd /d "%~dp0"

echo ========================================
echo   SmartSlot 正在启动...
echo   后端: Spring Boot (端口 8088)
echo   前端: Vite 开发服务器
echo ========================================

rem 启动后端（新窗口，/D 指定工作目录）
start "SmartSlot-Backend" /D "%~dp0smart-slot-backend" cmd /k mvn spring-boot:run

rem 启动前端（新窗口）
start "SmartSlot-Frontend" /D "%~dp0smart-slot-frontend" cmd /k npm run dev

echo 两个服务已在独立窗口启动。
echo 后端地址: http://localhost:8088
echo 前端地址: 启动完成后见前端窗口输出的端口（Vite 默认 http://localhost:5173）
echo 关闭对应窗口即可停止服务。
