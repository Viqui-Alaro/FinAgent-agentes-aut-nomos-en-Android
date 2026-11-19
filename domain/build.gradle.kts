// :domain es Kotlin puro (sin Android): modelos, contratos y reglas del agente.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin { jvmToolchain(17) }

dependencies {
    api(libs.kotlinx.coroutines.core) // Flow forma parte de los contratos públicos
    api(libs.javax.inject)            // @Inject sin acoplarse a Hilt/Android

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
