plugins { kotlin("multiplatform") version "2.1.21" }

group = "com.geometry.rowplacement"
version = "0.1.0-private"

kotlin {
    jvm()
    iosArm64()
    iosSimulatorArm64()
    macosArm64()
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs { nodejs() }
    jvmToolchain(21)
    sourceSets { commonTest.dependencies { implementation(kotlin("test")) } }
}
// No publication plugin, repository credentials, or remote release tasks.
