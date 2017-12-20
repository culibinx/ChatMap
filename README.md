**ChatMap** is a simple application for creating temporary markers on a map. On each marker you can open a chat and exchange messages or pictures.

### Preface

This is my first Android application. Such a test implementation of the idea. Therefore, the implementation is too primitive and not devoid of shortcomings.

### Installation

1. Create a Firebase project in the [Firebase console](https://console.firebase.google.com/), if you don't already have one. If you already have an existing Google project associated with your mobile app, click **Import Google Project**. Otherwise, click **Create New Project**.
2. Click **Add Firebase to your Android app** and follow the setup steps. If you're importing an existing Google project, this may happen automatically and you can just download the config file.
3. When prompted, enter your app's package name. It's important to enter the package name your app is using; this can only be set when you add an app to your Firebase project.
4. Open **Anonymous authentication** and **Storage** in settings Firebase project.
5. At the end, you'll download a **google-services.json** file. You can download this file again at any time.
6. If you haven't done so already, **copy/replace** this into your project's module folder, **app/google-services.json**.
7. Also define Google Maps API key in **app/src/debug/res/values/google_maps_api.xml**
8. Clean and rebuild project.

### Feedback/Updates

If you have suggestions, comments, or you know how to do best - write.
