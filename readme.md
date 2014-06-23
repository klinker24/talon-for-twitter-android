# Talon for Twitter #

This is the full and complete version of the Twitter client that I created for Android. The only thing that you will have to plug in is your own API keys.

I made the majority of this app when I was 19 years old, and you can tell at some places. I know it isn't the prettiest code that you have ever seen, but it works, and for 99% of people, it works very well.

As of now, it doesn't have to many comments throughout it, most of it is pretty easy to understand if you just dig for awhile, but as time goes on, I will go through and attempt to comment more of it.

There are some pretty neat things in this app, not just twitter related either. Someone looking at this will get the full rundown of our theme engines, windowed advances, clickable links, emoji support, and some other gems that Jacob and I spent a lot of time on. Feel free to use this as a resource for that kind of thing, that's why I wanted to open source it in the first place. Lots to learn from an app like this, and I think that is pretty important with the ever changing world of Android.

Talon does support TweetMarker and TwitLonger, both of which require API keys as well. I have not provided these keys, so any builds you compile yourself will not have those features.

### Compiling Talon ###

Please don't try to compile it as an ANT build. I beg you, just use Android Studio or IntelliJ and compile it with Gradle. It will make your life so much easier and I will not be answering questions about dependencies and compiling for Eclipse. We spent a long time changing all of our projects over to Gradle and I want it to help some people.

To get your Twitter API key, sign up on their developer site. You will have to implement a callback url. It really doesn't matter what this is, as long as you change it in the login activity. If you just want to use the same on that I have, I just used http://www.talonforandroid.com.

Once you get signed up and everything, just copy and paste your API key and API secret into the AppSettings class.

If you get TwitLonger or TweetMarker support, you can paste the keys for that into the respective utils/api_helpers classes.

The location of the keys may change in the future. Be aware of that.

### Pull Requests ###

One of the reasons that I decided to open source this wasn't just because people would be able to learn from it. I also need help. There are somethings *cough cough databases* that I just can't do any better. I don't have enough experience or knowledge yet to understand what is going wrong with them or why they randomly fail for some people. I have done the absolute best I can with this app, but the more minds working on it, the better. Chances are if you are here and actually reading the readme, you have far more experience programming than me anyways and know how things can be improved.

### Issues ###

If you think something could be done better, then tell me. I am not saying that I will agree with you on it or that it will ever be the way you think it should be, but there is no hurt in asking.

### Wrap Up ###

There isn't to much more I have to say about this. I have put a ton of time and effort into this project and I truly hope that this helps someone out there. Take the leap, try something you never have before, see what you can learn from me and my mistakes.

Let me know if you have questions and I will answer them to the best of my ability


Luke Klinker (Klinker Apps Lead Developer)



---

## License

    Copyright 2014 Luke Klinker

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
