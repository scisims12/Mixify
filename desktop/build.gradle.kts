import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":innertube"))
    if (providers.gradleProperty("useMavenLocalInnerTubeX").isPresent) {
        implementation("com.github.MetrolistGroup:innertubex:${libs.versions.innertubex.get()}")
    } else {
        implementation(libs.innertubex)
    }
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.foundation)
    implementation(compose.ui)
    implementation(compose.materialIconsExtended)
    implementation("uk.co.caprica:vlcj:4.8.3")
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)
    implementation(libs.ktor.client.encoding)
    testImplementation(libs.junit)
}

compose.desktop {
    application {
        mainClass = "com.mixify.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "Mixify Desktop"
            packageVersion = "1.0.0"
        }
    }
}
