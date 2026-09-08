# Alarms, quiet hours, and widgets

## Reminder chain

`ScheduleCoordinator` obtains a transactionally consistent 370-day Room snapshot. `AlarmPlanner` resolves weekly templates plus date exceptions, skips completed/disabled/holiday-inactive occurrences, and finds the next reminder batch. It also looks into the next date when a midnight block's five-minute preview falls on the previous date.

Only two stable broadcast PendingIntents are needed:

1. **Next reminder batch** — `RTC_WAKEUP` via `setExactAndAllowWhileIdle()` when permitted.
2. **Next widget / maintenance boundary** — inexact `RTC`, which does **not** wake a sleeping device.

The notification batch is rearmed before rendering its notifications. Simultaneous reminders are not overwritten. This constant-size chain avoids hundreds of alarms and Android's per-app alarm-count limit. A routine that starts beyond the search window is rechecked through non-wakeup daily maintenance. There is no periodic exact alarm or long-lived scheduling service.

- Ordinary blocks: one preview five minutes before start.
- `REST_BREAK`: one reminder at start, **instead of** another preview five minutes earlier.
- Milestones: calendar markers, not duration-bearing reminder blocks.
- Creating a block after its preview time does not send a retroactive notification.

## Receipt-time validation and deduplication

A broadcast contains only its expected trigger instant. `AlarmReceiver` never trusts cached title/time data. The coordinator re-reads Room and claims eligible deliveries in **one transaction**. A skipped, completed, deleted, disabled, holiday-inactive, or time-rescheduled occurrence therefore cannot produce a stale reminder from an old PendingIntent.

Claims use `(routineBlockId, occurrenceDate, kind)` and survive process death. Notifications use the full delivery key as a tag, not an integer hash. Claims older than 30 days are pruned. Delivery is **at most once within that retention window**: Android notification posting cannot participate in a SQLite transaction, so process death between a successful claim and `notify()` can lose a notification; it cannot guarantee exactly-once delivery.

Notifications more than ten minutes late, or for blocks already ended, are discarded. Late but still relevant notifications use their actual scheduled start in the text. This avoids dumping obsolete reminders after a long sleep or clock change.

## Permissions and platform limits

- `POST_NOTIFICATIONS` is requested from an explicit Settings action on API 33+.
- `SCHEDULE_EXACT_ALARM` is requested through the system special-access screen on API 31+. The app does not claim restricted `USE_EXACT_ALARM` access.
- Permission checks happen before arming/posting, and revocation races are caught. Without precise access, `setAndAllowWhileIdle()` provides an explicitly labeled **approximate** fallback. With notifications disabled, reminder wakeups are cancelled.
- Boot, app replacement, manual time changes, time-zone changes, precise-alarm access grants, and app resume reconstruct scheduling from Room.
- Credential-encrypted storage is not read at locked boot. No `LOCKED_BOOT_COMPLETED` receiver is registered.
- Each custom receiver uses `goAsync()` with an eight-second bound and always finishes the pending result. A non-wakeup retry is armed before work starts to recover from transient failures.
- Android Doze quotas, OEM power management, channel settings, permission revocation, and force-stop semantics still apply. Exact API usage is **not** a guarantee of delivery to the second. Revoking exact access can kill the process and cancel precise alarms; opening the app reestablishes the fallback. A force-stopped app must be opened again.

## Quiet school window

DataStore persists `muteDuringSchoolHours`, start/end times, and the teaching-end date. The default quiet window is **07:45–14:30** with an inclusive start and exclusive end. It applies daily, including weekends; an end before the start wraps midnight.

Quiet reminders use a separate LOW-importance channel with no sound or vibration and `setSilent(true)`. Normal reminders use the ordinary channel. **System-wide DND is not changed.** Android normally does not show low-priority notifications as heads-up banners, and the app does not promise otherwise. The user remains in control of channel settings.

## Civil times and DST

Blueprints store local wall times and follow the device's current zone, re-read after zone changes. Ambiguous fall-back times use the first offset. A nonexistent spring-forward start shifts the **whole occurrence** forward by the gap. The five-minute preview is then subtracted from the resolved instant. Zero/negative instant windows are not scheduled. Tests use `Europe/Ljubljana` to cover both transitions.

Daily/weekly layout preserves the user's civil-time blueprint. The daily card notes clock-change adjustments; its NOW indicator and the widget use actual instants.

## Glance widget

The widget reads the same Room snapshot/resolver, not a second schedule cache. During Glance's bounded composition session it collects the reactive Room/preference flows, so an edit is not lost when `update()` reuses an existing session. Glance preferences store only a render-revision token; boundary refreshes increment that token to refresh the clock/zone and switch the observed date at midnight without any periodic polling. It displays the current date, countdown, remaining blocks and incomplete milestones, NOW / UP NEXT labels, and a direct activity PendingIntent for Fast Add. The add action resolves today's date **at launch**, so a stale widget cannot add to yesterday accidentally.

`updatePeriodMillis = 0`. Updates are requested after edits, on widget add/delete, on app resume, and at the next start/end/midnight boundary. These are non-wakeup refreshes and may be deferred while idle. “As of HH:mm” makes that tradeoff explicit. RemoteViews content is capped at 60 rows with an “open app for more” link. Glance's own bounded AndroidX worker may perform the render; no extra app-owned polling or wake-lock service is added.
