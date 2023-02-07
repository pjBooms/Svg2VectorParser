import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

group = "org.jetbrains"
version = "1.0-SNAPSHOT"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "11"
        }
        withJava()
    }
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation("com.google.guava:guava:31.1-jre")
            }
        }
        val jvmTest by getting {
            dependencies {
                // on `org.junit.jupiter:junit-jupiter-engine:5.9.1` I have `No tests found`
                // Idea creates this task: :cleanJvmTest :jvmTest --tests "com.android.utils.TransformTest"
                implementation("junit:junit:4.13.2")
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Svg2VectorParser"
            packageVersion = "1.0.0"
        }
    }
}
