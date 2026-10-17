import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.turbo.gamebooster"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sv.booster"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "0.2.0-beta01"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    packaging {
        resources.excludes += setOf("META-INF/versions/9/OSGI-INF/MANIFEST.MF", "META-INF/DEPENDENCIES")
    }
}

// Build the examples as standalone DEX payloads and bundle each one as a .plgs asset.
val generatedPluginAssets = layout.buildDirectory.dir("generated/pluginAssets")
android.sourceSets.getByName("main").assets.srcDir(generatedPluginAssets)
val pluginSpecs = listOf(
    listOf(":plugin-optimization", "optimization", "org.svbooster.optimization", "Otimização de jogos", "1.0.0", "OTIMIZACAO", "com.svbooster.optimization.OptimizationPlugin"),
    listOf(":plugin-monitoring", "monitoring", "org.svbooster.monitoring", "Monitoramento e HUD", "1.0.0", "MONITORAMENTO", "com.svbooster.monitoring.MonitoringPlugin"),
    listOf(":plugin-diagnostics", "diagnostics", "org.svbooster.diagnostics", "Diagnóstico Shizuku", "1.0.0", "EXPERIMENTAL", "com.svbooster.diagnostics.DiagnosticsPlugin")
)
val packageBuiltinPlugins = tasks.register("packageBuiltinPlugins") {
    dependsOn(
        ":plugin-optimization:assembleDebug",
        ":plugin-monitoring:assembleDebug",
        ":plugin-diagnostics:assembleDebug"
    )
    inputs.dir(rootProject.file("plugin-optimization/src/main"))
    inputs.dir(rootProject.file("plugin-monitoring/src/main"))
    inputs.dir(rootProject.file("plugin-diagnostics/src/main"))
    inputs.file(rootProject.file("tools/plugin-diagnostics-icon.png"))
    inputs.files(
        rootProject.fileTree("plugin-optimization/build/outputs/apk/debug") { include("*.apk") },
        rootProject.fileTree("plugin-monitoring/build/outputs/apk/debug") { include("*.apk") },
        rootProject.fileTree("plugin-diagnostics/build/outputs/apk/debug") { include("*.apk") }
    )
    outputs.dir(generatedPluginAssets)
    doLast {
        val outputDir = generatedPluginAssets.get().dir("plugins").asFile
        outputDir.deleteRecursively()
        outputDir.mkdirs()
        pluginSpecs.forEach { spec ->
            val modulePath = spec[0]
            val key = spec[1]
            val module = rootProject.project(modulePath)
            val apkDir = module.layout.buildDirectory.dir("outputs/apk/debug").get().asFile
            val apk = apkDir.listFiles()?.firstOrNull { it.extension == "apk" }
                ?: error("APK temporário do plugin $modulePath não foi gerado.")
            val dexBytes = ZipFile(apk).use { archive ->
                val dex = archive.getEntry("classes.dex") ?: error("classes.dex não encontrado em ${apk.name}.")
                archive.getInputStream(dex).use { it.readBytes() }
            }
            require(dexBytes.size >= 8 && dexBytes[0] == 'd'.code.toByte()) { "DEX inválido em ${apk.name}." }
            val icon = if (key == "diagnostics") rootProject.file("tools/plugin-diagnostics-icon.png")
                else module.file("src/main/plugin/icon.png")
            require(icon.isFile) { "Ícone ausente: ${icon.path}" }
            val manifest = """{"formatVersion":1,"apiVersion":1,"id":"${spec[2]}","name":"${spec[3]}","version":"${spec[4]}","category":"${spec[5]}","entryClass":"${spec[6]}","icon":"icon.png"}"""
            val outFile = File(outputDir, "$key.plgs")
            ZipOutputStream(outFile.outputStream().buffered()).use { zip ->
                fun add(name: String, bytes: ByteArray) {
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(bytes)
                    zip.closeEntry()
                }
                add("manifest.json", manifest.toByteArray(Charsets.UTF_8))
                add("classes.dex", dexBytes)
                add("icon.png", icon.readBytes())
            }
        }
    }
}
tasks.named("preBuild").configure { dependsOn(packageBuiltinPlugins) }

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-android:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-service:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-savedstate:2.8.7")
    implementation("androidx.savedstate:savedstate-android:1.3.0")
    implementation("androidx.savedstate:savedstate:1.2.1")
    implementation("androidx.savedstate:savedstate-ktx:1.2.1")
    implementation(project(":plugin-api"))
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")
    // Conexão ADB própria (Depuração por Wi-Fi) — funciona sem Shizuku
    implementation("com.github.MuntashirAkon:libadb-android:3.1.1")
    implementation("org.conscrypt:conscrypt-android:2.5.3")
    implementation("org.bouncycastle:bcpkix-jdk15to18:1.81")

    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.test:core-ktx:1.6.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
}
