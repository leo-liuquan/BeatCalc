/*
 * Copyright 2022 Erfan Sn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.dagger.hilt)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.androidx.baselineprofile)
    alias(libs.plugins.androidx.room)
}

android {
    compileSdk = Configs.COMPILE_SDK_VERSION
    namespace = Configs.PACKAGE_NAME

    defaultConfig {
        // Application ID (runtime/Play Store package name).
        applicationId = "com.quickdev.app.calculator"
        minSdk = Configs.MIN_SDK_VERSION
        targetSdk = Configs.TARGET_SDK_VERSION
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        // Never hardcode signing secrets in source control.
        // Configure signing via Gradle properties, environment variables, or an untracked
        // `keystore.properties` file at the project root.
        val keystorePropsFile = rootProject.file("keystore.properties")
        val keystoreProps = Properties().apply {
            if (keystorePropsFile.exists()) {
                keystorePropsFile.inputStream().use { load(it) }
            }
        }

        fun readSecret(name: String): String? =
            providers.gradleProperty(name).orNull
                ?: System.getenv(name)
                ?: keystoreProps.getProperty(name)

        val storeFilePath =
            readSecret("RELEASE_STORE_FILE_PATH") ?: rootProject.file("my-release-key.jks").path
        val storeFileCandidate = rootProject.file(storeFilePath)

        val storePassword =
            readSecret("RELEASE_STORE_PASS") ?: readSecret("RELEASE_STORE_PASSWORD")
        val keyAlias = readSecret("RELEASE_KEY_ALIAS") ?: "my-key-alias"
        val keyPassword = readSecret("RELEASE_KEY_PASS") ?: readSecret("RELEASE_KEY_PASSWORD")

        if (storeFileCandidate.exists() && !storePassword.isNullOrBlank() && !keyPassword.isNullOrBlank()) {
            create("release") {
                storeFile = storeFileCandidate
                this.storePassword = storePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        } else {
            project.logger.lifecycle(
                "Release signing is disabled. Provide keystore + credentials via " +
                    "RELEASE_STORE_FILE_PATH/RELEASE_STORE_PASS/RELEASE_KEY_ALIAS/RELEASE_KEY_PASS " +
                    "(or Gradle properties / keystore.properties)."
            )
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.findByName("release")
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += listOf("/META-INF/{AL2.0,LGPL2.1}", "DebugProbesKt.bin")
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

kotlin {
    jvmToolchain(Configs.JVM_TOOLCHAIN_VERSION)
}

baselineProfile {
    dexLayoutOptimization = true
    val hasKvm = File("/dev/kvm").exists()
    val autoGenerate =
        providers.gradleProperty("baselineProfile.autoGenerate").orNull?.toBoolean()
            ?: hasKvm
    automaticGenerationDuringBuild = autoGenerate
    saveInSrc = !autoGenerate
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    baselineProfile(project(":benchmark"))

    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.profileinstaller)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)

    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.mathparser.org.mxparser)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.coroutines.android)

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation(libs.material)
    implementation(libs.material.icons.extended)
    implementation(libs.ui.tooling.preview)

    implementation(libs.activity.compose)
    implementation(libs.constraintlayout.compose)
    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    testImplementation(composeBom)
    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk.android)
    testImplementation(libs.mockk.agent)
    testImplementation(libs.robolectric)
    testImplementation(libs.ext.junit)
    testImplementation(libs.ui.test.junit4)

    androidTestImplementation(composeBom)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.androidx.ui.test.junit4)

    ksp(libs.room.compiler)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
