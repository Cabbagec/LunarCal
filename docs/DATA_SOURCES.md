# Calendar Data Sources

The app uses Hong Kong Observatory Gregorian-Lunar conversion tables as the lunar-calendar authority.

- Source page: `https://www.hko.gov.hk/tc/gts/time/conversion1_text.htm`
- Yearly text files: `https://www.hko.gov.hk/tc/gts/time/calendar/text/files/TYYYYc.txt`
- Coverage: 1901-2100

The importer in `scripts/fetch_hko_lunar_data.py` downloads those small official text files and writes a compact CSV asset to:

`app/src/main/assets/lunar/hko_lunar_1901_2100.csv`

The Hong Kong Observatory notes rare future edge cases where moon phase or solar-term times fall very close to midnight, so a later project pass should decide how to present those HKO-noted ambiguity dates to users instead of silently pretending there is no uncertainty.
