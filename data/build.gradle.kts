import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// La API key NUNCA va al repositorio: se lee de local.properties
val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}
val geminiKey: String = localProps.getProperty("GEMINI_API_KEY", "")
val geminiModel: String = localProps.getProperty("GEMINI_MODEL", "gemini-2.5-flash")
val forceFake: Boolean = localProps.getProperty("AGENT_USE_FAKE_LLM", "false").toBoolean()

android {
    namespace = "dev.devfest.agent.data"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiKey\"")
        buildConfigField("String", "GEMINI_MODEL", "\"$geminiModel\"")
        // Sin key (o forzado) -> LLM simulado: ideal para demos en vivo sin Wi-Fi fiable
        buildConfigField("boolean", "USE_FAKE_LLM", "${forceFake || geminiKey.isBlank()}")
    }
    buildFeatures { buildConfig = true }
}

kotlin { jvmToolchain(17) }

dependencies {
    implementation(project(":domain"))

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
}
