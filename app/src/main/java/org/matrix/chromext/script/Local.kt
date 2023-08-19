package org.matrix.chromext.script

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileReader
import kotlin.random.Random
import org.json.JSONArray
import org.json.JSONObject
import org.matrix.chromext.Chrome
import org.matrix.chromext.Resource
import org.matrix.chromext.utils.Log

object GM {
  private val localScript: Map<String, String>

  init {
    val ctx = Chrome.getContext()
    Resource.enrich(ctx)
    localScript =
        ctx.assets
            .open("GM.js")
            .bufferedReader()
            .use { it.readText() }
            .split("// Kotlin separator\n\n")
            .associateBy(
                {
                  val decalre = it.lines()[0]
                  val sep = if (decalre.startsWith("function")) "(" else " ="
                  decalre.split(sep)[0].split(" ").last()
                },
                { it })
  }

  fun bootstrap(script: Script): List<String> {
    var code = script.code
    var grants = ""

    if (!script.meta.startsWith("// ==UserScript==")) {
      code = script.meta + code
    }

    script.grant.forEach {
      when (it) {
        "GM_info" -> return@forEach
        "GM.ChromeXt" -> return@forEach
        else ->
            if (localScript.containsKey(it)) {
              grants += localScript.get(it)
            } else if (it.startsWith("GM_")) {
              grants +=
                  "function ${it}(){ console.error('${it} is not implemented in ChromeXt yet, called with', arguments) }\n"
            } else if (it.startsWith("GM.")) {
              val func = it.substring(3)
              val name =
                  "GM_" +
                      if (func == "xmlHttpRequest") {
                        "xmlhttpRequest"
                      } else {
                        func
                      }
              if (localScript.containsKey(name))
                  if (!script.grant.contains(name)) grants += localScript.get(name)
              grants += "${it}={sync: ${name}};\n"
            }
      }
    }

    if (script.resource.size > 0) {
      val Resources = JSONArray()
      runCatching {
            script.resource.forEach {
              val content = it.split(" ")
              if (content.size != 2) throw Exception("Invalid resource ${it}")
              val name = content.first()
              val url = content.last()
              val resource = JSONObject()
              resource.put("name", name)
              resource.put("url", url.split("#").first())
              val file =
                  File(Chrome.getContext().getExternalFilesDir(null), resourcePath(script.id, name))
              if (file.exists()) {
                val text = FileReader(file).use { it.readText() }
                resource.put("content", text)
              }
              Resources.put(resource)
            }
          }
          .onFailure { Log.i("Fail to process resources for ${script.id}: " + it.message) }
      grants += "GM_info.script.resources = ${Resources};"
    }

    grants += localScript.get("GM.bootstrap")!!
    code = localScript.get("globalThis")!! + "((key) => {${code}})(null);"

    val GM_info =
        JSONObject(
            mapOf("scriptMetaStr" to script.meta, "script" to JSONObject().put("id", script.id)))
    val codes =
        mutableListOf(
            "(() => { const GM = {key:${Local.key}}; const GM_info = ${GM_info}; GM_info.script.code = () => {${code}};\n${grants}GM.bootstrap();})();\n//# sourceURL=local://ChromeXt/${Uri.encode(script.id)}")
    if (script.storage != null) {
      val storage_info =
          JSONObject(mapOf("id" to script.id, "data" to JSONObject().put("init", script.storage!!)))
      codes.add("ChromeXt.unlock(${Local.key}).post('scriptStorage', ${storage_info});")
    }

    return codes
  }
}

object Local {

  val promptInstallUserScript: String
  val customizeDevTool: String
  val eruda: String
  val encoding: String
  val initChromeXt: String
  val openEruda: String
  val cspRule: String
  val cosmeticFilter: String
  val key = Random.nextDouble()

  var eruda_version: String

  val anchorInChromeXt: Int
  // lineNumber of the anchor in GM.js, used to verify ChromeXt.dispatch

  init {
    val ctx = Chrome.getContext()
    Resource.enrich(ctx)
    var css =
        JSONArray(
            ctx.assets.open("editor.css").bufferedReader().use { it.readText() }.split("\n\n"))
    promptInstallUserScript =
        "const _editor_style = ${css}[0];\n" +
            ctx.assets.open("editor.js").bufferedReader().use { it.readText() }
    customizeDevTool = ctx.assets.open("devtools.js").bufferedReader().use { it.readText() }
    css =
        JSONArray(ctx.assets.open("eruda.css").bufferedReader().use { it.readText() }.split("\n\n"))
    eruda =
        "eruda._styles = ${css};\n" +
            ctx.assets
                .open("eruda.js")
                .bufferedReader()
                .use { it.readText() }
                .replaceFirst("ChromeXtUnlockKeyForEruda", key.toString())
    encoding = ctx.assets.open("encoding.js").bufferedReader().use { it.readText() }
    eruda_version = getErudaVersion()
    val localScript =
        ctx.assets
            .open("scripts.js")
            .bufferedReader()
            .use { it.readText() }
            .split("// Kotlin separator\n\n")
    initChromeXt = localScript[0]
    anchorInChromeXt = initChromeXt.split("\n").indexOfFirst { it.endsWith("// Kotlin anchor") } + 2
    openEruda = localScript[1].replaceFirst("ChromeXtUnlockKeyForEruda", key.toString())
    cspRule = localScript[2]
    cosmeticFilter = localScript[3]
  }

  fun getErudaVersion(ctx: Context = Chrome.getContext(), versionText: String? = null): String {
    val eruda = File(ctx.getExternalFilesDir(null), "Download/Eruda.js")
    if (eruda.exists() || versionText != null) {
      val verisonReg = Regex(" eruda v(?<version>[\\d\\.]+) https://")
      val firstLine = (versionText ?: FileReader(eruda).use { it.readText() }).lines()[0]
      val vMatchGroup = verisonReg.find(firstLine)?.groups as? MatchNamedGroupCollection
      if (vMatchGroup != null) {
        return vMatchGroup.get("version")?.value as String
      }
    }
    return "latest"
  }
}
