####################################################################################################
####################################################################################################
####################################################################################################
######################################### PROGUARD #################################################
####################################################################################################
####################################################################################################
####################################################################################################

# This is a configuration file for ProGuard.
# http://proguard.sourceforge.net/index.html#manual/usage.html
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# Optimization is turned off by default. Dex does not like code run
# through the ProGuard optimize and preverify steps (and performs some
# of these optimizations on its own).
#-dontoptimize
#-dontpreverify

# If you want to enable optimization, you should include the
# following:
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
#
# Note that you cannot just include these flags in your own
# configuration file; if you are including this file, optimization
# will be turned off. You'll need to either edit this file, or
# duplicate the contents of this file and remove the include of this
# file from your project's proguard.config path property.

-keep class com.quantactions.sdk.data.model.SleepSummary.** { *; }
-keep class com.quantactions.sdk.data.model.SleepSummary
-keep class com.quantactions.sdk.TimeSeries.** { *; }
-keep class com.quantactions.sdk.TimeSeries { *; }
-keep class com.quantactions.sdk.ManageTimeSeries { *; }
-keep class com.quantactions.sdk.ManageTimeSeries.** { *; }

-keep class com.quantactions.sdk.Metric {
    public <methods>;
}

-keep class com.quantactions.sdk.data.model.JournalEntryWithEvents {
    public <methods>;
}

-keep class com.quantactions.sdk.data.model.ResolvedJournalEvent {
    public <methods>;
}

-keep class com.quantactions.sdk.SubscriptionIdResponse {
    public <methods>;
}

-keep class com.quantactions.sdk.PopulationRange {
    public <methods>;
}

-keep class com.quantactions.sdk.QA {
    public <methods>;
    public static <fields>;
}

-keep class com.quantactions.sdk.QAFirebaseMessagingService {
    public <methods>;
}

-keep class com.quantactions.sdk.exceptions.SDKNotInitialisedException {
    *;
}

-keep class com.quantactions.sdk.QATaps {
    public <methods>;
}

-keep class com.quantactions.sdk.QA$Strings { *; }
-keep class com.quantactions.sdk.QA$Permissions { *; }
-keep class com.quantactions.sdk.QA$Gender { *; }
-keep class com.quantactions.sdk.QA$Flag { *; }

-keep class com.quantactions.sdk.data.api.adapters.SubscriptionWithQuestionnairesJsonAdapter { *; }
-keep class com.quantactions.sdk.SubscriptionIdResponse { *; }
-keep class com.quantactions.sdk.data.entity.** { *; }

-keepattributes Signature, EnclosingMethod, InnerClasses

-keep class com.quantactions.sdk.QAResponse { *; }

-keep,includedescriptorclasses class net.sqlcipher.** { *; }
-keep,includedescriptorclasses interface net.sqlcipher.** { *; }
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }

 ############ Crashlytics ################################
 -keep class javax.inject.** { *; }
-keepattributes *Annotation*
-keep class com.crashlytics.** { *; }
-dontwarn com.crashlytics.**
-keepattributes SourceFile,LineNumberTable

-keep class com.google.android.gms.measurement.AppMeasurement { *; }
-keep class com.google.android.gms.measurement.AppMeasurement$OnEventListener { *; }

-dontwarn java.lang.invoke.StringConcatFactory

-keep class * extends com.google.protobuf.GeneratedMessageLite { *; }
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Retrofit does reflection on generic parameters. InnerClasses is required to use Signature and
# EnclosingMethod is required to use InnerClasses.
-keepattributes Signature, InnerClasses, EnclosingMethod

# Retrofit does reflection on method and parameter annotations.
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# Keep annotation default values (e.g., retrofit2.http.Field.encoded).
-keepattributes AnnotationDefault

# Retain service method parameters when optimizing.
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Ignore annotation used for build tooling.
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# Ignore JSR 305 annotations for embedding nullability information.
-dontwarn javax.annotation.**

# Guarded by a NoClassDefFoundError try/catch and only used when on the classpath.
-dontwarn kotlin.Unit

# Top-level functions that can only be used by Kotlin.
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# With R8 full mode, it sees no subtypes of Retrofit interfaces since they are created with a Proxy
# and replaces all potential values with null. Explicitly keeping the interfaces prevents this.
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>

-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>

# Keep inherited services.
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface * extends <1>

# With R8 full mode generic signatures are stripped for classes that are not
# kept. Suspend functions are wrapped in continuations where the type argument
# is used.
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# R8 full mode strips generic signatures from return types if not kept.
-if interface * { @retrofit2.http.* public *** *(...); }
-keep,allowoptimization,allowshrinking,allowobfuscation class <3>

# With R8 full mode generic signatures are stripped for classes that are not kept.
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# Gson uses generic type information stored in a class file when working with fields. Proguard
# removes such information by default, so configure it to keep all of it.
-keepattributes Signature

# For using GSON @Expose annotation
-keepattributes *Annotation*

# Gson specific classes
-dontwarn sun.misc.**
#-keep class com.google.gson.stream.** { *; }

# Application classes that will be serialized/deserialized over Gson
-keep class com.google.gson.examples.android.model.** { <fields>; }

# Prevent proguard from stripping interface information from TypeAdapter, TypeAdapterFactory,
# JsonSerializer, JsonDeserializer instances (so they can be used in @JsonAdapter)
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Prevent R8 from leaving Data object members always null
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# Retain generic signatures of TypeToken and its subclasses with R8 version 3.0 and higher.
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

-keep class com.hadiyarajesh.flower_core.ApiResponse
-keep @kotlinx.serialization.Serializable class * {*;}
# Keep `Companion` object fields of serializable classes.
# This avoids serializer lookup through `getDeclaredClasses` as done for named companion objects.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
   static <1>$Companion Companion;
}

# Keep `serializer()` on companion objects (both default and named) of serializable classes.
-if @kotlinx.serialization.Serializable class ** {
   static **$* *;
}
-keepclassmembers class <2>$<3> {
   kotlinx.serialization.KSerializer serializer(...);
}

# Keep `INSTANCE.serializer()` of serializable objects.
-if @kotlinx.serialization.Serializable class ** {
   public static ** INSTANCE;
}
-keepclassmembers class <1> {
   public static <1> INSTANCE;
   kotlinx.serialization.KSerializer serializer(...);
}

# @Serializable and @Polymorphic are used at runtime for polymorphic serialization.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

-keepattributes Annotation, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-dontnote kotlinx.serialization.SerializationKt

# Keep Serializers

-keep,includedescriptorclasses class com.company.package.**$$serializer { *; }
-keepclassmembers class com.company.package.** {
    *** Companion;
}
-keepclasseswithmembers class com.company.package.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# When kotlinx.serialization.json.JsonObjectSerializer occurs

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}