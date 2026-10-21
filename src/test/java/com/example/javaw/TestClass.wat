(module
  (@custom "sourceMappingURL" "\12TestClass.wasm.map")
  (import "dom" "getElementById" (func $dom_getElementById (param i32 i32) (result i32)))
  (import "dom" "getContext2D" (func $dom_getContext2D (param i32) (result i32)))
  (import "dom" "getWebGLContext" (func $dom_getWebGLContext (param i32) (result i32)))
  (import "dom" "getWidth" (func $dom_getWidth (param i32) (result i32)))
  (import "dom" "setWidth" (func $dom_setWidth (param i32 i32)))
  (import "dom" "getHeight" (func $dom_getHeight (param i32) (result i32)))
  (import "dom" "setHeight" (func $dom_setHeight (param i32 i32)))
  (import "dom" "setFillStyle" (func $dom_setFillStyle (param i32 i32 i32)))
  (import "dom" "fillRect" (func $dom_fillRect (param i32 f64 f64 f64 f64)))
  (import "dom" "beginPath" (func $dom_beginPath (param i32)))
  (import "dom" "arc" (func $dom_arc (param i32 f64 f64 f64 f64 f64 i32)))
  (import "dom" "fill" (func $dom_fill (param i32)))
  (import "dom" "webgl_clearColor" (func $dom_webgl_clearColor (param i32 f64 f64 f64 f64)))
  (import "dom" "webgl_clear" (func $dom_webgl_clear (param i32 i32)))
  (import "dom" "setTimeout" (func $dom_setTimeout (param i32 i32 i32) (result i32)))
  (import "dom" "setInterval" (func $dom_setInterval (param i32 i32 i32) (result i32)))
  (import "dom" "requestAnimationFrame" (func $dom_requestAnimationFrame (param i32 i32) (result i32)))
  (import "dom" "fetch" (func $dom_fetch (param i32 i32 i32 i32)))
  (import "dom" "confirm" (func $dom_confirm (param i32 i32) (result i32)))
  (import "dom" "js_global" (func $dom_js_global (result i32)))
  (import "dom" "js_eval" (func $dom_js_eval (param i32 i32) (result i32)))
  (import "dom" "js_createObject" (func $dom_js_createObject (result i32)))
  (import "dom" "js_createArray" (func $dom_js_createArray (result i32)))
  (import "dom" "js_get" (func $dom_js_get (param i32 i32 i32) (result i32)))
  (import "dom" "js_getString" (func $dom_js_getString (param i32 i32 i32) (result i32)))
  (import "dom" "js_getInt" (func $dom_js_getInt (param i32 i32 i32) (result i32)))
  (import "dom" "js_getBoolean" (func $dom_js_getBoolean (param i32 i32 i32) (result i32)))
  (import "dom" "js_setString" (func $dom_js_setString (param i32 i32 i32 i32 i32)))
  (import "dom" "js_setInt" (func $dom_js_setInt (param i32 i32 i32 i32)))
  (import "dom" "js_setDouble" (func $dom_js_setDouble (param i32 i32 i32 f64)))
  (import "dom" "js_setBoolean" (func $dom_js_setBoolean (param i32 i32 i32 i32)))
  (import "dom" "js_setObject" (func $dom_js_setObject (param i32 i32 i32 i32)))
  (import "dom" "js_callInt0" (func $dom_js_callInt0 (param i32 i32 i32) (result i32)))
  (import "dom" "js_callInt1_int" (func $dom_js_callInt1_int (param i32 i32 i32 i32) (result i32)))
  (import "dom" "js_asString" (func $dom_js_asString (param i32) (result i32)))
  (import "dom" "js_asInt" (func $dom_js_asInt (param i32) (result i32)))
  (import "dom" "js_asDouble" (func $dom_js_asDouble (param i32) (result f64)))
  (import "dom" "js_length" (func $dom_js_length (param i32) (result i32)))
  (import "dom" "js_getIndex" (func $dom_js_getIndex (param i32 i32) (result i32)))
  (import "dom" "js_setIndexObj" (func $dom_js_setIndexObj (param i32 i32 i32)))
  (import "dom" "js_setIndexStr" (func $dom_js_setIndexStr (param i32 i32 i32 i32)))
  (import "dom" "js_setIndexInt" (func $dom_js_setIndexInt (param i32 i32 i32)))
  (import "dom" "js_setIndexDouble" (func $dom_js_setIndexDouble (param i32 i32 f64)))
  (import "dom" "js_setIndexBoolean" (func $dom_js_setIndexBoolean (param i32 i32 i32)))
  (import "dom" "js_pushObj" (func $dom_js_pushObj (param i32 i32)))
  (import "dom" "js_pushStr" (func $dom_js_pushStr (param i32 i32 i32)))
  (import "dom" "js_pushInt" (func $dom_js_pushInt (param i32 i32)))
  (import "dom" "js_pushDouble" (func $dom_js_pushDouble (param i32 f64)))
  (import "dom" "js_pushBoolean" (func $dom_js_pushBoolean (param i32 i32)))
  (import "dom" "js_number_of_int" (func $dom_js_number_of_int (param i32) (result i32)))
  (import "dom" "js_number_of_double" (func $dom_js_number_of_double (param f64) (result i32)))
  (import "dom" "js_boolean_of" (func $dom_js_boolean_of (param i32) (result i32)))
  (import "dom" "js_string_of" (func $dom_js_string_of (param i32 i32) (result i32)))
  (import "dom" "js_bigint_of_i64" (func $dom_js_bigint_of_i64 (param i64) (result i32)))
  (import "dom" "js_bigint_of_str" (func $dom_js_bigint_of_str (param i32 i32) (result i32)))
  (import "dom" "js_symbol_of" (func $dom_js_symbol_of (param i32 i32) (result i32)))
  (import "dom" "js_undefined" (func $dom_js_undefined (result i32)))
  (import "dom" "js_null" (func $dom_js_null (result i32)))
  (import "dom" "js_type_error_of" (func $dom_js_type_error_of (param i32 i32) (result i32)))
  (import "dom" "js_typeof" (func $dom_js_typeof (param i32) (result i32)))
  (import "dom" "js_isNumber" (func $dom_js_isNumber (param i32) (result i32)))
  (import "dom" "js_isBoolean" (func $dom_js_isBoolean (param i32) (result i32)))
  (import "dom" "js_isString" (func $dom_js_isString (param i32) (result i32)))
  (import "dom" "js_isBigInt" (func $dom_js_isBigInt (param i32) (result i32)))
  (import "dom" "js_isSymbol" (func $dom_js_isSymbol (param i32) (result i32)))
  (import "dom" "js_isUndefined" (func $dom_js_isUndefined (param i32) (result i32)))
  (import "dom" "js_isNull" (func $dom_js_isNull (param i32) (result i32)))
  (import "dom" "js_isError" (func $dom_js_isError (param i32) (result i32)))
  (import "dom" "js_instanceof" (func $dom_js_instanceof (param i32 i32 i32) (result i32)))
  (import "dom" "js_strictEquals" (func $dom_js_strictEquals (param i32 i32) (result i32)))
  (import "dom" "js_isNaN" (func $dom_js_isNaN (param i32) (result i32)))
  (import "dom" "js_isFinite" (func $dom_js_isFinite (param i32) (result i32)))
  (import "dom" "js_isInteger" (func $dom_js_isInteger (param i32) (result i32)))
  (import "dom" "js_symbolDescription" (func $dom_js_symbolDescription (param i32) (result i32)))
  (import "dom" "js_errorMessage" (func $dom_js_errorMessage (param i32) (result i32)))
  (import "dom" "js_errorName" (func $dom_js_errorName (param i32) (result i32)))
  (import "dom" "thread_sleep" (func $dom_thread_sleep (param i32)))
  (import "dom" "thread_current_id" (func $dom_thread_current_id (result i32)))
  (import "dom" "thread_hardware_concurrency" (func $dom_thread_hardware_concurrency (result i32)))
  (import "dom" "js_resource_get_string" (func $dom_js_resource_get_string (param i32 i32) (result i32)))
  (import "dom" "js_resource_has" (func $dom_js_resource_has (param i32 i32) (result i32)))
  (import "dom" "js_resource_get_length" (func $dom_js_resource_get_length (param i32 i32) (result i32)))
  (import "dom" "js_resource_inject_style" (func $dom_js_resource_inject_style (param i32 i32)))
  (import "dom" "js_resource_inject_stylesheet" (func $dom_js_resource_inject_stylesheet (param i32 i32)))
  (import "dom" "js_resource_inject_script" (func $dom_js_resource_inject_script (param i32 i32)))
  (import "dom" "terminal_clear" (func $dom_terminal_clear))
  (import "dom" "terminal_get_columns" (func $dom_terminal_get_columns (result i32)))
  (import "dom" "terminal_get_rows" (func $dom_terminal_get_rows (result i32)))
  (@custom "javaw_js_snippets" "[{\22className\22:\22JSMath\22,\22methodName\22:\22hypot\22,\22importName\22:\22js_inline_JSMath_hypot\22,\22jsCode\22:\22return Math.hypot(a, b);\22,\22returnType\22:\22double\22,\22returnWasmType\22:\22f64\22,\22isStatic\22:true,\22isCoerced\22:false,\22parameters\22:[{\22name\22:\22a\22,\22javaType\22:\22double\22,\22wasmType\22:\22f64\22,\22coerced\22:false},{\22name\22:\22b\22,\22javaType\22:\22double\22,\22wasmType\22:\22f64\22,\22coerced\22:false}]},{\22className\22:\22JSMath\22,\22methodName\22:\22PI\22,\22importName\22:\22js_inline_JSMath_PI\22,\22jsCode\22:\22return Math.PI;\22,\22returnType\22:\22double\22,\22returnWasmType\22:\22f64\22,\22isStatic\22:true,\22isCoerced\22:false,\22parameters\22:[]},{\22className\22:\22JSDate\22,\22methodName\22:\22create\22,\22importName\22:\22js_inline_JSDate_create\22,\22jsCode\22:\22return new Date(ms);\22,\22returnType\22:\22JSDate\22,\22returnWasmType\22:\22i32\22,\22isStatic\22:true,\22isCoerced\22:false,\22parameters\22:[{\22name\22:\22ms\22,\22javaType\22:\22double\22,\22wasmType\22:\22f64\22,\22coerced\22:false}]},{\22className\22:\22JSDate\22,\22methodName\22:\22getTime\22,\22importName\22:\22js_inline_JSDate_getTime\22,\22jsCode\22:\22return this.getTime();\22,\22returnType\22:\22double\22,\22returnWasmType\22:\22f64\22,\22isStatic\22:false,\22isCoerced\22:false,\22parameters\22:[{\22name\22:\22this\22,\22javaType\22:\22JSDate\22,\22wasmType\22:\22i32\22,\22coerced\22:false}]},{\22className\22:\22JSJSON\22,\22methodName\22:\22stringify\22,\22importName\22:\22js_inline_JSJSON_stringify\22,\22jsCode\22:\22return JSON.stringify(val);\22,\22returnType\22:\22String\22,\22returnWasmType\22:\22i32\22,\22isStatic\22:true,\22isCoerced\22:false,\22parameters\22:[{\22name\22:\22val\22,\22javaType\22:\22JSValue\22,\22wasmType\22:\22i32\22,\22coerced\22:false}]},{\22className\22:\22TestClass\22,\22methodName\22:\22jsHypot\22,\22importName\22:\22js_inline_TestClass_jsHypot\22,\22jsCode\22:\22return Math.hypot(a, b);\22,\22returnType\22:\22double\22,\22returnWasmType\22:\22f64\22,\22isStatic\22:true,\22isCoerced\22:false,\22parameters\22:[{\22name\22:\22a\22,\22javaType\22:\22double\22,\22wasmType\22:\22f64\22,\22coerced\22:false},{\22name\22:\22b\22,\22javaType\22:\22double\22,\22wasmType\22:\22f64\22,\22coerced\22:false}]},{\22className\22:\22TestClass\22,\22methodName\22:\22jsGreet\22,\22importName\22:\22js_inline_TestClass_jsGreet\22,\22jsCode\22:\22return 'Hello ' + name + '!';\22,\22returnType\22:\22String\22,\22returnWasmType\22:\22i32\22,\22isStatic\22:true,\22isCoerced\22:false,\22parameters\22:[{\22name\22:\22name\22,\22javaType\22:\22String\22,\22wasmType\22:\22i32\22,\22coerced\22:false}]},{\22className\22:\22TestClass\22,\22methodName\22:\22jsMultiplyAndAdd\22,\22importName\22:\22js_inline_TestClass_jsMultiplyAndAdd\22,\22jsCode\22:\22return a * b + 10;\22,\22returnType\22:\22int\22,\22returnWasmType\22:\22i32\22,\22isStatic\22:true,\22isCoerced\22:false,\22parameters\22:[{\22name\22:\22a\22,\22javaType\22:\22int\22,\22wasmType\22:\22i32\22,\22coerced\22:false},{\22name\22:\22b\22,\22javaType\22:\22int\22,\22wasmType\22:\22i32\22,\22coerced\22:false}]},{\22className\22:\22TestClass\22,\22methodName\22:\22jsIsGreaterThan100\22,\22importName\22:\22js_inline_TestClass_jsIsGreaterThan100\22,\22jsCode\22:\22return x > 100;\22,\22returnType\22:\22boolean\22,\22returnWasmType\22:\22i32\22,\22isStatic\22:true,\22isCoerced\22:false,\22parameters\22:[{\22name\22:\22x\22,\22javaType\22:\22int\22,\22wasmType\22:\22i32\22,\22coerced\22:false}]},{\22className\22:\22TestClass\22,\22methodName\22:\22jsSumArray\22,\22importName\22:\22js_inline_TestClass_jsSumArray\22,\22jsCode\22:\22return arr.reduce((acc, x) => acc + x, 0);\22,\22returnType\22:\22int\22,\22returnWasmType\22:\22i32\22,\22isStatic\22:true,\22isCoerced\22:false,\22parameters\22:[{\22name\22:\22arr\22,\22javaType\22:\22int[]\22,\22wasmType\22:\22i32\22,\22coerced\22:true}]},{\22className\22:\22TestClass\22,\22methodName\22:\22jsDoubleArray\22,\22importName\22:\22js_inline_TestClass_jsDoubleArray\22,\22jsCode\22:\22return arr.map(x => x * 2);\22,\22returnType\22:\22int[]\22,\22returnWasmType\22:\22i32\22,\22isStatic\22:true,\22isCoerced\22:true,\22parameters\22:[{\22name\22:\22arr\22,\22javaType\22:\22int[]\22,\22wasmType\22:\22i32\22,\22coerced\22:true}]},{\22className\22:\22TestClass\22,\22methodName\22:\22jsJoinStrings\22,\22importName\22:\22js_inline_TestClass_jsJoinStrings\22,\22jsCode\22:\22return items.join(', ');\22,\22returnType\22:\22String\22,\22returnWasmType\22:\22i32\22,\22isStatic\22:true,\22isCoerced\22:false,\22parameters\22:[{\22name\22:\22items\22,\22javaType\22:\22String[]\22,\22wasmType\22:\22i32\22,\22coerced\22:true}]},{\22className\22:\22TestClass\22,\22methodName\22:\22jsGetBookJson\22,\22importName\22:\22js_inline_TestClass_jsGetBookJson\22,\22jsCode\22:\22return { title: 'WASM in Action', year: 2026 };\22,\22returnType\22:\22String\22,\22returnWasmType\22:\22i32\22,\22isStatic\22:true,\22isCoerced\22:true,\22parameters\22:[]}]")
  (import "dom" "js_inline_JSMath_hypot" (func $dom_js_inline_JSMath_hypot (param f64 f64) (result f64)))
  (import "dom" "js_inline_JSMath_PI" (func $dom_js_inline_JSMath_PI (result f64)))
  (import "dom" "js_inline_JSDate_create" (func $dom_js_inline_JSDate_create (param f64) (result i32)))
  (import "dom" "js_inline_JSDate_getTime" (func $dom_js_inline_JSDate_getTime (param i32) (result f64)))
  (import "dom" "js_inline_JSJSON_stringify" (func $dom_js_inline_JSJSON_stringify (param i32) (result i32)))
  (import "dom" "js_inline_TestClass_jsHypot" (func $dom_js_inline_TestClass_jsHypot (param f64 f64) (result f64)))
  (import "dom" "js_inline_TestClass_jsGreet" (func $dom_js_inline_TestClass_jsGreet (param i32 i32) (result i32)))
  (import "dom" "js_inline_TestClass_jsMultiplyAndAdd" (func $dom_js_inline_TestClass_jsMultiplyAndAdd (param i32 i32) (result i32)))
  (import "dom" "js_inline_TestClass_jsIsGreaterThan100" (func $dom_js_inline_TestClass_jsIsGreaterThan100 (param i32) (result i32)))
  (import "dom" "js_inline_TestClass_jsSumArray" (func $dom_js_inline_TestClass_jsSumArray (param i32) (result i32)))
  (import "dom" "js_inline_TestClass_jsDoubleArray" (func $dom_js_inline_TestClass_jsDoubleArray (param i32) (result i32)))
  (import "dom" "js_inline_TestClass_jsJoinStrings" (func $dom_js_inline_TestClass_jsJoinStrings (param i32) (result i32)))
  (import "dom" "js_inline_TestClass_jsGetBookJson" (func $dom_js_inline_TestClass_jsGetBookJson (result i32)))
  (@custom "javaw_js_exports" "[{\22className\22:\22ExportedCounter\22,\22exportName\22:\22JavaCounter\22,\22structSize\22:8,\22vtableBase\22:15,\22hasVtable\22:true,\22constructors\22:[{\22exportFunctionName\22:\22ExportedCounter__init\22,\22params\22:[{\22name\22:\22initialCount\22,\22javaType\22:\22int\22,\22wasmType\22:\22i32\22,\22coerced\22:false}]}],\22methods\22:[{\22methodName\22:\22increment\22,\22exportName\22:\22increment\22,\22wasmFunctionName\22:\22ExportedCounter_increment\22,\22returnType\22:\22void\22,\22returnWasmType\22:\22\22,\22isStatic\22:false,\22isCoerced\22:false,\22params\22:[]},{\22methodName\22:\22add\22,\22exportName\22:\22add\22,\22wasmFunctionName\22:\22ExportedCounter_add\22,\22returnType\22:\22void\22,\22returnWasmType\22:\22\22,\22isStatic\22:false,\22isCoerced\22:false,\22params\22:[{\22name\22:\22amount\22,\22javaType\22:\22int\22,\22wasmType\22:\22i32\22,\22coerced\22:false}]},{\22methodName\22:\22getCount\22,\22exportName\22:\22getCount\22,\22wasmFunctionName\22:\22ExportedCounter_getCount\22,\22returnType\22:\22int\22,\22returnWasmType\22:\22i32\22,\22isStatic\22:false,\22isCoerced\22:false,\22params\22:[]},{\22methodName\22:\22multiply\22,\22exportName\22:\22multiply\22,\22wasmFunctionName\22:\22multiply\22,\22returnType\22:\22int\22,\22returnWasmType\22:\22i32\22,\22isStatic\22:true,\22isCoerced\22:false,\22params\22:[{\22name\22:\22a\22,\22javaType\22:\22int\22,\22wasmType\22:\22i32\22,\22coerced\22:false},{\22name\22:\22b\22,\22javaType\22:\22int\22,\22wasmType\22:\22i32\22,\22coerced\22:false}]},{\22methodName\22:\22describe\22,\22exportName\22:\22describe\22,\22wasmFunctionName\22:\22ExportedCounter_describe\22,\22returnType\22:\22String\22,\22returnWasmType\22:\22i32\22,\22isStatic\22:false,\22isCoerced\22:false,\22params\22:[{\22name\22:\22label\22,\22javaType\22:\22String\22,\22wasmType\22:\22i32\22,\22coerced\22:false}]}],\22fields\22:[]}]")
  (@custom "javaw_js_code" "[\22globalThis.__bundled_test_var = 777; function bundledCalc(n) { return n * 3; }\22]")
  (@custom "javaw_js_resources" "{\22theme.css\22:\22body { background-color: #f0f0f0; }\22,\22config.json\22:\22{\5c\5c\5c\22theme\5c\5c\5c\22:\5c\5c\5c\22dark\5c\5c\5c\22,\5c\5c\5c\22port\5c\5c\5c\22:8080}\22}")
  ;; --- WASI Host Imports (wasi_snapshot_preview1) ---
  (import "wasi_snapshot_preview1" "fd_write" (func $__wasi_fd_write (param i32 i32 i32 i32) (result i32)))
  (import "wasi_snapshot_preview1" "fd_read" (func $__wasi_fd_read (param i32 i32 i32 i32) (result i32)))
  (import "wasi_snapshot_preview1" "proc_exit" (func $__wasi_proc_exit (param i32)))
  (import "wasi_snapshot_preview1" "args_sizes_get" (func $__wasi_args_sizes_get (param i32 i32) (result i32)))
  (import "wasi_snapshot_preview1" "args_get" (func $__wasi_args_get (param i32 i32) (result i32)))
  (import "wasi_snapshot_preview1" "environ_sizes_get" (func $__wasi_environ_sizes_get (param i32 i32) (result i32)))
  (import "wasi_snapshot_preview1" "environ_get" (func $__wasi_environ_get (param i32 i32) (result i32)))
  (import "wasi_snapshot_preview1" "clock_time_get" (func $__wasi_clock_time_get (param i32 i64 i32) (result i32)))
  (import "wasi_snapshot_preview1" "random_get" (func $__wasi_random_get (param i32 i32) (result i32)))
  (import "wasi_snapshot_preview1" "path_open" (func $__wasi_path_open (param i32 i32 i32 i32 i32 i64 i64 i32 i32) (result i32)))
  (import "wasi_snapshot_preview1" "fd_close" (func $__wasi_fd_close (param i32) (result i32)))
  (import "wasi_snapshot_preview1" "fd_seek" (func $__wasi_fd_seek (param i32 i64 i32 i32) (result i32)))
  (import "wasi_snapshot_preview1" "fd_filestat_get" (func $__wasi_fd_filestat_get (param i32 i32) (result i32)))
  (import "wasi_snapshot_preview1" "path_filestat_get" (func $__wasi_path_filestat_get (param i32 i32 i32 i32 i32) (result i32)))
  (import "wasi_snapshot_preview1" "path_create_directory" (func $__wasi_path_create_directory (param i32 i32 i32) (result i32)))
  (import "wasi_snapshot_preview1" "path_unlink_file" (func $__wasi_path_unlink_file (param i32 i32 i32) (result i32)))
  (tag $Exception (export "Exception") (param i32))
  (memory $strings 2)
  (export "memory" (memory $strings))
  (export "strings" (memory $strings))
  (memory $heap 2 20 shared)
  (export "heap" (memory $heap))
  (memory $stack 2 20 shared)
  (export "stack" (memory $stack))
  (global $asyncCounter (mut i32) (i32.const 0))
  (global $asyncFetchedData (mut i32) (i32.const 0))
  (data (memory $strings) (i32.const 4096) "globalThis.__bundled_test_var = 777; function bundledCalc(n) { return n * 3; }\00")
  (data (memory $strings) (i32.const 4175) "body { background-color: #f0f0f0; }\00")
  (data (memory $strings) (i32.const 4211) "theme.css\00")
  (data (memory $strings) (i32.const 4221) "{\5c\22theme\5c\22:\5c\22dark\5c\22,\5c\22port\5c\22:8080}\00")
  (data (memory $strings) (i32.const 4256) "config.json\00")
  (data (memory $strings) (i32.const 4268) "\00")
  (data (memory $strings) (i32.const 4269) "onTimerTick\00")
  (data (memory $strings) (i32.const 4281) "onFrameUpdate\00")
  (data (memory $strings) (i32.const 4295) "onFetchSuccess\00")
  (data (memory $strings) (i32.const 4310) "Hello\00")
  (data (memory $strings) (i32.const 4316) "WebAssembly\00")
  (data (memory $strings) (i32.const 4328) ", \00")
  (data (memory $strings) (i32.const 4331) "!\00")
  (data (memory $strings) (i32.const 4333) "User: \00")
  (data (memory $strings) (i32.const 4340) " (age: \00")
  (data (memory $strings) (i32.const 4348) ")\00")
  (data (memory $strings) (i32.const 4350) "Hello World\00")
  (data (memory $strings) (i32.const 4362) "World\00")
  (data (memory $strings) (i32.const 4368) "Java\00")
  (data (memory $strings) (i32.const 4373) "Count: \00")
  (data (memory $strings) (i32.const 4381) "testCanvas\00")
  (data (memory $strings) (i32.const 4392) "#ff0000\00")
  (data (memory $strings) (i32.const 4400) "glCanvas\00")
  (data (memory $strings) (i32.const 4409) "({ title: 'Dynamic JS', count: 42, active: true })\00")
  (data (memory $strings) (i32.const 4460) "title\00")
  (data (memory $strings) (i32.const 4466) "count\00")
  (data (memory $strings) (i32.const 4472) "active\00")
  (data (memory $strings) (i32.const 4479) "user\00")
  (data (memory $strings) (i32.const 4484) "Alice\00")
  (data (memory $strings) (i32.const 4490) "score\00")
  (data (memory $strings) (i32.const 4496) "rating\00")
  (data (memory $strings) (i32.const 4503) "premium\00")
  (data (memory $strings) (i32.const 4511) "item1\00")
  (data (memory $strings) (i32.const 4517) "item2\00")
  (data (memory $strings) (i32.const 4523) "updated\00")
  (data (memory $strings) (i32.const 4531) "Math\00")
  (data (memory $strings) (i32.const 4536) "max\00")
  (data (memory $strings) (i32.const 4540) "Testing WASI standard output from Java!\00")
  (data (memory $strings) (i32.const 4580) "Progress: \00")
  (data (memory $strings) (i32.const 4591) "WASI stderr stream message\00")
  (data (memory $strings) (i32.const 4618) "WASI Facade message: \00")
  (data (memory $strings) (i32.const 4640) "Active\00")
  (data (memory $strings) (i32.const 4647) "WASI Facade Error\00")
  (data (memory $strings) (i32.const 4665) "Test error\00")
  (data (memory $strings) (i32.const 4676) "Custom exception message\00")
  (data (memory $strings) (i32.const 4701) "App failed\00")
  (data (memory $strings) (i32.const 4712) "Code error\00")
  (data (memory $strings) (i32.const 4723) "Bad arg\00")
  (data (memory $strings) (i32.const 4731) "Generic error\00")
  (data (memory $strings) (i32.const 4745) "Inner error\00")
  (data (memory $strings) (i32.const 4757) "/hello.txt\00")
  (data (memory $strings) (i32.const 4768) "Hello WASI VFS\00")
  (data (memory $strings) (i32.const 4783) "/append.txt\00")
  (data (memory $strings) (i32.const 4795) "Part1\00")
  (data (memory $strings) (i32.const 4801) "Part2\00")
  (data (memory $strings) (i32.const 4807) "/to_delete.txt\00")
  (data (memory $strings) (i32.const 4822) "temp data\00")
  (data (memory $strings) (i32.const 4832) "/app/data.txt\00")
  (data (memory $strings) (i32.const 4846) "Config=42\00")
  (data (memory $strings) (i32.const 4856) ";Active=true\00")
  (data (memory $strings) (i32.const 4869) "/app/temp.log\00")
  (data (memory $strings) (i32.const 4883) "12345\00")
  (data (memory $strings) (i32.const 4889) "return Math.hypot(a, b);\00")
  (data (memory $strings) (i32.const 4914) "return 'Hello ' + name + '!';\00")
  (data (memory $strings) (i32.const 4944) "return a * b + 10;\00")
  (data (memory $strings) (i32.const 4963) "return x > 100;\00")
  (data (memory $strings) (i32.const 4979) "return arr.reduce((acc, x) => acc + x, 0);\00")
  (data (memory $strings) (i32.const 5022) "return arr.map(x => x * 2);\00")
  (data (memory $strings) (i32.const 5050) "return items.join(', ');\00")
  (data (memory $strings) (i32.const 5075) "return { title: 'WASM in Action', year: 2026 };\00")
  (data (memory $strings) (i32.const 5123) "hello world\00")
  (data (memory $strings) (i32.const 5135) "number\00")
  (data (memory $strings) (i32.const 5142) "string\00")
  (data (memory $strings) (i32.const 5149) "boolean\00")
  (data (memory $strings) (i32.const 5157) "id_key\00")
  (data (memory $strings) (i32.const 5164) "9007199254740995\00")
  (data (memory $strings) (i32.const 5181) "Invalid type argument\00")
  (data (memory $strings) (i32.const 5203) "TypeError\00")
  (data (memory $strings) (i32.const 5213) "Error\00")
  (data (memory $strings) (i32.const 5219) "42\00")
  (data (memory $strings) (i32.const 5222) "name\00")
  (data (memory $strings) (i32.const 5227) "version\00")
  (data (memory $strings) (i32.const 5235) "nonexistent.txt\00")
  (data (memory $strings) (i32.const 5251) ".app-header { font-size: 24px; }\00")
  (data (memory $strings) (i32.const 5284) "globalThis.__injected_script_run = true;\00")
  (data (memory $strings) (i32.const 5325) "__bundled_test_var\00")
  (data (memory $strings) (i32.const 5344) "[WASI] \00")
  (data (memory $strings) (i32.const 5352) "JavaCounter\00")
  (data (memory $strings) (i32.const 5364) ": \00")
  (data (memory $strings) (i32.const 5367) "\1b[32m\00")
  (global $__heap_ptr (mut i32) (i32.const 0))
  ;; --- WebAssembly Function Type Signatures ---
  (type $t_sig_1 (func (param i32) (result i32)))
  (type $t_sig_2 (func (param i32 i32 i32)))
  (type $t_sig_3 (func (param i32)))
  (type $t_sig_4 (func (param i32 i32)))
  (type $t_sig_5 (func (param i32 i32) (result i32)))
  ;; --- WebAssembly Function Table (VTable) ---
  (table (export "__vtable") 23 funcref)
  (elem (i32.const 0) $Throwable_getMessage $Exception_getMessage $Exception_getMessage $CustomAppException_getCode $Point_getX $Point_getY $Point_setLocation $Point_distanceSq $Shape_getColor $Shape_area $Shape_getColor $Circle_area $Circle_getRadius $Shape_getColor $Rectangle_area $ExportedCounter_increment $ExportedCounter_add $ExportedCounter_getCount $ExportedCounter_describe $RuntimeException_getMessage $IllegalArgumentException_getMessage $IllegalStateException_getMessage $NullPointerException_getMessage)
  ;; --- Heap Allocation Functions ---
  (func $__heap_alloc (export "__heap_alloc") (param $size i32) (result i32)
    (local $ptr i32)
    (local $aligned_size i32)
    (local $i i32)
    global.get $__heap_ptr
    local.set $ptr
    local.get $size
    i32.const 7
    i32.add
    i32.const -8
    i32.and
    local.set $aligned_size
    global.get $__heap_ptr
    local.get $aligned_size
    i32.add
    global.set $__heap_ptr
    local.get $ptr
    i32.const 0
    local.get $aligned_size
    memory.fill
    local.get $ptr
  )
  ;; --- String Runtime Functions ---
  (func $__str_alloc (export "__str_alloc") (param $len i32) (result i32)
    (local $ptr i32)
    (local $total_bytes i32)
    global.get $__heap_ptr
    local.set $ptr
    local.get $len
    i32.const 1
    i32.add
    i32.const 3
    i32.add
    i32.const -4
    i32.and
    local.set $total_bytes
    global.get $__heap_ptr
    local.get $total_bytes
    i32.add
    global.set $__heap_ptr
    local.get $ptr
    local.get $len
    i32.add
    i32.const 0
    i32.store8
    local.get $ptr
  )
  (func $__str_length (export "__str_length") (param $s i32) (result i32)
    (local $len i32)
    local.get $s
    i32.eqz
    if
      i32.const 0
      return
    end
    i32.const 0
    local.set $len
    (block $break_strlen
      (loop $loop_strlen
        local.get $s
        local.get $len
        i32.add
        i32.load8_u
        i32.eqz
        br_if $break_strlen
        local.get $len
        i32.const 1
        i32.add
        local.set $len
        br $loop_strlen
      )
    )
    local.get $len
  )
  (func $__str_concat (export "__str_concat") (param $s1 i32) (param $s2 i32) (result i32)
    (local $len1 i32)
    (local $len2 i32)
    (local $res i32)
    local.get $s1
    i32.eqz
    if
      local.get $s2
      return
    end
    local.get $s2
    i32.eqz
    if
      local.get $s1
      return
    end
    local.get $s1
    call $__str_length
    local.set $len1
    local.get $s2
    call $__str_length
    local.set $len2
    local.get $len1
    local.get $len2
    i32.add
    call $__str_alloc
    local.set $res
    local.get $res
    local.get $s1
    local.get $len1
    memory.copy
    local.get $res
    local.get $len1
    i32.add
    local.get $s2
    local.get $len2
    memory.copy
    local.get $res
    local.get $len1
    i32.add
    local.get $len2
    i32.add
    i32.const 0
    i32.store8
    local.get $res
  )
  (func $__str_from_i32 (export "__str_from_i32") (param $val i32) (result i32)
    (local $temp i32)
    (local $len i32)
    (local $is_neg i32)
    (local $res i32)
    (local $idx i32)
    (local $rem i32)
    (local $t i32)
    local.get $val
    i32.const 0
    i32.eq
    if
      i32.const 1
      call $__str_alloc
      local.set $res
      local.get $res
      i32.const 48
      i32.store8
      local.get $res
      i32.const 1
      i32.add
      i32.const 0
      i32.store8
      local.get $res
      return
    end
    local.get $val
    i32.const 0
    i32.lt_s
    if
      i32.const 1
      local.set $is_neg
      i32.const 1
      local.set $len
      i32.const 0
      local.get $val
      i32.sub
      local.set $temp
    else
      i32.const 0
      local.set $is_neg
      i32.const 0
      local.set $len
      local.get $val
      local.set $temp
    end
    local.get $temp
    local.set $t
    (block $break_len
      (loop $loop_len
        local.get $t
        i32.eqz
        br_if $break_len
        local.get $len
        i32.const 1
        i32.add
        local.set $len
        local.get $t
        i32.const 10
        i32.div_u
        local.set $t
        br $loop_len
      )
    )
    local.get $len
    call $__str_alloc
    local.set $res
    local.get $is_neg
    if
      local.get $res
      i32.const 45
      i32.store8
    end
    local.get $len
    local.set $idx
    (block $break_digits
      (loop $loop_digits
        local.get $temp
        i32.eqz
        br_if $break_digits
        local.get $idx
        i32.const 1
        i32.sub
        local.set $idx
        local.get $temp
        i32.const 10
        i32.rem_u
        local.set $rem
        local.get $res
        local.get $idx
        i32.add
        local.get $rem
        i32.const 48
        i32.add
        i32.store8
        local.get $temp
        i32.const 10
        i32.div_u
        local.set $temp
        br $loop_digits
      )
    )
    local.get $res
    local.get $len
    i32.add
    i32.const 0
    i32.store8
    local.get $res
  )
  (func $__str_from_char (export "__str_from_char") (param $c i32) (result i32)
    (local $res i32)
    i32.const 1
    call $__str_alloc
    local.set $res
    local.get $res
    local.get $c
    i32.store8
    local.get $res
    i32.const 1
    i32.add
    i32.const 0
    i32.store8
    local.get $res
  )
  (func $__str_from_bool (export "__str_from_bool") (param $b i32) (result i32)
    local.get $b
    if (result i32)
      i32.const 0
    else
      i32.const 0
    end
  )
  (func $__str_from_f64 (export "__str_from_f64") (param $val f64) (result i32)
    (local $int_part i32)
    (local $frac_part i32)
    (local $s_int i32)
    (local $s_frac i32)
    (local $s_dot i32)
    (local $s_res i32)
    (local $is_neg i32)
    local.get $val
    f64.const 0.0
    f64.lt
    local.set $is_neg
    local.get $is_neg
    if
      f64.const 0.0
      local.get $val
      f64.sub
      local.set $val
    end
    local.get $val
    i32.trunc_f64_s
    local.set $int_part
    local.get $val
    local.get $int_part
    f64.convert_i32_s
    f64.sub
    f64.const 100.0
    f64.mul
    i32.trunc_f64_s
    local.set $frac_part
    local.get $int_part
    call $__str_from_i32
    local.set $s_int
    i32.const 1
    call $__str_alloc
    local.set $s_dot
    local.get $s_dot
    i32.const 46
    i32.store8
    local.get $frac_part
    call $__str_from_i32
    local.set $s_frac
    local.get $s_int
    local.get $s_dot
    call $__str_concat
    local.get $s_frac
    call $__str_concat
    local.set $s_res
    local.get $is_neg
    if (result i32)
      i32.const 1
      call $__str_alloc
      local.tee $s_dot
      i32.const 45
      i32.store8
      local.get $s_dot
      local.get $s_res
      call $__str_concat
    else
      local.get $s_res
    end
  )
  (func $__str_charAt (export "__str_charAt") (param $s i32) (param $idx i32) (result i32)
    local.get $s
    i32.eqz
    if
      i32.const 0
      return
    end
    local.get $s
    local.get $idx
    i32.add
    i32.load8_u
  )
  (func $__str_substring (export "__str_substring") (param $s i32) (param $start i32) (param $end i32) (result i32)
    (local $subLen i32)
    (local $res i32)
    local.get $end
    local.get $start
    i32.sub
    local.set $subLen
    local.get $subLen
    i32.const 0
    i32.le_s
    if
      i32.const 0
      call $__str_alloc
      return
    end
    local.get $subLen
    call $__str_alloc
    local.set $res
    local.get $res
    local.get $s
    local.get $start
    i32.add
    local.get $subLen
    memory.copy
    local.get $res
    local.get $subLen
    i32.add
    i32.const 0
    i32.store8
    local.get $res
  )
  (func $__str_equals (export "__str_equals") (param $s1 i32) (param $s2 i32) (result i32)
    (local $i i32)
    (local $c1 i32)
    (local $c2 i32)
    local.get $s1
    local.get $s2
    i32.eq
    if
      i32.const 1
      return
    end
    local.get $s1
    i32.eqz
    local.get $s2
    i32.eqz
    i32.or
    if
      i32.const 0
      return
    end
    i32.const 0
    local.set $i
    (block $break_eq
      (loop $loop_eq
        local.get $s1
        local.get $i
        i32.add
        i32.load8_u
        local.set $c1
        local.get $s2
        local.get $i
        i32.add
        i32.load8_u
        local.set $c2
        local.get $c1
        local.get $c2
        i32.ne
        if
          i32.const 0
          return
        end
        local.get $c1
        i32.eqz
        br_if $break_eq
        local.get $i
        i32.const 1
        i32.add
        local.set $i
        br $loop_eq
      )
    )
    i32.const 1
  )
  (func $__str_indexOf (export "__str_indexOf") (param $s i32) (param $sub i32) (result i32)
    (local $len1 i32)
    (local $len2 i32)
    (local $maxI i32)
    (local $i i32)
    (local $j i32)
    (local $match i32)
    local.get $s
    i32.eqz
    local.get $sub
    i32.eqz
    i32.or
    if
      i32.const -1
      return
    end
    local.get $s
    call $__str_length
    local.set $len1
    local.get $sub
    call $__str_length
    local.set $len2
    local.get $len2
    i32.eqz
    if
      i32.const 0
      return
    end
    local.get $len1
    local.get $len2
    i32.lt_s
    if
      i32.const -1
      return
    end
    local.get $len1
    local.get $len2
    i32.sub
    local.set $maxI
    i32.const 0
    local.set $i
    (block $break_search
      (loop $loop_search
        local.get $i
        local.get $maxI
        i32.gt_s
        br_if $break_search
        i32.const 1
        local.set $match
        i32.const 0
        local.set $j
        (block $break_inner
          (loop $loop_inner
            local.get $j
            local.get $len2
            i32.ge_s
            br_if $break_inner
            local.get $s
            local.get $i
            i32.add
            local.get $j
            i32.add
            i32.load8_u
            local.get $sub
            local.get $j
            i32.add
            i32.load8_u
            i32.ne
            if
              i32.const 0
              local.set $match
              br $break_inner
            end
            local.get $j
            i32.const 1
            i32.add
            local.set $j
            br $loop_inner
          )
        )
        local.get $match
        if
          local.get $i
          return
        end
        local.get $i
        i32.const 1
        i32.add
        local.set $i
        br $loop_search
      )
    )
    i32.const -1
  )
  ;; --- StringBuilder Runtime Functions ---
  (func $__sb_new (export "__sb_new") (param $initial_cap i32) (result i32)
    (local $sb i32)
    (local $cap i32)
    (local $alloc_bytes i32)
    local.get $initial_cap
    local.set $cap
    local.get $cap
    i32.const 16
    i32.lt_s
    if
      i32.const 16
      local.set $cap
    end
    global.get $__heap_ptr
    local.set $sb
    local.get $cap
    i32.const 8
    i32.add
    i32.const 3
    i32.add
    i32.const -4
    i32.and
    local.set $alloc_bytes
    global.get $__heap_ptr
    local.get $alloc_bytes
    i32.add
    global.set $__heap_ptr
    local.get $sb
    local.get $cap
    i32.store
    local.get $sb
    i32.const 4
    i32.add
    i32.const 0
    i32.store
    local.get $sb
    i32.const 8
    i32.add
    i32.const 0
    i32.store8
    local.get $sb
  )
  (func $__sb_append_str (export "__sb_append_str") (param $sb i32) (param $str i32) (result i32)
    (local $cur_len i32)
    (local $str_len i32)
    local.get $str
    i32.eqz
    if
      local.get $sb
      return
    end
    local.get $sb
    i32.const 4
    i32.add
    i32.load
    local.set $cur_len
    local.get $str
    call $__str_length
    local.set $str_len
    local.get $sb
    i32.const 8
    i32.add
    local.get $cur_len
    i32.add
    local.get $str
    local.get $str_len
    memory.copy
    local.get $sb
    i32.const 4
    i32.add
    local.get $cur_len
    local.get $str_len
    i32.add
    i32.store
    local.get $sb
    i32.const 8
    i32.add
    local.get $cur_len
    local.get $str_len
    i32.add
    i32.add
    i32.const 0
    i32.store8
    local.get $sb
  )
  (func $__sb_append_i32 (export "__sb_append_i32") (param $sb i32) (param $val i32) (result i32)
    local.get $sb
    local.get $val
    call $__str_from_i32
    call $__sb_append_str
  )
  (func $__sb_append_char (export "__sb_append_char") (param $sb i32) (param $c i32) (result i32)
    local.get $sb
    local.get $c
    call $__str_from_char
    call $__sb_append_str
  )
  (func $__sb_append_bool (export "__sb_append_bool") (param $sb i32) (param $b i32) (result i32)
    local.get $sb
    local.get $b
    call $__str_from_bool
    call $__sb_append_str
  )
  (func $__sb_toString (export "__sb_toString") (param $sb i32) (result i32)
    (local $cur_len i32)
    (local $res i32)
    local.get $sb
    i32.const 4
    i32.add
    i32.load
    local.set $cur_len
    local.get $cur_len
    call $__str_alloc
    local.set $res
    local.get $res
    local.get $sb
    i32.const 8
    i32.add
    local.get $cur_len
    memory.copy
    local.get $res
    local.get $cur_len
    i32.add
    i32.const 0
    i32.store8
    local.get $res
  )
  (func $__sb_length (export "__sb_length") (param $sb i32) (result i32)
    local.get $sb
    i32.const 4
    i32.add
    i32.load
  )
  (func $__sb_charAt (export "__sb_charAt") (param $sb i32) (param $idx i32) (result i32)
    local.get $sb
    i32.const 8
    i32.add
    local.get $idx
    i32.add
    i32.load8_u
  )
  (func $__sb_setCharAt (export "__sb_setCharAt") (param $sb i32) (param $idx i32) (param $c i32)
    local.get $sb
    i32.const 8
    i32.add
    local.get $idx
    i32.add
    local.get $c
    i32.store8
  )
  ;; --- Array Runtime Functions (Bulk Memory) ---
  (func $__array_fill_i32 (export "__array_fill_i32") (param $arr i32) (param $from i32) (param $to i32) (param $val i32)
    (local $i i32)
    local.get $arr
    i32.eqz
    if return end
    local.get $val
    i32.eqz
    if
      local.get $arr
      i32.const 4
      i32.add
      local.get $from
      i32.const 2
      i32.shl
      i32.add
      i32.const 0
      local.get $to
      local.get $from
      i32.sub
      i32.const 2
      i32.shl
      memory.fill
      return
    end
    local.get $from
    local.set $i
    (block $break_fill_i32
      (loop $loop_fill_i32
        local.get $i
        local.get $to
        i32.ge_s
        br_if $break_fill_i32
        local.get $arr
        i32.const 4
        i32.add
        local.get $i
        i32.const 2
        i32.shl
        i32.add
        local.get $val
        i32.store
        local.get $i
        i32.const 1
        i32.add
        local.set $i
        br $loop_fill_i32
      )
    )
  )
  (func $__array_fill_f64 (export "__array_fill_f64") (param $arr i32) (param $from i32) (param $to i32) (param $val f64)
    (local $i i32)
    local.get $arr
    i32.eqz
    if return end
    local.get $val
    f64.const 0.0
    f64.eq
    if
      local.get $arr
      i32.const 8
      i32.add
      local.get $from
      i32.const 3
      i32.shl
      i32.add
      i32.const 0
      local.get $to
      local.get $from
      i32.sub
      i32.const 3
      i32.shl
      memory.fill
      return
    end
    local.get $from
    local.set $i
    (block $break_fill_f64
      (loop $loop_fill_f64
        local.get $i
        local.get $to
        i32.ge_s
        br_if $break_fill_f64
        local.get $arr
        i32.const 8
        i32.add
        local.get $i
        i32.const 3
        i32.shl
        i32.add
        local.get $val
        f64.store
        local.get $i
        i32.const 1
        i32.add
        local.set $i
        br $loop_fill_f64
      )
    )
  )
  (func $__array_copyOf_i32 (export "__array_copyOf_i32") (param $arr i32) (param $newLen i32) (result i32)
    (local $newArr i32)
    (local $oldLen i32)
    (local $copyLen i32)
    (local $totalBytes i32)
    local.get $newLen
    i32.const 2
    i32.shl
    i32.const 4
    i32.add
    local.set $totalBytes
    local.get $totalBytes
    call $__heap_alloc
    local.set $newArr
    local.get $newArr
    local.get $newLen
    i32.store
    local.get $arr
    if
      local.get $arr
      i32.load
      local.set $oldLen
      local.get $oldLen
      local.get $newLen
      i32.lt_s
      if (result i32)
        local.get $oldLen
      else
        local.get $newLen
      end
      local.set $copyLen
      local.get $copyLen
      i32.const 0
      i32.gt_s
      if
        local.get $newArr
        i32.const 4
        i32.add
        local.get $arr
        i32.const 4
        i32.add
        local.get $copyLen
        i32.const 2
        i32.shl
        memory.copy
      end
    end
    local.get $newArr
  )
  (func $__array_copyOf_f64 (export "__array_copyOf_f64") (param $arr i32) (param $newLen i32) (result i32)
    (local $newArr i32)
    (local $oldLen i32)
    (local $copyLen i32)
    (local $totalBytes i32)
    local.get $newLen
    i32.const 3
    i32.shl
    i32.const 8
    i32.add
    local.set $totalBytes
    local.get $totalBytes
    call $__heap_alloc
    local.set $newArr
    local.get $newArr
    local.get $newLen
    i32.store
    local.get $arr
    if
      local.get $arr
      i32.load
      local.set $oldLen
      local.get $oldLen
      local.get $newLen
      i32.lt_s
      if (result i32)
        local.get $oldLen
      else
        local.get $newLen
      end
      local.set $copyLen
      local.get $copyLen
      i32.const 0
      i32.gt_s
      if
        local.get $newArr
        i32.const 8
        i32.add
        local.get $arr
        i32.const 8
        i32.add
        local.get $copyLen
        i32.const 3
        i32.shl
        memory.copy
      end
    end
    local.get $newArr
  )
  (func $__array_copyOfRange_i32 (export "__array_copyOfRange_i32") (param $arr i32) (param $from i32) (param $to i32) (result i32)
    (local $newLen i32)
    (local $newArr i32)
    local.get $to
    local.get $from
    i32.sub
    local.set $newLen
    local.get $newLen
    i32.const 0
    i32.lt_s
    if
      i32.const 0
      local.set $newLen
    end
    local.get $newLen
    i32.const 2
    i32.shl
    i32.const 4
    i32.add
    call $__heap_alloc
    local.set $newArr
    local.get $newArr
    local.get $newLen
    i32.store
    local.get $arr
    if
      local.get $newLen
      i32.const 0
      i32.gt_s
      if
        local.get $newArr
        i32.const 4
        i32.add
        local.get $arr
        i32.const 4
        i32.add
        local.get $from
        i32.const 2
        i32.shl
        i32.add
        local.get $newLen
        i32.const 2
        i32.shl
        memory.copy
      end
    end
    local.get $newArr
  )
  (func $__array_copyOfRange_f64 (export "__array_copyOfRange_f64") (param $arr i32) (param $from i32) (param $to i32) (result i32)
    (local $newLen i32)
    (local $newArr i32)
    local.get $to
    local.get $from
    i32.sub
    local.set $newLen
    local.get $newLen
    i32.const 0
    i32.lt_s
    if
      i32.const 0
      local.set $newLen
    end
    local.get $newLen
    i32.const 3
    i32.shl
    i32.const 8
    i32.add
    call $__heap_alloc
    local.set $newArr
    local.get $newArr
    local.get $newLen
    i32.store
    local.get $arr
    if
      local.get $newLen
      i32.const 0
      i32.gt_s
      if
        local.get $newArr
        i32.const 8
        i32.add
        local.get $arr
        i32.const 8
        i32.add
        local.get $from
        i32.const 3
        i32.shl
        i32.add
        local.get $newLen
        i32.const 3
        i32.shl
        memory.copy
      end
    end
    local.get $newArr
  )
  (func $__array_equals_i32 (export "__array_equals_i32") (param $a i32) (param $b i32) (result i32)
    (local $len i32)
    (local $i i32)
    local.get $a
    local.get $b
    i32.eq
    if
      i32.const 1
      return
    end
    local.get $a
    i32.eqz
    local.get $b
    i32.eqz
    i32.or
    if
      i32.const 0
      return
    end
    local.get $a
    i32.load
    local.get $b
    i32.load
    i32.ne
    if
      i32.const 0
      return
    end
    local.get $a
    i32.load
    local.set $len
    i32.const 0
    local.set $i
    (block $break_eq
      (loop $loop_eq
        local.get $i
        local.get $len
        i32.ge_s
        br_if $break_eq
        local.get $a
        i32.const 4
        i32.add
        local.get $i
        i32.const 2
        i32.shl
        i32.add
        i32.load
        local.get $b
        i32.const 4
        i32.add
        local.get $i
        i32.const 2
        i32.shl
        i32.add
        i32.load
        i32.ne
        if
          i32.const 0
          return
        end
        local.get $i
        i32.const 1
        i32.add
        local.set $i
        br $loop_eq
      )
    )
    i32.const 1
  )
  (func $__array_equals_f64 (export "__array_equals_f64") (param $a i32) (param $b i32) (result i32)
    (local $len i32)
    (local $i i32)
    local.get $a
    local.get $b
    i32.eq
    if
      i32.const 1
      return
    end
    local.get $a
    i32.eqz
    local.get $b
    i32.eqz
    i32.or
    if
      i32.const 0
      return
    end
    local.get $a
    i32.load
    local.get $b
    i32.load
    i32.ne
    if
      i32.const 0
      return
    end
    local.get $a
    i32.load
    local.set $len
    i32.const 0
    local.set $i
    (block $break_eq_f64
      (loop $loop_eq_f64
        local.get $i
        local.get $len
        i32.ge_s
        br_if $break_eq_f64
        local.get $a
        i32.const 8
        i32.add
        local.get $i
        i32.const 3
        i32.shl
        i32.add
        f64.load
        local.get $b
        i32.const 8
        i32.add
        local.get $i
        i32.const 3
        i32.shl
        i32.add
        f64.load
        f64.ne
        if
          i32.const 0
          return
        end
        local.get $i
        i32.const 1
        i32.add
        local.set $i
        br $loop_eq_f64
      )
    )
    i32.const 1
  )
  ;; --- Exception Runtime Functions ---
  (func $Throwable__init (export "Throwable__init") (param $this i32)
    local.get $this
    i32.const 0
    i32.store offset=4
  )
  (func $Throwable__init_1 (export "Throwable__init_1") (param $this i32) (param $message i32)
    local.get $this
    local.get $message
    i32.store offset=4
  )
  (func $Throwable_getMessage (export "Throwable_getMessage") (param $this i32) (result i32)
    local.get $this
    i32.load offset=4
  )
  (func $Exception__init (export "Exception__init") (param $this i32)
    local.get $this
    call $Throwable__init
  )
  (func $Exception__init_1 (export "Exception__init_1") (param $this i32) (param $message i32)
    local.get $this
    local.get $message
    call $Throwable__init_1
  )
  (func $Exception_getMessage (export "Exception_getMessage") (param $this i32) (result i32)
    local.get $this
    call $Throwable_getMessage
  )
  (func $RuntimeException__init (export "RuntimeException__init") (param $this i32)
    local.get $this
    call $Exception__init
  )
  (func $RuntimeException__init_1 (export "RuntimeException__init_1") (param $this i32) (param $message i32)
    local.get $this
    local.get $message
    call $Exception__init_1
  )
  (func $RuntimeException_getMessage (export "RuntimeException_getMessage") (param $this i32) (result i32)
    local.get $this
    call $Exception_getMessage
  )
  (func $IllegalArgumentException__init (export "IllegalArgumentException__init") (param $this i32)
    local.get $this
    call $RuntimeException__init
  )
  (func $IllegalArgumentException__init_1 (export "IllegalArgumentException__init_1") (param $this i32) (param $message i32)
    local.get $this
    local.get $message
    call $RuntimeException__init_1
  )
  (func $IllegalArgumentException_getMessage (export "IllegalArgumentException_getMessage") (param $this i32) (result i32)
    local.get $this
    call $RuntimeException_getMessage
  )
  (func $IllegalStateException__init (export "IllegalStateException__init") (param $this i32)
    local.get $this
    call $RuntimeException__init
  )
  (func $IllegalStateException__init_1 (export "IllegalStateException__init_1") (param $this i32) (param $message i32)
    local.get $this
    local.get $message
    call $RuntimeException__init_1
  )
  (func $IllegalStateException_getMessage (export "IllegalStateException_getMessage") (param $this i32) (result i32)
    local.get $this
    call $RuntimeException_getMessage
  )
  (func $NullPointerException__init (export "NullPointerException__init") (param $this i32)
    local.get $this
    call $RuntimeException__init
  )
  (func $NullPointerException__init_1 (export "NullPointerException__init_1") (param $this i32) (param $message i32)
    local.get $this
    local.get $message
    call $RuntimeException__init_1
  )
  (func $NullPointerException_getMessage (export "NullPointerException_getMessage") (param $this i32) (result i32)
    local.get $this
    call $RuntimeException_getMessage
  )
  (func $WASIFile__init (export "WASIFile__init") (param $this i32) (param $path i32)
    local.get $this
    local.get $path
    i32.store offset=4
  )
  ;; --- WASI Runtime Helpers ---
  (func $__wasi_print_str (export "__wasi_print_str") (param $fd i32) (param $str_ptr i32) (param $str_len i32) (param $newline i32)
    (local $num_iovs i32)
    i32.const 4
    i32.const 10
    i32.store8
    i32.const 8
    local.get $str_ptr
    i32.store
    i32.const 12
    local.get $str_len
    i32.store
    i32.const 16
    i32.const 4
    i32.store
    i32.const 20
    i32.const 1
    i32.store
    local.get $newline
    if
      i32.const 2
      local.set $num_iovs
    else
      i32.const 1
      local.set $num_iovs
    end
    local.get $fd
    i32.const 8
    local.get $num_iovs
    i32.const 24
    call $__wasi_fd_write
    drop
  )
  (func $__wasi_print_int (export "__wasi_print_int") (param $fd i32) (param $val i32) (param $newline i32)
    (local $s i32)
    local.get $val
    call $__str_from_i32
    local.set $s
    local.get $fd
    local.get $s
    local.get $s
    call $__str_length
    local.get $newline
    call $__wasi_print_str
  )
  (func $__wasi_print_double (export "__wasi_print_double") (param $fd i32) (param $val f64) (param $newline i32)
    (local $s i32)
    local.get $val
    call $__str_from_f64
    local.set $s
    local.get $fd
    local.get $s
    local.get $s
    call $__str_length
    local.get $newline
    call $__wasi_print_str
  )
  (func $__wasi_print_bool (export "__wasi_print_bool") (param $fd i32) (param $val i32) (param $newline i32)
    (local $s i32)
    local.get $val
    call $__str_from_bool
    local.set $s
    local.get $fd
    local.get $s
    local.get $s
    call $__str_length
    local.get $newline
    call $__wasi_print_str
  )
  (func $__wasi_print_char (export "__wasi_print_char") (param $fd i32) (param $c i32) (param $newline i32)
    i32.const 0
    local.get $c
    i32.store8
    local.get $fd
    i32.const 0
    i32.const 1
    local.get $newline
    call $__wasi_print_str
  )
  (func $__wasi_println_empty (export "__wasi_println_empty") (param $fd i32)
    local.get $fd
    i32.const 4
    i32.const 0
    i32.const 1
    call $__wasi_print_str
  )
  (func $__wasi_read_char (export "__wasi_read_char") (result i32)
    (local $nread i32)
    i32.const 8
    i32.const 0
    i32.store
    i32.const 12
    i32.const 1
    i32.store
    i32.const 0
    i32.const 8
    i32.const 1
    i32.const 24
    call $__wasi_fd_read
    drop
    i32.const 24
    i32.load
    local.set $nread
    local.get $nread
    i32.const 0
    i32.le_s
    if (result i32)
      i32.const -1
    else
      i32.const 0
      i32.load8_u
    end
  )
  (func $__wasi_read_line (export "__wasi_read_line") (param $prompt_ptr i32) (result i32)
    (local $buf i32)
    (local $cap i32)
    (local $len i32)
    (local $c i32)
    (local $res i32)
    local.get $prompt_ptr
    i32.const 0
    i32.gt_s
    if
      i32.const 1
      local.get $prompt_ptr
      local.get $prompt_ptr
      call $__str_length
      i32.const 0
      call $__wasi_print_str
    end
    i32.const 512
    local.set $cap
    local.get $cap
    call $__heap_alloc
    local.set $buf
    i32.const 0
    local.set $len
    block $exit_loop
      loop $read_loop
        call $__wasi_read_char
        local.set $c
        local.get $c
        i32.const -1
        i32.eq
        br_if $exit_loop
        local.get $c
        i32.const 10
        i32.eq
        br_if $exit_loop
        local.get $c
        i32.const 13
        i32.eq
        if
          br $read_loop
        end
        local.get $buf
        local.get $len
        i32.add
        local.get $c
        i32.store8
        local.get $len
        i32.const 1
        i32.add
        local.set $len
        local.get $len
        local.get $cap
        i32.ge_s
        br_if $exit_loop
        br $read_loop
      end
    end
    local.get $len
    call $__str_alloc
    local.set $res
    local.get $res
    local.get $buf
    local.get $len
    memory.copy
    local.get $res
    local.get $len
    i32.add
    i32.const 0
    i32.store8
    local.get $res
  )
  (func $__wasi_path_open_helper (export "__wasi_path_open_helper") (param $path_ptr i32) (param $oflags i32) (result i32)
    (local $len i32)
    (local $res i32)
    local.get $path_ptr
    call $__str_length
    local.set $len
    i32.const 3
    i32.const 0
    local.get $path_ptr
    local.get $len
    local.get $oflags
    i64.const -1
    i64.const -1
    i32.const 0
    i32.const 28
    call $__wasi_path_open
    local.set $res
    local.get $res
    i32.eqz
    if (result i32)
      i32.const 28
      i32.load
    else
      i32.const -1
    end
  )
  (func $__wasi_read_file_str (export "__wasi_read_file_str") (param $path_ptr i32) (result i32)
    (local $fd i32)
    (local $size i32)
    (local $res i32)
    (local $nread i32)
    local.get $path_ptr
    i32.const 0
    call $__wasi_path_open_helper
    local.set $fd
    local.get $fd
    i32.const 0
    i32.lt_s
    if
      i32.const 0
      call $__str_alloc
      return
    end
    local.get $fd
    i32.const 32
    call $__wasi_fd_filestat_get
    drop
    i32.const 64
    i32.load
    local.set $size
    local.get $size
    i32.const 0
    i32.le_s
    if
      i32.const 4096
      local.set $size
    end
    local.get $size
    call $__str_alloc
    local.set $res
    i32.const 8
    local.get $res
    i32.store
    i32.const 12
    local.get $size
    i32.store
    local.get $fd
    i32.const 8
    i32.const 1
    i32.const 24
    call $__wasi_fd_read
    drop
    i32.const 24
    i32.load
    local.set $nread
    local.get $res
    local.get $nread
    i32.add
    i32.const 0
    i32.store8
    local.get $fd
    call $__wasi_fd_close
    drop
    local.get $res
  )
  (func $__wasi_write_file_str (export "__wasi_write_file_str") (param $path_ptr i32) (param $str_ptr i32) (param $append i32) (result i32)
    (local $fd i32)
    (local $str_len i32)
    (local $oflags i32)
    local.get $append
    if (result i32)
      i32.const 17
    else
      i32.const 9
    end
    local.set $oflags
    local.get $path_ptr
    local.get $oflags
    call $__wasi_path_open_helper
    local.set $fd
    local.get $fd
    i32.const 0
    i32.lt_s
    if
      i32.const 0
      return
    end
    local.get $str_ptr
    call $__str_length
    local.set $str_len
    local.get $append
    if
      local.get $fd
      i64.const 0
      i32.const 2
      i32.const 24
      call $__wasi_fd_seek
      drop
    end
    i32.const 8
    local.get $str_ptr
    i32.store
    i32.const 12
    local.get $str_len
    i32.store
    local.get $fd
    i32.const 8
    i32.const 1
    i32.const 24
    call $__wasi_fd_write
    drop
    local.get $fd
    call $__wasi_fd_close
    drop
    i32.const 1
  )
  (func $__wasi_file_exists (export "__wasi_file_exists") (param $path_ptr i32) (result i32)
    (local $fd i32)
    local.get $path_ptr
    i32.const 0
    call $__wasi_path_open_helper
    local.set $fd
    local.get $fd
    i32.const 0
    i32.lt_s
    if
      i32.const 0
      return
    end
    local.get $fd
    call $__wasi_fd_close
    drop
    i32.const 1
  )
  (func $__wasi_file_length (export "__wasi_file_length") (param $path_ptr i32) (result i32)
    (local $fd i32)
    (local $len i32)
    local.get $path_ptr
    i32.const 0
    call $__wasi_path_open_helper
    local.set $fd
    local.get $fd
    i32.const 0
    i32.lt_s
    if
      i32.const 0
      return
    end
    local.get $fd
    i32.const 32
    call $__wasi_fd_filestat_get
    drop
    i32.const 64
    i32.load
    local.set $len
    local.get $fd
    call $__wasi_fd_close
    drop
    local.get $len
  )
  (func $__wasi_file_delete (export "__wasi_file_delete") (param $path_ptr i32) (result i32)
    (local $len i32)
    (local $res i32)
    local.get $path_ptr
    call $__str_length
    local.set $len
    i32.const 3
    local.get $path_ptr
    local.get $len
    call $__wasi_path_unlink_file
    local.set $res
    local.get $res
    i32.eqz
  )
  (func $__wasi_create_dir (export "__wasi_create_dir") (param $path_ptr i32) (result i32)
    (local $len i32)
    (local $res i32)
    local.get $path_ptr
    call $__str_length
    local.set $len
    i32.const 3
    local.get $path_ptr
    local.get $len
    call $__wasi_path_create_directory
    local.set $res
    local.get $res
    i32.eqz
  )
  (func $__wasi_clock_time_ms (export "__wasi_clock_time_ms") (result f64)
    i32.const 0
    i64.const 1000000
    i32.const 32
    call $__wasi_clock_time_get
    drop
    i32.const 32
    i64.load
    f64.convert_i64_u
    f64.const 1000000.0
    f64.div
  )
  (func $__wasi_random_int (export "__wasi_random_int") (result i32)
    i32.const 32
    i32.const 4
    call $__wasi_random_get
    drop
    i32.const 32
    i32.load
  )
  ;; Class/Interface: TestClass
  (func $onTimerTick (export "onTimerTick")
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    global.get $asyncCounter
    i32.const 10
    i32.add
    global.set $asyncCounter
  )
  (func $onFrameUpdate (export "onFrameUpdate") (param $timestamp f64)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    global.get $asyncCounter
    i32.const 5
    i32.add
    global.set $asyncCounter
  )
  (func $onFetchSuccess (export "onFetchSuccess") (param $data i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    local.get $data
    global.set $asyncFetchedData
  )
  (func $testScheduleTimer (export "testScheduleTimer") (param $delay i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    i32.const 4269
    i32.const 11
    local.get $delay
    call $dom_setTimeout
    return
  )
  (func $testScheduleInterval (export "testScheduleInterval") (param $delay i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    i32.const 4269
    i32.const 11
    local.get $delay
    call $dom_setInterval
    return
  )
  (func $testRequestAnimationFrame (export "testRequestAnimationFrame") (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    i32.const 4281
    i32.const 13
    call $dom_requestAnimationFrame
    return
  )
  (func $testFetchData (export "testFetchData") (param $url i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    local.get $url
    local.set $__str_arg_ptr
    local.get $__str_arg_ptr
    local.get $__str_arg_ptr
    call $__str_length
    i32.const 4295
    i32.const 14
    call $dom_fetch
  )
  (func $testConfirm (export "testConfirm") (param $message i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    local.get $message
    local.set $__str_arg_ptr
    local.get $__str_arg_ptr
    local.get $__str_arg_ptr
    call $__str_length
    call $dom_confirm
    return
  )
  (func $sayHello (export "sayHello")
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
  )
  (func $addNumbers (export "addNumbers") (param $a i32) (param $b i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    local.get $a
    local.get $b
    i32.add
    return
  )
  (func $getPi (export "getPi") (result f64)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    f64.const 3.14159
    return
  )
  (func $calculateFactorial (export "calculateFactorial") (param $n i32) (result i32)
    (local $result i32)
    (local $i i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    i32.const 1
    local.set $result
    i32.const 1
    local.set $i
    (block $break_for_1
      (loop $loop_for_1
    local.get $i
    local.get $n
    i32.le_s
        i32.eqz
        br_if $break_for_1
        (block $continue_for_1
    local.get $result
    local.get $i
    i32.mul
    local.set $result
        )
    local.get $i
    i32.const 1
    i32.add
    local.set $i
        br $loop_for_1
      )
    )
    local.get $result
    return
  )
  (func $sumRangeWithSkips (export "sumRangeWithSkips") (param $n i32) (result i32)
    (local $sum i32)
    (local $i i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    i32.const 0
    local.set $sum
    i32.const 1
    local.set $i
    (block $break_for_2
      (loop $loop_for_2
    local.get $i
    local.get $n
    i32.le_s
        i32.eqz
        br_if $break_for_2
        (block $continue_for_2
    local.get $i
    i32.const 3
    i32.eq
    if
    br $continue_for_2
    end
    local.get $i
    i32.const 8
    i32.gt_s
    if
    br $break_for_2
    end
    local.get $sum
    local.get $i
    i32.add
    local.set $sum
        )
    local.get $i
    i32.const 1
    i32.add
    local.set $i
        br $loop_for_2
      )
    )
    local.get $sum
    return
  )
  (func $sumArray (export "sumArray") (param $numbers i32) (result i32)
    (local $total i32)
    (local $n i32)
    (local $__arr_ptr_1 i32)
    (local $__arr_len_1 i32)
    (local $__arr_idx_1 i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    i32.const 0
    local.set $total
    local.get $numbers
    local.set $__arr_ptr_1
    local.get $__arr_ptr_1
    i32.load
    local.set $__arr_len_1
    i32.const 0
    local.set $__arr_idx_1
    (block $break_foreach_3
      (loop $loop_foreach_3
        local.get $__arr_idx_1
        local.get $__arr_len_1
        i32.ge_s
        br_if $break_foreach_3
        local.get $__arr_ptr_1
        local.get $__arr_idx_1
        i32.const 4
        i32.mul
        i32.add
        i32.load offset=4
        local.set $n
        (block $continue_foreach_3
    local.get $total
    local.get $n
    i32.add
    local.set $total
        )
        local.get $__arr_idx_1
        i32.const 1
        i32.add
        local.set $__arr_idx_1
        br $loop_foreach_3
      )
    )
    local.get $total
    return
  )
  (func $sumArrayWithSkips (export "sumArrayWithSkips") (param $numbers i32) (result i32)
    (local $total i32)
    (local $n i32)
    (local $__arr_ptr_1 i32)
    (local $__arr_len_1 i32)
    (local $__arr_idx_1 i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    i32.const 0
    local.set $total
    local.get $numbers
    local.set $__arr_ptr_1
    local.get $__arr_ptr_1
    i32.load
    local.set $__arr_len_1
    i32.const 0
    local.set $__arr_idx_1
    (block $break_foreach_4
      (loop $loop_foreach_4
        local.get $__arr_idx_1
        local.get $__arr_len_1
        i32.ge_s
        br_if $break_foreach_4
        local.get $__arr_ptr_1
        local.get $__arr_idx_1
        i32.const 4
        i32.mul
        i32.add
        i32.load offset=4
        local.set $n
        (block $continue_foreach_4
    local.get $n
    i32.const 3
    i32.eq
    if
    br $continue_foreach_4
    end
    local.get $n
    i32.const 8
    i32.gt_s
    if
    br $break_foreach_4
    end
    local.get $total
    local.get $n
    i32.add
    local.set $total
        )
        local.get $__arr_idx_1
        i32.const 1
        i32.add
        local.set $__arr_idx_1
        br $loop_foreach_4
      )
    )
    local.get $total
    return
  )
  (func $sumDoubleArray (export "sumDoubleArray") (param $values i32) (result f64)
    (local $total f64)
    (local $v f64)
    (local $__arr_ptr_1 i32)
    (local $__arr_len_1 i32)
    (local $__arr_idx_1 i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    f64.const 0.0
    local.set $total
    local.get $values
    local.set $__arr_ptr_1
    local.get $__arr_ptr_1
    i32.load
    local.set $__arr_len_1
    i32.const 0
    local.set $__arr_idx_1
    (block $break_foreach_5
      (loop $loop_foreach_5
        local.get $__arr_idx_1
        local.get $__arr_len_1
        i32.ge_s
        br_if $break_foreach_5
        local.get $__arr_ptr_1
        local.get $__arr_idx_1
        i32.const 8
        i32.mul
        i32.add
        f64.load offset=8
        local.set $v
        (block $continue_foreach_5
    local.get $total
    local.get $v
    f64.add
    local.set $total
        )
        local.get $__arr_idx_1
        i32.const 1
        i32.add
        local.set $__arr_idx_1
        br $loop_foreach_5
      )
    )
    local.get $total
    return
  )
  (func $sumInlineArray (export "sumInlineArray") (result i32)
    (local $total i32)
    (local $x i32)
    (local $__arr_ptr_1 i32)
    (local $__arr_len_1 i32)
    (local $__arr_idx_1 i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    i32.const 0
    local.set $total
    global.get $__heap_ptr
    local.set $__arr_alloc_ptr
    global.get $__heap_ptr
    i32.const 24
    i32.add
    global.set $__heap_ptr
    local.get $__arr_alloc_ptr
    i32.const 4
    i32.store
    local.get $__arr_alloc_ptr
    i32.const 10
    i32.store offset=4
    local.get $__arr_alloc_ptr
    i32.const 20
    i32.store offset=8
    local.get $__arr_alloc_ptr
    i32.const 30
    i32.store offset=12
    local.get $__arr_alloc_ptr
    i32.const 40
    i32.store offset=16
    local.get $__arr_alloc_ptr
    local.set $__arr_ptr_1
    local.get $__arr_ptr_1
    i32.load
    local.set $__arr_len_1
    i32.const 0
    local.set $__arr_idx_1
    (block $break_foreach_6
      (loop $loop_foreach_6
        local.get $__arr_idx_1
        local.get $__arr_len_1
        i32.ge_s
        br_if $break_foreach_6
        local.get $__arr_ptr_1
        local.get $__arr_idx_1
        i32.const 4
        i32.mul
        i32.add
        i32.load offset=4
        local.set $x
        (block $continue_foreach_6
    local.get $total
    local.get $x
    i32.add
    local.set $total
        )
        local.get $__arr_idx_1
        i32.const 1
        i32.add
        local.set $__arr_idx_1
        br $loop_foreach_6
      )
    )
    local.get $total
    return
  )
  (func $createAndSumArray (export "createAndSumArray") (param $a i32) (param $b i32) (param $c i32) (result i32)
    (local $arr i32)
    (local $sum i32)
    (local $x i32)
    (local $__arr_ptr_1 i32)
    (local $__arr_len_1 i32)
    (local $__arr_idx_1 i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    global.get $__heap_ptr
    local.set $__arr_alloc_ptr
    global.get $__heap_ptr
    i32.const 16
    i32.add
    global.set $__heap_ptr
    local.get $__arr_alloc_ptr
    i32.const 3
    i32.store
    local.get $__arr_alloc_ptr
    local.get $a
    i32.store offset=4
    local.get $__arr_alloc_ptr
    local.get $b
    i32.store offset=8
    local.get $__arr_alloc_ptr
    local.get $c
    i32.store offset=12
    local.get $__arr_alloc_ptr
    local.set $arr
    i32.const 0
    local.set $sum
    local.get $arr
    local.set $__arr_ptr_1
    local.get $__arr_ptr_1
    i32.load
    local.set $__arr_len_1
    i32.const 0
    local.set $__arr_idx_1
    (block $break_foreach_7
      (loop $loop_foreach_7
        local.get $__arr_idx_1
        local.get $__arr_len_1
        i32.ge_s
        br_if $break_foreach_7
        local.get $__arr_ptr_1
        local.get $__arr_idx_1
        i32.const 4
        i32.mul
        i32.add
        i32.load offset=4
        local.set $x
        (block $continue_foreach_7
    local.get $sum
    local.get $x
    i32.add
    local.set $sum
        )
        local.get $__arr_idx_1
        i32.const 1
        i32.add
        local.set $__arr_idx_1
        br $loop_foreach_7
      )
    )
    local.get $sum
    return
  )
  (func $evaluateSwitch (export "evaluateSwitch") (param $code i32) (result i32)
    (local $result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__switch_val_1 i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    i32.const 0
    local.set $result
    local.get $code
    local.set $__switch_val_1
    (block $break_switch_8
      (block $case_8_3
      (block $case_8_2
      (block $case_8_1
      (block $case_8_0
        local.get $__switch_val_1
    i32.const 1
        i32.eq
        br_if $case_8_0
        local.get $__switch_val_1
    i32.const 2
        i32.eq
        br_if $case_8_1
        local.get $__switch_val_1
    i32.const 3
        i32.eq
        br_if $case_8_2
        br $case_8_3
        br $break_switch_8
      ) ;; end block $case_8_0
    i32.const 100
    local.set $result
    br $break_switch_8
      ) ;; end block $case_8_1
    i32.const 200
    local.set $result
    br $break_switch_8
      ) ;; end block $case_8_2
    i32.const 300
    local.set $result
    br $break_switch_8
      ) ;; end block $case_8_3
    i32.const 0
    i32.const 1
    i32.sub
    local.set $result
    br $break_switch_8
    )
    local.get $result
    return
  )
  (func $switchFallthrough (export "switchFallthrough") (param $code i32) (result i32)
    (local $result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__switch_val_1 i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    i32.const 0
    local.set $result
    local.get $code
    local.set $__switch_val_1
    (block $break_switch_9
      (block $case_9_3
      (block $case_9_2
      (block $case_9_1
      (block $case_9_0
        local.get $__switch_val_1
    i32.const 1
        i32.eq
        br_if $case_9_0
        local.get $__switch_val_1
    i32.const 2
        i32.eq
        br_if $case_9_1
        local.get $__switch_val_1
    i32.const 3
        i32.eq
        br_if $case_9_2
        br $case_9_3
        br $break_switch_9
      ) ;; end block $case_9_0
    local.get $result
    i32.const 10
    i32.add
    local.set $result
      ) ;; end block $case_9_1
    local.get $result
    i32.const 20
    i32.add
    local.set $result
    br $break_switch_9
      ) ;; end block $case_9_2
    local.get $result
    i32.const 30
    i32.add
    local.set $result
    br $break_switch_9
      ) ;; end block $case_9_3
    local.get $result
    i32.const 5
    i32.add
    local.set $result
    br $break_switch_9
    )
    local.get $result
    return
  )
  (func $switchInsideLoop (export "switchInsideLoop") (param $limit i32) (result i32)
    (local $total i32)
    (local $i i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__switch_val_1 i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    i32.const 0
    local.set $total
    i32.const 1
    local.set $i
    (block $break_for_10
      (loop $loop_for_10
    local.get $i
    local.get $limit
    i32.le_s
        i32.eqz
        br_if $break_for_10
        (block $continue_for_10
    local.get $i
    local.set $__switch_val_1
    (block $break_switch_11
      (block $case_11_2
      (block $case_11_1
      (block $case_11_0
        local.get $__switch_val_1
    i32.const 2
        i32.eq
        br_if $case_11_0
        local.get $__switch_val_1
    i32.const 4
        i32.eq
        br_if $case_11_1
        br $case_11_2
        br $break_switch_11
      ) ;; end block $case_11_0
    local.get $total
    i32.const 20
    i32.add
    local.set $total
    br $break_switch_11
      ) ;; end block $case_11_1
    local.get $total
    i32.const 40
    i32.add
    local.set $total
    br $break_switch_11
      ) ;; end block $case_11_2
    local.get $total
    i32.const 1
    i32.add
    local.set $total
    br $break_switch_11
    )
        )
    local.get $i
    i32.const 1
    i32.add
    local.set $i
        br $loop_for_10
      )
    )
    local.get $total
    return
  )
  (func $labeledBreakNestedLoops (export "labeledBreakNestedLoops") (param $maxI i32) (param $maxJ i32) (result i32)
    (local $count i32)
    (local $i i32)
    (local $j i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    i32.const 0
    local.set $count
    i32.const 0
    local.set $i
    (block $break_for_12
      (loop $loop_for_12
    local.get $i
    local.get $maxI
    i32.lt_s
        i32.eqz
        br_if $break_for_12
        (block $continue_for_12
    i32.const 0
    local.set $j
    (block $break_for_13
      (loop $loop_for_13
    local.get $j
    local.get $maxJ
    i32.lt_s
        i32.eqz
        br_if $break_for_13
        (block $continue_for_13
    local.get $i
    i32.const 2
    i32.eq
    local.get $j
    i32.const 1
    i32.eq
    i32.and
    if
    br $break_for_12
    end
    local.get $count
    i32.const 1
    i32.add
    local.set $count
        )
    local.get $j
    i32.const 1
    i32.add
    local.set $j
        br $loop_for_13
      )
    )
        )
    local.get $i
    i32.const 1
    i32.add
    local.set $i
        br $loop_for_12
      )
    )
    local.get $count
    return
  )
  (func $labeledContinueNestedLoops (export "labeledContinueNestedLoops") (param $maxI i32) (param $maxJ i32) (result i32)
    (local $count i32)
    (local $i i32)
    (local $j i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    i32.const 0
    local.set $count
    i32.const 0
    local.set $i
    (block $break_for_14
      (loop $loop_for_14
    local.get $i
    local.get $maxI
    i32.lt_s
        i32.eqz
        br_if $break_for_14
        (block $continue_for_14
    i32.const 0
    local.set $j
    (block $break_for_15
      (loop $loop_for_15
    local.get $j
    local.get $maxJ
    i32.lt_s
        i32.eqz
        br_if $break_for_15
        (block $continue_for_15
    local.get $j
    i32.const 2
    i32.eq
    if
    br $continue_for_14
    end
    local.get $count
    i32.const 1
    i32.add
    local.set $count
        )
    local.get $j
    i32.const 1
    i32.add
    local.set $j
        br $loop_for_15
      )
    )
        )
    local.get $i
    i32.const 1
    i32.add
    local.set $i
        br $loop_for_14
      )
    )
    local.get $count
    return
  )
  (func $labeledBlockBreak (export "labeledBlockBreak") (param $flag i32) (result i32)
    (local $res i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    i32.const 10
    local.set $res
    (block $break_label_16
    local.get $flag
    i32.const 1
    i32.eq
    if
    local.get $res
    i32.const 5
    i32.add
    local.set $res
    br $break_label_16
    end
    local.get $res
    i32.const 20
    i32.add
    local.set $res
    )
    local.get $res
    return
  )
  (func $labeledWhileBreak (export "labeledWhileBreak") (param $limit i32) (result i32)
    (local $sum i32)
    (local $i i32)
    (local $j i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    i32.const 0
    local.set $sum
    i32.const 0
    local.set $i
    (block $break_while_17
      (loop $loop_while_17
    local.get $i
    local.get $limit
    i32.lt_s
        i32.eqz
        br_if $break_while_17
    i32.const 0
    local.set $j
    (block $break_while_18
      (loop $loop_while_18
    local.get $j
    local.get $limit
    i32.lt_s
        i32.eqz
        br_if $break_while_18
    local.get $i
    i32.const 1
    i32.eq
    local.get $j
    i32.const 1
    i32.eq
    i32.and
    if
    br $break_while_17
    end
    local.get $sum
    i32.const 1
    i32.add
    local.set $sum
    local.get $j
    i32.const 1
    i32.add
    local.set $j
        br $loop_while_18
      )
    )
    local.get $i
    i32.const 1
    i32.add
    local.set $i
        br $loop_while_17
      )
    )
    local.get $sum
    return
  )
  (func $concatStrings (export "concatStrings") (result i32)
    (local $greeting i32)
    (local $target i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    i32.const 4310
    local.set $greeting
    i32.const 4316
    local.set $target
    local.get $greeting
    i32.const 4328
    call $__str_concat
    local.get $target
    call $__str_concat
    i32.const 4331
    call $__str_concat
    return
  )
  (func $formatUser (export "formatUser") (param $name i32) (param $age i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    i32.const 4333
    local.get $name
    call $__str_concat
    i32.const 4340
    call $__str_concat
    local.get $age
    call $__str_from_i32
    call $__str_concat
    i32.const 4348
    call $__str_concat
    return
  )
  (func $getStringLength (export "getStringLength") (result i32)
    (local $s i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    i32.const 4316
    local.set $s
    local.get $s
    call $__str_length
    return
  )
  (func $getCharAtIndex (export "getCharAtIndex") (param $idx i32) (result i32)
    (local $s i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    i32.const 4316
    local.set $s
    local.get $s
    local.get $idx
    call $__str_charAt
    return
  )
  (func $getSubString (export "getSubString") (param $start i32) (param $end i32) (result i32)
    (local $s i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    i32.const 4350
    local.set $s
    local.get $s
    local.get $start
    local.get $end
    call $__str_substring
    return
  )
  (func $compareStrings (export "compareStrings") (param $a i32) (param $b i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    local.get $a
    local.get $b
    call $__str_equals
    if
    i32.const 1
    return
    end
    i32.const 0
    return
  )
  (func $findSubstringIndex (export "findSubstringIndex") (result i32)
    (local $s i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    i32.const 4350
    local.set $s
    local.get $s
    i32.const 4362
    call $__str_indexOf
    return
  )
  (func $findMissingSubstringIndex (export "findMissingSubstringIndex") (result i32)
    (local $s i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    i32.const 4350
    local.set $s
    local.get $s
    i32.const 4368
    call $__str_indexOf
    return
  )
  (func $buildCustomString (export "buildCustomString") (param $count i32) (result i32)
    (local $sb i32)
    (local $i i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    i32.const 16
    call $__sb_new
    local.set $__sb_tmp
    local.get $__sb_tmp
    i32.const 4373
    call $__sb_append_str
    drop
    local.get $__sb_tmp
    local.set $sb
    i32.const 0
    local.set $i
    (block $break_for_19
      (loop $loop_for_19
    local.get $i
    local.get $count
    i32.lt_s
        i32.eqz
        br_if $break_for_19
        (block $continue_for_19
    local.get $sb
    local.get $i
    call $__sb_append_i32
    drop
    local.get $sb
    i32.const 32
    call $__sb_append_char
    drop
        )
    local.get $i
    i32.const 1
    i32.add
    local.set $i
        br $loop_for_19
      )
    )
    local.get $sb
    call $__sb_toString
    return
  )
  (func $manipulateStringBuilder (export "manipulateStringBuilder") (result i32)
    (local $sb i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    i32.const 16
    call $__sb_new
    local.set $__sb_tmp
    local.get $__sb_tmp
    i32.const 4368
    call $__sb_append_str
    drop
    local.get $__sb_tmp
    local.set $sb
    local.get $sb
    i32.const 0
    i32.const 106
    call $__sb_setCharAt
    local.get $sb
    i32.const 119
    call $__sb_append_char
    drop
    local.get $sb
    call $__sb_toString
    return
  )
  (func $testPointObject (export "testPointObject") (param $x i32) (param $y i32) (result i32)
    (local $p i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    i32.const 16
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 4
    i32.store offset=0
    local.get $__obj_alloc_ptr
    local.get $x
    local.get $y
    call $Point__init
    local.get $__obj_alloc_ptr
    local.set $p
    local.get $p
    local.set $__obj_tmp
    local.get $__obj_tmp
    local.get $x
    i32.const 1
    i32.add
    local.get $y
    i32.const 2
    i32.add
    local.get $__obj_tmp
    i32.load offset=0
    i32.const 2
    i32.add
    call_indirect (type $t_sig_2)
    local.get $p
    local.set $__obj_tmp
    local.get $__obj_tmp
    local.get $__obj_tmp
    i32.load offset=0
    i32.const 3
    i32.add
    call_indirect (type $t_sig_1)
    return
  )
  (func $testDirectFieldAccess (export "testDirectFieldAccess") (param $x i32) (param $y i32) (result i32)
    (local $p i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    i32.const 16
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 4
    i32.store offset=0
    local.get $__obj_alloc_ptr
    local.get $x
    local.get $y
    call $Point__init
    local.get $__obj_alloc_ptr
    local.set $p
    local.get $p
    local.set $__obj_tmp
    local.get $__obj_tmp
    local.get $__obj_tmp
    i32.load offset=4
    i32.const 5
    i32.add
    i32.store offset=4
    local.get $p
    local.set $__obj_tmp
    local.get $__obj_tmp
    local.get $__obj_tmp
    i32.load offset=8
    i32.const 10
    i32.add
    i32.store offset=8
    local.get $p
    i32.load offset=4
    local.get $p
    i32.load offset=8
    i32.add
    return
  )
  (func $testPolymorphismArea (export "testPolymorphismArea") (param $type i32) (param $param1 i32) (param $param2 i32) (result i32)
    (local $s i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    local.get $type
    i32.const 1
    i32.eq
    if
    i32.const 16
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 10
    i32.store offset=0
    local.get $__obj_alloc_ptr
    i32.const 100
    local.get $param1
    call $Circle__init
    local.get $__obj_alloc_ptr
    local.set $s
    else
    i32.const 16
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 13
    i32.store offset=0
    local.get $__obj_alloc_ptr
    i32.const 200
    local.get $param1
    local.get $param2
    call $Rectangle__init
    local.get $__obj_alloc_ptr
    local.set $s
    end
    local.get $s
    local.set $__obj_tmp
    local.get $__obj_tmp
    local.get $__obj_tmp
    i32.load offset=0
    i32.const 1
    i32.add
    call_indirect (type $t_sig_1)
    return
  )
  (func $testInheritedColor (export "testInheritedColor") (param $type i32) (param $color i32) (param $param1 i32) (param $param2 i32) (result i32)
    (local $s i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    local.get $type
    i32.const 1
    i32.eq
    if
    i32.const 16
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 10
    i32.store offset=0
    local.get $__obj_alloc_ptr
    local.get $color
    local.get $param1
    call $Circle__init
    local.get $__obj_alloc_ptr
    local.set $s
    else
    i32.const 16
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 13
    i32.store offset=0
    local.get $__obj_alloc_ptr
    local.get $color
    local.get $param1
    local.get $param2
    call $Rectangle__init
    local.get $__obj_alloc_ptr
    local.set $s
    end
    local.get $s
    local.set $__obj_tmp
    local.get $__obj_tmp
    local.get $__obj_tmp
    i32.load offset=0
    i32.const 0
    i32.add
    call_indirect (type $t_sig_1)
    return
  )
  (func $testInstanceOf (export "testInstanceOf") (param $type i32) (result i32)
    (local $s i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    local.get $type
    i32.const 1
    i32.eq
    if
    i32.const 16
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 10
    i32.store offset=0
    local.get $__obj_alloc_ptr
    i32.const 10
    i32.const 5
    call $Circle__init
    local.get $__obj_alloc_ptr
    local.set $s
    else
    i32.const 16
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 13
    i32.store offset=0
    local.get $__obj_alloc_ptr
    i32.const 20
    i32.const 4
    i32.const 6
    call $Rectangle__init
    local.get $__obj_alloc_ptr
    local.set $s
    end
    local.get $s
    local.set $__obj_tmp
    local.get $__obj_tmp
    i32.eqz
    if (result i32)
      i32.const 0
    else
      local.get $__obj_tmp
      i32.load offset=0
      local.set $__vtable_tmp
      i32.const 0
      local.get $__vtable_tmp
      i32.const 10
      i32.eq
      i32.or
    end
    if
    i32.const 1
    return
    else
    local.get $s
    local.set $__obj_tmp
    local.get $__obj_tmp
    i32.eqz
    if (result i32)
      i32.const 0
    else
      local.get $__obj_tmp
      i32.load offset=0
      local.set $__vtable_tmp
      i32.const 0
      local.get $__vtable_tmp
      i32.const 13
      i32.eq
      i32.or
    end
    if
    i32.const 2
    return
    end
    end
    i32.const 0
    return
  )
  (func $testObjectArray (export "testObjectArray") (result i32)
    (local $pts i32)
    (local $totalDist i32)
    (local $p i32)
    (local $__arr_ptr_1 i32)
    (local $__arr_len_1 i32)
    (local $__arr_idx_1 i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    i32.const 3
    local.set $__arr_alloc_len
    global.get $__heap_ptr
    local.set $__arr_alloc_ptr
    local.get $__arr_alloc_ptr
    local.get $__arr_alloc_len
    i32.store
    global.get $__heap_ptr
    i32.const 11
    local.get $__arr_alloc_len
    i32.const 4
    i32.mul
    i32.add
    i32.const -8
    i32.and
    i32.add
    global.set $__heap_ptr
    local.get $__arr_alloc_ptr
    local.set $pts
    local.get $pts
    i32.const 0
    i32.const 4
    i32.mul
    i32.add
    i32.const 16
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 4
    i32.store offset=0
    local.get $__obj_alloc_ptr
    i32.const 1
    i32.const 2
    call $Point__init
    local.get $__obj_alloc_ptr
    i32.store offset=4
    local.get $pts
    i32.const 1
    i32.const 4
    i32.mul
    i32.add
    i32.const 16
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 4
    i32.store offset=0
    local.get $__obj_alloc_ptr
    i32.const 3
    i32.const 4
    call $Point__init
    local.get $__obj_alloc_ptr
    i32.store offset=4
    local.get $pts
    i32.const 2
    i32.const 4
    i32.mul
    i32.add
    i32.const 16
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 4
    i32.store offset=0
    local.get $__obj_alloc_ptr
    i32.const 5
    i32.const 6
    call $Point__init
    local.get $__obj_alloc_ptr
    i32.store offset=4
    i32.const 0
    local.set $totalDist
    local.get $pts
    local.set $__arr_ptr_1
    local.get $__arr_ptr_1
    i32.load
    local.set $__arr_len_1
    i32.const 0
    local.set $__arr_idx_1
    (block $break_foreach_20
      (loop $loop_foreach_20
        local.get $__arr_idx_1
        local.get $__arr_len_1
        i32.ge_s
        br_if $break_foreach_20
        local.get $__arr_ptr_1
        local.get $__arr_idx_1
        i32.const 4
        i32.mul
        i32.add
        i32.load offset=4
        local.set $p
        (block $continue_foreach_20
    local.get $totalDist
    local.get $p
    local.set $__obj_tmp
    local.get $__obj_tmp
    local.get $__obj_tmp
    i32.load offset=0
    i32.const 3
    i32.add
    call_indirect (type $t_sig_1)
    i32.add
    local.set $totalDist
        )
        local.get $__arr_idx_1
        i32.const 1
        i32.add
        local.set $__arr_idx_1
        br $loop_foreach_20
      )
    )
    local.get $totalDist
    return
  )
  (func $testCanvasDraw (export "testCanvasDraw") (result i32)
    (local $canvas i32)
    (local $ctx i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    i32.const 4381
    i32.const 10
    call $dom_getElementById
    local.set $canvas
    local.get $canvas
    i32.const 500
    call $dom_setWidth
    local.get $canvas
    i32.const 300
    call $dom_setHeight
    local.get $canvas
    call $dom_getContext2D
    local.set $ctx
    local.get $ctx
    i32.const 4392
    i32.const 7
    call $dom_setFillStyle
    local.get $ctx
    f64.const 10
    f64.const 20
    f64.const 100
    f64.const 50
    call $dom_fillRect
    local.get $ctx
    call $dom_beginPath
    local.get $ctx
    f64.const 60
    f64.const 60
    f64.const 20
    f64.const 0
    f64.const 6.28
    i32.const 0
    call $dom_arc
    local.get $ctx
    call $dom_fill
    local.get $canvas
    call $dom_getWidth
    local.get $canvas
    call $dom_getHeight
    i32.add
    return
  )
  (func $testWebGLInit (export "testWebGLInit") (result i32)
    (local $glCanvas i32)
    (local $gl i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    i32.const 4400
    i32.const 8
    call $dom_getElementById
    local.set $glCanvas
    local.get $glCanvas
    call $dom_getWebGLContext
    local.set $gl
    local.get $gl
    f64.const 0.2
    f64.const 0.4
    f64.const 0.6
    f64.const 1.0
    call $dom_webgl_clearColor
    local.get $gl
    i32.const 16384
    call $dom_webgl_clear
    i32.const 1
    return
  )
  (func $testJsEvalAndGet (export "testJsEvalAndGet") (result i32)
    (local $obj i32)
    (local $title i32)
    (local $count i32)
    (local $active i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    i32.const 4409
    i32.const 50
    call $dom_js_eval
    local.set $obj
    local.get $obj
    i32.const 4460
    i32.const 5
    call $dom_js_getString
    local.set $title
    local.get $obj
    i32.const 4466
    i32.const 5
    call $dom_js_getInt
    local.set $count
    local.get $obj
    i32.const 4472
    i32.const 6
    call $dom_js_getBoolean
    local.set $active
    local.get $active
    local.get $count
    i32.const 42
    i32.eq
    i32.and
    local.get $title
    call $__str_length
    i32.const 10
    i32.eq
    i32.and
    if
    local.get $count
    return
    end
    i32.const 0
    return
  )
  (func $testJsCreateAndSet (export "testJsCreateAndSet") (result i32)
    (local $obj i32)
    (local $user i32)
    (local $score i32)
    (local $premium i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    call $dom_js_createObject
    local.set $obj
    local.get $obj
    i32.const 4479
    i32.const 4
    i32.const 4484
    i32.const 5
    call $dom_js_setString
    local.get $obj
    i32.const 4490
    i32.const 5
    i32.const 95
    call $dom_js_setInt
    local.get $obj
    i32.const 4496
    i32.const 6
    f64.const 4.8
    call $dom_js_setDouble
    local.get $obj
    i32.const 4503
    i32.const 7
    i32.const 1
    call $dom_js_setBoolean
    local.get $obj
    i32.const 4479
    i32.const 4
    call $dom_js_getString
    local.set $user
    local.get $obj
    i32.const 4490
    i32.const 5
    call $dom_js_getInt
    local.set $score
    local.get $obj
    i32.const 4503
    i32.const 7
    call $dom_js_getBoolean
    local.set $premium
    local.get $premium
    local.get $user
    call $__str_length
    i32.const 5
    i32.eq
    i32.and
    if
    local.get $score
    return
    end
    i32.const 0
    return
  )
  (func $testJsArrayOperations (export "testJsArrayOperations") (result i32)
    (local $arr i32)
    (local $len i32)
    (local $item1 i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    call $dom_js_createArray
    local.set $arr
    local.get $arr
    i32.const 4511
    i32.const 5
    call $dom_js_pushStr
    local.get $arr
    i32.const 4517
    i32.const 5
    call $dom_js_pushStr
    local.get $arr
    i32.const 123
    call $dom_js_pushInt
    local.get $arr
    call $dom_js_length
    local.set $len
    local.get $arr
    i32.const 1
    i32.const 4523
    i32.const 7
    call $dom_js_setIndexStr
    local.get $arr
    i32.const 1
    call $dom_js_getIndex
    call $dom_js_asString
    local.set $item1
    local.get $len
    i32.const 3
    i32.eq
    local.get $item1
    call $__str_length
    i32.const 7
    i32.eq
    i32.and
    if
    local.get $len
    return
    end
    i32.const 0
    return
  )
  (func $testJsGlobalMathCall (export "testJsGlobalMathCall") (result i32)
    (local $math i32)
    (local $max i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    call $dom_js_global
    i32.const 4531
    i32.const 4
    call $dom_js_get
    local.set $math
    local.get $math
    i32.const 4536
    i32.const 3
    i32.const 100
    call $dom_js_callInt1_int
    local.set $max
    local.get $max
    return
  )
  (func $testWasiOutput (export "testWasiOutput") (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    i32.const 1
    i32.const 4540
    i32.const 39
    i32.const 1
    call $__wasi_print_str
    i32.const 1
    i32.const 4580
    i32.const 10
    i32.const 0
    call $__wasi_print_str
    i32.const 1
    i32.const 100
    i32.const 1
    call $__wasi_print_int
    i32.const 2
    i32.const 4591
    i32.const 26
    i32.const 1
    call $__wasi_print_str
    i32.const 1
    return
  )
  (func $testWasiFacade (export "testWasiFacade") (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    i32.const 1
    i32.const 4618
    i32.const 21
    i32.const 0
    call $__wasi_print_str
    i32.const 1
    i32.const 4640
    i32.const 6
    i32.const 1
    call $__wasi_print_str
    i32.const 1
    i32.const 42
    i32.const 1
    call $__wasi_print_int
    i32.const 2
    i32.const 4647
    i32.const 17
    i32.const 1
    call $__wasi_print_str
    i32.const 42
    return
  )
  (func $testAtomicIntegerOperations (export "testAtomicIntegerOperations") (result i32)
    (local $counter i32)
    (local $val1 i32)
    (local $val2 i32)
    (local $cas i32)
    (local $val3 i32)
    (local $finalVal i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    i32.const 8
    call $__heap_alloc
    local.set $__atomic_tmp
    local.get $__atomic_tmp
    i32.const 10
    i32.atomic.store offset=4
    local.get $__atomic_tmp
    local.set $counter
    local.get $counter
    local.set $__atomic_tmp
    local.get $__atomic_tmp
    i32.const 1
    i32.atomic.rmw.add offset=4
    i32.const 1
    i32.add
    local.set $val1
    local.get $counter
    local.set $__atomic_tmp
    local.get $__atomic_tmp
    i32.const 5
    local.set $__atomic_val
    local.get $__atomic_val
    i32.atomic.rmw.add offset=4
    local.get $__atomic_val
    i32.add
    local.set $val2
    local.get $counter
    local.set $__atomic_tmp
    local.get $__atomic_tmp
    i32.const 16
    local.set $__atomic_val
    local.get $__atomic_val
    i32.const 50
    i32.atomic.rmw.cmpxchg offset=4
    local.get $__atomic_val
    i32.eq
    local.set $cas
    local.get $counter
    i32.const 10
    i32.atomic.rmw.add offset=4
    local.set $val3
    local.get $counter
    i32.atomic.load offset=4
    local.set $finalVal
    local.get $cas
    local.get $val1
    i32.const 11
    i32.eq
    i32.and
    local.get $val2
    i32.const 16
    i32.eq
    i32.and
    local.get $val3
    i32.const 50
    i32.eq
    i32.and
    local.get $finalVal
    i32.const 60
    i32.eq
    i32.and
    if
    i32.const 1
    return
    end
    i32.const 0
    return
  )
  (func $testAtomicsArrayOperations (export "testAtomicsArrayOperations") (result i32)
    (local $arr i32)
    (local $prev1 i32)
    (local $prev2 i32)
    (local $loaded0 i32)
    (local $loaded1 i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    global.get $__heap_ptr
    local.set $__arr_alloc_ptr
    global.get $__heap_ptr
    i32.const 16
    i32.add
    global.set $__heap_ptr
    local.get $__arr_alloc_ptr
    i32.const 3
    i32.store
    local.get $__arr_alloc_ptr
    i32.const 100
    i32.store offset=4
    local.get $__arr_alloc_ptr
    i32.const 200
    i32.store offset=8
    local.get $__arr_alloc_ptr
    i32.const 300
    i32.store offset=12
    local.get $__arr_alloc_ptr
    local.set $arr
    local.get $arr
    i32.const 4
    i32.add
    i32.const 0
    i32.const 2
    i32.shl
    i32.add
    i32.const 25
    i32.atomic.rmw.add
    local.set $prev1
    local.get $arr
    i32.const 4
    i32.add
    i32.const 1
    i32.const 2
    i32.shl
    i32.add
    i32.const 200
    i32.const 777
    i32.atomic.rmw.cmpxchg
    local.set $prev2
    local.get $arr
    i32.const 4
    i32.add
    i32.const 0
    i32.const 2
    i32.shl
    i32.add
    i32.atomic.load
    local.set $loaded0
    local.get $arr
    i32.const 4
    i32.add
    i32.const 1
    i32.const 2
    i32.shl
    i32.add
    i32.atomic.load
    local.set $loaded1
    local.get $prev1
    i32.const 100
    i32.eq
    local.get $prev2
    i32.const 200
    i32.eq
    i32.and
    local.get $loaded0
    i32.const 125
    i32.eq
    i32.and
    local.get $loaded1
    i32.const 777
    i32.eq
    i32.and
    if
    i32.const 1
    return
    end
    i32.const 0
    return
  )
  (func $testWasmThread (export "testWasmThread") (result i32)
    (local $hc i32)
    (local $tid i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    call $dom_thread_hardware_concurrency
    local.set $hc
    call $dom_thread_current_id
    local.set $tid
    i32.const 1
    call $dom_thread_sleep
    local.get $hc
    i32.const 1
    i32.ge_s
    local.get $tid
    i32.const 0
    i32.eq
    i32.and
    if
    i32.const 1
    return
    end
    i32.const 0
    return
  )
  (func $testSimpleTryCatch (export "testSimpleTryCatch") (param $shouldThrow i32) (result i32)
    (local $result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $e i32)
    (local $__caught_exc i32)
    i32.const 0
    local.set $result
    (try
      (do
    local.get $shouldThrow
    i32.const 1
    i32.eq
    if
    i32.const 8
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 19
    i32.store offset=0
    local.get $__obj_alloc_ptr
    i32.const 4665
    call $RuntimeException__init_1
    local.get $__obj_alloc_ptr
    throw $Exception
    end
    i32.const 100
    local.set $result
      )
      (catch $Exception
        local.set $__caught_exc
        local.get $__caught_exc
        local.set $e
    i32.const 999
    local.set $result
      )
    )
    local.get $result
    return
  )
  (func $testExceptionMessage (export "testExceptionMessage") (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $e i32)
    (local $__caught_exc i32)
    (try
      (do
    i32.const 8
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 1
    i32.store offset=0
    local.get $__obj_alloc_ptr
    i32.const 4676
    call $Exception__init_1
    local.get $__obj_alloc_ptr
    throw $Exception
      )
      (catch $Exception
        local.set $__caught_exc
        local.get $__caught_exc
        local.set $e
    local.get $e
    local.set $__obj_tmp
    local.get $__obj_tmp
    local.get $__obj_tmp
    i32.load offset=0
    i32.const 0
    i32.add
    call_indirect (type $t_sig_1)
    return
      )
    )
    i32.const 0
  )
  (func $testCustomExceptionHandling (export "testCustomExceptionHandling") (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $ce i32)
    (local $__caught_exc i32)
    (try
      (do
    i32.const 16
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 2
    i32.store offset=0
    local.get $__obj_alloc_ptr
    i32.const 4701
    i32.const 404
    call $CustomAppException__init
    local.get $__obj_alloc_ptr
    throw $Exception
      )
      (catch $Exception
        local.set $__caught_exc
        local.get $__caught_exc
        local.set $ce
    local.get $ce
    local.set $__obj_tmp
    local.get $__obj_tmp
    local.get $__obj_tmp
    i32.load offset=0
    i32.const 1
    i32.add
    call_indirect (type $t_sig_1)
    return
      )
    )
    i32.const 0
  )
  (func $testMultiCatchDispatch (export "testMultiCatchDispatch") (param $type i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $ce i32)
    (local $iae i32)
    (local $e i32)
    (local $__caught_exc i32)
    (try
      (do
    local.get $type
    i32.const 1
    i32.eq
    if
    i32.const 16
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 2
    i32.store offset=0
    local.get $__obj_alloc_ptr
    i32.const 4712
    i32.const 500
    call $CustomAppException__init
    local.get $__obj_alloc_ptr
    throw $Exception
    else
    local.get $type
    i32.const 2
    i32.eq
    if
    i32.const 8
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 20
    i32.store offset=0
    local.get $__obj_alloc_ptr
    i32.const 4723
    call $IllegalArgumentException__init_1
    local.get $__obj_alloc_ptr
    throw $Exception
    else
    i32.const 8
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 1
    i32.store offset=0
    local.get $__obj_alloc_ptr
    i32.const 4731
    call $Exception__init_1
    local.get $__obj_alloc_ptr
    throw $Exception
    end
    end
      )
      (catch $Exception
        local.set $__caught_exc
        local.get $__caught_exc
        i32.eqz
        if (result i32)
          i32.const 0
        else
          local.get $__caught_exc
          i32.load offset=0
          local.set $__vtable_tmp
          i32.const 0
          local.get $__vtable_tmp
          i32.const 2
          i32.eq
          i32.or
        end
        if
          local.get $__caught_exc
          local.set $ce
    i32.const 1000
    local.get $ce
    local.set $__obj_tmp
    local.get $__obj_tmp
    local.get $__obj_tmp
    i32.load offset=0
    i32.const 1
    i32.add
    call_indirect (type $t_sig_1)
    i32.add
    return
        else
        local.get $__caught_exc
        i32.eqz
        if (result i32)
          i32.const 0
        else
          local.get $__caught_exc
          i32.load offset=0
          local.set $__vtable_tmp
          i32.const 0
          local.get $__vtable_tmp
          i32.const 20
          i32.eq
          i32.or
        end
        if
          local.get $__caught_exc
          local.set $iae
    i32.const 2000
    return
        else
        local.get $__caught_exc
        local.set $e
    i32.const 3000
    return
        end
        end
      )
    )
    i32.const 0
  )
  (func $testTryFinally (export "testTryFinally") (param $shouldThrow i32) (result i32)
    (local $state i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $e i32)
    (local $__caught_exc i32)
    i32.const 0
    local.set $state
    (try
      (do
    local.get $shouldThrow
    i32.const 1
    i32.eq
    if
    (try
      (do
    i32.const 8
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 19
    i32.store offset=0
    local.get $__obj_alloc_ptr
    i32.const 4745
    call $RuntimeException__init_1
    local.get $__obj_alloc_ptr
    throw $Exception
      )
      (catch $Exception
        local.set $__caught_exc
        local.get $__caught_exc
        local.set $e
    local.get $state
    i32.const 10
    i32.add
    local.set $state
      )
    )
    else
    local.get $state
    i32.const 1
    i32.add
    local.set $state
    end
      )
      (catch_all
    local.get $state
    i32.const 100
    i32.add
    local.set $state
        (rethrow 0)
      )
    )
    local.get $state
    i32.const 100
    i32.add
    local.set $state
    local.get $state
    return
  )
  (func $makePair (export "makePair") (param $a i32) (param $b i32) (result i32 i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    local.get $a
    local.get $b
    return
  )
  (func $sumPair (export "sumPair") (param $p$first i32) (param $p$second i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    local.get $p$first
    local.get $p$second
    i32.add
    return
  )
  (func $computePairDifference (export "computePairDifference") (param $a i32) (param $b i32) (result i32)
    (local $p$first i32)
    (local $p$second i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    local.get $a
    local.get $b
    call $makePair
    local.set $p$second
    local.set $p$first
    local.get $p$first
    local.get $p$second
    i32.sub
    return
  )
  (func $divideWithRemainder (export "divideWithRemainder") (param $dividend i32) (param $divisor i32) (result i32 i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    local.get $dividend
    local.get $divisor
    i32.div_s
    local.get $dividend
    local.get $divisor
    i32.rem_s
    return
  )
  (func $sumDivisionResult (export "sumDivisionResult") (param $dividend i32) (param $divisor i32) (result i32)
    (local $res$quotient i32)
    (local $res$remainder i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    local.get $dividend
    local.get $divisor
    call $divideWithRemainder
    local.set $res$remainder
    local.set $res$quotient
    local.get $res$quotient
    local.get $res$remainder
    call $DivisionResult_total
    return
  )
  (func $scalePoint3D (export "scalePoint3D") (param $pt$x f64) (param $pt$y f64) (param $pt$z f64) (param $factor f64) (result f64 f64 f64)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    local.get $pt$x
    local.get $factor
    f64.mul
    local.get $pt$y
    local.get $factor
    f64.mul
    local.get $pt$z
    local.get $factor
    f64.mul
    return
  )
  (func $dotProductPoint3D (export "dotProductPoint3D") (param $a$x f64) (param $a$y f64) (param $a$z f64) (param $b$x f64) (param $b$y f64) (param $b$z f64) (result f64)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    local.get $a$x
    local.get $b$x
    f64.mul
    local.get $a$y
    local.get $b$y
    f64.mul
    f64.add
    local.get $a$z
    local.get $b$z
    f64.mul
    f64.add
    return
  )
  (func $testArrayCopyInt (export "testArrayCopyInt") (result i32)
    (local $src i32)
    (local $dest i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    global.get $__heap_ptr
    local.set $__arr_alloc_ptr
    global.get $__heap_ptr
    i32.const 24
    i32.add
    global.set $__heap_ptr
    local.get $__arr_alloc_ptr
    i32.const 5
    i32.store
    local.get $__arr_alloc_ptr
    i32.const 10
    i32.store offset=4
    local.get $__arr_alloc_ptr
    i32.const 20
    i32.store offset=8
    local.get $__arr_alloc_ptr
    i32.const 30
    i32.store offset=12
    local.get $__arr_alloc_ptr
    i32.const 40
    i32.store offset=16
    local.get $__arr_alloc_ptr
    i32.const 50
    i32.store offset=20
    local.get $__arr_alloc_ptr
    local.set $src
    i32.const 5
    local.set $__arr_alloc_len
    global.get $__heap_ptr
    local.set $__arr_alloc_ptr
    local.get $__arr_alloc_ptr
    local.get $__arr_alloc_len
    i32.store
    global.get $__heap_ptr
    i32.const 11
    local.get $__arr_alloc_len
    i32.const 4
    i32.mul
    i32.add
    i32.const -8
    i32.and
    i32.add
    global.set $__heap_ptr
    local.get $__arr_alloc_ptr
    local.set $dest
    local.get $dest
    i32.const 4
    i32.add
    i32.const 2
    i32.const 2
    i32.shl
    i32.add
    local.get $src
    i32.const 4
    i32.add
    i32.const 1
    i32.const 2
    i32.shl
    i32.add
    i32.const 3
    i32.const 2
    i32.shl
    memory.copy
    local.get $dest
    i32.const 0
    i32.const 4
    i32.mul
    i32.add
    i32.load offset=4
    local.get $dest
    i32.const 1
    i32.const 4
    i32.mul
    i32.add
    i32.load offset=4
    i32.add
    local.get $dest
    i32.const 2
    i32.const 4
    i32.mul
    i32.add
    i32.load offset=4
    i32.add
    local.get $dest
    i32.const 3
    i32.const 4
    i32.mul
    i32.add
    i32.load offset=4
    i32.add
    local.get $dest
    i32.const 4
    i32.const 4
    i32.mul
    i32.add
    i32.load offset=4
    i32.add
    return
  )
  (func $testArrayCopyDouble (export "testArrayCopyDouble") (result f64)
    (local $src i32)
    (local $dest i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    global.get $__heap_ptr
    local.set $__arr_alloc_ptr
    global.get $__heap_ptr
    i32.const 40
    i32.add
    global.set $__heap_ptr
    local.get $__arr_alloc_ptr
    i32.const 4
    i32.store
    local.get $__arr_alloc_ptr
    f64.const 1.5
    f64.store offset=8
    local.get $__arr_alloc_ptr
    f64.const 2.5
    f64.store offset=16
    local.get $__arr_alloc_ptr
    f64.const 3.5
    f64.store offset=24
    local.get $__arr_alloc_ptr
    f64.const 4.5
    f64.store offset=32
    local.get $__arr_alloc_ptr
    local.set $src
    i32.const 4
    local.set $__arr_alloc_len
    global.get $__heap_ptr
    local.set $__arr_alloc_ptr
    local.get $__arr_alloc_ptr
    local.get $__arr_alloc_len
    i32.store
    global.get $__heap_ptr
    i32.const 15
    local.get $__arr_alloc_len
    i32.const 8
    i32.mul
    i32.add
    i32.const -8
    i32.and
    i32.add
    global.set $__heap_ptr
    local.get $__arr_alloc_ptr
    local.set $dest
    local.get $dest
    i32.const 8
    i32.add
    i32.const 1
    i32.const 3
    i32.shl
    i32.add
    local.get $src
    i32.const 8
    i32.add
    i32.const 0
    i32.const 3
    i32.shl
    i32.add
    i32.const 3
    i32.const 3
    i32.shl
    memory.copy
    local.get $dest
    i32.const 0
    i32.const 8
    i32.mul
    i32.add
    f64.load offset=8
    local.get $dest
    i32.const 1
    i32.const 8
    i32.mul
    i32.add
    f64.load offset=8
    f64.add
    local.get $dest
    i32.const 2
    i32.const 8
    i32.mul
    i32.add
    f64.load offset=8
    f64.add
    local.get $dest
    i32.const 3
    i32.const 8
    i32.mul
    i32.add
    f64.load offset=8
    f64.add
    return
  )
  (func $testArraysFillInt (export "testArraysFillInt") (result i32)
    (local $arr i32)
    (local $sum1 i32)
    (local $sum2 i32)
    (local $sum3 i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    i32.const 5
    local.set $__arr_alloc_len
    global.get $__heap_ptr
    local.set $__arr_alloc_ptr
    local.get $__arr_alloc_ptr
    local.get $__arr_alloc_len
    i32.store
    global.get $__heap_ptr
    i32.const 11
    local.get $__arr_alloc_len
    i32.const 4
    i32.mul
    i32.add
    i32.const -8
    i32.and
    i32.add
    global.set $__heap_ptr
    local.get $__arr_alloc_ptr
    local.set $arr
    local.get $arr
    i32.const 0
    local.get $arr
    i32.load
    i32.const 7
    call $__array_fill_i32
    local.get $arr
    i32.const 0
    i32.const 4
    i32.mul
    i32.add
    i32.load offset=4
    local.get $arr
    i32.const 1
    i32.const 4
    i32.mul
    i32.add
    i32.load offset=4
    i32.add
    local.get $arr
    i32.const 2
    i32.const 4
    i32.mul
    i32.add
    i32.load offset=4
    i32.add
    local.get $arr
    i32.const 3
    i32.const 4
    i32.mul
    i32.add
    i32.load offset=4
    i32.add
    local.get $arr
    i32.const 4
    i32.const 4
    i32.mul
    i32.add
    i32.load offset=4
    i32.add
    local.set $sum1
    local.get $arr
    i32.const 1
    i32.const 4
    i32.const 9
    call $__array_fill_i32
    local.get $arr
    i32.const 0
    i32.const 4
    i32.mul
    i32.add
    i32.load offset=4
    local.get $arr
    i32.const 1
    i32.const 4
    i32.mul
    i32.add
    i32.load offset=4
    i32.add
    local.get $arr
    i32.const 2
    i32.const 4
    i32.mul
    i32.add
    i32.load offset=4
    i32.add
    local.get $arr
    i32.const 3
    i32.const 4
    i32.mul
    i32.add
    i32.load offset=4
    i32.add
    local.get $arr
    i32.const 4
    i32.const 4
    i32.mul
    i32.add
    i32.load offset=4
    i32.add
    local.set $sum2
    local.get $arr
    i32.const 0
    local.get $arr
    i32.load
    i32.const 0
    call $__array_fill_i32
    local.get $arr
    i32.const 0
    i32.const 4
    i32.mul
    i32.add
    i32.load offset=4
    local.get $arr
    i32.const 1
    i32.const 4
    i32.mul
    i32.add
    i32.load offset=4
    i32.add
    local.get $arr
    i32.const 2
    i32.const 4
    i32.mul
    i32.add
    i32.load offset=4
    i32.add
    local.get $arr
    i32.const 3
    i32.const 4
    i32.mul
    i32.add
    i32.load offset=4
    i32.add
    local.get $arr
    i32.const 4
    i32.const 4
    i32.mul
    i32.add
    i32.load offset=4
    i32.add
    local.set $sum3
    local.get $sum1
    local.get $sum2
    i32.add
    local.get $sum3
    i32.add
    return
  )
  (func $testArraysCopyOf (export "testArraysCopyOf") (result i32)
    (local $src i32)
    (local $copy1 i32)
    (local $sum1 i32)
    (local $copy2 i32)
    (local $sum2 i32)
    (local $eq1 i32)
    (local $src2 i32)
    (local $eq2 i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    global.get $__heap_ptr
    local.set $__arr_alloc_ptr
    global.get $__heap_ptr
    i32.const 16
    i32.add
    global.set $__heap_ptr
    local.get $__arr_alloc_ptr
    i32.const 3
    i32.store
    local.get $__arr_alloc_ptr
    i32.const 10
    i32.store offset=4
    local.get $__arr_alloc_ptr
    i32.const 20
    i32.store offset=8
    local.get $__arr_alloc_ptr
    i32.const 30
    i32.store offset=12
    local.get $__arr_alloc_ptr
    local.set $src
    local.get $src
    i32.const 5
    call $__array_copyOf_i32
    local.set $copy1
    local.get $copy1
    i32.const 0
    i32.const 4
    i32.mul
    i32.add
    i32.load offset=4
    local.get $copy1
    i32.const 1
    i32.const 4
    i32.mul
    i32.add
    i32.load offset=4
    i32.add
    local.get $copy1
    i32.const 2
    i32.const 4
    i32.mul
    i32.add
    i32.load offset=4
    i32.add
    local.get $copy1
    i32.const 3
    i32.const 4
    i32.mul
    i32.add
    i32.load offset=4
    i32.add
    local.get $copy1
    i32.const 4
    i32.const 4
    i32.mul
    i32.add
    i32.load offset=4
    i32.add
    local.set $sum1
    local.get $src
    i32.const 1
    i32.const 3
    call $__array_copyOfRange_i32
    local.set $copy2
    local.get $copy2
    i32.const 0
    i32.const 4
    i32.mul
    i32.add
    i32.load offset=4
    local.get $copy2
    i32.const 1
    i32.const 4
    i32.mul
    i32.add
    i32.load offset=4
    i32.add
    local.set $sum2
    local.get $src
    local.get $copy1
    call $__array_equals_i32
    local.set $eq1
    global.get $__heap_ptr
    local.set $__arr_alloc_ptr
    global.get $__heap_ptr
    i32.const 16
    i32.add
    global.set $__heap_ptr
    local.get $__arr_alloc_ptr
    i32.const 3
    i32.store
    local.get $__arr_alloc_ptr
    i32.const 10
    i32.store offset=4
    local.get $__arr_alloc_ptr
    i32.const 20
    i32.store offset=8
    local.get $__arr_alloc_ptr
    i32.const 30
    i32.store offset=12
    local.get $__arr_alloc_ptr
    local.set $src2
    local.get $src
    local.get $src2
    call $__array_equals_i32
    local.set $eq2
    local.get $sum1
    local.get $sum2
    i32.add
    local.get $eq1
    if (result i32)
    i32.const 100
    else
    i32.const 0
    end
    i32.add
    local.get $eq2
    if (result i32)
    i32.const 1
    else
    i32.const 0
    end
    i32.add
    return
  )
  (func $testWasiFileWriteAndRead (export "testWasiFileWriteAndRead") (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    i32.const 4757
    i32.const 4768
    i32.const 0
    call $__wasi_write_file_str
    drop
    i32.const 4757
    call $__wasi_read_file_str
    return
  )
  (func $testWasiFileExists (export "testWasiFileExists") (param $path i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    local.get $path
    call $__wasi_file_exists
    return
  )
  (func $testWasiFileLength (export "testWasiFileLength") (param $path i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    local.get $path
    call $__wasi_file_length
    return
  )
  (func $testWasiFileAppend (export "testWasiFileAppend") (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    i32.const 4783
    i32.const 4795
    i32.const 0
    call $__wasi_write_file_str
    drop
    i32.const 4783
    i32.const 4801
    i32.const 1
    call $__wasi_write_file_str
    drop
    i32.const 4783
    call $__wasi_read_file_str
    return
  )
  (func $testWasiFileDelete (export "testWasiFileDelete") (result i32)
    (local $existsBefore i32)
    (local $deleted i32)
    (local $existsAfter i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    i32.const 4807
    i32.const 4822
    i32.const 0
    call $__wasi_write_file_str
    drop
    i32.const 4807
    call $__wasi_file_exists
    local.set $existsBefore
    i32.const 4807
    call $__wasi_file_delete
    local.set $deleted
    i32.const 4807
    call $__wasi_file_exists
    local.set $existsAfter
    local.get $existsBefore
    local.get $deleted
    i32.and
    local.get $existsAfter
    i32.eqz
    i32.and
    return
  )
  (func $testWasiTimeAndRandom (export "testWasiTimeAndRandom") (result i32)
    (local $time f64)
    (local $rand i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    call $__wasi_clock_time_ms
    local.set $time
    call $__wasi_random_int
    local.set $rand
    local.get $time
    f64.const 0.0
    f64.gt
    return
  )
  (func $testWasiFileObject (export "testWasiFileObject") (result i32)
    (local $file i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    i32.const 8
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 4832
    call $WASIFile__init
    local.get $__obj_alloc_ptr
    local.set $file
    local.get $file
    i32.load offset=4
    i32.const 4846
    i32.const 0
    call $__wasi_write_file_str
    drop
    local.get $file
    i32.load offset=4
    i32.const 4856
    i32.const 1
    call $__wasi_write_file_str
    drop
    local.get $file
    i32.load offset=4
    call $__wasi_read_file_str
    return
  )
  (func $testWasiFileObjectOps (export "testWasiFileObjectOps") (result i32)
    (local $file i32)
    (local $len i32)
    (local $exists i32)
    (local $deleted i32)
    (local $existsAfter i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    i32.const 8
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 4869
    call $WASIFile__init
    local.get $__obj_alloc_ptr
    local.set $file
    local.get $file
    i32.load offset=4
    i32.const 4883
    i32.const 0
    call $__wasi_write_file_str
    drop
    local.get $file
    i32.load offset=4
    call $__wasi_file_length
    local.set $len
    local.get $file
    i32.load offset=4
    call $__wasi_file_exists
    local.set $exists
    local.get $file
    i32.load offset=4
    call $__wasi_file_delete
    local.set $deleted
    local.get $file
    i32.load offset=4
    call $__wasi_file_exists
    local.set $existsAfter
    local.get $len
    i32.const 5
    i32.eq
    local.get $exists
    i32.and
    local.get $deleted
    i32.and
    local.get $existsAfter
    i32.eqz
    i32.and
    return
  )
  (func $testSimdFloatAddAndSum (export "testSimdFloatAddAndSum") (result f64)
    (local $a v128)
    (local $b v128)
    (local $c v128)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    v128.const f32x4 1.0 2.0 3.0 4.0
    local.set $a
    v128.const f32x4 5.0 6.0 7.0 8.0
    local.set $b
    local.get $a
    local.get $b
    f32x4.add
    local.set $c
    local.get $c
    local.set $__simd_tmp
    local.get $__simd_tmp
    f32x4.extract_lane 0
    local.get $__simd_tmp
    f32x4.extract_lane 1
    f32.add
    local.get $__simd_tmp
    f32x4.extract_lane 2
    f32.add
    local.get $__simd_tmp
    f32x4.extract_lane 3
    f32.add
    f64.promote_f32
    return
  )
  (func $testSimdFloatMulAndDot (export "testSimdFloatMulAndDot") (result f64)
    (local $a v128)
    (local $b v128)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    v128.const f32x4 2.0 3.0 4.0 5.0
    local.set $a
    v128.const f32x4 1.0 2.0 3.0 4.0
    local.set $b
    local.get $a
    local.get $b
    f32x4.mul
    local.set $__simd_tmp
    local.get $__simd_tmp
    f32x4.extract_lane 0
    local.get $__simd_tmp
    f32x4.extract_lane 1
    f32.add
    local.get $__simd_tmp
    f32x4.extract_lane 2
    f32.add
    local.get $__simd_tmp
    f32x4.extract_lane 3
    f32.add
    f64.promote_f32
    return
  )
  (func $testSimdFloatMathOps (export "testSimdFloatMathOps") (result f64)
    (local $a v128)
    (local $b v128)
    (local $c v128)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    f32.const 16.0
    f32x4.splat
    local.set $a
    local.get $a
    f32x4.sqrt
    local.set $b
    local.get $b
    f32.const 100.0
    f32x4.replace_lane 0
    local.set $c
    local.get $c
    f32x4.extract_lane 0
    local.get $c
    f32x4.extract_lane 1
    f32.add
    f64.promote_f32
    return
  )
  (func $testSimdIntAddAndBitwise (export "testSimdIntAddAndBitwise") (result i32)
    (local $a v128)
    (local $b v128)
    (local $c v128)
    (local $d v128)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    v128.const i32x4 10 20 30 40
    local.set $a
    v128.const i32x4 1 2 3 4
    local.set $b
    local.get $a
    local.get $b
    i32x4.add
    local.set $c
    local.get $c
    i32.const 15
    i32x4.splat
    v128.and
    local.set $d
    local.get $d
    local.set $__simd_tmp
    local.get $__simd_tmp
    i32x4.extract_lane 0
    local.get $__simd_tmp
    i32x4.extract_lane 1
    i32.add
    local.get $__simd_tmp
    i32x4.extract_lane 2
    i32.add
    local.get $__simd_tmp
    i32x4.extract_lane 3
    i32.add
    return
  )
  (func $testSimdArrayLoadStore (export "testSimdArrayLoadStore") (result i32)
    (local $arr i32)
    (local $vec v128)
    (local $doubled v128)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    global.get $__heap_ptr
    local.set $__arr_alloc_ptr
    global.get $__heap_ptr
    i32.const 24
    i32.add
    global.set $__heap_ptr
    local.get $__arr_alloc_ptr
    i32.const 4
    i32.store
    local.get $__arr_alloc_ptr
    i32.const 100
    i32.store offset=4
    local.get $__arr_alloc_ptr
    i32.const 200
    i32.store offset=8
    local.get $__arr_alloc_ptr
    i32.const 300
    i32.store offset=12
    local.get $__arr_alloc_ptr
    i32.const 400
    i32.store offset=16
    local.get $__arr_alloc_ptr
    local.set $arr
    local.get $arr
    i32.const 4
    i32.add
    i32.const 0
    i32.const 4
    i32.mul
    i32.add
    v128.load
    local.set $vec
    local.get $vec
    local.get $vec
    i32x4.add
    local.set $doubled
    local.get $arr
    i32.const 4
    i32.add
    i32.const 0
    i32.const 4
    i32.mul
    i32.add
    local.get $doubled
    v128.store
    local.get $arr
    i32.const 0
    i32.const 4
    i32.mul
    i32.add
    i32.load offset=4
    local.get $arr
    i32.const 1
    i32.const 4
    i32.mul
    i32.add
    i32.load offset=4
    i32.add
    local.get $arr
    i32.const 2
    i32.const 4
    i32.mul
    i32.add
    i32.load offset=4
    i32.add
    local.get $arr
    i32.const 3
    i32.const 4
    i32.mul
    i32.add
    i32.load offset=4
    i32.add
    return
  )
  (func $testMultiMemoryHeapOps (export "testMultiMemoryHeapOps") (param $val i32) (result i32)
    (local $heap i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    i32.const 1
    local.set $heap
    i32.const 0
    local.get $val
    i32.store $heap
    i32.const 4
    local.get $val
    i32.const 2
    i32.mul
    i32.store $heap
    i32.const 0
    i32.load $heap
    i32.const 4
    i32.load $heap
    i32.add
    return
  )
  (func $testMultiMemoryDoubleOps (export "testMultiMemoryDoubleOps") (param $val f64) (result f64)
    (local $heap i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    i32.const 1
    local.set $heap
    i32.const 16
    local.get $val
    f64.store $heap
    i32.const 24
    local.get $val
    f64.const 3.0
    f64.mul
    f64.store $heap
    i32.const 16
    f64.load $heap
    i32.const 24
    f64.load $heap
    f64.add
    return
  )
  (func $testMultiMemoryCopyStringsToHeap (export "testMultiMemoryCopyStringsToHeap") (result i32)
    (local $heap i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    i32.const 1
    local.set $heap
    i32.const 100
    i32.const 0
    i32.const 5
    memory.copy $heap $strings
    i32.const 100
    i32.load8_u $heap
    return
  )
  (func $testMultiMemorySizeAndGrowth (export "testMultiMemorySizeAndGrowth") (result i32)
    (local $heap i32)
    (local $s1 i32)
    (local $s2 i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    i32.const 1
    local.set $heap
    memory.size $heap
    local.set $s1
    i32.const 1
    memory.grow $heap
    drop
    memory.size $heap
    local.set $s2
    local.get $s2
    local.get $s1
    i32.sub
    return
  )
  (func $jsHypot (export "jsHypot") (param $a f64) (param $b f64) (result f64)
    local.get $a
    local.get $b
    call $dom_js_inline_TestClass_jsHypot
  )
  (func $jsGreet (export "jsGreet") (param $name i32) (result i32)
    local.get $name
    local.get $name
    call $__str_length
    call $dom_js_inline_TestClass_jsGreet
  )
  (func $jsMultiplyAndAdd (export "jsMultiplyAndAdd") (param $a i32) (param $b i32) (result i32)
    local.get $a
    local.get $b
    call $dom_js_inline_TestClass_jsMultiplyAndAdd
  )
  (func $jsIsGreaterThan100 (export "jsIsGreaterThan100") (param $x i32) (result i32)
    local.get $x
    call $dom_js_inline_TestClass_jsIsGreaterThan100
  )
  (func $jsSumArray (export "jsSumArray") (param $arr i32) (result i32)
    local.get $arr
    call $dom_js_inline_TestClass_jsSumArray
  )
  (func $jsDoubleArray (export "jsDoubleArray") (param $arr i32) (result i32)
    local.get $arr
    call $dom_js_inline_TestClass_jsDoubleArray
  )
  (func $jsJoinStrings (export "jsJoinStrings") (param $items i32) (result i32)
    local.get $items
    call $dom_js_inline_TestClass_jsJoinStrings
  )
  (func $jsGetBookJson (export "jsGetBookJson") (result i32)
    call $dom_js_inline_TestClass_jsGetBookJson
  )
  (func $testJsCoerceArraySum (export "testJsCoerceArraySum") (result i32)
    (local $numbers i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    global.get $__heap_ptr
    local.set $__arr_alloc_ptr
    global.get $__heap_ptr
    i32.const 24
    i32.add
    global.set $__heap_ptr
    local.get $__arr_alloc_ptr
    i32.const 4
    i32.store
    local.get $__arr_alloc_ptr
    i32.const 10
    i32.store offset=4
    local.get $__arr_alloc_ptr
    i32.const 20
    i32.store offset=8
    local.get $__arr_alloc_ptr
    i32.const 30
    i32.store offset=12
    local.get $__arr_alloc_ptr
    i32.const 40
    i32.store offset=16
    local.get $__arr_alloc_ptr
    local.set $numbers
    local.get $numbers
    call $jsSumArray
    return
  )
  (func $testJsCoerceArrayMap (export "testJsCoerceArrayMap") (result i32)
    (local $numbers i32)
    (local $doubled i32)
    (local $total i32)
    (local $v i32)
    (local $__arr_ptr_1 i32)
    (local $__arr_len_1 i32)
    (local $__arr_idx_1 i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    global.get $__heap_ptr
    local.set $__arr_alloc_ptr
    global.get $__heap_ptr
    i32.const 24
    i32.add
    global.set $__heap_ptr
    local.get $__arr_alloc_ptr
    i32.const 4
    i32.store
    local.get $__arr_alloc_ptr
    i32.const 1
    i32.store offset=4
    local.get $__arr_alloc_ptr
    i32.const 2
    i32.store offset=8
    local.get $__arr_alloc_ptr
    i32.const 3
    i32.store offset=12
    local.get $__arr_alloc_ptr
    i32.const 4
    i32.store offset=16
    local.get $__arr_alloc_ptr
    local.set $numbers
    local.get $numbers
    call $jsDoubleArray
    local.set $doubled
    i32.const 0
    local.set $total
    local.get $doubled
    local.set $__arr_ptr_1
    local.get $__arr_ptr_1
    i32.load
    local.set $__arr_len_1
    i32.const 0
    local.set $__arr_idx_1
    (block $break_foreach_21
      (loop $loop_foreach_21
        local.get $__arr_idx_1
        local.get $__arr_len_1
        i32.ge_s
        br_if $break_foreach_21
        local.get $__arr_ptr_1
        local.get $__arr_idx_1
        i32.const 4
        i32.mul
        i32.add
        i32.load offset=4
        local.set $v
        (block $continue_foreach_21
    local.get $total
    local.get $v
    i32.add
    local.set $total
        )
        local.get $__arr_idx_1
        i32.const 1
        i32.add
        local.set $__arr_idx_1
        br $loop_foreach_21
      )
    )
    local.get $total
    return
  )
  (func $testJsValueHierarchy (export "testJsValueHierarchy") (result i32)
    (local $numVal i32)
    (local $strVal i32)
    (local $boolVal i32)
    (local $undefVal i32)
    (local $nullVal i32)
    (local $score i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    i32.const 42
    call $dom_js_number_of_int
    local.set $numVal
    i32.const 5123
    i32.const 11
    call $dom_js_string_of
    local.set $strVal
    i32.const 1
    call $dom_js_boolean_of
    local.set $boolVal
    call $dom_js_undefined
    local.set $undefVal
    call $dom_js_null
    local.set $nullVal
    i32.const 0
    local.set $score
    local.get $numVal
    call $dom_js_isNumber
    if
    local.get $score
    i32.const 1
    i32.add
    local.set $score
    end
    local.get $strVal
    call $dom_js_isString
    if
    local.get $score
    i32.const 2
    i32.add
    local.set $score
    end
    local.get $boolVal
    call $dom_js_isBoolean
    if
    local.get $score
    i32.const 4
    i32.add
    local.set $score
    end
    local.get $undefVal
    call $dom_js_isUndefined
    if
    local.get $score
    i32.const 8
    i32.add
    local.set $score
    end
    local.get $nullVal
    call $dom_js_isNull
    if
    local.get $score
    i32.const 16
    i32.add
    local.set $score
    end
    i32.const 5135
    local.get $numVal
    call $dom_js_typeof
    call $__str_equals
    if
    local.get $score
    i32.const 32
    i32.add
    local.set $score
    end
    i32.const 5142
    local.get $strVal
    call $dom_js_typeof
    call $__str_equals
    if
    local.get $score
    i32.const 64
    i32.add
    local.set $score
    end
    i32.const 5149
    local.get $boolVal
    call $dom_js_typeof
    call $__str_equals
    if
    local.get $score
    i32.const 128
    i32.add
    local.set $score
    end
    local.get $score
    return
  )
  (func $testJsNumberTyped (export "testJsNumberTyped") (result i32)
    (local $num i32)
    (local $intNum i32)
    (local $score i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    f64.const 100.5
    call $dom_js_number_of_double
    local.set $num
    i32.const 50
    call $dom_js_number_of_int
    local.set $intNum
    i32.const 0
    local.set $score
    local.get $num
    call $dom_js_asDouble
    f64.const 100.0
    f64.gt
    if
    local.get $score
    i32.const 1
    i32.add
    local.set $score
    end
    local.get $intNum
    call $dom_js_asInt
    i32.const 50
    i32.eq
    if
    local.get $score
    i32.const 2
    i32.add
    local.set $score
    end
    local.get $num
    call $dom_js_isNaN
    i32.eqz
    local.get $num
    call $dom_js_isFinite
    i32.and
    if
    local.get $score
    i32.const 4
    i32.add
    local.set $score
    end
    local.get $intNum
    call $dom_js_isInteger
    if
    local.get $score
    i32.const 8
    i32.add
    local.set $score
    end
    local.get $score
    return
  )
  (func $testJsSymbolAndBigInt (export "testJsSymbolAndBigInt") (result i32)
    (local $sym i32)
    (local $big i32)
    (local $score i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    i32.const 5157
    i32.const 6
    call $dom_js_symbol_of
    local.set $sym
    i32.const 5164
    i32.const 16
    call $dom_js_bigint_of_str
    local.set $big
    i32.const 0
    local.set $score
    local.get $sym
    call $dom_js_isSymbol
    if
    local.get $score
    i32.const 1
    i32.add
    local.set $score
    end
    i32.const 5157
    local.get $sym
    call $dom_js_symbolDescription
    call $__str_equals
    if
    local.get $score
    i32.const 2
    i32.add
    local.set $score
    end
    local.get $big
    call $dom_js_isBigInt
    if
    local.get $score
    i32.const 4
    i32.add
    local.set $score
    end
    local.get $score
    return
  )
  (func $testJsErrorAndInstanceof (export "testJsErrorAndInstanceof") (result i32)
    (local $err i32)
    (local $score i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    i32.const 5181
    i32.const 21
    call $dom_js_type_error_of
    local.set $err
    i32.const 0
    local.set $score
    local.get $err
    call $dom_js_isError
    if
    local.get $score
    i32.const 1
    i32.add
    local.set $score
    end
    i32.const 5181
    local.get $err
    call $dom_js_errorMessage
    call $__str_equals
    if
    local.get $score
    i32.const 2
    i32.add
    local.set $score
    end
    i32.const 5203
    local.get $err
    call $dom_js_errorName
    call $__str_equals
    if
    local.get $score
    i32.const 4
    i32.add
    local.set $score
    end
    local.get $err
    i32.const 5213
    i32.const 5
    call $dom_js_instanceof
    local.get $err
    i32.const 5203
    i32.const 9
    call $dom_js_instanceof
    i32.and
    if
    local.get $score
    i32.const 8
    i32.add
    local.set $score
    end
    local.get $score
    return
  )
  (func $testJsStrictEquals (export "testJsStrictEquals") (result i32)
    (local $v1 i32)
    (local $v2 i32)
    (local $v3 i32)
    (local $score i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    i32.const 42
    call $dom_js_number_of_int
    local.set $v1
    i32.const 42
    call $dom_js_number_of_int
    local.set $v2
    i32.const 5219
    i32.const 2
    call $dom_js_string_of
    local.set $v3
    i32.const 0
    local.set $score
    local.get $v1
    local.get $v2
    call $dom_js_strictEquals
    if
    local.get $score
    i32.const 1
    i32.add
    local.set $score
    end
    local.get $v1
    local.get $v3
    call $dom_js_strictEquals
    i32.eqz
    if
    local.get $score
    i32.const 2
    i32.add
    local.set $score
    end
    local.get $score
    return
  )
  (func $testJsMathImport (export "testJsMathImport") (result f64)
    (local $hyp f64)
    (local $pi f64)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    f64.const 3.0
    f64.const 4.0
    call $dom_js_inline_JSMath_hypot
    local.set $hyp
    call $dom_js_inline_JSMath_PI
    local.set $pi
    local.get $hyp
    local.get $pi
    f64.add
    return
  )
  (func $testJsDateImport (export "testJsDateImport") (result f64)
    (local $d i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    f64.const 1700000000000.0
    call $dom_js_inline_JSDate_create
    local.set $d
    local.get $d
    call $dom_js_inline_JSDate_getTime
    return
  )
  (func $testJsJsonImport (export "testJsJsonImport") (result i32)
    (local $obj i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    call $dom_js_createObject
    local.set $obj
    local.get $obj
    i32.const 5222
    i32.const 4
    i32.const 4316
    i32.const 11
    call $dom_js_setString
    local.get $obj
    i32.const 5227
    i32.const 7
    i32.const 2026
    call $dom_js_setInt
    local.get $obj
    call $dom_js_inline_JSJSON_stringify
    return
  )
  (func $testJsResourceGetString (export "testJsResourceGetString") (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    i32.const 4256
    i32.const 11
    call $dom_js_resource_get_string
    return
  )
  (func $testJsResourceHas (export "testJsResourceHas") (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    i32.const 4211
    i32.const 9
    call $dom_js_resource_has
    i32.const 5235
    i32.const 15
    call $dom_js_resource_has
    i32.eqz
    i32.and
    return
  )
  (func $testJsResourceLength (export "testJsResourceLength") (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    i32.const 4211
    i32.const 9
    call $dom_js_resource_get_length
    return
  )
  (func $testJsResourceInjectStyle (export "testJsResourceInjectStyle")
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    i32.const 5251
    i32.const 32
    call $dom_js_resource_inject_style
  )
  (func $testJsResourceInjectStylesheet (export "testJsResourceInjectStylesheet")
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    i32.const 4211
    i32.const 9
    call $dom_js_resource_inject_stylesheet
  )
  (func $testJsResourceInjectScript (export "testJsResourceInjectScript")
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    i32.const 5284
    i32.const 40
    call $dom_js_resource_inject_script
  )
  (func $testBundledJsCode (export "testBundledJsCode") (result i32)
    (local $val i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    call $dom_js_global
    i32.const 5325
    i32.const 18
    call $dom_js_get
    local.set $val
    local.get $val
    call $dom_js_asInt
    return
  )
  (func $testTerminalReadLine (export "testTerminalReadLine") (param $prompt i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    local.get $prompt
    local.set $__str_arg_ptr
    local.get $__str_arg_ptr
    local.get $__str_arg_ptr
    call $__str_length
    call $__wasi_read_line
    return
  )
  (func $testTerminalReadChar (export "testTerminalReadChar") (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    call $__wasi_read_char
    return
  )
  (func $testTerminalPrint (export "testTerminalPrint") (param $msg i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    i32.const 1
    i32.const 5367
    i32.const 5
    i32.const 0
    call $__wasi_print_str
    i32.const 1
    i32.const 5344
    i32.const 7
    i32.const 0
    call $__wasi_print_str
    i32.const 1
    i32.const 0
    i32.const 0
    i32.const 0
    call $__wasi_print_str
    i32.const 1
    local.get $msg
    local.set $__str_arg_ptr
    local.get $__str_arg_ptr
    local.get $__str_arg_ptr
    call $__str_length
    i32.const 1
    call $__wasi_print_str
  )
  (func $testTerminalDimensions (export "testTerminalDimensions") (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    call $dom_terminal_get_columns
    i32.const 100
    i32.mul
    call $dom_terminal_get_rows
    i32.add
    return
  )
  (func $testTerminalClear (export "testTerminalClear")
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    call $dom_terminal_clear
  )
  ;; Record: Pair
  ;; Record: Point3D
  ;; Record: DivisionResult
  (func $DivisionResult_total (export "DivisionResult_total") (param $this$quotient i32) (param $this$remainder i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    local.get $this$quotient
    local.get $this$remainder
    i32.add
    return
  )
  ;; Class/Interface: CustomAppException
  (func $CustomAppException__init (export "CustomAppException__init") (param $this i32) (param $msg i32) (param $code i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    local.get $this
    local.get $msg
    call $Exception__init_1
    local.get $this
    local.get $code
    i32.store offset=8
  )
  (func $CustomAppException_getCode (export "CustomAppException_getCode") (param $this i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    local.get $this
    i32.load offset=8
    return
  )
  ;; Class/Interface: Point
  (func $Point__init (export "Point__init") (param $this i32) (param $x i32) (param $y i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    local.get $this
    local.get $x
    i32.store offset=4
    local.get $this
    local.get $y
    i32.store offset=8
  )
  (func $Point_getX (export "Point_getX") (param $this i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    local.get $this
    i32.load offset=4
    return
  )
  (func $Point_getY (export "Point_getY") (param $this i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    local.get $this
    i32.load offset=8
    return
  )
  (func $Point_setLocation (export "Point_setLocation") (param $this i32) (param $newX i32) (param $newY i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    local.get $this
    local.get $newX
    i32.store offset=4
    local.get $this
    local.get $newY
    i32.store offset=8
  )
  (func $Point_distanceSq (export "Point_distanceSq") (param $this i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    local.get $this
    i32.load offset=4
    local.get $this
    i32.load offset=4
    i32.mul
    local.get $this
    i32.load offset=8
    local.get $this
    i32.load offset=8
    i32.mul
    i32.add
    return
  )
  ;; Class/Interface: Shape
  (func $Shape__init (export "Shape__init") (param $this i32) (param $color i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    local.get $this
    local.get $color
    i32.store offset=4
  )
  (func $Shape_getColor (export "Shape_getColor") (param $this i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    local.get $this
    i32.load offset=4
    return
  )
  (func $Shape_area (export "Shape_area") (param $this i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    i32.const 0
    return
  )
  ;; Class/Interface: Circle
  (func $Circle__init (export "Circle__init") (param $this i32) (param $color i32) (param $radius i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    local.get $this
    local.get $color
    call $Shape__init
    local.get $this
    local.get $radius
    i32.store offset=8
  )
  (func $Circle_getRadius (export "Circle_getRadius") (param $this i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    local.get $this
    i32.load offset=8
    return
  )
  (func $Circle_area (export "Circle_area") (param $this i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    i32.const 3
    local.get $this
    i32.load offset=8
    i32.mul
    local.get $this
    i32.load offset=8
    i32.mul
    return
  )
  ;; Class/Interface: Rectangle
  (func $Rectangle__init (export "Rectangle__init") (param $this i32) (param $color i32) (param $width i32) (param $height i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    local.get $this
    local.get $color
    call $Shape__init
    local.get $this
    local.get $width
    i32.store offset=8
    local.get $this
    local.get $height
    i32.store offset=12
  )
  (func $Rectangle_area (export "Rectangle_area") (param $this i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    local.get $this
    i32.load offset=8
    local.get $this
    i32.load offset=12
    i32.mul
    return
  )
  ;; Class/Interface: ExportedCounter
  (func $ExportedCounter__init (export "ExportedCounter__init") (param $this i32) (param $initialCount i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    local.get $this
    local.get $initialCount
    i32.store offset=4
  )
  (func $ExportedCounter_increment (export "ExportedCounter_increment") (param $this i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    local.get $this
    local.set $__obj_tmp
    local.get $__obj_tmp
    local.get $__obj_tmp
    i32.load offset=4
    i32.const 1
    i32.add
    i32.store offset=4
  )
  (func $ExportedCounter_add (export "ExportedCounter_add") (param $this i32) (param $amount i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    local.get $this
    local.set $__obj_tmp
    local.get $__obj_tmp
    local.get $__obj_tmp
    i32.load offset=4
    local.get $amount
    i32.add
    i32.store offset=4
  )
  (func $ExportedCounter_getCount (export "ExportedCounter_getCount") (param $this i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    local.get $this
    i32.load offset=4
    return
  )
  (func $multiply (export "multiply") (param $a i32) (param $b i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    local.get $a
    local.get $b
    i32.mul
    return
  )
  (func $ExportedCounter_describe (export "ExportedCounter_describe") (param $this i32) (param $label i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    (local $__atomic_tmp i32)
    (local $__atomic_val i32)
    (local $__simd_tmp v128)
    (local $__caught_exc i32)
    local.get $label
    i32.const 5364
    call $__str_concat
    local.get $this
    i32.load offset=4
    call $__str_from_i32
    call $__str_concat
    return
  )
)
