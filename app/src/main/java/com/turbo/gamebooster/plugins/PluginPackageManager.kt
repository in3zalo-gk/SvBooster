package com.turbo.gamebooster.plugins

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.graphics.BitmapFactory
import dalvik.system.DexClassLoader
import com.svbooster.pluginapi.PluginCategory
import com.svbooster.pluginapi.SvPlugin
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.zip.ZipInputStream

/** A validated plugin loaded from a .plgs package. The class loader is retained for its lifetime. */
data class LoadedPlugin(
    val plugin: SvPlugin,
    val packageFile: File,
    val iconFile: File,
    val builtIn: Boolean,
    val classLoader: ClassLoader
)

data class PluginLoadResult(val plugins: List<LoadedPlugin>, val errors: List<String>)

object PluginPackageManager {
    private const val MAX_PACKAGE_BYTES = 20 * 1024 * 1024
    private const val MAX_DEX_BYTES = 16 * 1024 * 1024
    private const val MAX_ICON_BYTES = 512 * 1024
    private const val MAX_MANIFEST_BYTES = 16 * 1024
    private const val API_VERSION = 1
    private val allowedEntries = setOf("manifest.json", "classes.dex", "icon.png")

    private data class Manifest(
        val id: String,
        val name: String,
        val version: String,
        val category: PluginCategory,
        val entryClass: String
    )

    private data class Payload(val manifest: Manifest, val dex: ByteArray, val icon: ByteArray)

    /** Import accepts only a .plgs filename and a valid package structure; MIME type is not trusted. */
    fun importPlugin(context: Context, uri: Uri, existingIds: Set<String>): LoadedPlugin {
        val displayName = queryDisplayName(context, uri)
        require(displayName.endsWith(".plgs", ignoreCase = true)) {
            "Formato recusado: selecione um arquivo com extensão .plgs."
        }
        val bytes = context.contentResolver.openInputStream(uri)?.use { readLimited(it, MAX_PACKAGE_BYTES) }
            ?: throw IllegalArgumentException("Não foi possível abrir o arquivo selecionado.")
        val payload = parsePackage(bytes)
        require(payload.manifest.id !in existingIds) { "Já existe um plugin com o ID ${payload.manifest.id}." }

        val pluginDir = File(context.filesDir, "plugins").apply { mkdirs() }
        val destination = File(pluginDir, "${payload.manifest.id}.plgs")
        require(!destination.exists()) { "Esse plugin já está importado. Remova a versão atual antes de importar outra." }
        writeAtomically(destination, bytes)
        return try {
            loadPayload(context, destination, payload, builtIn = false)
        } catch (t: Throwable) {
            destination.delete()
            throw IllegalArgumentException(t.message ?: "O plugin não pôde ser carregado.", t)
        }
    }

    /** Copies bundled .plgs examples into private storage and loads both bundled and imported packages. */
    fun loadInstalled(context: Context): PluginLoadResult {
        val pluginDir = File(context.filesDir, "plugins").apply { mkdirs() }
        val bundled = context.assets.list("plugins").orEmpty().filter { it.endsWith(".plgs", ignoreCase = true) }
        for (assetName in bundled) {
            val destination = File(pluginDir, "builtin-${assetName.substringAfterLast('/')}")
            context.assets.open("plugins/$assetName").use { input ->
                val data = readLimited(input, MAX_PACKAGE_BYTES)
                writeAtomically(destination, data)
            }
        }

        val loaded = mutableListOf<LoadedPlugin>()
        val errors = mutableListOf<String>()
        pluginDir.listFiles().orEmpty()
            .filter { it.isFile && it.name.endsWith(".plgs", ignoreCase = true) }
            .sortedBy { it.name }
            .forEach { file ->
                try {
                    val bytes = file.inputStream().use { readLimited(it, MAX_PACKAGE_BYTES) }
                    val payload = parsePackage(bytes)
                    if (loaded.any { it.plugin.id == payload.manifest.id }) {
                        errors += "ID duplicado ignorado: ${payload.manifest.id} (${file.name})."
                        return@forEach
                    }
                    val entry = loadPayload(context, file, payload, builtIn = file.name.startsWith("builtin-"))
                    loaded += entry
                } catch (t: Throwable) {
                    errors += "${file.name}: ${t.message ?: "pacote inválido"}"
                }
            }
        return PluginLoadResult(loaded, errors)
    }

    fun removePlugin(context: Context, entry: LoadedPlugin) {
        require(!entry.builtIn) { "Plugins incluídos no app não podem ser removidos." }
        if (entry.packageFile.exists() && !entry.packageFile.delete()) {
            throw IllegalStateException("Não foi possível remover o pacote .plgs do armazenamento do app.")
        }
        if (entry.iconFile.exists()) entry.iconFile.delete()
    }

