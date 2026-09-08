# Architecture

This is a small native application with **two Gradle modules**, not one module per screen:

- `core`: pure JVM domain values, constraints, recurrence, numerical scheduling, learning and execution contracts.
- `app`: Android persistence/adapters, feature UI, design system and the composition root.

## Android package layout

```text
com.example.mydailyroutine/
  MainActivity.kt                    # stable public Android entrypoint
  RoutineApplication.kt              # startup/locale, no demo import
  app/
    di/AppGraph.kt                   # explicit dependency wiring
    presentation/RoutineApp.kt       # one navigation/sheet host
    presentation/RoutineViewModel.kt # lifecycle/UDF command coordinator
  core/
    database/
      entities/                     # schedule, subjects, goals, learning, backlog
      daos/                         # indexed queries grouped by responsibility
      RoutineDatabase.kt            # one database; versioned migrations
      EntityMappings.kt
    preferences/                    # DataStore codecs and settings
    presentation/                   # immutable UI contracts/date/formatting primitives
    designsystem/                   # palette, typography, icons and haptics
    platform/                       # Slovenian resource/locale adapters
  features/
    timeline/{data,presentation,components}/
    entry/presentation/             # fast input and occurrence editing
    subjects/presentation/
    planning/{data,presentation}/    # goals, backlog, topics and reviews
    execution/data/                 # explicit persisted session/reconciliation
    settings/presentation/
    examples/data/                  # opt-in import only
  scheduling/                       # stable receiver class names / AlarmManager adapter
  widget/                           # stable provider identity / Glance adapter
```

The app coordinator owns navigation and serializes user commands; algorithms are not embedded in
composables. Feature UI depends on shared immutable contracts and repository interfaces, not another
feature's ViewModel. Persisted schedule facts have one canonical resolver. The widget and alarms
reuse it; they do not own a parallel agenda.

Room writes and dependent planning changes are transactional. History/actuals remain occurrence-
specific. Pure deterministic transformations run off the UI thread. An explicit execution session
stores instants, not a running background service. Reconciliation is lifecycle/event-driven.

The public Activity, receivers and widget provider retain their class identities so previously
installed launcher shortcuts, PendingIntents and widgets do not point at removed components.
The database filename and table IDs are preserved through migrations; package reorganization is
not treated as permission to clear user data.

Use further Gradle feature modules only if build isolation/ownership warrants them. The package
boundaries above are checked automatically, without inflating this application's dependency graph.
