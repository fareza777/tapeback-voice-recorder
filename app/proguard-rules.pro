# TapeBack R8 rules.
# Room, WorkManager, Media3 and Compose ship their own consumer rules.

# Keep enum names that are persisted in Room / DataStore by name().
-keepclassmembers enum com.f7developer.tapeback.domain.model.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep line numbers for readable crash traces (no crash reporter is bundled).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
