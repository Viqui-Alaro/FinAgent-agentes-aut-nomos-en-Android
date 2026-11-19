package dev.devfest.agent.data.tools

import dev.devfest.agent.domain.model.ParameterType
import dev.devfest.agent.domain.model.ToolDefinition
import dev.devfest.agent.domain.model.ToolParameter
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

/**
 * getCurrentWeather(location: String) — simulada.
 * Determinista por ciudad para que la demo sea reproducible en el escenario.
 * Para hacerla real: inyecta un servicio Retrofit (Open-Meteo, etc.) aquí.
 */
class WeatherTool @Inject constructor() : AgentTool {

    override val definition = ToolDefinition(
        name = NAME,
        description = "Obtiene el clima actual (temperatura, condición y humedad) de una ciudad.",
        parameters = listOf(
            ToolParameter("location", ParameterType.STRING, "Nombre de la ciudad, p. ej. 'La Paz'"),
        ),
    )

    override suspend fun execute(arguments: Map<String, Any?>): JsonObject {
        val location = arguments.requireString("location")
        delay(1_200) // latencia simulada de una API externa

        val seed = location.trim().lowercase().hashCode()
        val conditions = listOf("Soleado", "Parcialmente nublado", "Nublado", "Lluvia ligera", "Tormenta eléctrica")

        return buildJsonObject {
            put("location", location)
            put("temperatureC", 6 + Math.floorMod(seed, 24))
            put("condition", conditions[Math.floorMod(seed, conditions.size)])
            put("humidity", 25 + Math.floorMod(seed, 55))
        }
    }

    companion object { const val NAME = "getCurrentWeather" }
}
