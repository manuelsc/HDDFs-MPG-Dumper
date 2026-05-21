plugins {
    kotlin("jvm") version "1.9.23"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.caspari"
version = "0.21"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.ajalt.clikt:clikt:3.5.4")
    implementation("me.tongfei:progressbar:0.9.4")
}

sourceSets {
    main {
        kotlin {
            setSrcDirs(listOf("src"))
        }
    }
}

tasks {
    shadowJar {
        archiveFileName.set("hddfsdump.jar")
        manifest {
            attributes["Main-Class"] = "MainKt"
        }
    }

    build {
        dependsOn(shadowJar)
    }
}
