# Bundled school calendar

**Scope:** Slovenian secondary schools, Western/central region (Zahod; second winter-break group), **1 September 2026–31 August 2027**. Verified against the Ministry calendar on **6 September 2026**. No runtime downloads are made. [1](https://www.gov.si/teme/solski-koledar-za-srednje-sole/)

## Included ranges

| Stored period | Dates | Interpretation |
|---|---|---|
| Jesenske počitnice | 26–30 October 2026 | Official autumn vacation |
| Novoletni oddih | 25 December 2026–2 January 2027 | Continuous no-school window: public holidays plus the official 28–31 December secondary-school vacation |
| Zimske počitnice · Zahod | 22–26 February 2027 | Western/central group, not the preceding Eastern-region week |
| Pouka prost dan | 26 April 2027 | Additional teaching-free day |
| Prvomajski oddih | 27 April–2 May 2027 | Public holidays plus the official 28–30 April secondary-school vacation |
| Poletne počitnice | 28 June–31 August 2027 | Official summer vacation |

Vacation dates and the distinctions between formal vacation and continuous holiday windows follow the official secondary-school calendar. [1](https://www.gov.si/teme/solski-koledar-za-srednje-sole/)

**Teaching ends:** 24 June 2027 for non-final years; 21 May 2027 for final years. The default countdown uses 24 June, with an editable end date in Settings for final-year, IB, or school-specific schedules. Changing the countdown does not silently cancel weekly templates. School-specific calendar changes and IB submission dates are not invented. [1](https://www.gov.si/teme/solski-koledar-za-srednje-sole/)

The Western group includes Gorenjska, Goriška, Notranjsko-kraška, Obalno-kraška, Osrednjeslovenska, and Zasavska, plus the listed municipalities in southeastern Slovenia (Ribnica, Sodražica, Loški Potok, Kočevje, Osilnica, Kostel). [1](https://www.gov.si/teme/solski-koledar-za-srednje-sole/)

## Holidays and observances

The seed includes the work-free national/religious holidays inside the cycle: Reformation Day, All Saints' Day, Christmas, Independence and Unity Day, both New Year days, Prešeren Day, Easter Sunday/Monday, Pentecost, Resistance Day, both Labour Day dates, Statehood Day, and Assumption. It also includes non-work-free national observances with `isWorkFreeDay = false`, so these do not suppress school. The legal classification follows the government's holiday list. [2](https://www.gov.si/teme/drzavni-prazniki-in-dela-prosti-dnevi/)

Gregorian Easter is calculated locally with Meeus/Jones/Butcher computus. For the bundled cycle it resolves to 28 March 2027 (Easter Monday: 29 March), matching the calendar listings. [3](https://www.os-store.si/sola/solski-koledar/)

## Storage and update policy

`SlovenianAcademicCalendar.entries()` is deterministic. Room inserts the complete seed **synchronously inside its creation transaction**, using bound parameters and the unique `(date, title)` index. Public holidays and vacation labels can coexist on the same date.

- The resolver uses `any(isWorkFreeDay)`, not the presence of a calendar row, to tag school as inactive.
- Holiday suppression belongs to the occurrence's origin date, including an overnight carry-in.
- The seed is deliberately not extrapolated to later years: regional winter-break rotation and teaching-free dates can change.
- The UI identifies the bundled coverage. Outside it, recurrence still works, but there is no claimed verified holiday suppression.
- A later calendar update should be bundled in an app release with an explicit, idempotent database migration. Do not download calendars in the background or silently overwrite user data.
