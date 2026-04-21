# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Apache POI (Word export)
-keep class org.apache.poi.** { *; }
-keep class org.openxmlformats.** { *; }

# NHTSA VIN Decoder - keep data classes for JSON parsing
-keep class com.collisioncalc.app.data.lookups.** { *; }

# App data models
-keep class com.collisioncalc.app.data.** { *; }