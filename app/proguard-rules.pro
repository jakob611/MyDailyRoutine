# Room, Glance, DataStore and Compose ship their own consumer keep rules.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Enum constant names are a storage format in this app, not an implementation detail.
#
# Room's type converters write `value.name` into the column and read it back with `valueOf`, and
# the database's own CHECK triggers spell the same constants out in SQL:
#
#   NEW.category NOT IN ('SCHOOL','FOCUS_ANALYTICAL','FOCUS_SYNTHESIZING','ADMIN', ...)
#   NEW.cancellationReason NOT IN ('MANUAL','AUTO_HEAL','BACKLOG','BUFFER_CONSUMED')
#   NEW.kind NOT IN ('BLOCK_PREVIEW','RECOVERY_START')
#
# So the name is a contract with rows already on the reader's phone. If R8 renames a constant — or
# unboxes an enum to an int, which it is allowed to do for one it believes is only ever compared —
# the converter writes a string the trigger does not recognise and *every insert is rejected*. The
# app would install, start, and then quietly refuse to save anything.
#
# The default Android rules keep `values()` and `valueOf()`; they do not keep the constants those
# two methods look up. This does. It costs a few bytes of the mapping and removes the whole class
# of failure.
-keepclassmembers enum com.example.mydailyroutine.** {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
