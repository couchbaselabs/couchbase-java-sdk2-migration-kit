
plugins {
    `java-library`
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

dependencies {
    implementation("com.couchbase.client:java-client:3.3.4")

    testImplementation("junit:junit:4.13.2")
}
