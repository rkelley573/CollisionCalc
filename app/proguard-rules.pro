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
-keep class com.microsoft.schemas.** { *; }
-keep class org.apache.xmlbeans.** { *; }

# Apache POI optional or build-time dependencies that are not available or needed on Android
-dontwarn java.awt.**
-dontwarn javax.imageio.**
-dontwarn javax.swing.**
-dontwarn javax.xml.crypto.**
-dontwarn javax.xml.stream.**
-dontwarn org.apache.batik.**
-dontwarn org.apache.pdfbox.**
-dontwarn org.bouncycastle.**
-dontwarn net.sf.saxon.**
-dontwarn de.rototor.pdfbox.**
-dontwarn aQute.bnd.annotation.spi.**
-dontwarn org.osgi.framework.**
-dontwarn org.w3c.dom.events.**
-dontwarn org.w3c.dom.svg.**
-dontwarn org.w3c.dom.traversal.**
-dontwarn org.apache.xml.security.**
-dontwarn org.apache.jcp.xml.dsig.internal.dom.**
-dontwarn org.apache.logging.log4j.**
-dontwarn com.github.javaparser.**
-dontwarn com.sun.org.apache.xml.internal.resolver.**
-dontwarn org.apache.maven.**
-dontwarn org.apache.tools.ant.**
-dontwarn org.ietf.jgss.**

# Don't warn about missing schema classes in Apache POI ooxml-lite
-dontwarn org.openxmlformats.schemas.**
-dontwarn com.microsoft.schemas.**

# NHTSA VIN Decoder - keep data classes for JSON parsing
-keep class com.collisioncalc.app.data.lookups.** { *; }

# App data models
-keep class com.collisioncalc.app.data.** { *; }