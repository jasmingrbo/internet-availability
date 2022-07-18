Internet availability
=====================

An Android library that allows for internet availability observation.

How it works
------------

- Internet capable networks are observed and a URL is *pinged* through them. Each time a network 
change happens *pings* are made using currently enabled networks.
- When *pinging* the precedence is given to **non-metered** networks, so **no metered networks get
pinged** in case internet availability is detected on a non-metered network.
- Besides *pings* that happen on each network change, additional *pings* can happen in given 
intervals to continuously keep checking for internet availability.
- **Observation and *pinging* start on the first `availability` collector and stop 5s after the 
last collector completes.**

How to use
----------
Instantiate an `Internet` instance and collect from its `availability` flow in a **lifecycle-aware**
manner.

    val internet = Internet.getInstance(
        scope, // Application CoroutineScope
        pingUrl = "https://www.google.com", // A url to be pinged
        pingTimeoutMillis = 2_000, // Ping timeout
        pingRetryTimes = 0, // How many times to retry pinging
        pingRetryIntervalMillis= 0, // Delay between each ping retry
        pingOnNonMeteredNetworks = true, // Should pinging happen on non-metered networks
        nonMeteredNetworksPingIntervalMillis= 10_000, // Delay between each ping on non-metered networks
        pingOnMeteredNetworks = true, // Should pinging happen on metered networks
        meteredNetworksPingIntervalMillis = 10_000, // Delay between each ping on metered networks
        context: Context // Application Context
    )

    // For Jetpack Compose
    val hasInternet by internet.availability.collectAsStateWhenStarted() // Check the sample app

    // Alternatively for Jetpack Compose and for Android Views
    internet.availability.collectWhenStarted(scope, onEach) // Check the sample app


**Check out the `sample` app to see it in action.**

Download
--------
```groovy
repositories {
  mavenCentral()
}

dependencies {
  implementation "io.github.jasmingrbo:internet-availability:1.0.0"
}
```

License
-------
    Copyright 2022 Jasmin Grbo
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
        http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

Read the full [license](LICENSE) for more information.
