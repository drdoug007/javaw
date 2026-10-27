package one.dastec.javaw.js;

/**
 * Strongly-typed Java facade for accessing bundled static assets and embedded resources
 * (@JS.Resource, @JSResource) in WebAssembly and the browser runtime.
 */
public class JSResource {

    /**
     * Retrieves the text content of an embedded resource by its path or identifier.
     */
    public static native String getAsString(String path);

    /**
     * Checks whether an embedded resource exists.
     */
    public static native boolean hasResource(String path);

    /**
     * Returns the byte length or character length of an embedded resource.
     */
    public static native int getLength(String path);

    /**
     * Injects inline CSS styles into the browser document head (<style> tag).
     */
    public static native void injectStyle(String css);

    /**
     * Injects an embedded CSS stylesheet resource into the document head.
     */
    public static native void injectStyleSheet(String path);

    /**
     * Injects and executes arbitrary JavaScript code or script tag.
     */
    public static native void injectScript(String js);

    /**
     * Injects and executes an embedded JavaScript file resource.
     */
    public static native void injectScriptTag(String path);

    /**
     * Lists all bundled resource paths available in the WebAssembly module.
     */
    public static native String[] listResources();

    /**
     * Mounts all bundled resources into the virtual file system (VFS) for WASI / WASIFile access.
     */
    public static native void mountAll();
}
