AudioFetch Android SDK Sample App
=================================

This is a reference architecture app for AudioFetch's Android SDK, see the code comments, as well as the [API documentation](https://github.com/audiofetch/audiofetch-android-sdk-public-library/raw/master/AudioFetchSDKDocumentation_rev_1_0.pdf), and [API javadoc documentation](https://github.com/audiofetch/audiofetch-android-sdk-public-library/tree/master/docs) for further instructions on usage.

![alt tag](http://www.audiofetch.com/assets/audiofetch/audiofetch-sdk-android.png)




How To Integrate Into Your App

Add Packages to build.gradle

xxx


Add AudioFetch Service to ApplicationManifest.xml

        <service android:name="com.audiofetch.afaudiolib.bll.app.AFAudioService"
            android:stopWithTask="true"
            android:singleUser="true"
            android:exported="false"
            android:label="AudioFetch Music">
            <intent-filter>
                <action android:name="com.audiofetch.afaudiolib.bll.app.AFAudioService.NEXT"/>
                <action android:name="com.audiofetch.afaudiolib.bll.app.AFAudioService.PAUSE"/>
                <action android:name="com.audiofetch.afaudiolib.bll.app.AFAudioService.PLAY"/>
                <action android:name="com.audiofetch.afaudiolib.bll.app.AFAudioService.PREV"/>
                <action android:name="com.audiofetch.afaudiolib.bll.app.AFAudioService.STOP"/>
                <action android:name="com.audiofetch.afaudiolib.bll.app.AFAudioService.CLOSE"/>
                <category android:name="android.intent.category.DEFAULT" />
            </intent-filter>
        </service>


Add Permissions to ApplicationManifest.xml


Add AudioFetch SDK library to your application.




