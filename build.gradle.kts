import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "github.xzynine.checkupdata"
        compileSdk =
            libs.versions.compileSdk
                .get()
                .toInt()

        minSdk =
            libs.versions.minSdk
                .get()
                .toInt()

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }

        optimization {
            consumerKeepRules.file("consumer-rules.pro")
        }

        lint {
            targetSdk = 36
        }
    }

    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    sourceSets {
        commonMain.dependencies {
            // 公开签名（ReleaseInfo.toJson/fromJson 等）引用 kotlinx.serialization.json 类型
            api(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.semver)
        }

        androidMain.dependencies {
            implementation(libs.okhttp)
            implementation(libs.bundles.kotlinx.coroutines)
        }

        jvmMain.dependencies {
            implementation(libs.okhttp)
            implementation(libs.kotlinx.coroutines.core)
        }

        jvmTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}
