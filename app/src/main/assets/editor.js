async function installScript(force = false) {
  const dialog = document.querySelector("dialog#confirm");
  if (!force) {
    dialog.showModal();
  } else {
    dialog.close();
    const script = document.body.innerText;
    Symbol.ChromeXt.dispatch("installScript", script);
  }
}

function renderEditor(code, checkEncoding) {
  let scriptMeta = document.querySelector("#meta");
  if (scriptMeta) return;
  const separator = "==/UserScript==\n";
  const script = code.innerHTML.split(separator);
  if (separator.length == 1) return;
  let html = (script.shift() + separator).replace(
    "GM.ChromeXt",
    "<em>GM.ChromeXt</em>"
  );
  for (const api of ["GM_notification", "GM_setClipboard", "GM_cookie"]) {
    html = html.replace(api, `<span>${api}</span>`);
  }
  scriptMeta = document.createElement("pre");
  scriptMeta.innerHTML = html;
  code.innerHTML = script.join(separator);
  code.id = "code";
  code.removeAttribute("style");
  scriptMeta.id = "meta";
  document.body.prepend(scriptMeta);

  if (checkEncoding) {
    const msg =
      "Current script may contain badly decoded text.\n\nTo fix possible issues, you can download this script and open it locally.";
    createDialog(msg, false);
  } else {
    const msg =
      "Code editor is blocked on this page.\n\nPlease use page menu to install this UserScript, or reload current page to enable the editor.";
    createDialog(msg);
    setTimeout(fixDialog);
  }

  scriptMeta.setAttribute("contenteditable", true);
  code.setAttribute("contenteditable", true);
  scriptMeta.setAttribute("spellcheck", false);
  code.setAttribute("spellcheck", false);
  // Too many nodes heavily slow down the event-loop, should be improved
  import("https://unpkg.com/@speed-highlight/core/dist/index.js").then(
    (imports) => {
      imports.highlightElement(code, "js", "multiline", {
        hideLineNumbers: true,
      });
    }
  );
}

function createDialog(msg) {
  const dialog = document.createElement("dialog");
  dialog.id = "confirm";
  dialog.textContent = msg;
  document.body.prepend(dialog);
  dialog.show();
}

function fixDialog() {
  const dialog = document.querySelector("dialog#confirm");
  if (dialog.textContent == "") return;
  dialog.close();
  dialog.textContent = "";
  const text = document.createElement("p");
  text.textContent = "Confirm ChromeXt to install this UserScript?";
  const div = document.createElement("div");
  div.id = "interaction";
  const yes = document.createElement("button");
  yes.textContent = "Confirm";
  yes.addEventListener("click", () => installScript(true));
  const no = document.createElement("button");
  no.addEventListener("click", () => {
    dialog.close();
    setTimeout(() => dialog.show(), 30000);
  });
  no.textContent = "Ask 30s later";
  div.append(yes);
  div.append(no);
  dialog.append(text);
  const askChromeXt = document.querySelector("#meta > em") != undefined;
  if (askChromeXt) {
    const alert = document.createElement("p");
    alert.id = "alert";
    alert.textContent = "ATTENTION: GM.ChromeXt is declared";
    dialog.append(alert);
  }
  dialog.append(div);
  installScript();
}

async function prepareDOM() {
  if (Symbol.ChromeXt == undefined) return;
  if (document.querySelector("script,div,p") != null) return;
  const meta = document.createElement("meta");
  const style = document.createElement("style");

  style.setAttribute("type", "text/css");
  meta.setAttribute("name", "viewport");
  meta.setAttribute(
    "content",
    "width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no"
  );
  style.textContent = _editor_style;

  const code = document.querySelector("body > pre");
  if (code == null) return prepareDOM();
  Symbol.installScript = installScript;
  document.head.appendChild(meta);
  document.head.appendChild(style);

  const checkEncoding = !(await fixEncoding(true, true, code));
  renderEditor(code, checkEncoding);
}

prepareDOM();
