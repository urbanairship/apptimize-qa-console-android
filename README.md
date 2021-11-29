# Apptimize QA Console for Android

![https://github.com/urbanairship/apptimize-qa-console-android/releases/latest](https://img.shields.io/github/v/release/urbanairship/apptimize-qa-console-android) ![#license](https://img.shields.io/badge/license-Apache%202.0-orange)

For more information see the [QA Console FAQ page](https://faq.apptimize.com/hc/en-us/articles/360021675293-How-do-I-use-the-Apptimize-QA-Console-).

## Introduction

The Apptimize QA console is a framework that can be integrated into your mobile app. It enables you to preview variants in different combinations from all of your active feature flags and experiments on a simulator or device. This approach of QA works well for customers with large teams that would like to test hands-on while using the app. Integrating the QA console is a simple one-time process. Once the console is in place, it works by overriding your allocations and forcing your selected variants internally.

> #### Note
>
> The QA console is only intended to be integrated into debug/developer-build versions of your app and should not be included in releases to your end-users.

## Integration

1. Download the latest release of the `qaconsole.aar` from [here](https://sdk.apptimize.com/apptimize-qa-console/qaconsole.aar).

2. Create or open an Android Studio project.

3. Click **File—> New —> New Module**. In the window that appears, select **Import .JAR/.AAR Package** and click **Next**.

4. Expand **Gradle Scripts** and open `build.gradle (for Module:app)`. Add `implementation project (":qaconsole")` dependencies.

5. Open the source for the main activity or the activity where you wish to integrate QAConsole.

6. Add a private variable for QAConsole

   ````java
   private QAConsole qaConsole;
   ````

7. Initialize a new QAConsole in your `onCreate()` method

   ```java
   qaConsole = new QAConsole(getApplicationContext(), “<apptimizeAppKey>”);
   ```

8. Open the Apptimize dashboard, create and then launch your experiments.

9. Now run your app on a device/emulator.

10. If you are running on device, shake the device to launch the Apptimize QA Console.

11. Alternatively, you can launch the Apptimize QA Console programmatically.

  ```java
	// Add the following line to your onCreate method after initializing the qaConsole.
	console.isShakeGestureEnabled = false;
  
	// Call the launch method when you wish to display the console.
	console.launchQAConsoleActivity();
  ```

## Notes

* In order to display *Instant Updates* while using the QA console to force specific variants, you will need to set  `ApptimizeOptions` value `setForceVariantsShowWinnersAndInstantUpdates(true)` **Boolean** value to `true` and pass the options when calling `Apptimize.setup`. For example:

  ```java
  final String appKey = "YourAppKey";
  final ApptimizeOptions options = new ApptimizeOptions();
  options.setForceVariantsShowWinnersAndInstantUpdates(false);
  // ... set any additional options here
  Apptimize.setup(self, appKey, options);
  ```

  