# Weather Map
A simple Kotlin Android app integrating OpenWeatherMap and ArcGIS Runtime Android API's.

![weather-app](weather-app.png)

## Dependencies
- [Kotlin Anko](https://github.com/Kotlin/anko)
- [ArcGIS Android](https://developers.arcgis.com/android/)
- [Google Places API](https://developers.google.com/places/android-api/start)
- [Open Weather Map](https://openweathermap.org/)
- [Retrofit](http://square.github.io/retrofit/)

## ArcGIS Runtime SDK for Android
This app is explicitly intended for development and testing, you can become a member of the [ArcGIS Developer Program](https://developers.arcgis.com/pricing/) for free, more details about licensing your ArcGIS Runtime app can be found [here](https://developers.arcgis.com/arcgis-runtime/licensing/).

## Open Weather Map
To access the OpenWeatherMap API you need an [API Key](http://openweathermap.org/appid).  To use your API Key, create a **gradle.properties** file in the root of the **app** module with a string value pair representing your API Key.  This file is not tracked in Git as it is for personal use.

```groovy
API_KEY = "YOUR-API-KEY"
```

## Google Places API (Optional)
This app supports autocomplete service in the Google Places API, you must register this app project in your Google API Console and get a Google API key to add to the app. Refer to [Google](https://developers.google.com/places/android-api/signup) about signing up and getting your API keys.  Once you have your key, add it to the **gradle.properties** file created in the section above and append the you key as follows:

```groovy
...
PLACES_API_KEY = "YOUR-API-KEY"
```

## Licensing
A copy of the license is available in the repository's [LICENSE](LICENSE) file.
