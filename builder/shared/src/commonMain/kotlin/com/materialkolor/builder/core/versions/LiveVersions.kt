package com.materialkolor.builder.core.versions

import com.materialkolor.builder.codegen.ExportVersions
import com.materialkolor.builder.core.platform.LibraryVersionSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * The versions an export names, [baked] at first and the live ones once [source] has answered.
 *
 * It asks [source] once, in this scope. An answer that is missing or does not read leaves [baked]
 * where it is.
 */
internal fun CoroutineScope.liveExportVersions(
    baked: ExportVersions,
    source: LibraryVersionSource,
): StateFlow<ExportVersions> {
    val current = MutableStateFlow(baked)
    launch {
        val json = try {
            source.fetch()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
        current.value = exportVersionsOf(baked, json)
    }
    return current.asStateFlow()
}

/**
 * [baked] with each library's version picked by [pickVersion] from what [json] lists for it, the JSON
 * `/api/versions` serves.
 *
 * The baked version is the floor. A library [json] leaves out, and [json] that is null or does not
 * read, keep the baked version. The builder's own version and [ExportVersions.fluentModuleAvailable]
 * always stay baked.
 */
internal fun exportVersionsOf(
    baked: ExportVersions,
    json: String?,
): ExportVersions {
    val published = json?.let(::publishedVersions) ?: return baked

    fun pick(
        key: String,
        floor: String,
    ): String = published[key]?.let { versions -> pickVersion(floor, versions) } ?: floor

    return baked.copy(
        materialKolor = pick("materialKolor", baked.materialKolor),
        fluent = pick("fluent", baked.fluent),
        composeUnstyled = pick("composeUnstyled", baked.composeUnstyled),
        composeMaterial3 = pick("composeMaterial3", baked.composeMaterial3),
        androidxMaterial3 = pick("androidxMaterial3", baked.androidxMaterial3),
    )
}

/**
 * Each library's version list in [json], or null when [json] is not an object. A value that is not
 * a list of strings is left out.
 */
private fun publishedVersions(json: String): Map<String, List<String>>? {
    val root = try {
        Json.parseToJsonElement(json)
    } catch (_: SerializationException) {
        return null
    } catch (_: IllegalArgumentException) {
        return null
    }
    if (root !is JsonObject) return null
    return root
        .mapNotNull { (key, value) ->
            val versions = (value as? JsonArray)?.map { entry ->
                (entry as? JsonPrimitive)?.takeIf { primitive -> primitive.isString }?.content ?: return@mapNotNull null
            }
            versions?.let { list -> key to list }
        }.toMap()
}
