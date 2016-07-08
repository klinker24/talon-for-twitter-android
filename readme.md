# Talon for Twitter

![Main Drawer](https://raw.githubusercontent.com/klinker24/talon-plus/master/Other/Promo%20Stuff/Graphics/Plus/Final%20Promos/Feature%20graphic%204.png?token=AEpgLwVvs5sUhpFxrrFXqJe2MaM9ZP3Dks5XiORRwA%3D%3D)

This is the Material Design version of Talon, my popular Twitter client.

### Compiling Talon

Please don't try to compile it as an ANT build. I beg you, just use Android Studio or IntelliJ and compile it with Gradle. It will make your life so much easier and I will not be answering questions about dependencies and compiling for Eclipse. We spent a long time changing all of our projects over to Gradle and I want it to help some people.

To compile it:

1. Check out the project with a *git clone <clone path here>*
2. You can compile it from the command line by CDing into the folder then *./gradlew assembleDebug*
3. Import it to IntelliJ or Android Studio by going to File -> Import Project... then selecting the *build.gradle* file in the root of the project

To get your Twitter API key, go through these steps:

1. sign in on their developer site (https://apps.twitter.com/)
2. Click *Create New App* You will have to implement a callback url.
3. Choose a name, description, and website. These are all required and unique to your app, but it makes no difference what you call them. Anything will work here.
4. For the callback URL, you can do anything you like, but to have it work out of the box, use: *http://www.talonforandroid.com*
  * If you want a different one (stressing that it really DOES NOT matter..) then change it in the LoginActivity under com.klinker.android.twitter.ui.setup
5. Read and accept their *Rules of the Road*, then *Create your Twitter Application*
6. After it is created, you can change the icon and add some other info from the settings page.
7. You NEED to go to the *Permissions* page of the app and select the *Read, Write and Access direct messages* option, or else you won't be able to do anything but view your timeline.

Once you get signed up and everything, just copy and paste your API key and API secret into the APIKeys class.

If you get TwitLonger or TweetMarker support, you can paste the keys for that into APIKeys class as well. If you do not get them, then those services WILL NOT work.


### Pull Requests

One of the reasons that I decided to open source this wasn't just because people would be able to learn from it. I also need help. There are somethings that I just don't know how to do any better. I don't have experience or knowledge yet to understand what is going wrong with them or why they randomly fail for some people.

I have done the absolute best I can with this app, but the more minds working on it, the better. Chances are if you are here and actually reading the readme, you have far more experience programming than me anyways and know how things can be improved.


### Issues

If you think something could be done better, then tell me. I am not saying that I will agree with you on it or that it will ever be the way you think it should be, but there is no hurt in asking.


### Wrap Up

There isn't to much more I have to say about this. I have put a ton of time and effort into this project and I truly hope that this helps someone out there. Take the leap, try something you never have before, see what you can learn from me and my mistakes.

Let me know if you have questions and I will answer them to the best of my ability.

Thanks and have fun with Talon!




---

## License

    Copyright 2016 Luke Klinker

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
