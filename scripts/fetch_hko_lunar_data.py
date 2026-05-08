#!/usr/bin/env python3
import csv
import datetime as dt
import re
import sys
import time
import urllib.request
from pathlib import Path

START_YEAR = 1901
END_YEAR = 2100
BASE_URL = "https://www.hko.gov.hk/tc/gts/time/calendar/text/files/T{year}c.txt"
OUTPUT = Path("app/src/main/assets/lunar/hko_lunar_1901_2100.csv")
CACHE_DIR = Path("build/hko-lunar-cache")

MONTHS = ["正月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "十一月", "十二月"]
STEMS = "甲乙丙丁戊己庚辛壬癸"
BRANCHES = "子丑寅卯辰巳午未申酉戌亥"
ZODIACS = ["鼠", "牛", "虎", "兔", "龙", "蛇", "马", "羊", "猴", "鸡", "狗", "猪"]
DAY_NAMES = {
    "初一": 1, "初二": 2, "初三": 3, "初四": 4, "初五": 5, "初六": 6, "初七": 7, "初八": 8, "初九": 9, "初十": 10,
    "十一": 11, "十二": 12, "十三": 13, "十四": 14, "十五": 15, "十六": 16, "十七": 17, "十八": 18, "十九": 19, "二十": 20,
    "廿一": 21, "廿二": 22, "廿三": 23, "廿四": 24, "廿五": 25, "廿六": 26, "廿七": 27, "廿八": 28, "廿九": 29, "三十": 30,
}
TRAD_TO_SIMP = str.maketrans({
    "馬": "马",
    "龍": "龙",
    "雞": "鸡",
    "豬": "猪",
    "閏": "闰",
    "驚": "惊",
    "蟄": "蛰",
    "穀": "谷",
    "滿": "满",
    "處": "处",
    "種": "种",
})

ROW_RE = re.compile(
    r"(?P<date>\d{4}年\d{1,2}月\d{1,2}日)\s+"
    r"(?P<lunar>\S+)\s+"
    r"星期[一二三四五六日]"
    r"(?:\s+(?P<term>[^\d\s]+))?"
)


def simplify(value: str) -> str:
    return value.translate(TRAD_TO_SIMP)


def ganzhi_for_lunar_year(year: int) -> str:
    return STEMS[(year - 4) % 10] + BRANCHES[(year - 4) % 12]


def zodiac_for_lunar_year(year: int) -> str:
    return ZODIACS[(year - 4) % 12]


def month_number(month_name: str) -> int:
    normalized = month_name[1:] if month_name.startswith("闰") else month_name
    if normalized not in MONTHS:
        raise ValueError(f"Unknown lunar month: {month_name}")
    return MONTHS.index(normalized) + 1


def previous_month_name(month_name: str) -> str:
    normalized = month_name[1:] if month_name.startswith("闰") else month_name
    index = MONTHS.index(normalized)
    return MONTHS[index - 1]


def fetch_year(year: int) -> str:
    cache_file = CACHE_DIR / f"T{year}c.txt"
    if cache_file.exists():
        return cache_file.read_text(encoding="utf-8-sig")

    request = urllib.request.Request(
        BASE_URL.format(year=year),
        headers={"User-Agent": "LunarCal data importer (+https://www.hko.gov.hk/)"},
    )
    last_error = None
    for attempt in range(1, 6):
        try:
            with urllib.request.urlopen(request, timeout=30) as response:
                text = response.read().decode("utf-8-sig")
            CACHE_DIR.mkdir(parents=True, exist_ok=True)
            cache_file.write_text(text, encoding="utf-8")
            return text
        except Exception as error:
            last_error = error
            print(f"  retry {attempt}/5 for {year}: {error}", file=sys.stderr, flush=True)
            time.sleep(min(attempt * 2, 10))
    raise last_error


def parse_year(year: int, text: str):
    rows = []
    first_month = None
    current_month = None
    cny_date = None

    for match in ROW_RE.finditer(text):
        date = dt.datetime.strptime(match.group("date"), "%Y年%m月%d日").date()
        token = simplify(match.group("lunar"))
        term = simplify(match.group("term") or "")
        if token.endswith("月"):
            current_month = token
            first_month = first_month or token
            day_name = "初一"
            if token == "正月":
                cny_date = date
        else:
            day_name = token
        rows.append({
            "date": date,
            "raw_month": current_month,
            "lunar_token": token,
            "day_name": day_name,
            "solar_term": term,
        })

    if len(rows) not in (365, 366):
        raise ValueError(f"{year}: expected 365/366 rows, got {len(rows)}")
    if first_month is None or cny_date is None:
        raise ValueError(f"{year}: missing month-start or Chinese New Year row")

    inferred_leading_month = previous_month_name(first_month)
    for row in rows:
        if row["raw_month"] is None:
            row["raw_month"] = inferred_leading_month
        lunar_year = year if row["date"] >= cny_date else year - 1
        month_name = row["raw_month"]
        is_leap = month_name.startswith("闰")
        row.update({
            "lunar_year": lunar_year,
            "ganzhi": ganzhi_for_lunar_year(lunar_year),
            "zodiac": zodiac_for_lunar_year(lunar_year),
            "month_name": month_name,
            "month_number": month_number(month_name),
            "is_leap": "1" if is_leap else "0",
            "day_number": DAY_NAMES[row["day_name"]],
        })
    return rows


def main() -> int:
    all_rows = []
    for year in range(START_YEAR, END_YEAR + 1):
        print(f"Fetching HKO lunar table {year}", file=sys.stderr, flush=True)
        all_rows.extend(parse_year(year, fetch_year(year)))

    expected = sum(366 if (year % 4 == 0 and (year % 100 != 0 or year % 400 == 0)) else 365 for year in range(START_YEAR, END_YEAR + 1))
    if len(all_rows) != expected:
        raise ValueError(f"Expected {expected} rows, got {len(all_rows)}")

    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    with OUTPUT.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.writer(handle)
        writer.writerow(["date", "lunar_year", "ganzhi", "zodiac", "month_number", "month_name", "is_leap", "day_number", "day_name", "solar_term"])
        for row in all_rows:
            writer.writerow([
                row["date"].isoformat(),
                row["lunar_year"],
                row["ganzhi"],
                row["zodiac"],
                row["month_number"],
                row["month_name"],
                row["is_leap"],
                row["day_number"],
                row["day_name"],
                row["solar_term"],
            ])

    print(f"Wrote {len(all_rows)} rows to {OUTPUT}", file=sys.stderr, flush=True)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
