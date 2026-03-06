# Weather Flex Alarm (Android)

安卓闹钟原型：根据天气动态调整起床时间。

## 规则
- 基础闹钟时间：你手动设置（例如 07:30）
- 异常天气：雨/雪时，闹钟提前 N 分钟（默认 20）
- 正常天气：按基础时间响铃

## 技术实现
- 天气 API：Open-Meteo（无需 API Key）
- 后台任务：WorkManager 每 6 小时重算一次
- 闹钟调度：AlarmManager（尽量精确闹钟，失败时降级）
- 异常天气判断：基于 WMO 天气码（雨雪码）

## 目录
- `app/src/main/java/com/example/weatherflexalarm/`
  - `MainActivity.kt`：设置页
  - `WeatherAdjustWorker.kt`：天气拉取 + 闹钟重排
  - `AlarmScheduler.kt`：实际设置闹钟
  - `AlarmReceiver.kt`：闹钟触发并启动响铃服务
  - `AlarmRingingService.kt`：前台服务播放系统闹钟铃声

## 运行
1. 用 Android Studio 打开 `weather-flex-alarm`。
2. 首次运行后：
   - 设置基础闹钟时间
   - 设置提前分钟数
   - 填经纬度（或用“使用当前位置”）
   - 点击“保存并重算闹钟”
3. Android 12+ 建议授予“精确闹钟权限”。

## 说明
- 仓库已包含 `gradlew` wrapper，可直接使用 `./gradlew assembleDebug` 构建。
