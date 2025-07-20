## ASAPAndroid Library written in Kotlin

### Necessary permissions
The library requires the following runtime permissions which need to be requested from the user:

When targeting Android 12 and above:

* `android.Manifest.permission.BLUETOOTH_SCAN`
* `android.Manifest.permission.BLUETOOTH_CONNECT`
* `android.Manifest.permission.BLUETOOTH_ADVERTISE`
* `android.Manifest.permission.ACCESS_FINE_LOCATION`
* `android.Manifest.permission.ACCESS_COARSE_LOCATION` 

When targeting Android 11 and below:

* `android.Manifest.permission.ACCESS_FINE_LOCATION`

An example of how to do it can be found in the :sample module