    private fun queryDisplayName(context: Context, uri: Uri): String {
        if (uri.scheme == "file") return File(uri.path.orEmpty()).name
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) return cursor.getString(index).orEmpty()
            }
        }
        return uri.lastPathSegment.orEmpty()
    }

    private fun parsePackage(bytes: ByteArray): Payload {
        require(bytes.size <= MAX_PACKAGE_BYTES) { "O pacote excede o limite de 20 MB." }
        val items = linkedMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                require(!entry.isDirectory && entry.name in allowedEntries) { "O pacote contém um arquivo não permitido: ${entry.name}." }
                require(entry.name !in items) { "O pacote contém entradas duplicadas." }
                val cap = when (entry.name) {
                    "manifest.json" -> MAX_MANIFEST_BYTES
                    "classes.dex" -> MAX_DEX_BYTES
                    else -> MAX_ICON_BYTES
                }
                items[entry.name] = readLimited(zip, cap)
                zip.closeEntry()
            }
        }
        require(items.keys == allowedEntries) { "O .plgs deve conter somente manifest.json, classes.dex e icon.png." }

        val json = JSONObject(String(items.getValue("manifest.json"), Charsets.UTF_8))
        require(json.getInt("formatVersion") == 1) { "Versão do formato .plgs não compatível." }
        require(json.getInt("apiVersion") == API_VERSION) { "Versão da API do plugin incompatível (esperada $API_VERSION)." }
        val id = json.getString("id")
        require(id.matches(Regex("[A-Za-z0-9][A-Za-z0-9._-]{2,127}"))) { "ID do plugin inválido." }
        val name = json.getString("name").trim()
        val version = json.getString("version").trim()
        val entryClass = json.getString("entryClass").trim()
        require(name.isNotBlank() && name.length <= 64) { "Nome do plugin inválido." }
        require(version.isNotBlank() && version.length <= 32) { "Versão do plugin inválida." }
        require(entryClass.matches(Regex("[A-Za-z_$][A-Za-z0-9_$.]{2,200}"))) { "Classe de entrada inválida." }
        require(json.optString("icon", "icon.png") == "icon.png") { "O ícone deve se chamar icon.png." }
        val category = try { PluginCategory.valueOf(json.getString("category")) }
        catch (_: Exception) { throw IllegalArgumentException("Categoria de plugin desconhecida.") }

        val dex = items.getValue("classes.dex")
        require(dex.size >= 8 && dex[0] == 'd'.code.toByte() && dex[1] == 'e'.code.toByte() &&
            dex[2] == 'x'.code.toByte() && dex[3] == '\n'.code.toByte()) { "classes.dex não é um DEX válido." }
        val icon = items.getValue("icon.png")
        require(icon.size >= 24 && icon[0] == 0x89.toByte() && icon[1] == 0x50.toByte() &&
            icon[2] == 0x4e.toByte() && icon[3] == 0x47.toByte()) { "icon.png não é uma imagem PNG válida." }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(icon, 0, icon.size, bounds)
        require(bounds.outWidth in 1..512 && bounds.outHeight in 1..512) { "O ícone deve ter no máximo 512 × 512 pixels." }

        return Payload(Manifest(id, name, version, category, entryClass), dex, icon)
    }

    private fun loadPayload(context: Context, packageFile: File, payload: Payload, builtIn: Boolean): LoadedPlugin {
        val manifest = payload.manifest
        val iconFile = File(context.filesDir, "plugins/${manifest.id}.icon.png")
        return try {
            writeAtomically(iconFile, payload.icon)

            val digest = MessageDigest.getInstance("SHA-256").digest(payload.dex).joinToString("") { "%02x".format(it) }
            val dexDirectory = File(context.codeCacheDir, "plugin-dex/${manifest.id}-$digest").apply { mkdirs() }
            val dexFile = File(dexDirectory, "classes.dex")
            if (!dexFile.exists()) writeAtomically(dexFile, payload.dex)
            require(dexFile.setReadOnly()) { "Não foi possível proteger o arquivo DEX contra alterações." }
            val optimizedDir = File(context.codeCacheDir, "plugin-opt").apply { mkdirs() }
            val loader = DexClassLoader(dexFile.absolutePath, optimizedDir.absolutePath, null, SvPlugin::class.java.classLoader)
            val clazz = loader.loadClass(manifest.entryClass).asSubclass(SvPlugin::class.java)
            val instance = try {
                clazz.getField("INSTANCE").get(null) as SvPlugin
            } catch (_: NoSuchFieldException) {
                clazz.getDeclaredConstructor().apply { isAccessible = true }.newInstance()
            }
            require(instance.id == manifest.id) { "O ID no código do plugin não confere com o manifest." }
            require(instance.name == manifest.name) { "O nome no código do plugin não confere com o manifest." }
            require(instance.version == manifest.version) { "A versão no código do plugin não confere com o manifest." }
            require(instance.category == manifest.category) { "A categoria no código do plugin não confere com o manifest." }
            LoadedPlugin(instance, packageFile, iconFile, builtIn, loader)
        } catch (t: Throwable) {
            iconFile.delete()
            throw t
        }
    }

    private fun readLimited(input: java.io.InputStream, maxBytes: Int): ByteArray {
        val output = ByteArrayOutputStream(minOf(maxBytes, 8192))
        val buffer = ByteArray(8192)
        var total = 0
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            require(total <= maxBytes) { "Um arquivo dentro do pacote excede o limite permitido." }
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }

    private fun writeAtomically(destination: File, bytes: ByteArray) {
        destination.parentFile?.mkdirs()
        val temp = File(destination.parentFile, destination.name + ".tmp")
        FileOutputStream(temp).use { it.write(bytes); it.fd.sync() }
        if (destination.exists() && !destination.delete()) {
            temp.delete()
            throw IllegalStateException("Não foi possível substituir o arquivo do plugin.")
        }
        if (!temp.renameTo(destination)) {
            temp.delete()
            throw IllegalStateException("Não foi possível salvar o plugin no armazenamento privado.")
        }
    }
}
