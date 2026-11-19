package dev.devfest.agent.data.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import dev.devfest.agent.data.BuildConfig
import dev.devfest.agent.data.llm.FakeLlmDataSource
import dev.devfest.agent.data.llm.GeminiLlmDataSource
import dev.devfest.agent.data.llm.LlmDataSource
import dev.devfest.agent.data.remote.GeminiApi
import dev.devfest.agent.data.repository.AIAgentRepositoryImpl
import dev.devfest.agent.data.tools.AgentTool
import dev.devfest.agent.data.tools.BankTransferTool
import dev.devfest.agent.data.tools.GetBalanceTool
import dev.devfest.agent.data.tools.WeatherTool
import dev.devfest.agent.domain.repository.AIAgentRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Provider
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindAgentRepository(impl: AIAgentRepositoryImpl): AIAgentRepository
}

/** Registro de herramientas: añadir una tool = una línea aquí. */
@Module
@InstallIn(SingletonComponent::class)
abstract class ToolsModule {
    @Binds @IntoSet abstract fun weather(tool: WeatherTool): AgentTool
    @Binds @IntoSet abstract fun balance(tool: GetBalanceTool): AgentTool
    @Binds @IntoSet abstract fun transfer(tool: BankTransferTool): AgentTool
}

@Module
@InstallIn(SingletonComponent::class)
object LlmModule {

    private const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/"

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false // no enviar campos null a la API
    }

    @Provides
    @Singleton
    fun provideOkHttp(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("x-goog-api-key", BuildConfig.GEMINI_API_KEY)
                    .build()
            )
        }
        .readTimeout(60, TimeUnit.SECONDS) // los LLM pueden tardar
        .build()

    @Provides
    @Singleton
    fun provideGeminiApi(client: OkHttpClient, json: Json): GeminiApi =
        Retrofit.Builder()
            .baseUrl(GEMINI_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GeminiApi::class.java)

    /**
     * Punto único donde se elige el "cerebro". Provider<> evita construir
     * Retrofit si estamos en modo demo offline.
     */
    @Provides
    @Singleton
    fun provideLlmDataSource(
        fake: Provider<FakeLlmDataSource>,
        api: Provider<GeminiApi>,
    ): LlmDataSource =
        if (BuildConfig.USE_FAKE_LLM) fake.get()
        else GeminiLlmDataSource(api.get(), BuildConfig.GEMINI_MODEL)
}
