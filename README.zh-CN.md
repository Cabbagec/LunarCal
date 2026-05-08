<h1>
  <img src="docs/images/app-icon.png" width="42" height="42" alt="LunarCal 图标" />
  LunarCal / 农历提醒
</h1>

<p align="center">
  <a href="README.md">English</a> | 简体中文
</p>

LunarCal 是一款面向中国农历日期的 Android 日历提醒应用。

应用使用 Kotlin 和 Jetpack Compose 构建，支持英文和简体中文界面、浅色/深色主题，并内置覆盖 1901-2100 年的离线农历数据。LunarCal 本身不会收集或传输应用数据；事件投递和通知依赖 Android 本地 Calendar Provider。

<p align="center">
  <img src="docs/images/month-view.png" width="30%" alt="浅色和深色主题下的月视图" />
  <img src="docs/images/year-view.png" width="30%" alt="浅色和深色主题下的年视图" />
  <img src="docs/images/drawer.png" width="30%" alt="浅色和深色主题下的导航抽屉" />
</p>

## 功能

- 支持可滑动的月视图和年视图，同时显示公历日期和农历信息。
- 可以基于农历日期创建一次性、每年重复或每月重复的提醒。
- 支持为每个事件设置提前提醒，可选择按天/周偏移，并通过时间选择器设置提醒时间。

## 日历数据来源

本 App 使用香港天文台公开的公历-农历转换数据。

- 来源页面：`https://www.hko.gov.hk/tc/gts/time/conversion1_text.htm`
- 按年份提供的文本文件：`https://www.hko.gov.hk/tc/gts/time/calendar/text/files/TYYYYc.txt`
- App 内置生成数据文件：`app/src/main/assets/lunar/hko_lunar_1901_2100.csv`
- 支持日期范围：1901 年 1 月 1 日至 2100 年 12 月 31 日

仓库中包含转换脚本 `scripts/fetch_hko_lunar_data.py`，更多来源说明见 `docs/DATA_SOURCES.md`。

LunarCal 与香港天文台无关联，也未得到其认可或背书。相关数据仅用于离线日历转换和提醒排程。

## 资源文件

启动图标和抽屉背景图的源素材保存在 `assets/source/`。

Android 使用的压缩资源由 `scripts/PrepareAssets.java` 生成到 `app/src/main/res/`。
