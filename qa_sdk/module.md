#Module SDK

The QuantActions SDK for Android enables a developer to add all the QuantActions functionalities to an android app. This includes:

- Automatic data collection
- Access processed metrics and insights from the Tap Intelligence Engine (TIE)
- Enabling device communication
- Subscribe to different cohorts

The QuantActions SDK for Android can be set up with a few easy steps. We recommend the use of `gradle` as build tool and of Android Studio as IDE for the development.

## 1. Android Studio setup

The SDK is distributed via github maven artifacts, and can be accessed with 2 steps

1a. In the project-level `settings.gradle` add the GitHub maven repository, and the credentials to access it,
the repo and the package are public but you still need a github account to access them, it is best to use a personal access token for this.

```groovy
dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
     ...
     maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/QuantActions/quantactions-android-sdk")
         credentials {
             username = "..."
             password = "..."
         }
     }
  }
}
```

1b. In your app-level `build.gradle`: 
  - ensure that you have selected a minimum Android SDK of **21** 
    - use JAVA 17 compiler
    - enable desugaring

```groovy
android {
  defaultConfig {
   minSdkVersion 21
  }
  
  compileOptions {
     coreLibraryDesugaringEnabled true
     sourceCompatibility JavaVersion.VERSION_17
     targetCompatibility JavaVersion.VERSION_17
  }
  kotlinOptions {
     jvmTarget = JavaVersion.VERSION_17.toString()
  }
}
  
dependencies {
  coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:2.0.4'
}
```
       
1c. Add the QuantActions SDK dependency to your app-level `build.gradle` file
      
```groovy
implementation 'com.quantactions:quantactions-android-sdk:1.1.2'
```
      
and re-sync the project. Remember to check the latest SDK version in case you are reading an old version of the documentation.
      
1d. Done! You are now ready to start adding QA functionality to your code.

----

## 2. Adding QA functionality to an app

The whole QA functionality can be accessed everywhere in the code by the singleton `QA` that can be instantiate like this.

```kotlin
import com.quantactions.sdk.QA
val qa = QA.getInstance(context)
```

Before using any functionality, the QA singleton needs to be initialized. To do so you will need an `api_key` provided by QuantActions. If you have not yet received your `api_key` please [contact us](mailto:development@quantactions.com).

Once in possession of the `api_key`, the simplest thing to do is to add your `api_key` to the app-level `build.gradle`

 ```groovy
android {
   compileSdkVersion 34
   
   defaultConfig {
      minSdkVersion 21
      buildConfigField("String", "QA_API_KEY", "\"api_key\"")
   }
}
 ```

then you can access it in the code to initialize the SDK.

```kotlin
qa.init(context,
        apiKey=BuildConfig.QA_API_KEY,
        basicInfo=BasicInfo(
                yearOfBirth=1985,
                gender=QA.Gender.UNKNOWN,
                selfDeclaredHealthy=true
                )
        )
```



------------------------

## 3. Request the permissions

For the SDK to work properly, the app that uses it needs to request 3 permissions:
1. OVERLAY PERMISSION: `Settings.ACTION_MANAGE_OVERLAY_PERMISSION`
2. USAGE ACCESS: `Settings.ACTION_USAGE_ACCESS_SETTINGS`
3. ACTIVITY RECOGNITION: `Manifest.permission.ACTIVITY_RECOGNITION`

To prompt the user to enable 1. and 2. you can use the methods offered by the QA singleton

```kotlin
qa.requestOverlayPermission(context) // opens overlay settings page
qa.requestUsagePermission(context) // opens usage settings page
```

Since these permissions can be revoked at any time by the user, it is important to routinely check 
that they are granted, to do so the easiest way is to have background scheduled task that check that the permissions are granted
using `qa.canUsage(context)` and `qa.canDraw(context)` and fire a notification in case the permissions have been lost.

Permission 3. is needed to run from Android 14 on (SDK 34). Differently from the other two, this is 
a runtime permission that you can request in the usual way e.g.

```kotlin
if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
    ActivityCompat.requestPermissions(context, arrayOf(Manifest.permission.ACTIVITY_RECOGNITION), 0)
}
```

Note that without this permission the SDK will not be able to collect data (i.e. the background worker will not be able to run).
We suggest to ask this permission during the app onboarding process and to initialize the QuantActions SDK only after all the permissions have been granted.


-----

### 3a. Android 14+ 

#### 3a.1. Data Collection Notification

