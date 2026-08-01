plugins {
    java
    id("com.gradleup.shadow") version "9.0.0-beta4"
}

group = "dev.lunahook.roleconnectors"
version = "1.1.0"

repositories {
    mavenCentral()
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    compileOnly("net.luckperms:api:5.4")

    implementation("net.dv8tion:JDA:5.5.0") {
        exclude(module = "opus-java")
    }
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.20.0")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        isZip64 = true
        archiveFileName.set("RoleConnectors-${version}.jar")

        relocate("net.dv8tion.jda", "com.roleconnectors.libs.jda")
        relocate("com.iwebpp.crypto", "com.roleconnectors.libs.iwebpp")
        relocate("okhttp3", "com.roleconnectors.libs.okhttp3")
        relocate("okio", "com.roleconnectors.libs.okio")
        relocate("org.apache.logging.slf4j", "com.roleconnectors.libs.log4j.slf4j")

    }

    build {
        dependsOn(shadowJar)
    }
}
