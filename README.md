# AnimatedFragment

Project's `build.gradle`:
```groovy
allprojects {
    repositories {
        ...
        maven {
            url = "https://raw.githubusercontent.com/Onotolo/AndroidLibsMavenRepo/master"
        }
    }
}
```

Module's `build.gradle`:
```groovy
dependencies {
    ...
    implementation 'my.onotolo.android:animatedfragment:0.0.5'
}
```