Because QA SDK is always active in the background, Android API O and above require a notification to always be present to inform the user, by default the notification uses [this](https://fonts.google.com/icons?selected=Material%20Icons%20Outlined%3Aequalizer%3A) icon. To override it and use your own icon you can simply create a drawable named `ic_equalizer_black_24dp` and place it in your `res/drawable` folder and this will override the drawable of the SDK.

From Android 14 (SDK 34), the OS is more strict with background/foreground service, hence the foreground data collection needs to
be properly shown to the user with a explanatory notification. The SDK provides a [default notification](com.quantactions.sdk.UpdateTaps) that shows to the user the number of taps in the last 24 hours and the current tapping speed.
You can override this notification by implementing the interface [`DataCollectionNotification`](com.quantactions.sdk.DataCollectionNotification) and overriding the `createNotification` and `updateNotification` methods.
Finally you need to inform the SDK of the new notification in the following way:

```kotlin 
qa.updater = MyCustomNotification(qa)
```
Note that the SDK notification uses a separate notification channel called `QA Service` and can be easily unselected by the user from the OS notifications settings to avoid having it always present.

#### 3a.2. Missing Action Recognition Permission

From Android 14 (SDK 34) the `Manifest.permission.ACTIVITY_RECOGNITION` is needed to run the SDK. If the permission is not granted or lost for any reason, the SDK will not be able to collect data. 
While the data collection is set to restart itself if interrupted, in cases when this permission is missing the data collection cannot restart.
In these cases an automatic notification will be shown to the user to inform them that the data collection is not running and that the permission is needed to restart it.

You can customize the text of this notification by overwriting the following two strings in your `strings.xml` file:

```xml
<string name="qa_sdk_notification_title_app_needs_a_permission">Action Required: we need a permission</string>
<string name="qa_sdk_notification_body_tap_to_open_and_grant_permission">Tap to open and grant permission for seamless data collection</string>
```

### Android 15+
If your app targets Android 15 (SDK 35) or above, the automatic re-launch of data collection could be affected by Android SDK 35 that prevents background/foreground processes to be spawn when the phone is locked.
Similarly to the Android 14 behaviour, if the restarts of the data collection fails for reasons related to the OS, the SDK will show a notification to the user to inform them that the data collection is not running and that the app needs to be open for the collection to be restarted.

You can customize the text of this notification by overwriting the following two strings in your `strings.xml` file:

```xml
<string name="qa_sdk_notification_title_action_required_restart_needed">App Restart Required</string>
<string name="qa_sdk_notification_body_tap_to_open_and_restart">Tap to restart the app for seamless data collection.</string>
```

## 4. Sign-up to a cohort
To track a device it is **necessary** to subscribe the device to a cohort. Each device can be subscribed in two ways. For a cohort with a known number of devices to track, QuantActions provides a set of codes (subscriptionIds) that have to be distributed. The way the code is entered into the app is the choice of the developer. In our TapCounter R&D app we use both a copy&paste method and a QR code scanning method, once the code as been acquired the device can then be registered using the SDK

```kotlin
qa.subscribe(subscriptionId=subscriptionId)
```

where `subscriptionId` is the string corresponding to the subscription ID.

For cohorts where the number of participants is unknown the SDK can be used to register the device by simply using the `cohortId` provided by QA (this way needs special access so make sure you are authorized by QuantActions to use this functionality).

```kotlin
qa.subscribe(cohortId=cohortId)
```
 
When subscribing the device using a general `cohortId`, the device gets automatically assigned a `subscriptionId`, to retrieve this id one case use the following code.

```kotlin
val subscriptions = qa.subscriptions()
```

Note that each user can be subscribed to multiple cohorts at the same time

---

## 5. Multiple devices 

It is possible to link multiple devices to the same user by providing `identityId` and `password` during the initialization of the SDK. This way the SDK will be able to link the data from the different devices to the same user. 

In the first device, retrieve the `identityId` and `password`:
```kotlin
val identityId = qa.identityId
val password = qa.password
```

Then in the second device, initialize the SDK with the `identityId` and `password`:
```kotlin
qa.init(context,
   apiKey=BuildConfig.QA_API_KEY,
   identity=identityId,
    password=password
)
```

IMPORTANT NOTE: Since QuantActions does not have any connection with the user identity, data recovery in case of loss or damage of the device is not possible. 
For this reason we suggest storing the QuantActions' `identityId` and `password` together with the user's account info. 
In case of loss of damage of the device, the QuantActions SDK can be initialized on the new device with the user's
`identityId` and `password` which will restore the data and guarantee no interruptions of metrics between old and new device.

------------------

## 6. Firebase

For the best experience with the SDK we strongly recommend to add to your app [Firebase Messaging, Crashlytics and Analytics](https://firebase.google.com/), this will allow QA service to also communicate with the app to check its status and more.

------------------


## 7. TL:DR
Minimal example

- Get a `QA_API_KEY` from [QuantActions](mailto:development@quantactions.com)
- Get a `QA_COHORT_ID` from [QuantActions](mailto:development@quantactions.com)
- Get a `authToken` from [Jitpack](https://Jitpack.io)


`settings.gradle` (project level)

```groovy
dependencyResolutionManagement {
   repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

   repositories {
      ...
      maven {
         name = "GitHubPackages"
         url = uri("https://maven.pkg.github.com/QuantActions/quantactions-android-sdk")
      }
   }
}

```

`build.gradle` (app level)

```groovy
android {
   defaultConfig {
      minSdkVersion 21
      buildConfigField("String", "QA_API_KEY", "\"api_key\"")
      buildConfigField("String", "QA_COHORT_ID", "\"cohort_id\"")
   }

   compileOptions {
      coreLibraryDesugaringEnabled true
      sourceCompatibility JavaVersion.VERSION_17
      targetCompatibility JavaVersion.VERSION_17
   }
   kotlinOptions {
      jvmTarget = JavaVersion.VERSION_17.toString()
   }
}

dependencies {
   coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:2.1.2'
   implementation 'com.quantactions:quantactions-android-sdk:1.1.2'
}

```

`MainActivity.kt`

```kotlin
package com.example.sdktestapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.quantactions.sdk.QA
import com.example.sdktestapp.BuildConfig.QA_AUTH_KEY
import com.example.sdktestapp.BuildConfig.QA_COHORT_ID

class MainActivity : AppCompatActivity() {

    private lateinit var mainViewModel: MainViewModel
    private val calendar: Calendar = Calendar.getInstance(Locale.ENGLISH)
    private lateinit var mainTextView: TextView
    
    @ExperimentalCoroutinesApi
    override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val qa = QA.getInstance(MainActivity.this)
        qa.init(MainActivity.this, QA_API_KEY, 1985, QA.Gender.MALE, true) // initialize SDK
        qa.requestOverlayPermission(MainActivity.this) // opens overlay settings page
        qa.requestUsagePermission(MainActivity.this) // opens usage settings page
        ActivityCompat.requestPermissions(context, arrayOf(Manifest.permission.ACTIVITY_RECOGNITION), 0) // requests runtime permission
        qa.subscribe(cohortId=QA_COHORT_ID)  // register the device with the backend
        qa.syncData() // optional: sync the data
    }
}
```

---

## 9. Checking that everything is running fine 
After the integration of the SDK has been done, you can add some checks to make sure everything is running fine.
1. You can check that the SDK has been initialized correctly by using `qa.isInit(context)` (returns a bool)
2. You can check that the data collection is running fine by using `qa.isDataCollectionRunning(context)` (returns a bool)
3. You can check that the device has been registered with the QA backend and/or the registration to a cohort was successful
```kotlin
 viewModelScope.launch {
     withContext(Dispatchers.Default) {
         qa.subscriptions()
     }
 }
```

## 10. Pausing data collection

Although we do not recommend it, the data collection can be paused/resumed via the methods offered by QA singleton
[qa.pauseDataCollection(context)](com.quantactions.sdk.QA.pauseDataCollection) and [qa.resumeDataCollection(context)](com.quantactions.sdk.QA.resumeDataCollection)

## 11. Metrics and Trends retrieval
While the data collection and synchronization is automated within the SDK. Retrieval of metrics and insights 
has to be done manually within the app in order to access only the subset of metrics and trends that the application needs.

The metrics and trends can be retrieve programmatically in the following way:

```kotlin
qa.getMetric(Metric.SLEEP_SCORE)
qa.getMetric(Trend.SLEEP_SCORE)
qa.getMetric(Metric.COGNITIVE_FITNESS)
qa.getMetric(Trend.COGNITIVE_FITNESS)
```

Check [Metric](com.quantactions.sdk.Metric) and [Trend](com.quantactions.sdk.Trends) for the list of 
metrics and trends available in the current version of the SDK

The function returns an asynchronous [Kotlin Flow](https://kotlinlang.org/docs/flow.html) containing a 
[TimeSeries](com.quantactions.sdk.TimeSeries) object. 
The test app accompanying the SDK has some examples on how to handle the return data.

Since the metrics for a user take from 2 to 7 days to be available (see [ETA](com.quantactions.sdk.Metric.eta)).
Developer can have access to sample metrics (that update every day) from a sampled device with [getMetricSample](com.quantactions.sdk.QA.getMetricSample)

## 12. Metrics and Trends description

### 12.1. Metrics

The current version is shipped with the following metrics:

- [SLEEP_SCORE](com.quantactions.sdk.Metric.SLEEP_SCORE)
- [COGNITIVE_FITNESS](com.quantactions.sdk.Metric.COGNITIVE_FITNESS)
- [SOCIAL_ENGAGEMENT](com.quantactions.sdk.Metric.SOCIAL_ENGAGEMENT)


Each of these metric has a value in the range 0-100 and is shipped with confidence intervals (high and low)
and a general confidence about the metric. 

- [SLEEP_SUMMARY](com.quantactions.sdk.Metric.SLEEP_SUMMARY)

This metric reports more information about the sleep of the user including bed time, wake up time 
and number and length of interruptions. See also [SleepSummary](com.quantactions.sdk.data.model.SleepSummary) 
for more information on how this metric is organized. 

- [SCREEN_TIME_AGGREGATE](com.quantactions.sdk.Metric.SCREEN_TIME_AGGREGATE)
- [ACTION_SPEED](com.quantactions.sdk.Metric.ACTION_SPEED)
- [TYPING_SPEED](com.quantactions.sdk.Metric.TYPING_SPEED)
- [SOCIAL_TAPS](com.quantactions.sdk.Metric.SOCIAL_TAPS)
- [BEHAVIOURAL_AGE](com.quantactions.sdk.Metric.BEHAVIOURAL_AGE)

### 12.2. Trends

The trends objects are TimeSeries that hold the daily values of the trend. For each day you get a 
short (2 weeks), medium (6 weeks) and long (1 year) trend. 

- [COGNITIVE_FITNESS](com.quantactions.sdk.Trend.COGNITIVE_FITNESS)
- [ACTION_SPEED](com.quantactions.sdk.Trend.ACTION_SPEED)
- [TYPING_SPEED](com.quantactions.sdk.Trend.TYPING_SPEED)
- [SLEEP_SCORE](com.quantactions.sdk.Trend.SLEEP_SCORE)
- [SLEEP_LENGTH](com.quantactions.sdk.Trend.SLEEP_LENGTH)
- [SLEEP_INTERRUPTIONS](com.quantactions.sdk.Trend.SLEEP_INTERRUPTIONS)
- [SOCIAL_ENGAGEMENT](com.quantactions.sdk.Trend.SOCIAL_ENGAGEMENT)
- [SOCIAL_SCREEN_TIME](com.quantactions.sdk.Trend.SOCIAL_SCREEN_TIME)
- [SOCIAL_TAPS](com.quantactions.sdk.Trend.SOCIAL_TAPS)
- [THE_WAVE](com.quantactions.sdk.Trend.THE_WAVE)


While the metrics have daily resolutions, the are updated every two hours.

In some cases the metrics for a day might be missing, in that case it means that there was not enough 
data collect for that day to give an estimate or that the confidence was too low to give an appropriate estimate.
When some days are missing you can fill in the gaps with NaNs using [fillMissingDays](com.quantactions.sdk.TimeSeries.fillMissingDays)

Checkout the metrics individual pages for more information about each metric.

## 13. Journaling 

The SDK allows also to use a journaling function to log a series of entries. Each entry is composed of:
- A date
- A small text describing the entry
- A list of events (from a predefined pool)
- A rating (1 to 5) for each event in the entry

The predefined [events](com.quantactions.sdk.data.entity.JournalEventEntity) can be retrieved with [journalEventKinds](com.quantactions.sdk.QA.journalEventKinds).
With that adding (or editing) an entry is done by using [saveJournalEntry](com.quantactions.sdk.QA.saveJournalEntry).

Entries can also be [deleted](com.quantactions.sdk.QA.deleteJournalEntry).

The full journal (all entries) can be retrieved with [journalEntries](com.quantactions.sdk.QA.journalEntries)

As for the metrics an example journal can be retrieved for debug purposes with [getSampleJournal](com.quantactions.sdk.QA.getSampleJournal). 

## 14. Questionnaires 

Both classic and custom questionnaires can be sub-ministered to users via the SDK. 
Get in touch [with us](mailto:development@quantactions.com) for more information about this feature.

## 15. Issue tracking and contact
Feel free to use the issue tracking of this repo to report any problem with the SDK, in case of more immediate assistance need feel free to contact us at [development@quantactions.com](mailto:development@quantactions.com)

#Package com.quantactions.sdk
The main functionality of the Quantactions Android SDK.

#Package com.quantactions.sdk.data.entity
Entities used across the SDK for particular functionalities.

#Package com.quantactions.sdk.exceptions
Exceptions that the SDK might be throwing.



