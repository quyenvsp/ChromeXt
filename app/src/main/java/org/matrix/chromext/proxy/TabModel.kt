package org.matrix.chromext.proxy

import java.io.File
import java.io.FileReader
import java.lang.ref.WeakReference
import java.util.Collections
import org.matrix.chromext.Chrome
import org.matrix.chromext.script.erudaToggle
import org.matrix.chromext.utils.Download
import org.matrix.chromext.utils.Log
import org.matrix.chromext.utils.invokeMethod

object TabModel {
  private var tabModels = mutableListOf<WeakReference<Any>>()
  private var eruda_loaded = mutableMapOf<Int, Boolean>()

  fun update(model: Any) {
    tabModels += WeakReference(model)
  }

  fun dropModel(model: Any) {
    tabModels.removeAll { it.get()!! == model }
  }

  private fun index(): Int {
    return tabModels.last().get()!!.invokeMethod() { name == "index" } as Int
  }

  fun getTab(): Any? {
    return tabModels.last().get()!!.invokeMethod(index()) { name == "getTabAt" }
  }

  fun getUrl(): String {
    return UserScriptProxy.parseUrl(getTab()?.invokeMethod { name == "getUrl" }) ?: ""
  }

  fun refresh(tab: Any, refreshEruda: Boolean = true) {
    val n = tabModels.size
    if (n > 1 && getTab() != tab) {
      // Only fix for incognito mode, should be sufficient for normal usage
      Collections.swap(tabModels, n - 1, n - 2)
    }
    if (refreshEruda) {
      eruda_loaded.put(index(), false)
    }
  }

  private fun erudaLoaded(): Boolean {
    return eruda_loaded.get(index()) ?: false
  }

  fun openEruda(): String {
    val ctx = Chrome.getContext()
    var script = ""
    if (!erudaLoaded()) {
      val eruda = File(ctx.getExternalFilesDir(null), "Download/Eruda.js")
      if (eruda.exists()) {
        script += FileReader(eruda).use { it.readText() } + "\n"
        script += ctx.assets.open("local_eruda.js").bufferedReader().use { it.readText() }
        script += erudaToggle
        eruda_loaded.put(index(), true)
      } else {
        Log.toast(ctx, "Updating Eruda...")
        Download.start(ERUD_URL, "Download/Eruda.js", true) {
          UserScriptProxy.evaluateJavaScript(openEruda())
        }
      }
    } else {
      script = erudaToggle
    }

    return script
  }
}
