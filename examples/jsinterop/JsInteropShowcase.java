package com.example.javaw.examples.jsinterop;

import com.example.javaw.dom.DOM;
import com.example.javaw.dom.Element;
import com.example.javaw.dom.JSObject;
import com.example.javaw.js.JS;
import com.example.javaw.js.JSConsole;
import com.example.javaw.js.JSDate;
import com.example.javaw.js.JSJSON;
import com.example.javaw.js.JSMath;
import com.example.javaw.js.JSResource;
import com.example.javaw.js.JSStorage;

@JS.Export("JavaCalculator")
public class JsInteropShowcase {

    public int memoryValue = 0;

    public static void main(String[] args) {
        start();
    }

    // Direct inline JavaScript snippet execution
    @JS("return window.navigator ? window.navigator.userAgent : 'Node.js Runtime';")
    public static native String getUserAgent();

    // Inline JS with automatic argument coercion
    @JS("return Math.hypot(a, b);")
    public static native double calculateHypotenuse(@JS.Coerce double a, @JS.Coerce double b);

    public static void start() {
        Element root = DOM.getElementById("js-interop-app");
        if (root == null) {
            root = DOM.getBody();
        }

        Element title = DOM.createElement("h2");
        title.setInnerText("GraalVM Web Image-Style JavaScript Interop");
        title.setStyle("color", "#f59e0b");
        root.appendChild(title);

        // Display User Agent via @JS snippet
        Element uaInfo = DOM.createElement("p");
        uaInfo.setStyle("color", "#cbd5e1");
        uaInfo.setStyle("font-size", "14px");
        uaInfo.setInnerText("Host User Agent: " + getUserAgent());
        root.appendChild(uaInfo);

        // Compute with JSMath facade
        double hyp = calculateHypotenuse(3.0, 4.0);
        Element mathInfo = DOM.createElement("p");
        mathInfo.setStyle("color", "#a3e635");
        mathInfo.setInnerText("Hypotenuse of (3.0, 4.0) via @JS: " + hyp);
        root.appendChild(mathInfo);

        // Access JSDate facade
        JSDate now = JSDate.create();
        Element dateInfo = DOM.createElement("p");
        dateInfo.setStyle("color", "#38bdf8");
        dateInfo.setInnerText("Current Date/Time via JSDate: " + now.toISOString());
        root.appendChild(dateInfo);

        // JSON Serialization via JSJSON facade
        JSObject jsonObj = JSObject.createObject();
        jsonObj.set("engine", "javaw");
        jsonObj.set("version", "1.0");
        Element jsonInfo = DOM.createElement("p");
        jsonInfo.setStyle("color", "#e879f9");
        jsonInfo.setInnerText("JSON Stringify demo: " + JSJSON.stringify(jsonObj));
        root.appendChild(jsonInfo);

        // Local Storage interaction via JSStorage facade
        JSStorage.setItem("javaw_last_access", now.toISOString());
        String stored = JSStorage.getItem("javaw_last_access");
        Element storeInfo = DOM.createElement("p");
        storeInfo.setStyle("color", "#fbbf24");
        storeInfo.setInnerText("Saved to localStorage: " + stored);
        root.appendChild(storeInfo);

        JSConsole.log("JsInteropShowcase successfully initialized!");
    }

    // Exported method to JavaScript
    public int add(int a, int b) {
        memoryValue = a + b;
        return memoryValue;
    }

    public int getMemory() {
        return memoryValue;
    }
}
