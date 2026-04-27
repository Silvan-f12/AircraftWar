plugins {
    id("application")
}

application {
    mainClass.set("com.example.myserver.MyClass")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
