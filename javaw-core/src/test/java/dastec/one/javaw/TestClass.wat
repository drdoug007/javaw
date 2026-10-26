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
  (import "dom" "math_sin" (func $dom_math_sin (param f64) (result f64)))
  (import "dom" "math_cos" (func $dom_math_cos (param f64) (result f64)))
  (import "dom" "math_tan" (func $dom_math_tan (param f64) (result f64)))
  (import "dom" "math_exp" (func $dom_math_exp (param f64) (result f64)))
  (import "dom" "math_log" (func $dom_math_log (param f64) (result f64)))
  (import "dom" "math_pow" (func $dom_math_pow (param f64 f64) (result f64)))
  (import "dom" "math_hypot" (func $dom_math_hypot (param f64 f64) (result f64)))
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
  (@custom "javaw_js_exports" "[{\22className\22:\22ExportedCounter\22,\22exportName\22:\22JavaCounter\22,\22structSize\22:8,\22vtableBase\22:96,\22hasVtable\22:true,\22constructors\22:[{\22exportFunctionName\22:\22ExportedCounter__init\22,\22params\22:[{\22name\22:\22initialCount\22,\22javaType\22:\22int\22,\22wasmType\22:\22i32\22,\22coerced\22:false}]}],\22methods\22:[{\22methodName\22:\22increment\22,\22exportName\22:\22increment\22,\22wasmFunctionName\22:\22ExportedCounter_increment\22,\22returnType\22:\22void\22,\22returnWasmType\22:\22\22,\22isStatic\22:false,\22isCoerced\22:false,\22params\22:[]},{\22methodName\22:\22add\22,\22exportName\22:\22add\22,\22wasmFunctionName\22:\22ExportedCounter_add\22,\22returnType\22:\22void\22,\22returnWasmType\22:\22\22,\22isStatic\22:false,\22isCoerced\22:false,\22params\22:[{\22name\22:\22amount\22,\22javaType\22:\22int\22,\22wasmType\22:\22i32\22,\22coerced\22:false}]},{\22methodName\22:\22getCount\22,\22exportName\22:\22getCount\22,\22wasmFunctionName\22:\22ExportedCounter_getCount\22,\22returnType\22:\22int\22,\22returnWasmType\22:\22i32\22,\22isStatic\22:false,\22isCoerced\22:false,\22params\22:[]},{\22methodName\22:\22multiply\22,\22exportName\22:\22multiply\22,\22wasmFunctionName\22:\22multiply\22,\22returnType\22:\22int\22,\22returnWasmType\22:\22i32\22,\22isStatic\22:true,\22isCoerced\22:false,\22params\22:[{\22name\22:\22a\22,\22javaType\22:\22int\22,\22wasmType\22:\22i32\22,\22coerced\22:false},{\22name\22:\22b\22,\22javaType\22:\22int\22,\22wasmType\22:\22i32\22,\22coerced\22:false}]},{\22methodName\22:\22describe\22,\22exportName\22:\22describe\22,\22wasmFunctionName\22:\22ExportedCounter_describe\22,\22returnType\22:\22String\22,\22returnWasmType\22:\22i32\22,\22isStatic\22:false,\22isCoerced\22:false,\22params\22:[{\22name\22:\22label\22,\22javaType\22:\22String\22,\22wasmType\22:\22i32\22,\22coerced\22:false}]}],\22fields\22:[]}]")
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
  (global $Status_PENDING (mut i32) (i32.const 0))
  (global $Status_ACTIVE (mut i32) (i32.const 0))
  (global $Status_COMPLETED (mut i32) (i32.const 0))
  (global $Priority_LOW (mut i32) (i32.const 0))
  (global $Priority_HIGH (mut i32) (i32.const 0))
  (data (memory $strings) (i32.const 4096) "globalThis.__bundled_test_var = 777; function bundledCalc(n) { return n * 3; }\00")
  (data (memory $strings) (i32.const 4175) "body { background-color: #f0f0f0; }\00")
  (data (memory $strings) (i32.const 4211) "theme.css\00")
  (data (memory $strings) (i32.const 4221) "{\22theme\22:\22dark\22,\22port\22:8080}\00")
  (data (memory $strings) (i32.const 4250) "config.json\00")
  (data (memory $strings) (i32.const 4262) "\00")
  (data (memory $strings) (i32.const 4263) "onTimerTick\00")
  (data (memory $strings) (i32.const 4275) "onFrameUpdate\00")
  (data (memory $strings) (i32.const 4289) "onFetchSuccess\00")
  (data (memory $strings) (i32.const 4304) "1A\00")
  (data (memory $strings) (i32.const 4307) "negative id\00")
  (data (memory $strings) (i32.const 4319) "background-worker\00")
  (data (memory $strings) (i32.const 4337) "SimpleWorkerTask\00")
  (data (memory $strings) (i32.const 4354) "Hello\00")
  (data (memory $strings) (i32.const 4360) "WebAssembly\00")
  (data (memory $strings) (i32.const 4372) ", \00")
  (data (memory $strings) (i32.const 4375) "!\00")
  (data (memory $strings) (i32.const 4377) "User: \00")
  (data (memory $strings) (i32.const 4384) " (age: \00")
  (data (memory $strings) (i32.const 4392) ")\00")
  (data (memory $strings) (i32.const 4394) "Hello World\00")
  (data (memory $strings) (i32.const 4406) "World\00")
  (data (memory $strings) (i32.const 4412) "Java\00")
  (data (memory $strings) (i32.const 4417) "Count: \00")
  (data (memory $strings) (i32.const 4425) "testCanvas\00")
  (data (memory $strings) (i32.const 4436) "#ff0000\00")
  (data (memory $strings) (i32.const 4444) "glCanvas\00")
  (data (memory $strings) (i32.const 4453) "({ title: 'Dynamic JS', count: 42, active: true })\00")
  (data (memory $strings) (i32.const 4504) "title\00")
  (data (memory $strings) (i32.const 4510) "count\00")
  (data (memory $strings) (i32.const 4516) "active\00")
  (data (memory $strings) (i32.const 4523) "user\00")
  (data (memory $strings) (i32.const 4528) "Alice\00")
  (data (memory $strings) (i32.const 4534) "score\00")
  (data (memory $strings) (i32.const 4540) "rating\00")
  (data (memory $strings) (i32.const 4547) "premium\00")
  (data (memory $strings) (i32.const 4555) "item1\00")
  (data (memory $strings) (i32.const 4561) "item2\00")
  (data (memory $strings) (i32.const 4567) "updated\00")
  (data (memory $strings) (i32.const 4575) "Math\00")
  (data (memory $strings) (i32.const 4580) "max\00")
  (data (memory $strings) (i32.const 4584) "Testing WASI standard output from Java!\00")
  (data (memory $strings) (i32.const 4624) "Progress: \00")
  (data (memory $strings) (i32.const 4635) "WASI stderr stream message\00")
  (data (memory $strings) (i32.const 4662) "WASI Facade message: \00")
  (data (memory $strings) (i32.const 4684) "Active\00")
  (data (memory $strings) (i32.const 4691) "WASI Facade Error\00")
  (data (memory $strings) (i32.const 4709) "Test error\00")
  (data (memory $strings) (i32.const 4720) "Custom exception message\00")
  (data (memory $strings) (i32.const 4745) "App failed\00")
  (data (memory $strings) (i32.const 4756) "Code error\00")
  (data (memory $strings) (i32.const 4767) "Bad arg\00")
  (data (memory $strings) (i32.const 4775) "Generic error\00")
  (data (memory $strings) (i32.const 4789) "Inner error\00")
  (data (memory $strings) (i32.const 4801) "/hello.txt\00")
  (data (memory $strings) (i32.const 4812) "Hello WASI VFS\00")
  (data (memory $strings) (i32.const 4827) "/append.txt\00")
  (data (memory $strings) (i32.const 4839) "Part1\00")
  (data (memory $strings) (i32.const 4845) "Part2\00")
  (data (memory $strings) (i32.const 4851) "/to_delete.txt\00")
  (data (memory $strings) (i32.const 4866) "temp data\00")
  (data (memory $strings) (i32.const 4876) "/app/data.txt\00")
  (data (memory $strings) (i32.const 4890) "Config=42\00")
  (data (memory $strings) (i32.const 4900) ";Active=true\00")
  (data (memory $strings) (i32.const 4913) "/app/temp.log\00")
  (data (memory $strings) (i32.const 4927) "12345\00")
  (data (memory $strings) (i32.const 4933) "return Math.hypot(a, b);\00")
  (data (memory $strings) (i32.const 4958) "return 'Hello ' + name + '!';\00")
  (data (memory $strings) (i32.const 4988) "return a * b + 10;\00")
  (data (memory $strings) (i32.const 5007) "return x > 100;\00")
  (data (memory $strings) (i32.const 5023) "return arr.reduce((acc, x) => acc + x, 0);\00")
  (data (memory $strings) (i32.const 5066) "return arr.map(x => x * 2);\00")
  (data (memory $strings) (i32.const 5094) "return items.join(', ');\00")
  (data (memory $strings) (i32.const 5119) "return { title: 'WASM in Action', year: 2026 };\00")
  (data (memory $strings) (i32.const 5167) "hello world\00")
  (data (memory $strings) (i32.const 5179) "number\00")
  (data (memory $strings) (i32.const 5186) "string\00")
  (data (memory $strings) (i32.const 5193) "boolean\00")
  (data (memory $strings) (i32.const 5201) "id_key\00")
  (data (memory $strings) (i32.const 5208) "9007199254740995\00")
  (data (memory $strings) (i32.const 5225) "Invalid type argument\00")
  (data (memory $strings) (i32.const 5247) "TypeError\00")
  (data (memory $strings) (i32.const 5257) "Error\00")
  (data (memory $strings) (i32.const 5263) "42\00")
  (data (memory $strings) (i32.const 5266) "name\00")
  (data (memory $strings) (i32.const 5271) "version\00")
  (data (memory $strings) (i32.const 5279) "nonexistent.txt\00")
  (data (memory $strings) (i32.const 5295) ".app-header { font-size: 24px; }\00")
  (data (memory $strings) (i32.const 5328) "globalThis.__injected_script_run = true;\00")
  (data (memory $strings) (i32.const 5369) "__bundled_test_var\00")
  (data (memory $strings) (i32.const 5388) "[WASI] \00")
  (data (memory $strings) (i32.const 5396) "Bob\00")
  (data (memory $strings) (i32.const 5400) "Web\00")
  (data (memory $strings) (i32.const 5404) "Assembly\00")
  (data (memory $strings) (i32.const 5413) "-Based \00")
  (data (memory $strings) (i32.const 5421) "\0a\00")
  (data (memory $strings) (i32.const 5423) "line.separator\00")
  (data (memory $strings) (i32.const 5438) "file.separator\00")
  (data (memory $strings) (i32.const 5453) "os.name\00")
  (data (memory $strings) (i32.const 5461) "os.arch\00")
  (data (memory $strings) (i32.const 5469) "custom.unknown.property\00")
  (data (memory $strings) (i32.const 5493) "fallback\00")
  (data (memory $strings) (i32.const 5502) "/\00")
  (data (memory $strings) (i32.const 5504) "wasm32\00")
  (data (memory $strings) (i32.const 5511) "PATH\00")
  (data (memory $strings) (i32.const 5516) "JavaCounter\00")
  (data (memory $strings) (i32.const 5528) ": \00")
  (data (memory $strings) (i32.const 5531) "Status\00")
  (data (memory $strings) (i32.const 5538) "PENDING\00")
  (data (memory $strings) (i32.const 5546) "ACTIVE\00")
  (data (memory $strings) (i32.const 5553) "COMPLETED\00")
  (data (memory $strings) (i32.const 5563) "Priority\00")
  (data (memory $strings) (i32.const 5572) "LOW\00")
  (data (memory $strings) (i32.const 5576) "HIGH\00")
  (data (memory $strings) (i32.const 5581) "TestClass\00")
  (data (memory $strings) (i32.const 5591) "Person\00")
  (data (memory $strings) (i32.const 5598) "CustomAppException\00")
  (data (memory $strings) (i32.const 5617) "Point\00")
  (data (memory $strings) (i32.const 5623) "Shape\00")
  (data (memory $strings) (i32.const 5629) "Circle\00")
  (data (memory $strings) (i32.const 5636) "Rectangle\00")
  (data (memory $strings) (i32.const 5646) "ExportedCounter\00")
  (data (memory $strings) (i32.const 5662) "ClosableSession\00")
  (data (memory $strings) (i32.const 5678) "BoxItem\00")
  (data (memory $strings) (i32.const 5686) "IntRangeIterator\00")
  (data (memory $strings) (i32.const 5703) "IntRange\00")
  (data (memory $strings) (i32.const 5712) "@\00")
  (data (memory $strings) (i32.const 5714) "true\00")
  (data (memory $strings) (i32.const 5719) "false\00")
  (data (memory $strings) (i32.const 5725) "class \00")
  (data (memory $strings) (i32.const 5732) "interface \00")
  (data (memory $strings) (i32.const 5743) "Object\00")
  (data (memory $strings) (i32.const 5750) "String\00")
  (data (memory $strings) (i32.const 5757) "Class\00")
  (data (memory $strings) (i32.const 5763) "Thread\00")
  (data (memory $strings) (i32.const 5770) "Runnable\00")
  (data (memory $strings) (i32.const 5779) "Enum\00")
  (data (memory $strings) (i32.const 5784) "int\00")
  (data (memory $strings) (i32.const 5788) "long\00")
  (data (memory $strings) (i32.const 5793) "double\00")
  (data (memory $strings) (i32.const 5800) "float\00")
  (data (memory $strings) (i32.const 5806) "char\00")
  (data (memory $strings) (i32.const 5811) "byte\00")
  (data (memory $strings) (i32.const 5816) "short\00")
  (data (memory $strings) (i32.const 5822) "void\00")
  (data (memory $strings) (i32.const 5827) "\1b[32m\00")
  (global $__heap_ptr (mut i32) (i32.const 65536))
  (global $__class_TestClass (mut i32) (i32.const 0))
  (global $__class_Person (mut i32) (i32.const 0))
  (global $__class_CustomAppException (mut i32) (i32.const 0))
  (global $__class_Point (mut i32) (i32.const 0))
  (global $__class_Shape (mut i32) (i32.const 0))
  (global $__class_Circle (mut i32) (i32.const 0))
  (global $__class_Rectangle (mut i32) (i32.const 0))
  (global $__class_ExportedCounter (mut i32) (i32.const 0))
  (global $__class_ClosableSession (mut i32) (i32.const 0))
  (global $__class_BoxItem (mut i32) (i32.const 0))
  (global $__class_IntRangeIterator (mut i32) (i32.const 0))
  (global $__class_IntRange (mut i32) (i32.const 0))
  (global $__class_SimpleWorkerTask (mut i32) (i32.const 0))
  (global $__class_Status (mut i32) (i32.const 0))
  (global $__class_Priority (mut i32) (i32.const 0))
  (global $__class_Throwable (mut i32) (i32.const 0))
  (global $__class_Exception (mut i32) (i32.const 0))
  (global $__class_RuntimeException (mut i32) (i32.const 0))
  (global $__class_IllegalArgumentException (mut i32) (i32.const 0))
  (global $__class_IllegalStateException (mut i32) (i32.const 0))
  (global $__class_NullPointerException (mut i32) (i32.const 0))
  (global $__class_WASIFile (mut i32) (i32.const 0))
  (global $__class_CharSequence (mut i32) (i32.const 0))
  (global $__class_StringBuilder (mut i32) (i32.const 0))
  (global $__class_AutoCloseable (mut i32) (i32.const 0))
  (global $__class_Comparable (mut i32) (i32.const 0))
  (global $__class_Iterable (mut i32) (i32.const 0))
  (global $__class_Iterator (mut i32) (i32.const 0))
  (global $__class_Runnable (mut i32) (i32.const 0))
  (global $__class_Thread (mut i32) (i32.const 0))
  (global $__class_Class (mut i32) (i32.const 0))
  (global $__class_int (mut i32) (i32.const 0))
  (global $__class_long (mut i32) (i32.const 0))
  (global $__class_double (mut i32) (i32.const 0))
  (global $__class_float (mut i32) (i32.const 0))
  (global $__class_boolean (mut i32) (i32.const 0))
  (global $__class_char (mut i32) (i32.const 0))
  (global $__class_byte (mut i32) (i32.const 0))
  (global $__class_short (mut i32) (i32.const 0))
  (global $__class_void (mut i32) (i32.const 0))
  ;; --- WebAssembly Function Type Signatures ---
  (type $t_sig_1 (func (param i32) (result i32)))
  (type $t_sig_2 (func (param i32 i32) (result i32)))
  (type $t_sig_3 (func (param i32)))
  (type $t_sig_4 (func (param i32 i32 i32)))
  (type $t_sig_5 (func (param i32 i32)))
  ;; --- WebAssembly Function Table (VTable) ---
  (table (export "__vtable") 260 funcref)
  (elem (i32.const 0) $Person_toString $Person_equals $Person_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Person_run $Person_getClass $Person_clonePerson $Throwable_toString $Throwable_equals $Throwable_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Throwable_run $Throwable_getClass $Throwable_getMessage $Throwable_toString $Throwable_equals $Throwable_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Throwable_run $Throwable_getClass $Exception_getMessage $Throwable_toString $Throwable_equals $Throwable_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Throwable_run $Throwable_getClass $Exception_getMessage $CustomAppException_getCode $Point_toString $Point_equals $Point_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Point_run $Point_getClass $Point_getX $Point_getY $Point_setLocation $Point_distanceSq $Shape_toString $Shape_equals $Shape_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Shape_run $Shape_getClass $Shape_getColor $Shape_area $Shape_toString $Shape_equals $Shape_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Shape_run $Shape_getClass $Shape_getColor $Circle_area $Circle_getRadius $Shape_toString $Shape_equals $Shape_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Shape_run $Shape_getClass $Shape_getColor $Rectangle_area $ExportedCounter_toString $ExportedCounter_equals $ExportedCounter_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $ExportedCounter_run $ExportedCounter_getClass $ExportedCounter_increment $ExportedCounter_add $ExportedCounter_getCount $ExportedCounter_describe $ClosableSession_toString $ClosableSession_equals $ClosableSession_hashCode $ClosableSession_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $ClosableSession_run $ClosableSession_getClass $ClosableSession_doWork $BoxItem_toString $BoxItem_equals $BoxItem_hashCode $AutoCloseable_close $BoxItem_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $BoxItem_run $BoxItem_getClass $IntRangeIterator_toString $IntRangeIterator_equals $IntRangeIterator_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $IntRangeIterator_hasNext $IntRangeIterator_next $IntRangeIterator_run $IntRangeIterator_getClass $IntRange_toString $IntRange_equals $IntRange_hashCode $AutoCloseable_close $Comparable_compareTo $IntRange_iterator $Iterator_hasNext $Iterator_next $IntRange_run $IntRange_getClass $SimpleWorkerTask_toString $SimpleWorkerTask_equals $SimpleWorkerTask_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $SimpleWorkerTask_run $SimpleWorkerTask_getClass $Status_toString $Status_equals $Status_hashCode $AutoCloseable_close $Status_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Status_run $Status_getClass $Status_name $Status_ordinal $Priority_toString $Priority_equals $Priority_hashCode $AutoCloseable_close $Priority_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Priority_run $Priority_getClass $Priority_getLevel $Priority_name $Priority_ordinal $Throwable_toString $Throwable_equals $Throwable_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Throwable_run $Throwable_getClass $RuntimeException_getMessage $Throwable_toString $Throwable_equals $Throwable_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Throwable_run $Throwable_getClass $IllegalArgumentException_getMessage $Throwable_toString $Throwable_equals $Throwable_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Throwable_run $Throwable_getClass $IllegalStateException_getMessage $Throwable_toString $Throwable_equals $Throwable_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Throwable_run $Throwable_getClass $NullPointerException_getMessage $WASIFile_toString $WASIFile_equals $WASIFile_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $WASIFile_run $WASIFile_getClass $Thread_toString $Thread_equals $Thread_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Thread_run $Thread_getClass $Class_toString $Class_equals $Class_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Class_run $Class_getClass)
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
  ;; --- Root Object Functions ---
  (func $Object_toString (export "Object_toString") (param $this i32) (result i32)
    (local $addr_str i32)
    local.get $this
    call $__str_from_i32
    local.set $addr_str
    local.get $addr_str
  )
  (func $Object_equals (export "Object_equals") (param $this i32) (param $other i32) (result i32)
    local.get $this
    local.get $other
    i32.eq
  )
  (func $Object_hashCode (export "Object_hashCode") (param $this i32) (result i32)
    local.get $this
  )
  (func $Object_clone (export "Object_clone") (param $src i32) (param $size i32) (result i32)
    (local $dst i32)
    local.get $size
    call $__heap_alloc
    local.set $dst
    local.get $dst
    local.get $src
    local.get $size
    memory.copy
    local.get $dst
  )
  ;; --- Enum Standard Functions ---
  (func $Enum_name (export "Enum_name") (param $this i32) (result i32)
    local.get $this
    i32.load offset=4
  )
  (func $Enum_ordinal (export "Enum_ordinal") (param $this i32) (result i32)
    local.get $this
    i32.load offset=8
  )
  (func $Enum_compareTo (export "Enum_compareTo") (param $this i32) (param $other i32) (result i32)
    local.get $this
    i32.load offset=8
    local.get $other
    i32.load offset=8
    i32.sub
  )
  ;; --- Standard Interface Functions ---
  (func $AutoCloseable_close (export "AutoCloseable_close") (param $this i32)
  )
  (func $Comparable_compareTo (export "Comparable_compareTo") (param $this i32) (param $other i32) (result i32)
    local.get $this
    local.get $other
    i32.sub
  )
  (func $Iterable_iterator (export "Iterable_iterator") (param $this i32) (result i32)
    i32.const 0
  )
  (func $Iterator_hasNext (export "Iterator_hasNext") (param $this i32) (result i32)
    i32.const 0
  )
  (func $Iterator_next (export "Iterator_next") (param $this i32) (result i32)
    i32.const 0
  )
  (func $Runnable_run (export "Runnable_run") (param $this i32)
  )
  (func $Object_getClass (export "Object_getClass") (param $this i32) (result i32)
    local.get $this
    i32.load offset=0
    call $__get_class_from_vtable
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
  (func $__str_from_char_array (export "__str_from_char_array") (param $arr i32) (result i32)
    (local $len i32)
    (local $res i32)
    (local $i i32)
    local.get $arr
    i32.eqz
    if
      i32.const 0
      call $__str_alloc
      return
    end
    local.get $arr
    i32.load
    local.set $len
    local.get $len
    call $__str_alloc
    local.set $res
    i32.const 0
    local.set $i
    (block $break_fca
      (loop $loop_fca
        local.get $i
        local.get $len
        i32.ge_s
        br_if $break_fca
        local.get $res
        local.get $i
        i32.add
        local.get $arr
        i32.const 4
        i32.add
        local.get $i
        i32.const 4
        i32.mul
        i32.add
        i32.load
        i32.store8
        local.get $i
        i32.const 1
        i32.add
        local.set $i
        br $loop_fca
      )
    )
    local.get $res
    local.get $len
    i32.add
    i32.const 0
    i32.store8
    local.get $res
  )
  (func $__str_from_bool (export "__str_from_bool") (param $b i32) (result i32)
    local.get $b
    if (result i32)
      i32.const 5714
    else
      i32.const 5719
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
  (func $__str_startsWith (export "__str_startsWith") (param $s i32) (param $prefix i32) (result i32)
    (local $lenS i32)
    (local $lenP i32)
    (local $i i32)
    (local $cS i32)
    (local $cP i32)
    local.get $prefix
    i32.eqz
    if
      i32.const 1
      return
    end
    local.get $s
    i32.eqz
    if
      i32.const 0
      return
    end
    local.get $s
    call $__str_length
    local.set $lenS
    local.get $prefix
    call $__str_length
    local.set $lenP
    local.get $lenS
    local.get $lenP
    i32.lt_s
    if
      i32.const 0
      return
    end
    i32.const 0
    local.set $i
    (block $break_sw
      (loop $loop_sw
        local.get $i
        local.get $lenP
        i32.ge_s
        br_if $break_sw
        local.get $s
        local.get $i
        i32.add
        i32.load8_u
        local.set $cS
        local.get $prefix
        local.get $i
        i32.add
        i32.load8_u
        local.set $cP
        local.get $cS
        local.get $cP
        i32.ne
        if
          i32.const 0
          return
        end
        local.get $i
        i32.const 1
        i32.add
        local.set $i
        br $loop_sw
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
  (func $__str_trim (export "__str_trim") (param $s i32) (result i32)
    (local $len i32)
    (local $start i32)
    (local $end i32)
    (local $subLen i32)
    (local $res i32)
    local.get $s
    i32.eqz
    if
      i32.const 0
      return
    end
    local.get $s
    call $__str_length
    local.set $len
    local.get $len
    i32.eqz
    if
      i32.const 0
      call $__str_alloc
      return
    end
    i32.const 0
    local.set $start
    (block $break_trim_start
      (loop $loop_trim_start
        local.get $start
        local.get $len
        i32.ge_s
        br_if $break_trim_start
        local.get $s
        local.get $start
        i32.add
        i32.load8_u
        i32.const 32
        i32.gt_u
        br_if $break_trim_start
        local.get $start
        i32.const 1
        i32.add
        local.set $start
        br $loop_trim_start
      )
    )
    local.get $start
    local.get $len
    i32.ge_s
    if
      i32.const 0
      call $__str_alloc
      return
    end
    local.get $len
    i32.const 1
    i32.sub
    local.set $end
    (block $break_trim_end
      (loop $loop_trim_end
        local.get $end
        local.get $start
        i32.lt_s
        br_if $break_trim_end
        local.get $s
        local.get $end
        i32.add
        i32.load8_u
        i32.const 32
        i32.gt_u
        br_if $break_trim_end
        local.get $end
        i32.const 1
        i32.sub
        local.set $end
        br $loop_trim_end
      )
    )
    local.get $end
    local.get $start
    i32.sub
    i32.const 1
    i32.add
    local.set $subLen
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
  (func $__str_strip (export "__str_strip") (param $s i32) (result i32)
    local.get $s
    call $__str_trim
  )
  (func $__str_toLowerCase (export "__str_toLowerCase") (param $s i32) (result i32)
    (local $len i32)
    (local $res i32)
    (local $i i32)
    (local $c i32)
    local.get $s
    i32.eqz
    if
      i32.const 0
      return
    end
    local.get $s
    call $__str_length
    local.set $len
    local.get $len
    call $__str_alloc
    local.set $res
    i32.const 0
    local.set $i
    (block $break_lc
      (loop $loop_lc
        local.get $i
        local.get $len
        i32.ge_s
        br_if $break_lc
        local.get $s
        local.get $i
        i32.add
        i32.load8_u
        local.set $c
        local.get $c
        i32.const 65
        i32.ge_s
        local.get $c
        i32.const 90
        i32.le_s
        i32.and
        if
          local.get $c
          i32.const 32
          i32.add
          local.set $c
        end
        local.get $res
        local.get $i
        i32.add
        local.get $c
        i32.store8
        local.get $i
        i32.const 1
        i32.add
        local.set $i
        br $loop_lc
      )
    )
    local.get $res
    local.get $len
    i32.add
    i32.const 0
    i32.store8
    local.get $res
  )
  (func $__str_toUpperCase (export "__str_toUpperCase") (param $s i32) (result i32)
    (local $len i32)
    (local $res i32)
    (local $i i32)
    (local $c i32)
    local.get $s
    i32.eqz
    if
      i32.const 0
      return
    end
    local.get $s
    call $__str_length
    local.set $len
    local.get $len
    call $__str_alloc
    local.set $res
    i32.const 0
    local.set $i
    (block $break_uc
      (loop $loop_uc
        local.get $i
        local.get $len
        i32.ge_s
        br_if $break_uc
        local.get $s
        local.get $i
        i32.add
        i32.load8_u
        local.set $c
        local.get $c
        i32.const 97
        i32.ge_s
        local.get $c
        i32.const 122
        i32.le_s
        i32.and
        if
          local.get $c
          i32.const 32
          i32.sub
          local.set $c
        end
        local.get $res
        local.get $i
        i32.add
        local.get $c
        i32.store8
        local.get $i
        i32.const 1
        i32.add
        local.set $i
        br $loop_uc
      )
    )
    local.get $res
    local.get $len
    i32.add
    i32.const 0
    i32.store8
    local.get $res
  )
  (func $__str_contains (export "__str_contains") (param $s i32) (param $sub i32) (result i32)
    local.get $s
    local.get $sub
    call $__str_indexOf
    i32.const -1
    i32.ne
  )
  (func $__str_endsWith (export "__str_endsWith") (param $s i32) (param $suffix i32) (result i32)
    (local $lenS i32)
    (local $lenP i32)
    (local $offset i32)
    (local $i i32)
    (local $cS i32)
    (local $cP i32)
    local.get $suffix
    i32.eqz
    if
      i32.const 1
      return
    end
    local.get $s
    i32.eqz
    if
      i32.const 0
      return
    end
    local.get $s
    call $__str_length
    local.set $lenS
    local.get $suffix
    call $__str_length
    local.set $lenP
    local.get $lenS
    local.get $lenP
    i32.lt_s
    if
      i32.const 0
      return
    end
    local.get $lenS
    local.get $lenP
    i32.sub
    local.set $offset
    i32.const 0
    local.set $i
    (block $break_ew
      (loop $loop_ew
        local.get $i
        local.get $lenP
        i32.ge_s
        br_if $break_ew
        local.get $s
        local.get $offset
        i32.add
        local.get $i
        i32.add
        i32.load8_u
        local.set $cS
        local.get $suffix
        local.get $i
        i32.add
        i32.load8_u
        local.set $cP
        local.get $cS
        local.get $cP
        i32.ne
        if
          i32.const 0
          return
        end
        local.get $i
        i32.const 1
        i32.add
        local.set $i
        br $loop_ew
      )
    )
    i32.const 1
  )
  (func $__str_compareTo (export "__str_compareTo") (param $s1 i32) (param $s2 i32) (result i32)
    (local $len1 i32)
    (local $len2 i32)
    (local $lim i32)
    (local $k i32)
    (local $c1 i32)
    (local $c2 i32)
    local.get $s1
    local.get $s2
    i32.eq
    if
      i32.const 0
      return
    end
    local.get $s1
    i32.eqz
    if
      i32.const -1
      return
    end
    local.get $s2
    i32.eqz
    if
      i32.const 1
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
    local.get $len1
    local.get $len2
    i32.lt_s
    select
    local.set $lim
    i32.const 0
    local.set $k
    (block $break_cmp
      (loop $loop_cmp
        local.get $k
        local.get $lim
        i32.ge_s
        br_if $break_cmp
        local.get $s1
        local.get $k
        i32.add
        i32.load8_u
        local.set $c1
        local.get $s2
        local.get $k
        i32.add
        i32.load8_u
        local.set $c2
        local.get $c1
        local.get $c2
        i32.ne
        if
          local.get $c1
          local.get $c2
          i32.sub
          return
        end
        local.get $k
        i32.const 1
        i32.add
        local.set $k
        br $loop_cmp
      )
    )
    local.get $len1
    local.get $len2
    i32.sub
  )
  (func $__str_equalsIgnoreCase (export "__str_equalsIgnoreCase") (param $s1 i32) (param $s2 i32) (result i32)
    (local $len1 i32)
    (local $len2 i32)
    (local $k i32)
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
    local.get $s1
    call $__str_length
    local.set $len1
    local.get $s2
    call $__str_length
    local.set $len2
    local.get $len1
    local.get $len2
    i32.ne
    if
      i32.const 0
      return
    end
    i32.const 0
    local.set $k
    (block $break_eqic
      (loop $loop_eqic
        local.get $k
        local.get $len1
        i32.ge_s
        br_if $break_eqic
        local.get $s1
        local.get $k
        i32.add
        i32.load8_u
        local.set $c1
        local.get $s2
        local.get $k
        i32.add
        i32.load8_u
        local.set $c2
        local.get $c1
        i32.const 65
        i32.ge_s
        local.get $c1
        i32.const 90
        i32.le_s
        i32.and
        if
          local.get $c1
          i32.const 32
          i32.add
          local.set $c1
        end
        local.get $c2
        i32.const 65
        i32.ge_s
        local.get $c2
        i32.const 90
        i32.le_s
        i32.and
        if
          local.get $c2
          i32.const 32
          i32.add
          local.set $c2
        end
        local.get $c1
        local.get $c2
        i32.ne
        if
          i32.const 0
          return
        end
        local.get $k
        i32.const 1
        i32.add
        local.set $k
        br $loop_eqic
      )
    )
    i32.const 1
  )
  (func $__str_repeat (export "__str_repeat") (param $s i32) (param $count i32) (result i32)
    (local $len i32)
    (local $totalLen i32)
    (local $res i32)
    (local $k i32)
    local.get $count
    i32.const 0
    i32.le_s
    if
      i32.const 0
      call $__str_alloc
      return
    end
    local.get $s
    i32.eqz
    if
      i32.const 0
      call $__str_alloc
      return
    end
    local.get $s
    call $__str_length
    local.set $len
    local.get $len
    i32.eqz
    if
      i32.const 0
      call $__str_alloc
      return
    end
    local.get $len
    local.get $count
    i32.mul
    local.set $totalLen
    local.get $totalLen
    call $__str_alloc
    local.set $res
    i32.const 0
    local.set $k
    (block $break_rep
      (loop $loop_rep
        local.get $k
        local.get $count
        i32.ge_s
        br_if $break_rep
        local.get $res
        local.get $k
        local.get $len
        i32.mul
        i32.add
        local.get $s
        local.get $len
        memory.copy
        local.get $k
        i32.const 1
        i32.add
        local.set $k
        br $loop_rep
      )
    )
    local.get $res
    local.get $totalLen
    i32.add
    i32.const 0
    i32.store8
    local.get $res
  )
  (func $__str_toCharArray (export "__str_toCharArray") (param $s i32) (result i32)
    (local $len i32)
    (local $arr i32)
    (local $i i32)
    local.get $s
    i32.eqz
    if
      i32.const 0
      local.set $len
    else
      local.get $s
      call $__str_length
      local.set $len
    end
    local.get $len
    i32.const 4
    i32.mul
    i32.const 4
    i32.add
    call $__heap_alloc
    local.set $arr
    local.get $arr
    local.get $len
    i32.store
    i32.const 0
    local.set $i
    (block $break_tca
      (loop $loop_tca
        local.get $i
        local.get $len
        i32.ge_s
        br_if $break_tca
        local.get $arr
        i32.const 4
        i32.add
        local.get $i
        i32.const 4
        i32.mul
        i32.add
        local.get $s
        local.get $i
        i32.add
        i32.load8_u
        i32.store
        local.get $i
        i32.const 1
        i32.add
        local.set $i
        br $loop_tca
      )
    )
    local.get $arr
  )
  (func $__str_getBytes (export "__str_getBytes") (param $s i32) (result i32)
    local.get $s
    call $__str_toCharArray
  )
  (func $__str_replace_char (export "__str_replace_char") (param $s i32) (param $oldC i32) (param $newC i32) (result i32)
    (local $len i32)
    (local $res i32)
    (local $i i32)
    (local $c i32)
    local.get $s
    i32.eqz
    if
      i32.const 0
      return
    end
    local.get $s
    call $__str_length
    local.set $len
    local.get $len
    call $__str_alloc
    local.set $res
    i32.const 0
    local.set $i
    (block $break_rc
      (loop $loop_rc
        local.get $i
        local.get $len
        i32.ge_s
        br_if $break_rc
        local.get $s
        local.get $i
        i32.add
        i32.load8_u
        local.set $c
        local.get $c
        local.get $oldC
        i32.eq
        if
          local.get $newC
          local.set $c
        end
        local.get $res
        local.get $i
        i32.add
        local.get $c
        i32.store8
        local.get $i
        i32.const 1
        i32.add
        local.set $i
        br $loop_rc
      )
    )
    local.get $res
    local.get $len
    i32.add
    i32.const 0
    i32.store8
    local.get $res
  )
  (func $__str_replace_str (export "__str_replace_str") (param $s i32) (param $target i32) (param $replacement i32) (result i32)
    (local $lenS i32)
    (local $lenT i32)
    (local $lenR i32)
    (local $sb i32)
    (local $i i32)
    (local $match i32)
    (local $j i32)
    local.get $s
    i32.eqz
    if
      i32.const 0
      return
    end
    local.get $target
    i32.eqz
    if
      local.get $s
      return
    end
    local.get $target
    call $__str_length
    local.set $lenT
    local.get $lenT
    i32.eqz
    if
      local.get $s
      return
    end
    local.get $s
    call $__str_length
    local.set $lenS
    local.get $replacement
    if (result i32)
      local.get $replacement
      call $__str_length
    else
      i32.const 0
    end
    local.set $lenR
    i32.const 1024
    call $__sb_new
    local.set $sb
    i32.const 0
    local.set $i
    (block $break_rep_str
      (loop $loop_rep_str
        local.get $i
        local.get $lenS
        i32.ge_s
        br_if $break_rep_str
        local.get $i
        local.get $lenT
        i32.add
        local.get $lenS
        i32.le_s
        if
          i32.const 1
          local.set $match
          i32.const 0
          local.set $j
          (block $break_chk
            (loop $loop_chk
              local.get $j
              local.get $lenT
              i32.ge_s
              br_if $break_chk
              local.get $s
              local.get $i
              i32.add
              local.get $j
              i32.add
              i32.load8_u
              local.get $target
              local.get $j
              i32.add
              i32.load8_u
              i32.ne
              if
                i32.const 0
                local.set $match
                br $break_chk
              end
              local.get $j
              i32.const 1
              i32.add
              local.set $j
              br $loop_chk
            )
          )
          local.get $match
          if
            local.get $sb
            local.get $replacement
            call $__sb_append_str
            drop
            local.get $i
            local.get $lenT
            i32.add
            local.set $i
            br $loop_rep_str
          end
        end
        local.get $sb
        local.get $s
        local.get $i
        i32.add
        i32.load8_u
        call $__sb_append_char
        drop
        local.get $i
        i32.const 1
        i32.add
        local.set $i
        br $loop_rep_str
      )
    )
    local.get $sb
    call $__sb_toString
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
  (func $__sb_append_f64 (export "__sb_append_f64") (param $sb i32) (param $val f64) (result i32)
    local.get $sb
    local.get $val
    call $__str_from_f64
    call $__sb_append_str
  )
  (func $__sb_insert_str (export "__sb_insert_str") (param $sb i32) (param $offset i32) (param $str i32) (result i32)
    (local $cur_len i32)
    (local $str_len i32)
    (local $off i32)
    local.get $sb
    i32.eqz
    if
      i32.const 0
      return
    end
    local.get $str
    i32.eqz
    if
      local.get $sb
      return
    end
    local.get $str
    call $__str_length
    local.set $str_len
    local.get $str_len
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
    local.get $offset
    local.set $off
    local.get $off
    i32.const 0
    i32.lt_s
    if
      i32.const 0
      local.set $off
    end
    local.get $off
    local.get $cur_len
    i32.gt_s
    if
      local.get $cur_len
      local.set $off
    end
    local.get $sb
    i32.const 8
    i32.add
    local.get $off
    i32.add
    local.get $str_len
    i32.add
    local.get $sb
    i32.const 8
    i32.add
    local.get $off
    i32.add
    local.get $cur_len
    local.get $off
    i32.sub
    memory.copy
    local.get $sb
    i32.const 8
    i32.add
    local.get $off
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
  (func $__sb_insert_i32 (export "__sb_insert_i32") (param $sb i32) (param $offset i32) (param $val i32) (result i32)
    local.get $sb
    local.get $offset
    local.get $val
    call $__str_from_i32
    call $__sb_insert_str
  )
  (func $__sb_insert_char (export "__sb_insert_char") (param $sb i32) (param $offset i32) (param $c i32) (result i32)
    local.get $sb
    local.get $offset
    local.get $c
    call $__str_from_char
    call $__sb_insert_str
  )
  (func $__sb_insert_bool (export "__sb_insert_bool") (param $sb i32) (param $offset i32) (param $b i32) (result i32)
    local.get $sb
    local.get $offset
    local.get $b
    call $__str_from_bool
    call $__sb_insert_str
  )
  (func $__sb_insert_f64 (export "__sb_insert_f64") (param $sb i32) (param $offset i32) (param $d f64) (result i32)
    local.get $sb
    local.get $offset
    local.get $d
    call $__str_from_f64
    call $__sb_insert_str
  )
  (func $__sb_delete (export "__sb_delete") (param $sb i32) (param $start i32) (param $end i32) (result i32)
    (local $cur_len i32)
    (local $s i32)
    (local $e i32)
    (local $del_len i32)
    local.get $sb
    i32.eqz
    if
      i32.const 0
      return
    end
    local.get $sb
    i32.const 4
    i32.add
    i32.load
    local.set $cur_len
    local.get $start
    local.set $s
    local.get $s
    i32.const 0
    i32.lt_s
    if
      i32.const 0
      local.set $s
    end
    local.get $s
    local.get $cur_len
    i32.gt_s
    if
      local.get $cur_len
      local.set $s
    end
    local.get $end
    local.set $e
    local.get $e
    local.get $cur_len
    i32.gt_s
    if
      local.get $cur_len
      local.set $e
    end
    local.get $s
    local.get $e
    i32.ge_s
    if
      local.get $sb
      return
    end
    local.get $e
    local.get $s
    i32.sub
    local.set $del_len
    local.get $sb
    i32.const 8
    i32.add
    local.get $s
    i32.add
    local.get $sb
    i32.const 8
    i32.add
    local.get $e
    i32.add
    local.get $cur_len
    local.get $e
    i32.sub
    memory.copy
    local.get $sb
    i32.const 4
    i32.add
    local.get $cur_len
    local.get $del_len
    i32.sub
    i32.store
    local.get $sb
    i32.const 8
    i32.add
    local.get $cur_len
    local.get $del_len
    i32.sub
    i32.add
    i32.const 0
    i32.store8
    local.get $sb
  )
  (func $__sb_deleteCharAt (export "__sb_deleteCharAt") (param $sb i32) (param $idx i32) (result i32)
    local.get $sb
    local.get $idx
    local.get $idx
    i32.const 1
    i32.add
    call $__sb_delete
  )
  (func $__sb_reverse (export "__sb_reverse") (param $sb i32) (result i32)
    (local $cur_len i32)
    (local $i i32)
    (local $j i32)
    (local $ci i32)
    (local $cj i32)
    local.get $sb
    i32.eqz
    if
      i32.const 0
      return
    end
    local.get $sb
    i32.const 4
    i32.add
    i32.load
    local.set $cur_len
    local.get $cur_len
    i32.const 1
    i32.le_s
    if
      local.get $sb
      return
    end
    i32.const 0
    local.set $i
    local.get $cur_len
    i32.const 1
    i32.sub
    local.set $j
    (block $break_rev
      (loop $loop_rev
        local.get $i
        local.get $j
        i32.ge_s
        br_if $break_rev
        local.get $sb
        i32.const 8
        i32.add
        local.get $i
        i32.add
        i32.load8_u
        local.set $ci
        local.get $sb
        i32.const 8
        i32.add
        local.get $j
        i32.add
        i32.load8_u
        local.set $cj
        local.get $sb
        i32.const 8
        i32.add
        local.get $i
        i32.add
        local.get $cj
        i32.store8
        local.get $sb
        i32.const 8
        i32.add
        local.get $j
        i32.add
        local.get $ci
        i32.store8
        local.get $i
        i32.const 1
        i32.add
        local.set $i
        local.get $j
        i32.const 1
        i32.sub
        local.set $j
        br $loop_rev
      )
    )
    local.get $sb
  )
  (func $__sb_replace (export "__sb_replace") (param $sb i32) (param $start i32) (param $end i32) (param $str i32) (result i32)
    local.get $sb
    local.get $start
    local.get $end
    call $__sb_delete
    local.set $sb
    local.get $sb
    local.get $start
    local.get $str
    call $__sb_insert_str
  )
  (func $__sb_substring (export "__sb_substring") (param $sb i32) (param $start i32) (param $end i32) (result i32)
    (local $cur_len i32)
    (local $s i32)
    (local $e i32)
    (local $subLen i32)
    (local $res i32)
    local.get $sb
    i32.eqz
    if
      i32.const 0
      call $__str_alloc
      return
    end
    local.get $sb
    i32.const 4
    i32.add
    i32.load
    local.set $cur_len
    local.get $start
    local.set $s
    local.get $s
    i32.const 0
    i32.lt_s
    if
      i32.const 0
      local.set $s
    end
    local.get $end
    local.set $e
    local.get $e
    local.get $cur_len
    i32.gt_s
    if
      local.get $cur_len
      local.set $e
    end
    local.get $s
    local.get $e
    i32.ge_s
    if
      i32.const 0
      call $__str_alloc
      return
    end
    local.get $e
    local.get $s
    i32.sub
    local.set $subLen
    local.get $subLen
    call $__str_alloc
    local.set $res
    local.get $res
    local.get $sb
    i32.const 8
    i32.add
    local.get $s
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
  (func $__sb_indexOf (export "__sb_indexOf") (param $sb i32) (param $str i32) (result i32)
    local.get $sb
    i32.eqz
    if
      i32.const -1
      return
    end
    local.get $sb
    i32.const 8
    i32.add
    local.get $str
    call $__str_indexOf
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
  ;; --- Standard Library Numeric & Math Functions ---
  (func $__i64_min (export "__i64_min") (param $a i64) (param $b i64) (result i64)
    local.get $a
    local.get $b
    local.get $a
    local.get $b
    i64.lt_s
    select
  )
  (func $__i64_max (export "__i64_max") (param $a i64) (param $b i64) (result i64)
    local.get $a
    local.get $b
    local.get $a
    local.get $b
    i64.gt_s
    select
  )
  (func $__i64_compare (export "__i64_compare") (param $a i64) (param $b i64) (result i32)
    local.get $a
    local.get $b
    i64.eq
    if (result i32)
      i32.const 0
    else
      i32.const -1
      i32.const 1
      local.get $a
      local.get $b
      i64.lt_s
      select
    end
  )
  (func $__i64_compare_u (export "__i64_compare_u") (param $a i64) (param $b i64) (result i32)
    local.get $a
    local.get $b
    i64.eq
    if (result i32)
      i32.const 0
    else
      i32.const -1
      i32.const 1
      local.get $a
      local.get $b
      i64.lt_u
      select
    end
  )
  (func $__i64_signum (export "__i64_signum") (param $a i64) (result i32)
    local.get $a
    i64.const 0
    i64.eq
    if (result i32)
      i32.const 0
    else
      i32.const -1
      i32.const 1
      local.get $a
      i64.const 0
      i64.lt_s
      select
    end
  )
  (func $__f64_compare (export "__f64_compare") (param $a f64) (param $b f64) (result i32)
    (local $a_nan i32)
    (local $b_nan i32)
    local.get $a
    local.get $a
    f64.ne
    local.set $a_nan
    local.get $b
    local.get $b
    f64.ne
    local.set $b_nan
    local.get $a_nan
    local.get $b_nan
    i32.and
    if (result i32)
      i32.const 0
    else
      local.get $a_nan
      if (result i32)
        i32.const 1
      else
        local.get $b_nan
        if (result i32)
          i32.const -1
        else
          local.get $a
          local.get $b
          f64.eq
          if (result i32)
            i32.const 0
          else
            i32.const -1
            i32.const 1
            local.get $a
            local.get $b
            f64.lt
            select
          end
        end
      end
    end
  )
  (func $__i32_reverse_bytes (export "__i32_reverse_bytes") (param $x i32) (result i32)
    local.get $x
    i32.const 24
    i32.shl
    local.get $x
    i32.const 8
    i32.shl
    i32.const 16711680
    i32.and
    i32.or
    local.get $x
    i32.const 8
    i32.shr_u
    i32.const 65280
    i32.and
    i32.or
    local.get $x
    i32.const 24
    i32.shr_u
    i32.or
  )
  (func $__i32_reverse_bits (export "__i32_reverse_bits") (param $x i32) (result i32)
    (local $res i32)
    (local $i i32)
    i32.const 0
    local.set $res
    i32.const 0
    local.set $i
    (block $break_rev
      (loop $loop_rev
        local.get $i
        i32.const 32
        i32.ge_s
        br_if $break_rev
        local.get $res
        i32.const 1
        i32.shl
        local.get $x
        i32.const 1
        i32.and
        i32.or
        local.set $res
        local.get $x
        i32.const 1
        i32.shr_u
        local.set $x
        local.get $i
        i32.const 1
        i32.add
        local.set $i
        br $loop_rev
      )
    )
    local.get $res
  )
  (func $__i64_reverse_bytes (export "__i64_reverse_bytes") (param $x i64) (result i64)
    (local $lo i32)
    (local $hi i32)
    local.get $x
    i32.wrap_i64
    call $__i32_reverse_bytes
    local.set $lo
    local.get $x
    i64.const 32
    i64.shr_u
    i32.wrap_i64
    call $__i32_reverse_bytes
    local.set $hi
    local.get $lo
    i64.extend_i32_u
    i64.const 32
    i64.shl
    local.get $hi
    i64.extend_i32_u
    i64.or
  )
  (func $__i64_reverse_bits (export "__i64_reverse_bits") (param $x i64) (result i64)
    (local $res i64)
    (local $i i32)
    i64.const 0
    local.set $res
    i32.const 0
    local.set $i
    (block $break_rev64
      (loop $loop_rev64
        local.get $i
        i32.const 64
        i32.ge_s
        br_if $break_rev64
        local.get $res
        i64.const 1
        i64.shl
        local.get $x
        i64.const 1
        i64.and
        i64.or
        local.set $res
        local.get $x
        i64.const 1
        i64.shr_u
        local.set $x
        local.get $i
        i32.const 1
        i32.add
        local.set $i
        br $loop_rev64
      )
    )
    local.get $res
  )
  ;; --- String Parsing & Formatting Functions ---
  (func $__str_parse_i32 (export "__str_parse_i32") (param $s i32) (result i32)
    (local $res i32)
    (local $neg i32)
    (local $i i32)
    (local $c i32)
    local.get $s
    i32.eqz
    if
      i32.const 0
      return
    end
    i32.const 0
    local.set $res
    i32.const 0
    local.set $neg
    i32.const 0
    local.set $i
    local.get $s
    i32.load8_u
    local.set $c
    local.get $c
    i32.const 45
    i32.eq
    if
      i32.const 1
      local.set $neg
      i32.const 1
      local.set $i
    else
      local.get $c
      i32.const 43
      i32.eq
      if
        i32.const 1
        local.set $i
      end
    end
    (block $break_p32
      (loop $loop_p32
        local.get $s
        local.get $i
        i32.add
        i32.load8_u
        local.set $c
        local.get $c
        i32.const 48
        i32.lt_u
        br_if $break_p32
        local.get $c
        i32.const 57
        i32.gt_u
        br_if $break_p32
        local.get $res
        i32.const 10
        i32.mul
        local.get $c
        i32.const 48
        i32.sub
        i32.add
        local.set $res
        local.get $i
        i32.const 1
        i32.add
        local.set $i
        br $loop_p32
      )
    )
    local.get $neg
    if (result i32)
      i32.const 0
      local.get $res
      i32.sub
    else
      local.get $res
    end
  )
  (func $__str_parse_i32_radix (export "__str_parse_i32_radix") (param $s i32) (param $radix i32) (result i32)
    (local $res i32)
    (local $neg i32)
    (local $i i32)
    (local $c i32)
    (local $d i32)
    local.get $s
    i32.eqz
    if
      i32.const 0
      return
    end
    local.get $radix
    i32.const 2
    i32.lt_s
    local.get $radix
    i32.const 36
    i32.gt_s
    i32.or
    if
      i32.const 10
      local.set $radix
    end
    i32.const 0
    local.set $res
    i32.const 0
    local.set $neg
    i32.const 0
    local.set $i
    local.get $s
    i32.load8_u
    local.set $c
    local.get $c
    i32.const 45
    i32.eq
    if
      i32.const 1
      local.set $neg
      i32.const 1
      local.set $i
    else
      local.get $c
      i32.const 43
      i32.eq
      if
        i32.const 1
        local.set $i
      end
    end
    (block $break_pradix
      (loop $loop_pradix
        local.get $s
        local.get $i
        i32.add
        i32.load8_u
        local.set $c
        local.get $c
        i32.const 48
        i32.ge_u
        local.get $c
        i32.const 57
        i32.le_u
        i32.and
        if
          local.get $c
          i32.const 48
          i32.sub
          local.set $d
        else
          local.get $c
          i32.const 97
          i32.ge_u
          local.get $c
          i32.const 122
          i32.le_u
          i32.and
          if
            local.get $c
            i32.const 87
            i32.sub
            local.set $d
          else
            local.get $c
            i32.const 65
            i32.ge_u
            local.get $c
            i32.const 90
            i32.le_u
            i32.and
            if
              local.get $c
              i32.const 55
              i32.sub
              local.set $d
            else
              br $break_pradix
            end
          end
        end
        local.get $d
        local.get $radix
        i32.ge_s
        br_if $break_pradix
        local.get $res
        local.get $radix
        i32.mul
        local.get $d
        i32.add
        local.set $res
        local.get $i
        i32.const 1
        i32.add
        local.set $i
        br $loop_pradix
      )
    )
    local.get $neg
    if (result i32)
      i32.const 0
      local.get $res
      i32.sub
    else
      local.get $res
    end
  )
  (func $__str_from_i32_radix (export "__str_from_i32_radix") (param $val i32) (param $radix i32) (result i32)
    (local $temp i32)
    (local $len i32)
    (local $is_neg i32)
    (local $res i32)
    (local $idx i32)
    (local $rem i32)
    local.get $radix
    i32.const 2
    i32.lt_s
    local.get $radix
    i32.const 36
    i32.gt_s
    i32.or
    if
      i32.const 10
      local.set $radix
    end
    local.get $val
    i32.eqz
    if
      i32.const 1
      call $__str_alloc
      local.set $res
      local.get $res
      i32.const 48
      i32.store8
      local.get $res
      return
    end
    i32.const 0
    local.set $is_neg
    local.get $val
    i32.const 0
    i32.lt_s
    if
      i32.const 1
      local.set $is_neg
      i32.const 0
      local.get $val
      i32.sub
      local.set $temp
    else
      local.get $val
      local.set $temp
    end
    local.get $is_neg
    local.set $len
    (block $break_cradix
      (loop $loop_cradix
        local.get $temp
        i32.eqz
        br_if $break_cradix
        local.get $len
        i32.const 1
        i32.add
        local.set $len
        local.get $temp
        local.get $radix
        i32.div_u
        local.set $temp
        br $loop_cradix
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
      i32.const 0
      local.get $val
      i32.sub
      local.set $temp
    else
      local.get $val
      local.set $temp
    end
    local.get $len
    local.set $idx
    (block $break_wradix
      (loop $loop_wradix
        local.get $temp
        i32.eqz
        br_if $break_wradix
        local.get $idx
        i32.const 1
        i32.sub
        local.set $idx
        local.get $temp
        local.get $radix
        i32.rem_u
        local.set $rem
        local.get $res
        local.get $idx
        i32.add
        local.get $rem
        i32.const 10
        i32.lt_u
        if (result i32)
          i32.const 48
          local.get $rem
          i32.add
        else
          i32.const 87
          local.get $rem
          i32.add
        end
        i32.store8
        local.get $temp
        local.get $radix
        i32.div_u
        local.set $temp
        br $loop_wradix
      )
    )
    local.get $res
  )
  (func $__str_to_hex_i32 (export "__str_to_hex_i32") (param $val i32) (result i32)
    (local $temp i32)
    (local $len i32)
    (local $res i32)
    (local $idx i32)
    (local $rem i32)
    local.get $val
    i32.eqz
    if
      i32.const 1
      call $__str_alloc
      local.set $res
      local.get $res
      i32.const 48
      i32.store8
      local.get $res
      return
    end
    local.get $val
    local.set $temp
    i32.const 0
    local.set $len
    (block $break_chex
      (loop $loop_chex
        local.get $temp
        i32.eqz
        br_if $break_chex
        local.get $len
        i32.const 1
        i32.add
        local.set $len
        local.get $temp
        i32.const 4
        i32.shr_u
        local.set $temp
        br $loop_chex
      )
    )
    local.get $len
    call $__str_alloc
    local.set $res
    local.get $val
    local.set $temp
    local.get $len
    local.set $idx
    (block $break_whex
      (loop $loop_whex
        local.get $temp
        i32.eqz
        br_if $break_whex
        local.get $idx
        i32.const 1
        i32.sub
        local.set $idx
        local.get $temp
        i32.const 15
        i32.and
        local.set $rem
        local.get $res
        local.get $idx
        i32.add
        local.get $rem
        i32.const 10
        i32.lt_u
        if (result i32)
          i32.const 48
          local.get $rem
          i32.add
        else
          i32.const 87
          local.get $rem
          i32.add
        end
        i32.store8
        local.get $temp
        i32.const 4
        i32.shr_u
        local.set $temp
        br $loop_whex
      )
    )
    local.get $res
  )
  (func $__str_to_bin_i32 (export "__str_to_bin_i32") (param $val i32) (result i32)
    local.get $val
    i32.const 2
    call $__str_from_i32_radix
  )
  (func $__str_to_oct_i32 (export "__str_to_oct_i32") (param $val i32) (result i32)
    local.get $val
    i32.const 8
    call $__str_from_i32_radix
  )
  (func $__str_parse_i64 (export "__str_parse_i64") (param $s i32) (result i64)
    (local $res i64)
    (local $neg i32)
    (local $i i32)
    (local $c i32)
    local.get $s
    i32.eqz
    if
      i64.const 0
      return
    end
    i64.const 0
    local.set $res
    i32.const 0
    local.set $neg
    i32.const 0
    local.set $i
    local.get $s
    i32.load8_u
    local.set $c
    local.get $c
    i32.const 45
    i32.eq
    if
      i32.const 1
      local.set $neg
      i32.const 1
      local.set $i
    else
      local.get $c
      i32.const 43
      i32.eq
      if
        i32.const 1
        local.set $i
      end
    end
    (block $break_p64
      (loop $loop_p64
        local.get $s
        local.get $i
        i32.add
        i32.load8_u
        local.set $c
        local.get $c
        i32.const 48
        i32.lt_u
        br_if $break_p64
        local.get $c
        i32.const 57
        i32.gt_u
        br_if $break_p64
        local.get $res
        i64.const 10
        i64.mul
        local.get $c
        i32.const 48
        i32.sub
        i64.extend_i32_u
        i64.add
        local.set $res
        local.get $i
        i32.const 1
        i32.add
        local.set $i
        br $loop_p64
      )
    )
    local.get $neg
    if (result i64)
      i64.const 0
      local.get $res
      i64.sub
    else
      local.get $res
    end
  )
  (func $__str_from_i64 (export "__str_from_i64") (param $val i64) (result i32)
    (local $temp i64)
    (local $len i32)
    (local $is_neg i32)
    (local $res i32)
    (local $idx i32)
    (local $rem i64)
    local.get $val
    i64.eqz
    if
      i32.const 1
      call $__str_alloc
      local.set $res
      local.get $res
      i32.const 48
      i32.store8
      local.get $res
      return
    end
    i32.const 0
    local.set $is_neg
    local.get $val
    i64.const 0
    i64.lt_s
    if
      i32.const 1
      local.set $is_neg
      i64.const 0
      local.get $val
      i64.sub
      local.set $temp
    else
      local.get $val
      local.set $temp
    end
    local.get $is_neg
    local.set $len
    (block $break_c64
      (loop $loop_c64
        local.get $temp
        i64.eqz
        br_if $break_c64
        local.get $len
        i32.const 1
        i32.add
        local.set $len
        local.get $temp
        i64.const 10
        i64.div_u
        local.set $temp
        br $loop_c64
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
      i64.const 0
      local.get $val
      i64.sub
      local.set $temp
    else
      local.get $val
      local.set $temp
    end
    local.get $len
    local.set $idx
    (block $break_w64
      (loop $loop_w64
        local.get $temp
        i64.eqz
        br_if $break_w64
        local.get $idx
        i32.const 1
        i32.sub
        local.set $idx
        local.get $temp
        i64.const 10
        i64.rem_u
        local.set $rem
        local.get $res
        local.get $idx
        i32.add
        i32.const 48
        local.get $rem
        i32.wrap_i64
        i32.add
        i32.store8
        local.get $temp
        i64.const 10
        i64.div_u
        local.set $temp
        br $loop_w64
      )
    )
    local.get $res
  )
  (func $__str_to_hex_i64 (export "__str_to_hex_i64") (param $val i64) (result i32)
    (local $temp i64)
    (local $len i32)
    (local $res i32)
    (local $idx i32)
    (local $rem i64)
    local.get $val
    i64.eqz
    if
      i32.const 1
      call $__str_alloc
      local.set $res
      local.get $res
      i32.const 48
      i32.store8
      local.get $res
      return
    end
    local.get $val
    local.set $temp
    i32.const 0
    local.set $len
    (block $break_chex64
      (loop $loop_chex64
        local.get $temp
        i64.eqz
        br_if $break_chex64
        local.get $len
        i32.const 1
        i32.add
        local.set $len
        local.get $temp
        i64.const 4
        i64.shr_u
        local.set $temp
        br $loop_chex64
      )
    )
    local.get $len
    call $__str_alloc
    local.set $res
    local.get $val
    local.set $temp
    local.get $len
    local.set $idx
    (block $break_whex64
      (loop $loop_whex64
        local.get $temp
        i64.eqz
        br_if $break_whex64
        local.get $idx
        i32.const 1
        i32.sub
        local.set $idx
        local.get $temp
        i64.const 15
        i64.and
        local.set $rem
        local.get $res
        local.get $idx
        i32.add
        local.get $rem
        i64.const 10
        i64.lt_u
        if (result i32)
          i32.const 48
          local.get $rem
          i32.wrap_i64
          i32.add
        else
          i32.const 87
          local.get $rem
          i32.wrap_i64
          i32.add
        end
        i32.store8
        local.get $temp
        i64.const 4
        i64.shr_u
        local.set $temp
        br $loop_whex64
      )
    )
    local.get $res
  )
  (func $__str_to_bin_i64 (export "__str_to_bin_i64") (param $val i64) (result i32)
    (local $temp i64)
    (local $len i32)
    (local $res i32)
    (local $idx i32)
    (local $rem i64)
    local.get $val
    i64.eqz
    if
      i32.const 1
      call $__str_alloc
      local.set $res
      local.get $res
      i32.const 48
      i32.store8
      local.get $res
      return
    end
    local.get $val
    local.set $temp
    i32.const 0
    local.set $len
    (block $break_cbin64
      (loop $loop_cbin64
        local.get $temp
        i64.eqz
        br_if $break_cbin64
        local.get $len
        i32.const 1
        i32.add
        local.set $len
        local.get $temp
        i64.const 1
        i64.shr_u
        local.set $temp
        br $loop_cbin64
      )
    )
    local.get $len
    call $__str_alloc
    local.set $res
    local.get $val
    local.set $temp
    local.get $len
    local.set $idx
    (block $break_wbin64
      (loop $loop_wbin64
        local.get $temp
        i64.eqz
        br_if $break_wbin64
        local.get $idx
        i32.const 1
        i32.sub
        local.set $idx
        local.get $temp
        i64.const 1
        i64.and
        local.set $rem
        local.get $res
        local.get $idx
        i32.add
        i32.const 48
        local.get $rem
        i32.wrap_i64
        i32.add
        i32.store8
        local.get $temp
        i64.const 1
        i64.shr_u
        local.set $temp
        br $loop_wbin64
      )
    )
    local.get $res
  )
  (func $__str_to_oct_i64 (export "__str_to_oct_i64") (param $val i64) (result i32)
    (local $temp i64)
    (local $len i32)
    (local $res i32)
    (local $idx i32)
    (local $rem i64)
    local.get $val
    i64.eqz
    if
      i32.const 1
      call $__str_alloc
      local.set $res
      local.get $res
      i32.const 48
      i32.store8
      local.get $res
      return
    end
    local.get $val
    local.set $temp
    i32.const 0
    local.set $len
    (block $break_coct64
      (loop $loop_coct64
        local.get $temp
        i64.eqz
        br_if $break_coct64
        local.get $len
        i32.const 1
        i32.add
        local.set $len
        local.get $temp
        i64.const 3
        i64.shr_u
        local.set $temp
        br $loop_coct64
      )
    )
    local.get $len
    call $__str_alloc
    local.set $res
    local.get $val
    local.set $temp
    local.get $len
    local.set $idx
    (block $break_woct64
      (loop $loop_woct64
        local.get $temp
        i64.eqz
        br_if $break_woct64
        local.get $idx
        i32.const 1
        i32.sub
        local.set $idx
        local.get $temp
        i64.const 7
        i64.and
        local.set $rem
        local.get $res
        local.get $idx
        i32.add
        i32.const 48
        local.get $rem
        i32.wrap_i64
        i32.add
        i32.store8
        local.get $temp
        i64.const 3
        i64.shr_u
        local.set $temp
        br $loop_woct64
      )
    )
    local.get $res
  )
  (func $__str_parse_f64 (export "__str_parse_f64") (param $s i32) (result f64)
    (local $res f64)
    (local $neg i32)
    (local $i i32)
    (local $c i32)
    (local $div f64)
    (local $exp_neg i32)
    (local $exp_val i32)
    local.get $s
    i32.eqz
    if
      f64.const 0.0
      return
    end
    f64.const 0.0
    local.set $res
    i32.const 0
    local.set $neg
    i32.const 0
    local.set $i
    local.get $s
    i32.load8_u
    local.set $c
    local.get $c
    i32.const 45
    i32.eq
    if
      i32.const 1
      local.set $neg
      i32.const 1
      local.set $i
    else
      local.get $c
      i32.const 43
      i32.eq
      if
        i32.const 1
        local.set $i
      end
    end
    (block $break_fint
      (loop $loop_fint
        local.get $s
        local.get $i
        i32.add
        i32.load8_u
        local.set $c
        local.get $c
        i32.const 48
        i32.lt_u
        br_if $break_fint
        local.get $c
        i32.const 57
        i32.gt_u
        br_if $break_fint
        local.get $res
        f64.const 10.0
        f64.mul
        local.get $c
        i32.const 48
        i32.sub
        f64.convert_i32_u
        f64.add
        local.set $res
        local.get $i
        i32.const 1
        i32.add
        local.set $i
        br $loop_fint
      )
    )
    local.get $c
    i32.const 46
    i32.eq
    if
      local.get $i
      i32.const 1
      i32.add
      local.set $i
      f64.const 10.0
      local.set $div
      (block $break_ffrac
        (loop $loop_ffrac
          local.get $s
          local.get $i
          i32.add
          i32.load8_u
          local.set $c
          local.get $c
          i32.const 48
          i32.lt_u
          br_if $break_ffrac
          local.get $c
          i32.const 57
          i32.gt_u
          br_if $break_ffrac
          local.get $res
          local.get $c
          i32.const 48
          i32.sub
          f64.convert_i32_u
          local.get $div
          f64.div
          f64.add
          local.set $res
          local.get $div
          f64.const 10.0
          f64.mul
          local.set $div
          local.get $i
          i32.const 1
          i32.add
          local.set $i
          br $loop_ffrac
        )
      )
    end
    local.get $c
    i32.const 101
    i32.eq
    local.get $c
    i32.const 69
    i32.eq
    i32.or
    if
      local.get $i
      i32.const 1
      i32.add
      local.set $i
      i32.const 0
      local.set $exp_neg
      local.get $s
      local.get $i
      i32.add
      i32.load8_u
      local.set $c
      local.get $c
      i32.const 45
      i32.eq
      if
        i32.const 1
        local.set $exp_neg
        local.get $i
        i32.const 1
        i32.add
        local.set $i
      else
        local.get $c
        i32.const 43
        i32.eq
        if
          i32.const 1
          local.set $i
        end
      end
      i32.const 0
      local.set $exp_val
      (block $break_fexp
        (loop $loop_fexp
          local.get $s
          local.get $i
          i32.add
          i32.load8_u
          local.set $c
          local.get $c
          i32.const 48
          i32.lt_u
          br_if $break_fexp
          local.get $c
          i32.const 57
          i32.gt_u
          br_if $break_fexp
          local.get $exp_val
          i32.const 10
          i32.mul
          local.get $c
          i32.const 48
          i32.sub
          i32.add
          local.set $exp_val
          local.get $i
          i32.const 1
          i32.add
          local.set $i
          br $loop_fexp
        )
      )
      (block $break_appexp
        (loop $loop_appexp
          local.get $exp_val
          i32.eqz
          br_if $break_appexp
          local.get $exp_neg
          if
            local.get $res
            f64.const 10.0
            f64.div
            local.set $res
          else
            local.get $res
            f64.const 10.0
            f64.mul
            local.set $res
          end
          local.get $exp_val
          i32.const 1
          i32.sub
          local.set $exp_val
          br $loop_appexp
        )
      )
    end
    local.get $neg
    if (result f64)
      f64.const 0.0
      local.get $res
      f64.sub
    else
      local.get $res
    end
  )
  (func $__str_parse_bool (export "__str_parse_bool") (param $s i32) (result i32)
    (local $c0 i32)
    (local $c1 i32)
    (local $c2 i32)
    (local $c3 i32)
    (local $c4 i32)
    local.get $s
    i32.eqz
    if
      i32.const 0
      return
    end
    local.get $s
    i32.load8_u offset=0
    local.set $c0
    local.get $s
    i32.load8_u offset=1
    local.set $c1
    local.get $s
    i32.load8_u offset=2
    local.set $c2
    local.get $s
    i32.load8_u offset=3
    local.set $c3
    local.get $s
    i32.load8_u offset=4
    local.set $c4
    local.get $c4
    i32.eqz
    local.get $c0
    i32.const 116
    i32.eq
    local.get $c0
    i32.const 84
    i32.eq
    i32.or
    i32.and
    local.get $c1
    i32.const 114
    i32.eq
    local.get $c1
    i32.const 82
    i32.eq
    i32.or
    i32.and
    local.get $c2
    i32.const 117
    i32.eq
    local.get $c2
    i32.const 85
    i32.eq
    i32.or
    i32.and
    local.get $c3
    i32.const 101
    i32.eq
    local.get $c3
    i32.const 69
    i32.eq
    i32.or
    i32.and
  )
  ;; --- Character Utility Functions ---
  (func $__char_is_digit (export "__char_is_digit") (param $c i32) (result i32)
    local.get $c
    i32.const 48
    i32.ge_u
    local.get $c
    i32.const 57
    i32.le_u
    i32.and
  )
  (func $__char_is_letter (export "__char_is_letter") (param $c i32) (result i32)
    local.get $c
    i32.const 65
    i32.ge_u
    local.get $c
    i32.const 90
    i32.le_u
    i32.and
    local.get $c
    i32.const 97
    i32.ge_u
    local.get $c
    i32.const 122
    i32.le_u
    i32.and
    i32.or
  )
  (func $__char_is_letter_or_digit (export "__char_is_letter_or_digit") (param $c i32) (result i32)
    local.get $c
    call $__char_is_letter
    local.get $c
    call $__char_is_digit
    i32.or
  )
  (func $__char_is_whitespace (export "__char_is_whitespace") (param $c i32) (result i32)
    local.get $c
    i32.const 32
    i32.eq
    local.get $c
    i32.const 9
    i32.eq
    i32.or
    local.get $c
    i32.const 10
    i32.eq
    i32.or
    local.get $c
    i32.const 13
    i32.eq
    i32.or
    local.get $c
    i32.const 12
    i32.eq
    i32.or
  )
  (func $__char_is_upper (export "__char_is_upper") (param $c i32) (result i32)
    local.get $c
    i32.const 65
    i32.ge_u
    local.get $c
    i32.const 90
    i32.le_u
    i32.and
  )
  (func $__char_is_lower (export "__char_is_lower") (param $c i32) (result i32)
    local.get $c
    i32.const 97
    i32.ge_u
    local.get $c
    i32.const 122
    i32.le_u
    i32.and
  )
  (func $__char_to_upper (export "__char_to_upper") (param $c i32) (result i32)
    local.get $c
    call $__char_is_lower
    if (result i32)
      local.get $c
      i32.const 32
      i32.sub
    else
      local.get $c
    end
  )
  (func $__char_to_lower (export "__char_to_lower") (param $c i32) (result i32)
    local.get $c
    call $__char_is_upper
    if (result i32)
      local.get $c
      i32.const 32
      i32.add
    else
      local.get $c
    end
  )
  (func $__char_digit (export "__char_digit") (param $c i32) (param $radix i32) (result i32)
    (local $d i32)
    local.get $radix
    i32.const 2
    i32.lt_s
    local.get $radix
    i32.const 36
    i32.gt_s
    i32.or
    if
      i32.const -1
      return
    end
    local.get $c
    i32.const 48
    i32.ge_u
    local.get $c
    i32.const 57
    i32.le_u
    i32.and
    if
      local.get $c
      i32.const 48
      i32.sub
      local.set $d
    else
      local.get $c
      i32.const 97
      i32.ge_u
      local.get $c
      i32.const 122
      i32.le_u
      i32.and
      if
        local.get $c
        i32.const 87
        i32.sub
        local.set $d
      else
        local.get $c
        i32.const 65
        i32.ge_u
        local.get $c
        i32.const 90
        i32.le_u
        i32.and
        if
          local.get $c
          i32.const 55
          i32.sub
          local.set $d
        else
          i32.const -1
          return
        end
      end
    end
    local.get $d
    local.get $radix
    i32.lt_s
    if (result i32)
      local.get $d
    else
      i32.const -1
    end
  )
  (func $__char_for_digit (export "__char_for_digit") (param $d i32) (param $radix i32) (result i32)
    local.get $d
    i32.const 0
    i32.lt_s
    local.get $d
    local.get $radix
    i32.ge_s
    i32.or
    local.get $radix
    i32.const 2
    i32.lt_s
    i32.or
    local.get $radix
    i32.const 36
    i32.gt_s
    i32.or
    if
      i32.const 0
      return
    end
    local.get $d
    i32.const 10
    i32.lt_s
    if (result i32)
      i32.const 48
      local.get $d
      i32.add
    else
      i32.const 87
      local.get $d
      i32.add
    end
  )
  ;; --- System Utility & Environment Functions ---
  (func $__system_identity_hash_code (export "__system_identity_hash_code") (param $obj i32) (result i32)
    local.get $obj
  )
  (func $__system_line_separator (export "__system_line_separator") (result i32)
    (local $res i32)
    i32.const 1
    call $__str_alloc
    local.set $res
    local.get $res
    i32.const 10
    i32.store8
    local.get $res
    i32.const 1
    i32.add
    i32.const 0
    i32.store8
    local.get $res
  )
  (func $__system_get_property (export "__system_get_property") (param $key i32) (result i32)
    (local $klen i32)
    (local $res i32)
    local.get $key
    i32.eqz
    if
      i32.const 0
      return
    end
    local.get $key
    call $__str_length
    local.set $klen
    local.get $klen
    i32.const 14
    i32.eq
    if
      local.get $key
      i32.load8_u
      i32.const 108
      i32.eq
      if
        local.get $key
        i32.const 13
        i32.add
        i32.load8_u
        i32.const 114
        i32.eq
        if
          call $__system_line_separator
          return
        end
      end
      local.get $key
      i32.load8_u
      i32.const 102
      i32.eq
      if
        local.get $key
        i32.const 13
        i32.add
        i32.load8_u
        i32.const 114
        i32.eq
        if
          i32.const 47
          call $__str_from_char
          return
        end
      end
      local.get $key
      i32.load8_u
      i32.const 112
      i32.eq
      if
        i32.const 58
        call $__str_from_char
        return
      end
    end
    local.get $klen
    i32.const 12
    i32.eq
    if
      local.get $key
      i32.load8_u
      i32.const 106
      i32.eq
      if
        i32.const 17
        call $__str_from_i32
        return
      end
    end
    local.get $klen
    i32.const 11
    i32.eq
    if
      local.get $key
      i32.load8_u
      i32.const 106
      i32.eq
      if
        i32.const 5
        call $__str_alloc
        local.set $res
        local.get $res
        i32.const 0
        i32.add
        i32.const 106
        i32.store8
        local.get $res
        i32.const 1
        i32.add
        i32.const 97
        i32.store8
        local.get $res
        i32.const 2
        i32.add
        i32.const 118
        i32.store8
        local.get $res
        i32.const 3
        i32.add
        i32.const 97
        i32.store8
        local.get $res
        i32.const 4
        i32.add
        i32.const 119
        i32.store8
        local.get $res
        i32.const 5
        i32.add
        i32.const 0
        i32.store8
        local.get $res
        return
      end
    end
    local.get $klen
    i32.const 7
    i32.eq
    if
      local.get $key
      i32.load8_u
      i32.const 111
      i32.eq
      if
        local.get $key
        i32.const 3
        i32.add
        i32.load8_u
        i32.const 110
        i32.eq
        if
          i32.const 11
          call $__str_alloc
          local.set $res
          local.get $res
          i32.const 0
          i32.add
          i32.const 87
          i32.store8
          local.get $res
          i32.const 1
          i32.add
          i32.const 101
          i32.store8
          local.get $res
          i32.const 2
          i32.add
          i32.const 98
          i32.store8
          local.get $res
          i32.const 3
          i32.add
          i32.const 65
          i32.store8
          local.get $res
          i32.const 4
          i32.add
          i32.const 115
          i32.store8
          local.get $res
          i32.const 5
          i32.add
          i32.const 115
          i32.store8
          local.get $res
          i32.const 6
          i32.add
          i32.const 101
          i32.store8
          local.get $res
          i32.const 7
          i32.add
          i32.const 109
          i32.store8
          local.get $res
          i32.const 8
          i32.add
          i32.const 98
          i32.store8
          local.get $res
          i32.const 9
          i32.add
          i32.const 108
          i32.store8
          local.get $res
          i32.const 10
          i32.add
          i32.const 121
          i32.store8
          local.get $res
          i32.const 11
          i32.add
          i32.const 0
          i32.store8
          local.get $res
          return
        end
        local.get $key
        i32.const 3
        i32.add
        i32.load8_u
        i32.const 97
        i32.eq
        if
          i32.const 6
          call $__str_alloc
          local.set $res
          local.get $res
          i32.const 0
          i32.add
          i32.const 119
          i32.store8
          local.get $res
          i32.const 1
          i32.add
          i32.const 97
          i32.store8
          local.get $res
          i32.const 2
          i32.add
          i32.const 115
          i32.store8
          local.get $res
          i32.const 3
          i32.add
          i32.const 109
          i32.store8
          local.get $res
          i32.const 4
          i32.add
          i32.const 51
          i32.store8
          local.get $res
          i32.const 5
          i32.add
          i32.const 50
          i32.store8
          local.get $res
          i32.const 6
          i32.add
          i32.const 0
          i32.store8
          local.get $res
          return
        end
      end
    end
    local.get $klen
    i32.const 13
    i32.eq
    if
      local.get $key
      i32.load8_u
      i32.const 102
      i32.eq
      if
        i32.const 5
        call $__str_alloc
        local.set $res
        local.get $res
        i32.const 0
        i32.add
        i32.const 85
        i32.store8
        local.get $res
        i32.const 1
        i32.add
        i32.const 84
        i32.store8
        local.get $res
        i32.const 2
        i32.add
        i32.const 70
        i32.store8
        local.get $res
        i32.const 3
        i32.add
        i32.const 45
        i32.store8
        local.get $res
        i32.const 4
        i32.add
        i32.const 56
        i32.store8
        local.get $res
        i32.const 5
        i32.add
        i32.const 0
        i32.store8
        local.get $res
        return
      end
    end
    local.get $klen
    i32.const 8
    i32.eq
    if
      local.get $key
      i32.load8_u
      i32.const 117
      i32.eq
      if
        local.get $key
        i32.const 5
        i32.add
        i32.load8_u
        i32.const 100
        i32.eq
        if
          i32.const 47
          call $__str_from_char
          return
        end
      end
    end
    local.get $klen
    i32.const 9
    i32.eq
    if
      local.get $key
      i32.load8_u
      i32.const 117
      i32.eq
      if
        local.get $key
        i32.const 5
        i32.add
        i32.load8_u
        i32.const 104
        i32.eq
        if
          i32.const 5
          call $__str_alloc
          local.set $res
          local.get $res
          i32.const 0
          i32.add
          i32.const 47
          i32.store8
          local.get $res
          i32.const 1
          i32.add
          i32.const 104
          i32.store8
          local.get $res
          i32.const 2
          i32.add
          i32.const 111
          i32.store8
          local.get $res
          i32.const 3
          i32.add
          i32.const 109
          i32.store8
          local.get $res
          i32.const 4
          i32.add
          i32.const 101
          i32.store8
          local.get $res
          i32.const 5
          i32.add
          i32.const 0
          i32.store8
          local.get $res
          return
        end
        local.get $key
        i32.const 5
        i32.add
        i32.load8_u
        i32.const 110
        i32.eq
        if
          i32.const 4
          call $__str_alloc
          local.set $res
          local.get $res
          i32.const 0
          i32.add
          i32.const 119
          i32.store8
          local.get $res
          i32.const 1
          i32.add
          i32.const 97
          i32.store8
          local.get $res
          i32.const 2
          i32.add
          i32.const 115
          i32.store8
          local.get $res
          i32.const 3
          i32.add
          i32.const 109
          i32.store8
          local.get $res
          i32.const 4
          i32.add
          i32.const 0
          i32.store8
          local.get $res
          return
        end
      end
    end
    local.get $key
    call $__system_getenv
    local.set $res
    local.get $res
  )
  (func $__system_get_property_default (export "__system_get_property_default") (param $key i32) (param $def i32) (result i32)
    (local $res i32)
    local.get $key
    call $__system_get_property
    local.set $res
    local.get $res
    i32.eqz
    if
      local.get $def
      return
    end
    local.get $res
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
    i32.eqz
    local.get $c
    i32.const -1
    i32.eq
    i32.and
    if (result i32)
      i32.const 0
    else
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
    end
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
  (func $__wasi_clock_time_ns (export "__wasi_clock_time_ns") (result i64)
    i32.const 0
    i64.const 1000
    i32.const 32
    call $__wasi_clock_time_get
    drop
    i32.const 32
    i64.load
  )
  (func $__wasi_clock_time_ms_i64 (export "__wasi_clock_time_ms_i64") (result i64)
    call $__wasi_clock_time_ns
    i64.const 1000000
    i64.div_u
  )
  (func $__wasi_random_int (export "__wasi_random_int") (result i32)
    i32.const 32
    i32.const 4
    call $__wasi_random_get
    drop
    i32.const 32
    i32.load
  )
  (func $__system_getenv (export "__system_getenv") (param $key i32) (result i32)
    (local $count i32)
    (local $buf_size i32)
    (local $ptrs i32)
    (local $buf i32)
    (local $i i32)
    (local $entry i32)
    (local $eq_pos i32)
    (local $key_len i32)
    (local $val_len i32)
    (local $res i32)
    (local $j i32)
    (local $match i32)
    local.get $key
    i32.eqz
    if
      i32.const 0
      return
    end
    local.get $key
    call $__str_length
    local.set $key_len
    i32.const 32
    i32.const 36
    call $__wasi_environ_sizes_get
    drop
    i32.const 32
    i32.load
    local.set $count
    i32.const 36
    i32.load
    local.set $buf_size
    local.get $count
    i32.eqz
    if
      i32.const 0
      return
    end
    local.get $buf_size
    i32.eqz
    if
      i32.const 0
      return
    end
    local.get $count
    i32.const 4
    i32.mul
    call $__heap_alloc
    local.set $ptrs
    local.get $buf_size
    call $__heap_alloc
    local.set $buf
    local.get $ptrs
    local.get $buf
    call $__wasi_environ_get
    drop
    i32.const 0
    local.set $i
    (block $break_env
      (loop $loop_env
        local.get $i
        local.get $count
        i32.ge_s
        br_if $break_env
        local.get $ptrs
        local.get $i
        i32.const 4
        i32.mul
        i32.add
        i32.load
        local.set $entry
        i32.const 0
        local.set $eq_pos
        (block $break_find_eq
          (loop $loop_find_eq
            local.get $entry
            local.get $eq_pos
            i32.add
            i32.load8_u
            i32.eqz
            br_if $break_find_eq
            local.get $entry
            local.get $eq_pos
            i32.add
            i32.load8_u
            i32.const 61
            i32.eq
            br_if $break_find_eq
            local.get $eq_pos
            i32.const 1
            i32.add
            local.set $eq_pos
            br $loop_find_eq
          )
        )
        local.get $eq_pos
        local.get $key_len
        i32.eq
        if
          i32.const 1
          local.set $match
          i32.const 0
          local.set $j
          (block $break_cmp
            (loop $loop_cmp
              local.get $j
              local.get $key_len
              i32.ge_s
              br_if $break_cmp
              local.get $key
              local.get $j
              i32.add
              i32.load8_u
              local.get $entry
              local.get $j
              i32.add
              i32.load8_u
              i32.ne
              if
                i32.const 0
                local.set $match
                br $break_cmp
              end
              local.get $j
              i32.const 1
              i32.add
              local.set $j
              br $loop_cmp
            )
          )
          local.get $match
          if
            local.get $entry
            local.get $eq_pos
            i32.add
            i32.const 1
            i32.add
            call $__str_length
            local.set $val_len
            local.get $val_len
            call $__str_alloc
            local.set $res
            local.get $res
            local.get $entry
            local.get $eq_pos
            i32.add
            i32.const 1
            i32.add
            local.get $val_len
            memory.copy
            local.get $res
            local.get $val_len
            i32.add
            i32.const 0
            i32.store8
            local.get $res
            return
          end
        end
        local.get $i
        i32.const 1
        i32.add
        local.set $i
        br $loop_env
      )
    )
    i32.const 0
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    i32.const 4263
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    i32.const 4263
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    i32.const 4275
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    local.get $url
    local.set $__str_arg_ptr
    local.get $__str_arg_ptr
    local.get $__str_arg_ptr
    call $__str_length
    i32.const 4289
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    local.get $message
    local.set $__str_arg_ptr
    local.get $__str_arg_ptr
    local.get $__str_arg_ptr
    call $__str_length
    call $dom_confirm
    return
  )
  (func $testMathHardware (export "testMathHardware") (param $a f64) (param $b f64) (result f64)
    (local $f f64)
    (local $c f64)
    (local $s f64)
    (local $ab f64)
    (local $mn f64)
    (local $mx f64)
    (local $r f64)
    (local $cp f64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    local.get $a
    f64.floor
    local.set $f
    local.get $a
    f64.ceil
    local.set $c
    local.get $b
    f64.sqrt
    local.set $s
    local.get $a
    f64.abs
    local.set $ab
    local.get $a
    local.get $b
    f64.min
    local.set $mn
    local.get $a
    local.get $b
    f64.max
    local.set $mx
    local.get $a
    f64.nearest
    local.set $r
    local.get $a
    local.get $b
    f64.copysign
    local.set $cp
    local.get $f
    local.get $c
    f64.add
    local.get $s
    f64.add
    local.get $ab
    f64.add
    local.get $mn
    f64.add
    local.get $mx
    f64.add
    local.get $r
    f64.add
    local.get $cp
    f64.add
    return
  )
  (func $testMathTranscendental (export "testMathTranscendental") (param $x f64) (result f64)
    (local $s f64)
    (local $c f64)
    (local $t f64)
    (local $e f64)
    (local $l f64)
    (local $p f64)
    (local $h f64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    local.get $x
    call $dom_math_sin
    local.set $s
    local.get $x
    call $dom_math_cos
    local.set $c
    local.get $x
    call $dom_math_tan
    local.set $t
    local.get $x
    call $dom_math_exp
    local.set $e
    local.get $x
    call $dom_math_log
    local.set $l
    local.get $x
    f64.const 2.0
    call $dom_math_pow
    local.set $p
    local.get $x
    local.get $x
    call $dom_math_hypot
    local.set $h
    local.get $s
    local.get $c
    f64.add
    local.get $t
    f64.add
    local.get $e
    f64.add
    local.get $l
    f64.add
    local.get $p
    f64.add
    local.get $h
    f64.add
    return
  )
  (func $testMathConstants (export "testMathConstants") (result f64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    f64.const 3.141592653589793
    f64.const 2.718281828459045
    f64.add
    return
  )
  (func $testIntegerWrappers (export "testIntegerWrappers") (param $s i32) (param $val i32) (result i32)
    (local $parsed i32)
    (local $parsedHex i32)
    (local $str i32)
    (local $hex i32)
    (local $bin i32)
    (local $oct i32)
    (local $bc i32)
    (local $clz i32)
    (local $ctz i32)
    (local $revBytes i32)
    (local $rotl i32)
    (local $rotr i32)
    (local $cmp i32)
    (local $sgn i32)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    local.get $s
    call $__str_parse_i32
    local.set $parsed
    i32.const 4304
    i32.const 16
    call $__str_parse_i32_radix
    local.set $parsedHex
    local.get $val
    call $__str_from_i32
    local.set $str
    local.get $val
    call $__str_to_hex_i32
    local.set $hex
    local.get $val
    call $__str_to_bin_i32
    local.set $bin
    local.get $val
    call $__str_to_oct_i32
    local.set $oct
    local.get $val
    i32.popcnt
    local.set $bc
    local.get $val
    i32.clz
    local.set $clz
    local.get $val
    i32.ctz
    local.set $ctz
    local.get $val
    call $__i32_reverse_bytes
    local.set $revBytes
    local.get $val
    i32.const 2
    i32.rotl
    local.set $rotl
    local.get $val
    i32.const 2
    i32.rotr
    local.set $rotr
    local.get $val
    local.get $parsed
    local.set $__str_sub_start
    local.set $__str_arg_ptr
    local.get $__str_arg_ptr
    local.get $__str_sub_start
    i32.eq
    if (result i32)
      i32.const 0
    else
      i32.const -1
      i32.const 1
      local.get $__str_arg_ptr
      local.get $__str_sub_start
      i32.lt_s
      select
    end
    local.set $cmp
    local.get $val
    local.set $__str_arg_ptr
    local.get $__str_arg_ptr
    i32.const 0
    i32.eq
    if (result i32)
      i32.const 0
    else
      i32.const -1
      i32.const 1
      local.get $__str_arg_ptr
      i32.const 0
      i32.lt_s
      select
    end
    local.set $sgn
    local.get $parsed
    local.get $parsedHex
    i32.add
    local.get $bc
    i32.add
    local.get $clz
    i32.add
    local.get $ctz
    i32.add
    local.get $cmp
    i32.add
    local.get $sgn
    i32.add
    return
  )
  (func $testLongWrappers (export "testLongWrappers") (param $s i32) (param $val i64) (result i64)
    (local $parsed i64)
    (local $str i32)
    (local $hex i32)
    (local $bin i32)
    (local $bc i32)
    (local $clz i32)
    (local $ctz i32)
    (local $revBytes i64)
    (local $cmp i32)
    (local $sgn i32)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    local.get $s
    call $__str_parse_i64
    local.set $parsed
    local.get $val
    call $__str_from_i64
    local.set $str
    local.get $val
    call $__str_to_hex_i64
    local.set $hex
    local.get $val
    call $__str_to_bin_i64
    local.set $bin
    local.get $val
    i64.popcnt
    i32.wrap_i64
    local.set $bc
    local.get $val
    i64.clz
    i32.wrap_i64
    local.set $clz
    local.get $val
    i64.ctz
    i32.wrap_i64
    local.set $ctz
    local.get $val
    call $__i64_reverse_bytes
    local.set $revBytes
    local.get $val
    local.get $parsed
    call $__i64_compare
    local.set $cmp
    local.get $val
    call $__i64_signum
    local.set $sgn
    local.get $parsed
    local.get $bc
    i64.extend_i32_s
    i64.add
    local.get $clz
    i64.extend_i32_s
    i64.add
    local.get $ctz
    i64.extend_i32_s
    i64.add
    local.get $cmp
    i64.extend_i32_s
    i64.add
    local.get $sgn
    i64.extend_i32_s
    i64.add
    return
  )
  (func $testDoubleWrappers (export "testDoubleWrappers") (param $s i32) (param $val f64) (result f64)
    (local $parsed f64)
    (local $str i32)
    (local $nan i32)
    (local $inf i32)
    (local $fin i32)
    (local $cmp i32)
    (local $bits i64)
    (local $fromBits f64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    local.get $s
    call $__str_parse_f64
    local.set $parsed
    local.get $val
    call $__str_from_f64
    local.set $str
    local.get $val
    local.set $__f64_tmp
    local.get $__f64_tmp
    local.get $__f64_tmp
    f64.ne
    local.set $nan
    local.get $val
    local.set $__f64_tmp
    local.get $__f64_tmp
    f64.const inf
    f64.eq
    local.get $__f64_tmp
    f64.const -inf
    f64.eq
    i32.or
    local.set $inf
    local.get $val
    local.set $__f64_tmp
    local.get $__f64_tmp
    local.get $__f64_tmp
    f64.ne
    local.get $__f64_tmp
    f64.const inf
    f64.eq
    i32.or
    local.get $__f64_tmp
    f64.const -inf
    f64.eq
    i32.or
    i32.eqz
    local.set $fin
    local.get $val
    local.get $parsed
    call $__f64_compare
    local.set $cmp
    local.get $val
    i64.reinterpret_f64
    local.set $bits
    local.get $bits
    f64.reinterpret_i64
    local.set $fromBits
    local.get $parsed
    local.get $fromBits
    f64.add
    local.get $cmp
    f64.convert_i32_s
    f64.add
    local.get $fin
    if (result f64)
    f64.const 1.0
    else
    f64.const 0.0
    end
    f64.add
    return
  )
  (func $testFloatWrappers (export "testFloatWrappers") (param $s i32) (param $val f32) (result f32)
    (local $parsed f32)
    (local $str i32)
    (local $nan i32)
    (local $inf i32)
    (local $fin i32)
    (local $bits i32)
    (local $fromBits f32)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    local.get $s
    call $__str_parse_f64
    f32.demote_f64
    local.set $parsed
    local.get $val
    f64.promote_f32
    call $__str_from_f64
    local.set $str
    local.get $val
    local.set $__f32_tmp
    local.get $__f32_tmp
    local.get $__f32_tmp
    f32.ne
    local.set $nan
    local.get $val
    local.set $__f32_tmp
    local.get $__f32_tmp
    f32.const inf
    f32.eq
    local.get $__f32_tmp
    f32.const -inf
    f32.eq
    i32.or
    local.set $inf
    local.get $val
    local.set $__f32_tmp
    local.get $__f32_tmp
    local.get $__f32_tmp
    f32.ne
    local.get $__f32_tmp
    f32.const inf
    f32.eq
    i32.or
    local.get $__f32_tmp
    f32.const -inf
    f32.eq
    i32.or
    i32.eqz
    local.set $fin
    local.get $val
    i32.reinterpret_f32
    local.set $bits
    local.get $bits
    f32.reinterpret_i32
    local.set $fromBits
    local.get $parsed
    local.get $fromBits
    f32.add
    f64.promote_f32
    local.get $fin
    if (result f64)
    f64.const 1.0
    else
    f64.const 0.0
    end
    f64.add
    f32.demote_f64
    return
  )
  (func $testBooleanWrappers (export "testBooleanWrappers") (param $s i32) (param $val i32) (result i32)
    (local $parsed i32)
    (local $str i32)
    (local $cmp i32)
    (local $land i32)
    (local $lor i32)
    (local $lxor i32)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    local.get $s
    call $__str_parse_bool
    local.set $parsed
    local.get $val
    call $__str_from_bool
    local.set $str
    local.get $val
    local.get $parsed
    i32.sub
    local.set $cmp
    local.get $val
    local.get $parsed
    i32.and
    local.set $land
    local.get $val
    local.get $parsed
    i32.or
    local.set $lor
    local.get $val
    local.get $parsed
    i32.xor
    local.set $lxor
    local.get $parsed
    local.get $lor
    i32.and
    return
  )
  (func $testCharacterWrappers (export "testCharacterWrappers") (param $c i32) (result i32)
    (local $isD i32)
    (local $isL i32)
    (local $isLD i32)
    (local $isW i32)
    (local $isU i32)
    (local $isLow i32)
    (local $up i32)
    (local $low i32)
    (local $d i32)
    (local $fd i32)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    local.get $c
    call $__char_is_digit
    local.set $isD
    local.get $c
    call $__char_is_letter
    local.set $isL
    local.get $c
    call $__char_is_letter_or_digit
    local.set $isLD
    local.get $c
    call $__char_is_whitespace
    local.set $isW
    local.get $c
    call $__char_is_upper
    local.set $isU
    local.get $c
    call $__char_is_lower
    local.set $isLow
    local.get $c
    call $__char_to_upper
    local.set $up
    local.get $c
    call $__char_to_lower
    local.set $low
    i32.const 102
    i32.const 16
    call $__char_digit
    local.set $d
    i32.const 15
    i32.const 16
    call $__char_for_digit
    local.set $fd
    local.get $isL
    if (result i32)
    i32.const 1
    else
    i32.const 0
    end
    local.get $isU
    if (result i32)
    i32.const 2
    else
    i32.const 0
    end
    i32.add
    local.get $d
    i32.add
    local.get $up
    i32.add
    local.get $low
    i32.add
    return
  )
  (func $testBitwiseIntrinsics (export "testBitwiseIntrinsics") (param $i i32) (param $l i64) (result i32)
    (local $p1 i32)
    (local $z1 i32)
    (local $t1 i32)
    (local $r1 i32)
    (local $rb1 i32)
    (local $p2 i32)
    (local $z2 i32)
    (local $t2 i32)
    (local $r2 i64)
    (local $rb2 i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    local.get $i
    i32.popcnt
    local.set $p1
    local.get $i
    i32.clz
    local.set $z1
    local.get $i
    i32.ctz
    local.set $t1
    local.get $i
    call $__i32_reverse_bytes
    local.set $r1
    local.get $i
    call $__i32_reverse_bits
    local.set $rb1
    local.get $l
    i64.popcnt
    i32.wrap_i64
    local.set $p2
    local.get $l
    i64.clz
    i32.wrap_i64
    local.set $z2
    local.get $l
    i64.ctz
    i32.wrap_i64
    local.set $t2
    local.get $l
    call $__i64_reverse_bytes
    local.set $r2
    local.get $l
    call $__i64_reverse_bits
    local.set $rb2
    local.get $p1
    local.get $z1
    i32.add
    local.get $t1
    i32.add
    local.get $p2
    i32.add
    local.get $z2
    i32.add
    local.get $t2
    i32.add
    return
  )
  (func $testTryWithResourcesNormal (export "testTryWithResourcesNormal") (param $id i32) (result i32)
    (local $result i32)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $e i32)
    (local $__caught_exc i32)
    i32.const 0
    local.set $result
    (try
      (do
    i32.const 16
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 110
    i32.store offset=0
    local.get $__obj_alloc_ptr
    local.get $id
    call $ClosableSession__init
    local.get $__obj_alloc_ptr
    local.set $s
    (try
      (do
    local.get $s
    local.set $__obj_tmp
    local.get $__obj_tmp
    local.get $__obj_tmp
    i32.load offset=0
    i32.const 10
    i32.add
    call_indirect (type $t_sig_1)
    local.set $result
      )
      (catch_all
        local.get $s
        if
          local.get $s
          local.get $s
          i32.load offset=0
          i32.const 3
          i32.add
          call_indirect (type $t_sig_3)
        end
        (rethrow 0)
      )
    )
        local.get $s
        if
          local.get $s
          local.get $s
          i32.load offset=0
          i32.const 3
          i32.add
          call_indirect (type $t_sig_3)
        end
      )
      (catch $Exception
        local.set $__caught_exc
        local.get $__caught_exc
        local.set $e
    i32.const 0
    i32.const 1
    i32.sub
    local.set $result
      )
    )
    local.get $result
    return
  )
  (func $testTryWithResourcesException (export "testTryWithResourcesException") (param $id i32) (result i32)
    (local $result i32)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $e i32)
    (local $__caught_exc i32)
    i32.const 0
    local.set $result
    (try
      (do
    i32.const 16
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 110
    i32.store offset=0
    local.get $__obj_alloc_ptr
    local.get $id
    call $ClosableSession__init
    local.get $__obj_alloc_ptr
    local.set $s
    (try
      (do
    local.get $s
    local.set $__obj_tmp
    local.get $__obj_tmp
    local.get $__obj_tmp
    i32.load offset=0
    i32.const 10
    i32.add
    call_indirect (type $t_sig_1)
    local.set $result
    local.get $id
    i32.const 0
    i32.lt_s
    if
    i32.const 8
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 197
    i32.store offset=0
    local.get $__obj_alloc_ptr
    i32.const 4307
    call $IllegalArgumentException__init_1
    local.get $__obj_alloc_ptr
    throw $Exception
    end
      )
      (catch_all
        local.get $s
        if
          local.get $s
          local.get $s
          i32.load offset=0
          i32.const 3
          i32.add
          call_indirect (type $t_sig_3)
        end
        (rethrow 0)
      )
    )
        local.get $s
        if
          local.get $s
          local.get $s
          i32.load offset=0
          i32.const 3
          i32.add
          call_indirect (type $t_sig_3)
        end
      )
      (catch $Exception
        local.set $__caught_exc
        local.get $__caught_exc
        local.set $e
    i32.const 0
    i32.const 100
    i32.sub
    local.set $result
      )
    )
    local.get $result
    return
  )
  (func $testComparableAndIterablePhase5 (export "testComparableAndIterablePhase5") (param $w1 i32) (param $w2 i32) (param $rangeLimit i32) (result i32)
    (local $b1 i32)
    (local $b2 i32)
    (local $cmp i32)
    (local $range i32)
    (local $it i32)
    (local $sum i32)
    (local $item i32)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    i32.const 8
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 121
    i32.store offset=0
    local.get $__obj_alloc_ptr
    local.get $w1
    call $BoxItem__init
    local.get $__obj_alloc_ptr
    local.set $b1
    i32.const 8
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 121
    i32.store offset=0
    local.get $__obj_alloc_ptr
    local.get $w2
    call $BoxItem__init
    local.get $__obj_alloc_ptr
    local.set $b2
    local.get $b1
    local.set $__obj_tmp
    local.get $__obj_tmp
    local.get $b2
    local.get $__obj_tmp
    i32.load offset=0
    i32.const 4
    i32.add
    call_indirect (type $t_sig_2)
    local.set $cmp
    i32.const 8
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 141
    i32.store offset=0
    local.get $__obj_alloc_ptr
    local.get $rangeLimit
    call $IntRange__init
    local.get $__obj_alloc_ptr
    local.set $range
    local.get $range
    local.set $__obj_tmp
    local.get $__obj_tmp
    local.get $__obj_tmp
    i32.load offset=0
    i32.const 5
    i32.add
    call_indirect (type $t_sig_1)
    local.set $it
    i32.const 0
    local.set $sum
    (block $break_while_1
      (loop $loop_while_1
    local.get $it
    local.set $__obj_tmp
    local.get $__obj_tmp
    local.get $__obj_tmp
    i32.load offset=0
    i32.const 6
    i32.add
    call_indirect (type $t_sig_1)
        i32.eqz
        br_if $break_while_1
    local.get $it
    local.set $__obj_tmp
    local.get $__obj_tmp
    local.get $__obj_tmp
    i32.load offset=0
    i32.const 7
    i32.add
    call_indirect (type $t_sig_1)
    local.set $item
    local.get $sum
    local.get $item
    i32.load offset=4
    i32.add
    local.set $sum
        br $loop_while_1
      )
    )
    local.get $cmp
    local.get $sum
    i32.add
    return
  )
  (func $testInterfacesInstanceOf (export "testInterfacesInstanceOf") (param $type i32) (result i32)
    (local $s i32)
    (local $b i32)
    (local $r i32)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    local.get $type
    i32.const 1
    i32.eq
    if
    i32.const 16
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 110
    i32.store offset=0
    local.get $__obj_alloc_ptr
    i32.const 1
    call $ClosableSession__init
    local.get $__obj_alloc_ptr
    local.set $s
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
      i32.const 110
      i32.eq
      i32.or
      local.get $__vtable_tmp
      i32.const 0
      i32.eq
      i32.or
    end
    if (result i32)
    i32.const 10
    else
    i32.const 0
    end
    return
    else
    local.get $type
    i32.const 2
    i32.eq
    if
    i32.const 8
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 121
    i32.store offset=0
    local.get $__obj_alloc_ptr
    i32.const 5
    call $BoxItem__init
    local.get $__obj_alloc_ptr
    local.set $b
    local.get $b
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
      i32.const 121
      i32.eq
      i32.or
      local.get $__vtable_tmp
      i32.const 0
      i32.eq
      i32.or
    end
    if (result i32)
    i32.const 20
    else
    i32.const 0
    end
    return
    else
    local.get $type
    i32.const 3
    i32.eq
    if
    i32.const 8
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 141
    i32.store offset=0
    local.get $__obj_alloc_ptr
    i32.const 3
    call $IntRange__init
    local.get $__obj_alloc_ptr
    local.set $r
    local.get $r
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
      i32.const 141
      i32.eq
      i32.or
      local.get $__vtable_tmp
      i32.const 0
      i32.eq
      i32.or
    end
    if (result i32)
    i32.const 30
    else
    i32.const 0
    end
    return
    end
    end
    end
    i32.const 0
    return
  )
  (func $testThreadAndRunnablePhase6 (export "testThreadAndRunnablePhase6") (result i32)
    (local $task i32)
    (local $t i32)
    (local $isAlive i32)
    (local $id i32)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    i32.const 8
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 151
    i32.store offset=0
    local.get $__obj_alloc_ptr
    call $SimpleWorkerTask__init
    local.get $__obj_alloc_ptr
    local.set $task
    i32.const 8
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 240
    i32.store offset=0
    local.get $__obj_alloc_ptr
    local.get $task
    i32.const 4319
    call $Thread__init_2
    local.get $__obj_alloc_ptr
    local.set $t
    local.get $t
    call $Thread_start
    local.get $t
    call $Thread_isAlive
    local.set $isAlive
    local.get $t
    call $Thread_getId
    local.set $id
    local.get $task
    i32.load offset=4
    local.get $isAlive
    if (result i32)
    i32.const 1
    else
    i32.const 0
    end
    i32.add
    local.get $id
    i32.const 0
    i32.gt_s
    if (result i32)
    i32.const 2
    else
    i32.const 0
    end
    i32.add
    return
  )
  (func $testClassIntrospectionPhase6 (export "testClassIntrospectionPhase6") (result i32)
    (local $task i32)
    (local $cls i32)
    (local $name i32)
    (local $simpleName i32)
    (local $isIface i32)
    (local $isEnum i32)
    (local $isInst i32)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    i32.const 8
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 151
    i32.store offset=0
    local.get $__obj_alloc_ptr
    call $SimpleWorkerTask__init
    local.get $__obj_alloc_ptr
    local.set $task
    local.get $task
    local.set $__obj_tmp
    local.get $__obj_tmp
    local.get $__obj_tmp
    i32.load offset=0
    i32.const 9
    i32.add
    call_indirect (type $t_sig_1)
    local.set $cls
    local.get $cls
    call $Class_getName
    local.set $name
    local.get $cls
    call $Class_getSimpleName
    local.set $simpleName
    local.get $cls
    call $Class_isInterface
    local.set $isIface
    local.get $cls
    call $Class_isEnum
    local.set $isEnum
    local.get $cls
    local.get $task
    call $Class_isInstance
    local.set $isInst
    local.get $name
    i32.const 4337
    call $__str_equals
    local.get $simpleName
    i32.const 4337
    call $__str_equals
    i32.and
    local.get $isIface
    i32.eqz
    i32.and
    local.get $isEnum
    i32.eqz
    i32.and
    local.get $isInst
    i32.and
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    i32.const 1
    local.set $result
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
    local.get $result
    local.get $i
    i32.mul
    local.set $result
        )
    local.get $i
    i32.const 1
    i32.add
    local.set $i
        br $loop_for_2
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    i32.const 0
    local.set $sum
    i32.const 1
    local.set $i
    (block $break_for_3
      (loop $loop_for_3
    local.get $i
    local.get $n
    i32.le_s
        i32.eqz
        br_if $break_for_3
        (block $continue_for_3
    local.get $i
    i32.const 3
    i32.eq
    if
    br $continue_for_3
    end
    local.get $i
    i32.const 8
    i32.gt_s
    if
    br $break_for_3
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
        br $loop_for_3
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (block $break_foreach_5
      (loop $loop_foreach_5
        local.get $__arr_idx_1
        local.get $__arr_len_1
        i32.ge_s
        br_if $break_foreach_5
        local.get $__arr_ptr_1
        local.get $__arr_idx_1
        i32.const 4
        i32.mul
        i32.add
        i32.load offset=4
        local.set $n
        (block $continue_foreach_5
    local.get $n
    i32.const 3
    i32.eq
    if
    br $continue_foreach_5
    end
    local.get $n
    i32.const 8
    i32.gt_s
    if
    br $break_foreach_5
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
        br $loop_foreach_5
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (block $break_foreach_6
      (loop $loop_foreach_6
        local.get $__arr_idx_1
        local.get $__arr_len_1
        i32.ge_s
        br_if $break_foreach_6
        local.get $__arr_ptr_1
        local.get $__arr_idx_1
        i32.const 8
        i32.mul
        i32.add
        f64.load offset=8
        local.set $v
        (block $continue_foreach_6
    local.get $total
    local.get $v
    f64.add
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    local.get $total
    local.get $x
    i32.add
    local.set $total
        )
        local.get $__arr_idx_1
        i32.const 1
        i32.add
        local.set $__arr_idx_1
        br $loop_foreach_7
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (block $break_foreach_8
      (loop $loop_foreach_8
        local.get $__arr_idx_1
        local.get $__arr_len_1
        i32.ge_s
        br_if $break_foreach_8
        local.get $__arr_ptr_1
        local.get $__arr_idx_1
        i32.const 4
        i32.mul
        i32.add
        i32.load offset=4
        local.set $x
        (block $continue_foreach_8
    local.get $sum
    local.get $x
    i32.add
    local.set $sum
        )
        local.get $__arr_idx_1
        i32.const 1
        i32.add
        local.set $__arr_idx_1
        br $loop_foreach_8
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    i32.const 100
    local.set $result
    br $break_switch_9
      ) ;; end block $case_9_1
    i32.const 200
    local.set $result
    br $break_switch_9
      ) ;; end block $case_9_2
    i32.const 300
    local.set $result
    br $break_switch_9
      ) ;; end block $case_9_3
    i32.const 0
    i32.const 1
    i32.sub
    local.set $result
    br $break_switch_9
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    i32.const 0
    local.set $result
    local.get $code
    local.set $__switch_val_1
    (block $break_switch_10
      (block $case_10_3
      (block $case_10_2
      (block $case_10_1
      (block $case_10_0
        local.get $__switch_val_1
    i32.const 1
        i32.eq
        br_if $case_10_0
        local.get $__switch_val_1
    i32.const 2
        i32.eq
        br_if $case_10_1
        local.get $__switch_val_1
    i32.const 3
        i32.eq
        br_if $case_10_2
        br $case_10_3
        br $break_switch_10
      ) ;; end block $case_10_0
    local.get $result
    i32.const 10
    i32.add
    local.set $result
      ) ;; end block $case_10_1
    local.get $result
    i32.const 20
    i32.add
    local.set $result
    br $break_switch_10
      ) ;; end block $case_10_2
    local.get $result
    i32.const 30
    i32.add
    local.set $result
    br $break_switch_10
      ) ;; end block $case_10_3
    local.get $result
    i32.const 5
    i32.add
    local.set $result
    br $break_switch_10
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    i32.const 0
    local.set $total
    i32.const 1
    local.set $i
    (block $break_for_11
      (loop $loop_for_11
    local.get $i
    local.get $limit
    i32.le_s
        i32.eqz
        br_if $break_for_11
        (block $continue_for_11
    local.get $i
    local.set $__switch_val_1
    (block $break_switch_12
      (block $case_12_2
      (block $case_12_1
      (block $case_12_0
        local.get $__switch_val_1
    i32.const 2
        i32.eq
        br_if $case_12_0
        local.get $__switch_val_1
    i32.const 4
        i32.eq
        br_if $case_12_1
        br $case_12_2
        br $break_switch_12
      ) ;; end block $case_12_0
    local.get $total
    i32.const 20
    i32.add
    local.set $total
    br $break_switch_12
      ) ;; end block $case_12_1
    local.get $total
    i32.const 40
    i32.add
    local.set $total
    br $break_switch_12
      ) ;; end block $case_12_2
    local.get $total
    i32.const 1
    i32.add
    local.set $total
    br $break_switch_12
    )
        )
    local.get $i
    i32.const 1
    i32.add
    local.set $i
        br $loop_for_11
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    i32.const 0
    local.set $count
    i32.const 0
    local.set $i
    (block $break_for_13
      (loop $loop_for_13
    local.get $i
    local.get $maxI
    i32.lt_s
        i32.eqz
        br_if $break_for_13
        (block $continue_for_13
    i32.const 0
    local.set $j
    (block $break_for_14
      (loop $loop_for_14
    local.get $j
    local.get $maxJ
    i32.lt_s
        i32.eqz
        br_if $break_for_14
        (block $continue_for_14
    local.get $i
    i32.const 2
    i32.eq
    local.get $j
    i32.const 1
    i32.eq
    i32.and
    if
    br $break_for_13
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
        br $loop_for_14
      )
    )
        )
    local.get $i
    i32.const 1
    i32.add
    local.set $i
        br $loop_for_13
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    i32.const 0
    local.set $count
    i32.const 0
    local.set $i
    (block $break_for_15
      (loop $loop_for_15
    local.get $i
    local.get $maxI
    i32.lt_s
        i32.eqz
        br_if $break_for_15
        (block $continue_for_15
    i32.const 0
    local.set $j
    (block $break_for_16
      (loop $loop_for_16
    local.get $j
    local.get $maxJ
    i32.lt_s
        i32.eqz
        br_if $break_for_16
        (block $continue_for_16
    local.get $j
    i32.const 2
    i32.eq
    if
    br $continue_for_15
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
        br $loop_for_16
      )
    )
        )
    local.get $i
    i32.const 1
    i32.add
    local.set $i
        br $loop_for_15
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    i32.const 10
    local.set $res
    (block $break_label_17
    local.get $flag
    i32.const 1
    i32.eq
    if
    local.get $res
    i32.const 5
    i32.add
    local.set $res
    br $break_label_17
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    i32.const 0
    local.set $sum
    i32.const 0
    local.set $i
    (block $break_while_18
      (loop $loop_while_18
    local.get $i
    local.get $limit
    i32.lt_s
        i32.eqz
        br_if $break_while_18
    i32.const 0
    local.set $j
    (block $break_while_19
      (loop $loop_while_19
    local.get $j
    local.get $limit
    i32.lt_s
        i32.eqz
        br_if $break_while_19
    local.get $i
    i32.const 1
    i32.eq
    local.get $j
    i32.const 1
    i32.eq
    i32.and
    if
    br $break_while_18
    end
    local.get $sum
    i32.const 1
    i32.add
    local.set $sum
    local.get $j
    i32.const 1
    i32.add
    local.set $j
        br $loop_while_19
      )
    )
    local.get $i
    i32.const 1
    i32.add
    local.set $i
        br $loop_while_18
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    i32.const 4354
    local.set $greeting
    i32.const 4360
    local.set $target
    local.get $greeting
    i32.const 4372
    call $__str_concat
    local.get $target
    call $__str_concat
    i32.const 4375
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    i32.const 4377
    local.get $name
    call $__str_concat
    i32.const 4384
    call $__str_concat
    local.get $age
    call $__str_from_i32
    call $__str_concat
    i32.const 4392
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    i32.const 4360
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    i32.const 4360
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    i32.const 4394
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    i32.const 4394
    local.set $s
    local.get $s
    i32.const 4406
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    i32.const 4394
    local.set $s
    local.get $s
    i32.const 4412
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    i32.const 16
    call $__sb_new
    local.set $__sb_tmp
    local.get $__sb_tmp
    i32.const 4417
    call $__sb_append_str
    drop
    local.get $__sb_tmp
    local.set $sb
    i32.const 0
    local.set $i
    (block $break_for_20
      (loop $loop_for_20
    local.get $i
    local.get $count
    i32.lt_s
        i32.eqz
        br_if $break_for_20
        (block $continue_for_20
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
        br $loop_for_20
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    i32.const 16
    call $__sb_new
    local.set $__sb_tmp
    local.get $__sb_tmp
    i32.const 4412
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    i32.const 16
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 45
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
    i32.const 12
    i32.add
    call_indirect (type $t_sig_4)
    local.get $p
    local.set $__obj_tmp
    local.get $__obj_tmp
    local.get $__obj_tmp
    i32.load offset=0
    i32.const 13
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    i32.const 16
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 45
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    local.get $type
    i32.const 1
    i32.eq
    if
    i32.const 16
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 71
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
    i32.const 84
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
    i32.const 11
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    local.get $type
    i32.const 1
    i32.eq
    if
    i32.const 16
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 71
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
    i32.const 84
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
    i32.const 10
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    local.get $type
    i32.const 1
    i32.eq
    if
    i32.const 16
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 71
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
    i32.const 84
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
      i32.const 71
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
      i32.const 84
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    i32.const 45
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
    i32.const 45
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
    i32.const 45
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
        local.set $p
        (block $continue_foreach_21
    local.get $totalDist
    local.get $p
    local.set $__obj_tmp
    local.get $__obj_tmp
    local.get $__obj_tmp
    i32.load offset=0
    i32.const 13
    i32.add
    call_indirect (type $t_sig_1)
    i32.add
    local.set $totalDist
        )
        local.get $__arr_idx_1
        i32.const 1
        i32.add
        local.set $__arr_idx_1
        br $loop_foreach_21
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    i32.const 4425
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
    i32.const 4436
    i32.const 7
    call $dom_setFillStyle
    local.get $ctx
    f64.const 10.0
    f64.const 20.0
    f64.const 100.0
    f64.const 50.0
    call $dom_fillRect
    local.get $ctx
    call $dom_beginPath
    local.get $ctx
    f64.const 60.0
    f64.const 60.0
    f64.const 20.0
    f64.const 0.0
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    i32.const 4444
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    i32.const 4453
    i32.const 50
    call $dom_js_eval
    local.set $obj
    local.get $obj
    i32.const 4504
    i32.const 5
    call $dom_js_getString
    local.set $title
    local.get $obj
    i32.const 4510
    i32.const 5
    call $dom_js_getInt
    local.set $count
    local.get $obj
    i32.const 4516
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    call $dom_js_createObject
    local.set $obj
    local.get $obj
    i32.const 4523
    i32.const 4
    i32.const 4528
    i32.const 5
    call $dom_js_setString
    local.get $obj
    i32.const 4534
    i32.const 5
    i32.const 95
    call $dom_js_setInt
    local.get $obj
    i32.const 4540
    i32.const 6
    f64.const 4.8
    call $dom_js_setDouble
    local.get $obj
    i32.const 4547
    i32.const 7
    i32.const 1
    call $dom_js_setBoolean
    local.get $obj
    i32.const 4523
    i32.const 4
    call $dom_js_getString
    local.set $user
    local.get $obj
    i32.const 4534
    i32.const 5
    call $dom_js_getInt
    local.set $score
    local.get $obj
    i32.const 4547
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    call $dom_js_createArray
    local.set $arr
    local.get $arr
    i32.const 4555
    i32.const 5
    call $dom_js_pushStr
    local.get $arr
    i32.const 4561
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
    i32.const 4567
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    call $dom_js_global
    i32.const 4575
    i32.const 4
    call $dom_js_get
    local.set $math
    local.get $math
    i32.const 4580
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    i32.const 1
    i32.const 4584
    i32.const 39
    i32.const 1
    call $__wasi_print_str
    i32.const 1
    i32.const 4624
    i32.const 10
    i32.const 0
    call $__wasi_print_str
    i32.const 1
    i32.const 100
    i32.const 1
    call $__wasi_print_int
    i32.const 2
    i32.const 4635
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    i32.const 1
    i32.const 4662
    i32.const 21
    i32.const 0
    call $__wasi_print_str
    i32.const 1
    i32.const 4684
    i32.const 6
    i32.const 1
    call $__wasi_print_str
    i32.const 1
    i32.const 42
    i32.const 1
    call $__wasi_print_int
    i32.const 2
    i32.const 4691
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    i32.const 186
    i32.store offset=0
    local.get $__obj_alloc_ptr
    i32.const 4709
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $e i32)
    (local $__caught_exc i32)
    (try
      (do
    i32.const 8
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 22
    i32.store offset=0
    local.get $__obj_alloc_ptr
    i32.const 4720
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
    i32.const 10
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $ce i32)
    (local $__caught_exc i32)
    (try
      (do
    i32.const 16
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 33
    i32.store offset=0
    local.get $__obj_alloc_ptr
    i32.const 4745
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
    i32.const 11
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    i32.const 33
    i32.store offset=0
    local.get $__obj_alloc_ptr
    i32.const 4756
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
    i32.const 197
    i32.store offset=0
    local.get $__obj_alloc_ptr
    i32.const 4767
    call $IllegalArgumentException__init_1
    local.get $__obj_alloc_ptr
    throw $Exception
    else
    i32.const 8
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 22
    i32.store offset=0
    local.get $__obj_alloc_ptr
    i32.const 4775
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
          i32.const 33
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
    i32.const 11
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
          i32.const 197
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    i32.const 186
    i32.store offset=0
    local.get $__obj_alloc_ptr
    i32.const 4789
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    i32.const 4801
    i32.const 4812
    i32.const 0
    call $__wasi_write_file_str
    drop
    i32.const 4801
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    i32.const 4827
    i32.const 4839
    i32.const 0
    call $__wasi_write_file_str
    drop
    i32.const 4827
    i32.const 4845
    i32.const 1
    call $__wasi_write_file_str
    drop
    i32.const 4827
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    i32.const 4851
    i32.const 4866
    i32.const 0
    call $__wasi_write_file_str
    drop
    i32.const 4851
    call $__wasi_file_exists
    local.set $existsBefore
    i32.const 4851
    call $__wasi_file_delete
    local.set $deleted
    i32.const 4851
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    i32.const 8
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 230
    i32.store offset=0
    local.get $__obj_alloc_ptr
    i32.const 4876
    call $WASIFile__init
    local.get $__obj_alloc_ptr
    local.set $file
    local.get $file
    i32.load offset=4
    i32.const 4890
    i32.const 0
    call $__wasi_write_file_str
    drop
    local.get $file
    i32.load offset=4
    i32.const 4900
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    i32.const 8
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 230
    i32.store offset=0
    local.get $__obj_alloc_ptr
    i32.const 4913
    call $WASIFile__init
    local.get $__obj_alloc_ptr
    local.set $file
    local.get $file
    i32.load offset=4
    i32.const 4927
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (block $break_foreach_22
      (loop $loop_foreach_22
        local.get $__arr_idx_1
        local.get $__arr_len_1
        i32.ge_s
        br_if $break_foreach_22
        local.get $__arr_ptr_1
        local.get $__arr_idx_1
        i32.const 4
        i32.mul
        i32.add
        i32.load offset=4
        local.set $v
        (block $continue_foreach_22
    local.get $total
    local.get $v
    i32.add
    local.set $total
        )
        local.get $__arr_idx_1
        i32.const 1
        i32.add
        local.set $__arr_idx_1
        br $loop_foreach_22
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    i32.const 42
    call $dom_js_number_of_int
    local.set $numVal
    i32.const 5167
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
    i32.const 5179
    local.get $numVal
    call $dom_js_typeof
    call $__str_equals
    if
    local.get $score
    i32.const 32
    i32.add
    local.set $score
    end
    i32.const 5186
    local.get $strVal
    call $dom_js_typeof
    call $__str_equals
    if
    local.get $score
    i32.const 64
    i32.add
    local.set $score
    end
    i32.const 5193
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    i32.const 5201
    i32.const 6
    call $dom_js_symbol_of
    local.set $sym
    i32.const 5208
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
    i32.const 5201
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    i32.const 5225
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
    i32.const 5225
    local.get $err
    call $dom_js_errorMessage
    call $__str_equals
    if
    local.get $score
    i32.const 2
    i32.add
    local.set $score
    end
    i32.const 5247
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
    i32.const 5257
    i32.const 5
    call $dom_js_instanceof
    local.get $err
    i32.const 5247
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    i32.const 42
    call $dom_js_number_of_int
    local.set $v1
    i32.const 42
    call $dom_js_number_of_int
    local.set $v2
    i32.const 5263
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    call $dom_js_createObject
    local.set $obj
    local.get $obj
    i32.const 5266
    i32.const 4
    i32.const 4360
    i32.const 11
    call $dom_js_setString
    local.get $obj
    i32.const 5271
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    i32.const 4250
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    i32.const 4211
    i32.const 9
    call $dom_js_resource_has
    i32.const 5279
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    i32.const 5295
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    i32.const 5328
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    call $dom_js_global
    i32.const 5369
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    i32.const 1
    i32.const 5827
    i32.const 5
    i32.const 0
    call $__wasi_print_str
    i32.const 1
    i32.const 5388
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    call $dom_terminal_clear
  )
  (func $testEnumName (export "testEnumName") (param $idx i32) (result i32)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    local.get $idx
    i32.const 0
    i32.eq
    if
    global.get $Status_PENDING
    local.set $__obj_tmp
    local.get $__obj_tmp
    local.get $__obj_tmp
    i32.load offset=0
    i32.const 10
    i32.add
    call_indirect (type $t_sig_1)
    return
    end
    local.get $idx
    i32.const 1
    i32.eq
    if
    global.get $Status_ACTIVE
    local.set $__obj_tmp
    local.get $__obj_tmp
    local.get $__obj_tmp
    i32.load offset=0
    i32.const 10
    i32.add
    call_indirect (type $t_sig_1)
    return
    end
    global.get $Status_COMPLETED
    local.set $__obj_tmp
    local.get $__obj_tmp
    local.get $__obj_tmp
    i32.load offset=0
    i32.const 10
    i32.add
    call_indirect (type $t_sig_1)
    return
  )
  (func $testEnumOrdinal (export "testEnumOrdinal") (param $idx i32) (result i32)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    local.get $idx
    i32.const 0
    i32.eq
    if
    global.get $Status_PENDING
    local.set $__obj_tmp
    local.get $__obj_tmp
    local.get $__obj_tmp
    i32.load offset=0
    i32.const 11
    i32.add
    call_indirect (type $t_sig_1)
    return
    end
    local.get $idx
    i32.const 1
    i32.eq
    if
    global.get $Status_ACTIVE
    local.set $__obj_tmp
    local.get $__obj_tmp
    local.get $__obj_tmp
    i32.load offset=0
    i32.const 11
    i32.add
    call_indirect (type $t_sig_1)
    return
    end
    global.get $Status_COMPLETED
    local.set $__obj_tmp
    local.get $__obj_tmp
    local.get $__obj_tmp
    i32.load offset=0
    i32.const 11
    i32.add
    call_indirect (type $t_sig_1)
    return
  )
  (func $testEnumCompareTo (export "testEnumCompareTo") (result i32)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    global.get $Status_COMPLETED
    local.set $__obj_tmp
    local.get $__obj_tmp
    global.get $Status_PENDING
    local.get $__obj_tmp
    i32.load offset=0
    i32.const 4
    i32.add
    call_indirect (type $t_sig_2)
    return
  )
  (func $testEnumValues (export "testEnumValues") (result i32)
    (local $all i32)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    call $Status_values
    local.set $all
    local.get $all
    i32.load
    return
  )
  (func $testEnumValueOf (export "testEnumValueOf") (param $name i32) (result i32)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    local.get $name
    call $Status_valueOf
    local.set $s
    local.get $s
    local.set $__obj_tmp
    local.get $__obj_tmp
    local.get $__obj_tmp
    i32.load offset=0
    i32.const 11
    i32.add
    call_indirect (type $t_sig_1)
    return
  )
  (func $testEnumSwitch (export "testEnumSwitch") (param $status i32) (result i32)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    i32.const 0
    local.set $result
    local.get $status
    local.set $__switch_val_1
    (block $break_switch_23
      (block $case_23_2
      (block $case_23_1
      (block $case_23_0
        local.get $__switch_val_1
    global.get $Status_PENDING
        i32.eq
        br_if $case_23_0
        local.get $__switch_val_1
    global.get $Status_ACTIVE
        i32.eq
        br_if $case_23_1
        local.get $__switch_val_1
    global.get $Status_COMPLETED
        i32.eq
        br_if $case_23_2
        br $break_switch_23
      ) ;; end block $case_23_0
    i32.const 100
    local.set $result
    br $break_switch_23
      ) ;; end block $case_23_1
    i32.const 200
    local.set $result
    br $break_switch_23
      ) ;; end block $case_23_2
    i32.const 300
    local.set $result
    br $break_switch_23
    )
    local.get $result
    return
  )
  (func $testEnumSwitchExpr (export "testEnumSwitchExpr") (param $status i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__switchexpr_val_1 i32)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    local.get $status
    local.set $__switchexpr_val_1
    (block $break_switchexpr_24 (result i32)
      (block $case_expr_24_2
      (block $case_expr_24_1
      (block $case_expr_24_0
        local.get $__switchexpr_val_1
    global.get $Status_PENDING
        i32.eq
        br_if $case_expr_24_0
        local.get $__switchexpr_val_1
    global.get $Status_ACTIVE
        i32.eq
        br_if $case_expr_24_1
        local.get $__switchexpr_val_1
    global.get $Status_COMPLETED
        i32.eq
        br_if $case_expr_24_2
        i32.const 0
        br $break_switchexpr_24
      ) ;; end block $case_expr_24_0
    i32.const 1
        br $break_switchexpr_24
      ) ;; end block $case_expr_24_1
    i32.const 2
        br $break_switchexpr_24
      ) ;; end block $case_expr_24_2
    i32.const 3
        br $break_switchexpr_24
    )
    return
  )
  (func $testEnumWithFields (export "testEnumWithFields") (result i32)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    global.get $Priority_LOW
    local.set $__obj_tmp
    local.get $__obj_tmp
    local.get $__obj_tmp
    i32.load offset=0
    i32.const 10
    i32.add
    call_indirect (type $t_sig_1)
    global.get $Priority_HIGH
    local.set $__obj_tmp
    local.get $__obj_tmp
    local.get $__obj_tmp
    i32.load offset=0
    i32.const 10
    i32.add
    call_indirect (type $t_sig_1)
    i32.add
    return
  )
  (func $testObjectEqualsAndHash (export "testObjectEqualsAndHash") (result i32)
    (local $p1 i32)
    (local $p2 i32)
    (local $p3 i32)
    (local $eq1 i32)
    (local $eq2 i32)
    (local $h1 i32)
    (local $h2 i32)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    i32.const 16
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 0
    i32.store offset=0
    local.get $__obj_alloc_ptr
    i32.const 4528
    i32.const 30
    call $Person__init
    local.get $__obj_alloc_ptr
    local.set $p1
    i32.const 16
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 0
    i32.store offset=0
    local.get $__obj_alloc_ptr
    i32.const 4528
    i32.const 30
    call $Person__init
    local.get $__obj_alloc_ptr
    local.set $p2
    i32.const 16
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 0
    i32.store offset=0
    local.get $__obj_alloc_ptr
    i32.const 5396
    i32.const 25
    call $Person__init
    local.get $__obj_alloc_ptr
    local.set $p3
    local.get $p1
    local.set $__obj_tmp
    local.get $__obj_tmp
    local.get $p2
    local.get $__obj_tmp
    i32.load offset=0
    i32.const 1
    i32.add
    call_indirect (type $t_sig_2)
    local.set $eq1
    local.get $p1
    local.set $__obj_tmp
    local.get $__obj_tmp
    local.get $p3
    local.get $__obj_tmp
    i32.load offset=0
    i32.const 1
    i32.add
    call_indirect (type $t_sig_2)
    local.set $eq2
    local.get $p1
    local.set $__obj_tmp
    local.get $__obj_tmp
    local.get $__obj_tmp
    i32.load offset=0
    i32.const 2
    i32.add
    call_indirect (type $t_sig_1)
    local.set $h1
    local.get $p2
    local.set $__obj_tmp
    local.get $__obj_tmp
    local.get $__obj_tmp
    i32.load offset=0
    i32.const 2
    i32.add
    call_indirect (type $t_sig_1)
    local.set $h2
    local.get $eq1
    local.get $eq2
    i32.eqz
    i32.and
    local.get $h1
    local.get $h2
    i32.eq
    i32.and
    return
  )
  (func $testObjectToString (export "testObjectToString") (result i32)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    i32.const 16
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 0
    i32.store offset=0
    local.get $__obj_alloc_ptr
    i32.const 4528
    i32.const 30
    call $Person__init
    local.get $__obj_alloc_ptr
    local.set $p
    local.get $p
    local.set $__obj_tmp
    local.get $__obj_tmp
    local.get $__obj_tmp
    i32.load offset=0
    i32.const 0
    i32.add
    call_indirect (type $t_sig_1)
    return
  )
  (func $testObjectClone (export "testObjectClone") (result i32)
    (local $p1 i32)
    (local $p2 i32)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    i32.const 16
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 0
    i32.store offset=0
    local.get $__obj_alloc_ptr
    i32.const 4528
    i32.const 30
    call $Person__init
    local.get $__obj_alloc_ptr
    local.set $p1
    local.get $p1
    local.set $__obj_tmp
    local.get $__obj_tmp
    local.get $__obj_tmp
    i32.load offset=0
    i32.const 10
    i32.add
    call_indirect (type $t_sig_1)
    local.set $p2
    local.get $p2
    i32.load offset=8
    i32.const 30
    i32.eq
    local.get $p2
    i32.load offset=4
    i32.const 4528
    call $__str_equals
    i32.and
    local.get $p1
    local.get $p2
    i32.ne
    i32.and
    return
  )
  (func $testStringTrim (export "testStringTrim") (param $s i32) (result i32)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    local.get $s
    call $__str_trim
    return
  )
  (func $testStringCase (export "testStringCase") (param $s i32) (param $upper i32) (result i32)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    local.get $upper
    if (result i32)
    local.get $s
    call $__str_toUpperCase
    else
    local.get $s
    call $__str_toLowerCase
    end
    return
  )
  (func $testStringReplace (export "testStringReplace") (param $s i32) (param $target i32) (param $rep i32) (result i32)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    local.get $s
    local.get $target
    local.get $rep
    call $__str_replace_str
    return
  )
  (func $testStringRepeat (export "testStringRepeat") (param $s i32) (param $count i32) (result i32)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    local.get $s
    local.get $count
    call $__str_repeat
    return
  )
  (func $testStringContainsAndEnds (export "testStringContainsAndEnds") (param $s i32) (param $sub i32) (param $suffix i32) (result i32)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    local.get $s
    local.get $sub
    call $__str_contains
    local.get $s
    local.get $suffix
    call $__str_endsWith
    i32.and
    local.get $s
    i32.const 4412
    call $__str_startsWith
    i32.and
    return
  )
  (func $testStringCompare (export "testStringCompare") (param $s1 i32) (param $s2 i32) (param $ignoreCase i32) (result i32)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    local.get $ignoreCase
    if (result i32)
    local.get $s1
    local.get $s2
    call $__str_equalsIgnoreCase
    if (result i32)
    i32.const 1
    else
    i32.const 0
    end
    else
    local.get $s1
    local.get $s2
    call $__str_compareTo
    end
    return
  )
  (func $testStringToCharArray (export "testStringToCharArray") (param $s i32) (result i32)
    (local $chars i32)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    local.get $s
    call $__str_toCharArray
    local.set $chars
    local.get $chars
    i32.load
    return
  )
  (func $testStringBuilderOps (export "testStringBuilderOps") (result i32)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    i32.const 16
    call $__sb_new
    local.set $sb
    local.get $sb
    i32.const 5400
    call $__sb_append_str
    drop
    local.get $sb
    i32.const 5404
    call $__sb_append_str
    drop
    local.get $sb
    i32.const 3
    i32.const 5413
    call $__sb_insert_str
    local.get $sb
    i32.const 0
    i32.const 4
    call $__sb_delete
    drop
    local.get $sb
    i32.const 5
    call $__sb_deleteCharAt
    local.get $sb
    i32.const 0
    i32.const 5
    i32.const 4412
    call $__sb_replace
    local.get $sb
    call $__sb_reverse
    local.get $sb
    call $__sb_toString
    return
  )
  (func $testStringValueOf (export "testStringValueOf") (param $n i32) (result i32)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    local.get $n
    call $__str_from_i32
    return
  )
  (func $testSystemTimeAndClock (export "testSystemTimeAndClock") (result i32)
    (local $ms i64)
    (local $ns i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    call $__wasi_clock_time_ms_i64
    local.set $ms
    call $__wasi_clock_time_ns
    local.set $ns
    local.get $ms
    i64.const 0
    i64.gt_s
    local.get $ns
    i64.const 0
    i64.gt_s
    i32.and
    return
  )
  (func $testSystemIdentityHashCode (export "testSystemIdentityHashCode") (result i32)
    (local $p i32)
    (local $hash i32)
    (local $nullHash i32)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    i32.const 16
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 45
    i32.store offset=0
    local.get $__obj_alloc_ptr
    i32.const 10
    i32.const 20
    call $Point__init
    local.get $__obj_alloc_ptr
    local.set $p
    local.get $p
    call $__system_identity_hash_code
    local.set $hash
    i32.const 0
    call $__system_identity_hash_code
    local.set $nullHash
    local.get $hash
    i32.const 0
    i32.gt_s
    local.get $nullHash
    i32.const 0
    i32.eq
    i32.and
    return
  )
  (func $testSystemLineSeparator (export "testSystemLineSeparator") (result i32)
    (local $sep i32)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    call $__system_line_separator
    local.set $sep
    local.get $sep
    i32.const 5421
    call $__str_equals
    return
  )
  (func $testSystemProperties (export "testSystemProperties") (result i32)
    (local $lineSep i32)
    (local $fileSep i32)
    (local $osName i32)
    (local $osArch i32)
    (local $defVal i32)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    i32.const 5423
    call $__system_get_property
    local.set $lineSep
    i32.const 5438
    call $__system_get_property
    local.set $fileSep
    i32.const 5453
    call $__system_get_property
    local.set $osName
    i32.const 5461
    call $__system_get_property
    local.set $osArch
    i32.const 5469
    i32.const 5493
    call $__system_get_property_default
    local.set $defVal
    local.get $lineSep
    i32.const 5421
    call $__str_equals
    local.get $fileSep
    i32.const 5502
    call $__str_equals
    i32.and
    local.get $osName
    i32.const 4360
    call $__str_equals
    i32.and
    local.get $osArch
    i32.const 5504
    call $__str_equals
    i32.and
    local.get $defVal
    i32.const 5493
    call $__str_equals
    i32.and
    return
  )
  (func $testSystemGetenv (export "testSystemGetenv") (result i32)
    (local $path i32)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    i32.const 5511
    call $__system_getenv
    local.set $path
    local.get $path
    i32.const 0
    i32.ne
    local.get $path
    call $__str_length
    i32.const 0
    i32.gt_s
    i32.and
    return
  )
  ;; Enum: Status
  (func $Status__init (export "Status__init") (param $this i32)
  )
  ;; Enum: Priority
  (func $Priority__init (export "Priority__init") (param $this i32) (param $level i32)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    local.get $this
    local.get $level
    i32.store offset=4
  )
  (func $Priority_getLevel (export "Priority_getLevel") (param $this i32) (result i32)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    local.get $this
    i32.load offset=4
    return
  )
  ;; Class/Interface: Person
  (func $Person__init (export "Person__init") (param $this i32) (param $name i32) (param $age i32)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    local.get $this
    local.get $name
    i32.store offset=4
    local.get $this
    local.get $age
    i32.store offset=8
  )
  (func $Person_equals (export "Person_equals") (param $this i32) (param $other i32) (result i32)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    local.get $other
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
      i32.const 0
      i32.eq
      i32.or
    end
    if
    local.get $other
    local.set $p
    local.get $this
    i32.load offset=8
    local.get $p
    i32.load offset=8
    i32.eq
    local.get $this
    i32.load offset=4
    local.get $p
    i32.load offset=4
    call $__str_equals
    i32.and
    return
    end
    i32.const 0
    return
  )
  (func $Person_hashCode (export "Person_hashCode") (param $this i32) (result i32)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    local.get $this
    i32.load offset=8
    i32.const 31
    i32.mul
    return
  )
  (func $Person_toString (export "Person_toString") (param $this i32) (result i32)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    local.get $this
    i32.load offset=4
    return
  )
  (func $Person_clonePerson (export "Person_clonePerson") (param $this i32) (result i32)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $e i32)
    (local $__caught_exc i32)
    (try
      (do
    local.get $this
    i32.const 32
    call $Object_clone
    return
      )
      (catch $Exception
        local.set $__caught_exc
        local.get $__caught_exc
        local.set $e
    i32.const 0
    return
      )
    )
    i32.const 0
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    local.get $label
    i32.const 5528
    call $__str_concat
    local.get $this
    i32.load offset=4
    call $__str_from_i32
    call $__str_concat
    return
  )
  ;; Class/Interface: ClosableSession
  (func $ClosableSession__init (export "ClosableSession__init") (param $this i32) (param $id i32)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    local.get $this
    local.get $id
    i32.store offset=4
    local.get $this
    i32.const 0
    i32.store offset=8
  )
  (func $ClosableSession_doWork (export "ClosableSession_doWork") (param $this i32) (result i32)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    local.get $this
    i32.load offset=4
    i32.const 2
    i32.mul
    return
  )
  (func $ClosableSession_close (export "ClosableSession_close") (param $this i32)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    local.get $this
    local.set $__obj_tmp
    local.get $__obj_tmp
    local.get $__obj_tmp
    i32.load offset=8
    i32.const 1
    i32.add
    i32.store offset=8
  )
  ;; Class/Interface: BoxItem
  (func $BoxItem__init (export "BoxItem__init") (param $this i32) (param $w i32)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    local.get $this
    local.get $w
    i32.store offset=4
  )
  (func $BoxItem_compareTo (export "BoxItem_compareTo") (param $this i32) (param $other i32) (result i32)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    local.get $this
    i32.load offset=4
    local.get $other
    i32.load offset=4
    i32.sub
    return
  )
  ;; Class/Interface: IntRangeIterator
  (func $IntRangeIterator__init (export "IntRangeIterator__init") (param $this i32) (param $start i32) (param $end i32)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    local.get $this
    local.get $start
    i32.store offset=4
    local.get $this
    local.get $end
    i32.store offset=8
  )
  (func $IntRangeIterator_hasNext (export "IntRangeIterator_hasNext") (param $this i32) (result i32)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    local.get $this
    i32.load offset=4
    local.get $this
    i32.load offset=8
    i32.lt_s
    return
  )
  (func $IntRangeIterator_next (export "IntRangeIterator_next") (param $this i32) (result i32)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    local.get $this
    local.set $__obj_tmp
    local.get $__obj_tmp
    local.get $__obj_tmp
    i32.load offset=4
    i32.const 1
    i32.add
    i32.store offset=4
    i32.const 8
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 121
    i32.store offset=0
    local.get $__obj_alloc_ptr
    local.get $this
    i32.load offset=4
    call $BoxItem__init
    local.get $__obj_alloc_ptr
    return
  )
  ;; Class/Interface: IntRange
  (func $IntRange__init (export "IntRange__init") (param $this i32) (param $limit i32)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    local.get $this
    local.get $limit
    i32.store offset=4
  )
  (func $IntRange_iterator (export "IntRange_iterator") (param $this i32) (result i32)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    i32.const 16
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 131
    i32.store offset=0
    local.get $__obj_alloc_ptr
    i32.const 0
    local.get $this
    i32.load offset=4
    call $IntRangeIterator__init
    local.get $__obj_alloc_ptr
    return
  )
  ;; Class/Interface: SimpleWorkerTask
  (func $SimpleWorkerTask__init (export "SimpleWorkerTask__init") (param $this i32)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    local.get $this
    i32.const 0
    i32.store offset=4
  )
  (func $SimpleWorkerTask_run (export "SimpleWorkerTask_run") (param $this i32)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    local.get $this
    local.set $__obj_tmp
    local.get $__obj_tmp
    local.get $__obj_tmp
    i32.load offset=4
    i32.const 10
    i32.add
    i32.store offset=4
  )
  ;; Default run() for Person
  (func $Person_run (export "Person_run") (param $this i32)
  )
  ;; Default getClass() for Person
  (func $Person_getClass (export "Person_getClass") (param $this i32) (result i32)
    global.get $__class_Person
  )
  ;; Default toString() for Point
  (func $Point_toString (export "Point_toString") (param $this i32) (result i32)
    local.get $this
    call $__str_from_i32
  )
  ;; Default equals() for Point
  (func $Point_equals (export "Point_equals") (param $this i32) (param $other i32) (result i32)
    local.get $this
    local.get $other
    i32.eq
  )
  ;; Default hashCode() for Point
  (func $Point_hashCode (export "Point_hashCode") (param $this i32) (result i32)
    local.get $this
  )
  ;; Default run() for Point
  (func $Point_run (export "Point_run") (param $this i32)
  )
  ;; Default getClass() for Point
  (func $Point_getClass (export "Point_getClass") (param $this i32) (result i32)
    global.get $__class_Point
  )
  ;; Default toString() for Shape
  (func $Shape_toString (export "Shape_toString") (param $this i32) (result i32)
    local.get $this
    call $__str_from_i32
  )
  ;; Default equals() for Shape
  (func $Shape_equals (export "Shape_equals") (param $this i32) (param $other i32) (result i32)
    local.get $this
    local.get $other
    i32.eq
  )
  ;; Default hashCode() for Shape
  (func $Shape_hashCode (export "Shape_hashCode") (param $this i32) (result i32)
    local.get $this
  )
  ;; Default run() for Shape
  (func $Shape_run (export "Shape_run") (param $this i32)
  )
  ;; Default getClass() for Shape
  (func $Shape_getClass (export "Shape_getClass") (param $this i32) (result i32)
    global.get $__class_Shape
  )
  ;; Default toString() for ExportedCounter
  (func $ExportedCounter_toString (export "ExportedCounter_toString") (param $this i32) (result i32)
    local.get $this
    call $__str_from_i32
  )
  ;; Default equals() for ExportedCounter
  (func $ExportedCounter_equals (export "ExportedCounter_equals") (param $this i32) (param $other i32) (result i32)
    local.get $this
    local.get $other
    i32.eq
  )
  ;; Default hashCode() for ExportedCounter
  (func $ExportedCounter_hashCode (export "ExportedCounter_hashCode") (param $this i32) (result i32)
    local.get $this
  )
  ;; Default run() for ExportedCounter
  (func $ExportedCounter_run (export "ExportedCounter_run") (param $this i32)
  )
  ;; Default getClass() for ExportedCounter
  (func $ExportedCounter_getClass (export "ExportedCounter_getClass") (param $this i32) (result i32)
    global.get $__class_ExportedCounter
  )
  ;; Default toString() for ClosableSession
  (func $ClosableSession_toString (export "ClosableSession_toString") (param $this i32) (result i32)
    local.get $this
    call $__str_from_i32
  )
  ;; Default equals() for ClosableSession
  (func $ClosableSession_equals (export "ClosableSession_equals") (param $this i32) (param $other i32) (result i32)
    local.get $this
    local.get $other
    i32.eq
  )
  ;; Default hashCode() for ClosableSession
  (func $ClosableSession_hashCode (export "ClosableSession_hashCode") (param $this i32) (result i32)
    local.get $this
  )
  ;; Default run() for ClosableSession
  (func $ClosableSession_run (export "ClosableSession_run") (param $this i32)
  )
  ;; Default getClass() for ClosableSession
  (func $ClosableSession_getClass (export "ClosableSession_getClass") (param $this i32) (result i32)
    global.get $__class_ClosableSession
  )
  ;; Default toString() for BoxItem
  (func $BoxItem_toString (export "BoxItem_toString") (param $this i32) (result i32)
    local.get $this
    call $__str_from_i32
  )
  ;; Default equals() for BoxItem
  (func $BoxItem_equals (export "BoxItem_equals") (param $this i32) (param $other i32) (result i32)
    local.get $this
    local.get $other
    i32.eq
  )
  ;; Default hashCode() for BoxItem
  (func $BoxItem_hashCode (export "BoxItem_hashCode") (param $this i32) (result i32)
    local.get $this
  )
  ;; Default run() for BoxItem
  (func $BoxItem_run (export "BoxItem_run") (param $this i32)
  )
  ;; Default getClass() for BoxItem
  (func $BoxItem_getClass (export "BoxItem_getClass") (param $this i32) (result i32)
    global.get $__class_BoxItem
  )
  ;; Default toString() for IntRangeIterator
  (func $IntRangeIterator_toString (export "IntRangeIterator_toString") (param $this i32) (result i32)
    local.get $this
    call $__str_from_i32
  )
  ;; Default equals() for IntRangeIterator
  (func $IntRangeIterator_equals (export "IntRangeIterator_equals") (param $this i32) (param $other i32) (result i32)
    local.get $this
    local.get $other
    i32.eq
  )
  ;; Default hashCode() for IntRangeIterator
  (func $IntRangeIterator_hashCode (export "IntRangeIterator_hashCode") (param $this i32) (result i32)
    local.get $this
  )
  ;; Default run() for IntRangeIterator
  (func $IntRangeIterator_run (export "IntRangeIterator_run") (param $this i32)
  )
  ;; Default getClass() for IntRangeIterator
  (func $IntRangeIterator_getClass (export "IntRangeIterator_getClass") (param $this i32) (result i32)
    global.get $__class_IntRangeIterator
  )
  ;; Default toString() for IntRange
  (func $IntRange_toString (export "IntRange_toString") (param $this i32) (result i32)
    local.get $this
    call $__str_from_i32
  )
  ;; Default equals() for IntRange
  (func $IntRange_equals (export "IntRange_equals") (param $this i32) (param $other i32) (result i32)
    local.get $this
    local.get $other
    i32.eq
  )
  ;; Default hashCode() for IntRange
  (func $IntRange_hashCode (export "IntRange_hashCode") (param $this i32) (result i32)
    local.get $this
  )
  ;; Default run() for IntRange
  (func $IntRange_run (export "IntRange_run") (param $this i32)
  )
  ;; Default getClass() for IntRange
  (func $IntRange_getClass (export "IntRange_getClass") (param $this i32) (result i32)
    global.get $__class_IntRange
  )
  ;; Default toString() for SimpleWorkerTask
  (func $SimpleWorkerTask_toString (export "SimpleWorkerTask_toString") (param $this i32) (result i32)
    local.get $this
    call $__str_from_i32
  )
  ;; Default equals() for SimpleWorkerTask
  (func $SimpleWorkerTask_equals (export "SimpleWorkerTask_equals") (param $this i32) (param $other i32) (result i32)
    local.get $this
    local.get $other
    i32.eq
  )
  ;; Default hashCode() for SimpleWorkerTask
  (func $SimpleWorkerTask_hashCode (export "SimpleWorkerTask_hashCode") (param $this i32) (result i32)
    local.get $this
  )
  ;; Default getClass() for SimpleWorkerTask
  (func $SimpleWorkerTask_getClass (export "SimpleWorkerTask_getClass") (param $this i32) (result i32)
    global.get $__class_SimpleWorkerTask
  )
  ;; Default toString() for Status
  (func $Status_toString (export "Status_toString") (param $this i32) (result i32)
    local.get $this
    i32.load offset=4
  )
  ;; Default equals() for Status
  (func $Status_equals (export "Status_equals") (param $this i32) (param $other i32) (result i32)
    local.get $this
    local.get $other
    i32.eq
  )
  ;; Default hashCode() for Status
  (func $Status_hashCode (export "Status_hashCode") (param $this i32) (result i32)
    local.get $this
  )
  ;; Default run() for Status
  (func $Status_run (export "Status_run") (param $this i32)
  )
  ;; Default getClass() for Status
  (func $Status_getClass (export "Status_getClass") (param $this i32) (result i32)
    global.get $__class_Status
  )
  ;; Default toString() for Priority
  (func $Priority_toString (export "Priority_toString") (param $this i32) (result i32)
    local.get $this
    i32.load offset=4
  )
  ;; Default equals() for Priority
  (func $Priority_equals (export "Priority_equals") (param $this i32) (param $other i32) (result i32)
    local.get $this
    local.get $other
    i32.eq
  )
  ;; Default hashCode() for Priority
  (func $Priority_hashCode (export "Priority_hashCode") (param $this i32) (result i32)
    local.get $this
  )
  ;; Default run() for Priority
  (func $Priority_run (export "Priority_run") (param $this i32)
  )
  ;; Default getClass() for Priority
  (func $Priority_getClass (export "Priority_getClass") (param $this i32) (result i32)
    global.get $__class_Priority
  )
  ;; Default toString() for Throwable
  (func $Throwable_toString (export "Throwable_toString") (param $this i32) (result i32)
    local.get $this
    call $__str_from_i32
  )
  ;; Default equals() for Throwable
  (func $Throwable_equals (export "Throwable_equals") (param $this i32) (param $other i32) (result i32)
    local.get $this
    local.get $other
    i32.eq
  )
  ;; Default hashCode() for Throwable
  (func $Throwable_hashCode (export "Throwable_hashCode") (param $this i32) (result i32)
    local.get $this
  )
  ;; Default run() for Throwable
  (func $Throwable_run (export "Throwable_run") (param $this i32)
  )
  ;; Default getClass() for Throwable
  (func $Throwable_getClass (export "Throwable_getClass") (param $this i32) (result i32)
    global.get $__class_Throwable
  )
  ;; Default toString() for WASIFile
  (func $WASIFile_toString (export "WASIFile_toString") (param $this i32) (result i32)
    local.get $this
    call $__str_from_i32
  )
  ;; Default equals() for WASIFile
  (func $WASIFile_equals (export "WASIFile_equals") (param $this i32) (param $other i32) (result i32)
    local.get $this
    local.get $other
    i32.eq
  )
  ;; Default hashCode() for WASIFile
  (func $WASIFile_hashCode (export "WASIFile_hashCode") (param $this i32) (result i32)
    local.get $this
  )
  ;; Default run() for WASIFile
  (func $WASIFile_run (export "WASIFile_run") (param $this i32)
  )
  ;; Default getClass() for WASIFile
  (func $WASIFile_getClass (export "WASIFile_getClass") (param $this i32) (result i32)
    global.get $__class_WASIFile
  )
  ;; Default toString() for Thread
  (func $Thread_toString (export "Thread_toString") (param $this i32) (result i32)
    local.get $this
    call $__str_from_i32
  )
  ;; Default equals() for Thread
  (func $Thread_equals (export "Thread_equals") (param $this i32) (param $other i32) (result i32)
    local.get $this
    local.get $other
    i32.eq
  )
  ;; Default hashCode() for Thread
  (func $Thread_hashCode (export "Thread_hashCode") (param $this i32) (result i32)
    local.get $this
  )
  ;; Default getClass() for Thread
  (func $Thread_getClass (export "Thread_getClass") (param $this i32) (result i32)
    global.get $__class_Thread
  )
  ;; Default toString() for Class
  (func $Class_toString (export "Class_toString") (param $this i32) (result i32)
    local.get $this
    call $__str_from_i32
  )
  ;; Default equals() for Class
  (func $Class_equals (export "Class_equals") (param $this i32) (param $other i32) (result i32)
    local.get $this
    local.get $other
    i32.eq
  )
  ;; Default hashCode() for Class
  (func $Class_hashCode (export "Class_hashCode") (param $this i32) (result i32)
    local.get $this
  )
  ;; Default run() for Class
  (func $Class_run (export "Class_run") (param $this i32)
  )
  ;; Default getClass() for Class
  (func $Class_getClass (export "Class_getClass") (param $this i32) (result i32)
    global.get $__class_Class
  )
  ;; --- Class Reflection Methods ---
  (func $Class_getName (export "Class_getName") (param $this i32) (result i32)
    local.get $this
    i32.load offset=4
  )
  (func $Class_getSimpleName (export "Class_getSimpleName") (param $this i32) (result i32)
    local.get $this
    i32.load offset=8
  )
  (func $Class_getId (export "Class_getId") (param $this i32) (result i32)
    local.get $this
    i32.load offset=12
  )
  (func $Class_isInterface (export "Class_isInterface") (param $this i32) (result i32)
    local.get $this
    i32.load offset=16
  )
  (func $Class_isEnum (export "Class_isEnum") (param $this i32) (result i32)
    local.get $this
    i32.load offset=20
  )
  (func $Class_isArray (export "Class_isArray") (param $this i32) (result i32)
    local.get $this
    i32.load offset=4
    i32.load8_u
    i32.const 91
    i32.eq
  )
  (func $Class_getSuperclass (export "Class_getSuperclass") (param $this i32) (result i32)
    local.get $this
    i32.load offset=24
  )
  (func $Class_isInstance (export "Class_isInstance") (param $this i32) (param $obj i32) (result i32)
    (local $objCls i32)
    local.get $obj
    i32.eqz
    if
      i32.const 0
      return
    end
    local.get $obj
    i32.load offset=0
    call $__get_class_from_vtable
    local.set $objCls
    (block $done
      (loop $check
        local.get $objCls
        i32.eqz
        br_if $done
        local.get $objCls
        local.get $this
        i32.eq
        if
          i32.const 1
          return
        end
        local.get $objCls
        i32.load offset=24
        local.set $objCls
        br $check
      )
    )
    i32.const 0
  )
  (func $Class_forName (export "Class_forName") (param $name i32) (result i32)
    local.get $name
    i32.const 5581
    call $__str_equals
    if
      global.get $__class_TestClass
      return
    end
    local.get $name
    i32.const 5591
    call $__str_equals
    if
      global.get $__class_Person
      return
    end
    local.get $name
    i32.const 5598
    call $__str_equals
    if
      global.get $__class_CustomAppException
      return
    end
    local.get $name
    i32.const 5617
    call $__str_equals
    if
      global.get $__class_Point
      return
    end
    local.get $name
    i32.const 5623
    call $__str_equals
    if
      global.get $__class_Shape
      return
    end
    local.get $name
    i32.const 5629
    call $__str_equals
    if
      global.get $__class_Circle
      return
    end
    local.get $name
    i32.const 5636
    call $__str_equals
    if
      global.get $__class_Rectangle
      return
    end
    local.get $name
    i32.const 5646
    call $__str_equals
    if
      global.get $__class_ExportedCounter
      return
    end
    local.get $name
    i32.const 5662
    call $__str_equals
    if
      global.get $__class_ClosableSession
      return
    end
    local.get $name
    i32.const 5678
    call $__str_equals
    if
      global.get $__class_BoxItem
      return
    end
    local.get $name
    i32.const 5686
    call $__str_equals
    if
      global.get $__class_IntRangeIterator
      return
    end
    local.get $name
    i32.const 5703
    call $__str_equals
    if
      global.get $__class_IntRange
      return
    end
    local.get $name
    i32.const 4337
    call $__str_equals
    if
      global.get $__class_SimpleWorkerTask
      return
    end
    local.get $name
    i32.const 5531
    call $__str_equals
    if
      global.get $__class_Status
      return
    end
    local.get $name
    i32.const 5563
    call $__str_equals
    if
      global.get $__class_Priority
      return
    end
    local.get $name
    i32.const 0
    call $__str_equals
    if
      global.get $__class_Throwable
      return
    end
    local.get $name
    i32.const 0
    call $__str_equals
    if
      global.get $__class_Exception
      return
    end
    local.get $name
    i32.const 0
    call $__str_equals
    if
      global.get $__class_RuntimeException
      return
    end
    local.get $name
    i32.const 0
    call $__str_equals
    if
      global.get $__class_IllegalArgumentException
      return
    end
    local.get $name
    i32.const 0
    call $__str_equals
    if
      global.get $__class_IllegalStateException
      return
    end
    local.get $name
    i32.const 0
    call $__str_equals
    if
      global.get $__class_NullPointerException
      return
    end
    local.get $name
    i32.const 0
    call $__str_equals
    if
      global.get $__class_WASIFile
      return
    end
    local.get $name
    i32.const 0
    call $__str_equals
    if
      global.get $__class_CharSequence
      return
    end
    local.get $name
    i32.const 0
    call $__str_equals
    if
      global.get $__class_StringBuilder
      return
    end
    local.get $name
    i32.const 0
    call $__str_equals
    if
      global.get $__class_AutoCloseable
      return
    end
    local.get $name
    i32.const 0
    call $__str_equals
    if
      global.get $__class_Comparable
      return
    end
    local.get $name
    i32.const 0
    call $__str_equals
    if
      global.get $__class_Iterable
      return
    end
    local.get $name
    i32.const 0
    call $__str_equals
    if
      global.get $__class_Iterator
      return
    end
    local.get $name
    i32.const 5770
    call $__str_equals
    if
      global.get $__class_Runnable
      return
    end
    local.get $name
    i32.const 5763
    call $__str_equals
    if
      global.get $__class_Thread
      return
    end
    local.get $name
    i32.const 5757
    call $__str_equals
    if
      global.get $__class_Class
      return
    end
    local.get $name
    i32.const 5784
    call $__str_equals
    if
      global.get $__class_int
      return
    end
    local.get $name
    i32.const 5788
    call $__str_equals
    if
      global.get $__class_long
      return
    end
    local.get $name
    i32.const 5793
    call $__str_equals
    if
      global.get $__class_double
      return
    end
    local.get $name
    i32.const 5800
    call $__str_equals
    if
      global.get $__class_float
      return
    end
    local.get $name
    i32.const 5193
    call $__str_equals
    if
      global.get $__class_boolean
      return
    end
    local.get $name
    i32.const 5806
    call $__str_equals
    if
      global.get $__class_char
      return
    end
    local.get $name
    i32.const 5811
    call $__str_equals
    if
      global.get $__class_byte
      return
    end
    local.get $name
    i32.const 5816
    call $__str_equals
    if
      global.get $__class_short
      return
    end
    local.get $name
    i32.const 5822
    call $__str_equals
    if
      global.get $__class_void
      return
    end
    i32.const 0
  )
  (func $__get_class_from_vtable (param $vtableBase i32) (result i32)
    local.get $vtableBase
    i32.const 0
    i32.eq
    if
      global.get $__class_Person
      return
    end
    local.get $vtableBase
    i32.const 33
    i32.eq
    if
      global.get $__class_CustomAppException
      return
    end
    local.get $vtableBase
    i32.const 45
    i32.eq
    if
      global.get $__class_Point
      return
    end
    local.get $vtableBase
    i32.const 59
    i32.eq
    if
      global.get $__class_Shape
      return
    end
    local.get $vtableBase
    i32.const 71
    i32.eq
    if
      global.get $__class_Circle
      return
    end
    local.get $vtableBase
    i32.const 84
    i32.eq
    if
      global.get $__class_Rectangle
      return
    end
    local.get $vtableBase
    i32.const 96
    i32.eq
    if
      global.get $__class_ExportedCounter
      return
    end
    local.get $vtableBase
    i32.const 110
    i32.eq
    if
      global.get $__class_ClosableSession
      return
    end
    local.get $vtableBase
    i32.const 121
    i32.eq
    if
      global.get $__class_BoxItem
      return
    end
    local.get $vtableBase
    i32.const 131
    i32.eq
    if
      global.get $__class_IntRangeIterator
      return
    end
    local.get $vtableBase
    i32.const 141
    i32.eq
    if
      global.get $__class_IntRange
      return
    end
    local.get $vtableBase
    i32.const 151
    i32.eq
    if
      global.get $__class_SimpleWorkerTask
      return
    end
    local.get $vtableBase
    i32.const 161
    i32.eq
    if
      global.get $__class_Status
      return
    end
    local.get $vtableBase
    i32.const 173
    i32.eq
    if
      global.get $__class_Priority
      return
    end
    local.get $vtableBase
    i32.const 11
    i32.eq
    if
      global.get $__class_Throwable
      return
    end
    local.get $vtableBase
    i32.const 22
    i32.eq
    if
      global.get $__class_Exception
      return
    end
    local.get $vtableBase
    i32.const 186
    i32.eq
    if
      global.get $__class_RuntimeException
      return
    end
    local.get $vtableBase
    i32.const 197
    i32.eq
    if
      global.get $__class_IllegalArgumentException
      return
    end
    local.get $vtableBase
    i32.const 208
    i32.eq
    if
      global.get $__class_IllegalStateException
      return
    end
    local.get $vtableBase
    i32.const 219
    i32.eq
    if
      global.get $__class_NullPointerException
      return
    end
    local.get $vtableBase
    i32.const 230
    i32.eq
    if
      global.get $__class_WASIFile
      return
    end
    local.get $vtableBase
    i32.const 240
    i32.eq
    if
      global.get $__class_Thread
      return
    end
    local.get $vtableBase
    i32.const 250
    i32.eq
    if
      global.get $__class_Class
      return
    end
    i32.const 0
  )
  ;; --- Standard Thread Methods ---
  (func $Thread__init (export "Thread__init") (param $this i32)
    local.get $this
    i32.const 0
    i32.const 0
    call $Thread__init_2
  )
  (func $Thread__init_1 (export "Thread__init_1") (param $this i32) (param $target i32)
    local.get $this
    local.get $target
    i32.const 0
    call $Thread__init_2
  )
  (func $Thread__init_2 (export "Thread__init_2") (param $this i32) (param $target i32) (param $name i32)
    local.get $this
    local.get $target
    i32.store offset=4
    local.get $this
    local.get $name
    i32.store offset=8
    local.get $this
    i32.const 1
    i32.store offset=12
    local.get $this
    i32.const 0
    i32.store offset=16
  )
  (func $Thread_start (export "Thread_start") (param $this i32)
    local.get $this
    i32.const 1
    i32.store offset=16
    local.get $this
    i32.load offset=4
    if
      local.get $this
      i32.load offset=4
      local.get $this
      i32.load offset=4
      i32.load offset=0
      i32.const 8
      i32.add
      call_indirect (type $t_sig_3)
    else
      local.get $this
      local.get $this
      i32.load offset=0
      i32.const 8
      i32.add
      call_indirect (type $t_sig_3)
    end
  )
  (func $Thread_run (export "Thread_run") (param $this i32)
    local.get $this
    i32.load offset=4
    if
      local.get $this
      i32.load offset=4
      local.get $this
      i32.load offset=4
      i32.load offset=0
      i32.const 8
      i32.add
      call_indirect (type $t_sig_3)
    end
  )
  (func $Thread_getId (export "Thread_getId") (param $this i32) (result i32)
    local.get $this
    i32.load offset=12
  )
  (func $Thread_getName (export "Thread_getName") (param $this i32) (result i32)
    local.get $this
    i32.load offset=8
  )
  (func $Thread_setName (export "Thread_setName") (param $this i32) (param $name i32)
    local.get $this
    local.get $name
    i32.store offset=8
  )
  (func $Thread_isAlive (export "Thread_isAlive") (param $this i32) (result i32)
    local.get $this
    i32.load offset=16
  )
  (func $Thread_currentThread (export "Thread_currentThread") (result i32)
    (local $t i32)
    i32.const 24
    call $__heap_alloc
    local.set $t
    local.get $t
    i32.const 240
    i32.store offset=0
    local.get $t
    i32.const 0
    i32.store offset=4
    local.get $t
    i32.const 0
    i32.store offset=8
    local.get $t
    i32.const 1
    i32.store offset=12
    local.get $t
    i32.const 1
    i32.store offset=16
    local.get $t
  )
  ;; Class Descriptors Initializer
  (func $__init_classes (export "__init_classes")
    (local $inst i32)
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 250
    i32.store offset=0
    local.get $inst
    i32.const 5581
    i32.store offset=4
    local.get $inst
    i32.const 5581
    i32.store offset=8
    local.get $inst
    i32.const 1
    i32.store offset=12
    local.get $inst
    i32.const 0
    i32.store offset=16
    local.get $inst
    i32.const 0
    i32.store offset=20
    local.get $inst
    i32.const 0
    i32.store offset=24
    local.get $inst
    global.set $__class_TestClass
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 250
    i32.store offset=0
    local.get $inst
    i32.const 5591
    i32.store offset=4
    local.get $inst
    i32.const 5591
    i32.store offset=8
    local.get $inst
    i32.const 2
    i32.store offset=12
    local.get $inst
    i32.const 0
    i32.store offset=16
    local.get $inst
    i32.const 0
    i32.store offset=20
    local.get $inst
    i32.const 0
    i32.store offset=24
    local.get $inst
    global.set $__class_Person
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 250
    i32.store offset=0
    local.get $inst
    i32.const 5598
    i32.store offset=4
    local.get $inst
    i32.const 5598
    i32.store offset=8
    local.get $inst
    i32.const 3
    i32.store offset=12
    local.get $inst
    i32.const 0
    i32.store offset=16
    local.get $inst
    i32.const 0
    i32.store offset=20
    local.get $inst
    i32.const 0
    i32.store offset=24
    local.get $inst
    global.set $__class_CustomAppException
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 250
    i32.store offset=0
    local.get $inst
    i32.const 5617
    i32.store offset=4
    local.get $inst
    i32.const 5617
    i32.store offset=8
    local.get $inst
    i32.const 4
    i32.store offset=12
    local.get $inst
    i32.const 0
    i32.store offset=16
    local.get $inst
    i32.const 0
    i32.store offset=20
    local.get $inst
    i32.const 0
    i32.store offset=24
    local.get $inst
    global.set $__class_Point
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 250
    i32.store offset=0
    local.get $inst
    i32.const 5623
    i32.store offset=4
    local.get $inst
    i32.const 5623
    i32.store offset=8
    local.get $inst
    i32.const 5
    i32.store offset=12
    local.get $inst
    i32.const 0
    i32.store offset=16
    local.get $inst
    i32.const 0
    i32.store offset=20
    local.get $inst
    i32.const 0
    i32.store offset=24
    local.get $inst
    global.set $__class_Shape
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 250
    i32.store offset=0
    local.get $inst
    i32.const 5629
    i32.store offset=4
    local.get $inst
    i32.const 5629
    i32.store offset=8
    local.get $inst
    i32.const 6
    i32.store offset=12
    local.get $inst
    i32.const 0
    i32.store offset=16
    local.get $inst
    i32.const 0
    i32.store offset=20
    local.get $inst
    i32.const 0
    i32.store offset=24
    local.get $inst
    global.set $__class_Circle
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 250
    i32.store offset=0
    local.get $inst
    i32.const 5636
    i32.store offset=4
    local.get $inst
    i32.const 5636
    i32.store offset=8
    local.get $inst
    i32.const 7
    i32.store offset=12
    local.get $inst
    i32.const 0
    i32.store offset=16
    local.get $inst
    i32.const 0
    i32.store offset=20
    local.get $inst
    i32.const 0
    i32.store offset=24
    local.get $inst
    global.set $__class_Rectangle
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 250
    i32.store offset=0
    local.get $inst
    i32.const 5646
    i32.store offset=4
    local.get $inst
    i32.const 5646
    i32.store offset=8
    local.get $inst
    i32.const 8
    i32.store offset=12
    local.get $inst
    i32.const 0
    i32.store offset=16
    local.get $inst
    i32.const 0
    i32.store offset=20
    local.get $inst
    i32.const 0
    i32.store offset=24
    local.get $inst
    global.set $__class_ExportedCounter
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 250
    i32.store offset=0
    local.get $inst
    i32.const 5662
    i32.store offset=4
    local.get $inst
    i32.const 5662
    i32.store offset=8
    local.get $inst
    i32.const 9
    i32.store offset=12
    local.get $inst
    i32.const 0
    i32.store offset=16
    local.get $inst
    i32.const 0
    i32.store offset=20
    local.get $inst
    i32.const 0
    i32.store offset=24
    local.get $inst
    global.set $__class_ClosableSession
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 250
    i32.store offset=0
    local.get $inst
    i32.const 5678
    i32.store offset=4
    local.get $inst
    i32.const 5678
    i32.store offset=8
    local.get $inst
    i32.const 10
    i32.store offset=12
    local.get $inst
    i32.const 0
    i32.store offset=16
    local.get $inst
    i32.const 0
    i32.store offset=20
    local.get $inst
    i32.const 0
    i32.store offset=24
    local.get $inst
    global.set $__class_BoxItem
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 250
    i32.store offset=0
    local.get $inst
    i32.const 5686
    i32.store offset=4
    local.get $inst
    i32.const 5686
    i32.store offset=8
    local.get $inst
    i32.const 11
    i32.store offset=12
    local.get $inst
    i32.const 0
    i32.store offset=16
    local.get $inst
    i32.const 0
    i32.store offset=20
    local.get $inst
    i32.const 0
    i32.store offset=24
    local.get $inst
    global.set $__class_IntRangeIterator
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 250
    i32.store offset=0
    local.get $inst
    i32.const 5703
    i32.store offset=4
    local.get $inst
    i32.const 5703
    i32.store offset=8
    local.get $inst
    i32.const 12
    i32.store offset=12
    local.get $inst
    i32.const 0
    i32.store offset=16
    local.get $inst
    i32.const 0
    i32.store offset=20
    local.get $inst
    i32.const 0
    i32.store offset=24
    local.get $inst
    global.set $__class_IntRange
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 250
    i32.store offset=0
    local.get $inst
    i32.const 4337
    i32.store offset=4
    local.get $inst
    i32.const 4337
    i32.store offset=8
    local.get $inst
    i32.const 13
    i32.store offset=12
    local.get $inst
    i32.const 0
    i32.store offset=16
    local.get $inst
    i32.const 0
    i32.store offset=20
    local.get $inst
    i32.const 0
    i32.store offset=24
    local.get $inst
    global.set $__class_SimpleWorkerTask
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 250
    i32.store offset=0
    local.get $inst
    i32.const 5531
    i32.store offset=4
    local.get $inst
    i32.const 5531
    i32.store offset=8
    local.get $inst
    i32.const 14
    i32.store offset=12
    local.get $inst
    i32.const 0
    i32.store offset=16
    local.get $inst
    i32.const 1
    i32.store offset=20
    local.get $inst
    i32.const 0
    i32.store offset=24
    local.get $inst
    global.set $__class_Status
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 250
    i32.store offset=0
    local.get $inst
    i32.const 5563
    i32.store offset=4
    local.get $inst
    i32.const 5563
    i32.store offset=8
    local.get $inst
    i32.const 15
    i32.store offset=12
    local.get $inst
    i32.const 0
    i32.store offset=16
    local.get $inst
    i32.const 1
    i32.store offset=20
    local.get $inst
    i32.const 0
    i32.store offset=24
    local.get $inst
    global.set $__class_Priority
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 250
    i32.store offset=0
    local.get $inst
    i32.const 0
    i32.store offset=4
    local.get $inst
    i32.const 0
    i32.store offset=8
    local.get $inst
    i32.const 16
    i32.store offset=12
    local.get $inst
    i32.const 0
    i32.store offset=16
    local.get $inst
    i32.const 0
    i32.store offset=20
    local.get $inst
    i32.const 0
    i32.store offset=24
    local.get $inst
    global.set $__class_Throwable
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 250
    i32.store offset=0
    local.get $inst
    i32.const 0
    i32.store offset=4
    local.get $inst
    i32.const 0
    i32.store offset=8
    local.get $inst
    i32.const 17
    i32.store offset=12
    local.get $inst
    i32.const 0
    i32.store offset=16
    local.get $inst
    i32.const 0
    i32.store offset=20
    local.get $inst
    i32.const 0
    i32.store offset=24
    local.get $inst
    global.set $__class_Exception
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 250
    i32.store offset=0
    local.get $inst
    i32.const 0
    i32.store offset=4
    local.get $inst
    i32.const 0
    i32.store offset=8
    local.get $inst
    i32.const 18
    i32.store offset=12
    local.get $inst
    i32.const 0
    i32.store offset=16
    local.get $inst
    i32.const 0
    i32.store offset=20
    local.get $inst
    i32.const 0
    i32.store offset=24
    local.get $inst
    global.set $__class_RuntimeException
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 250
    i32.store offset=0
    local.get $inst
    i32.const 0
    i32.store offset=4
    local.get $inst
    i32.const 0
    i32.store offset=8
    local.get $inst
    i32.const 19
    i32.store offset=12
    local.get $inst
    i32.const 0
    i32.store offset=16
    local.get $inst
    i32.const 0
    i32.store offset=20
    local.get $inst
    i32.const 0
    i32.store offset=24
    local.get $inst
    global.set $__class_IllegalArgumentException
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 250
    i32.store offset=0
    local.get $inst
    i32.const 0
    i32.store offset=4
    local.get $inst
    i32.const 0
    i32.store offset=8
    local.get $inst
    i32.const 20
    i32.store offset=12
    local.get $inst
    i32.const 0
    i32.store offset=16
    local.get $inst
    i32.const 0
    i32.store offset=20
    local.get $inst
    i32.const 0
    i32.store offset=24
    local.get $inst
    global.set $__class_IllegalStateException
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 250
    i32.store offset=0
    local.get $inst
    i32.const 0
    i32.store offset=4
    local.get $inst
    i32.const 0
    i32.store offset=8
    local.get $inst
    i32.const 21
    i32.store offset=12
    local.get $inst
    i32.const 0
    i32.store offset=16
    local.get $inst
    i32.const 0
    i32.store offset=20
    local.get $inst
    i32.const 0
    i32.store offset=24
    local.get $inst
    global.set $__class_NullPointerException
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 250
    i32.store offset=0
    local.get $inst
    i32.const 0
    i32.store offset=4
    local.get $inst
    i32.const 0
    i32.store offset=8
    local.get $inst
    i32.const 22
    i32.store offset=12
    local.get $inst
    i32.const 0
    i32.store offset=16
    local.get $inst
    i32.const 0
    i32.store offset=20
    local.get $inst
    i32.const 0
    i32.store offset=24
    local.get $inst
    global.set $__class_WASIFile
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 250
    i32.store offset=0
    local.get $inst
    i32.const 0
    i32.store offset=4
    local.get $inst
    i32.const 0
    i32.store offset=8
    local.get $inst
    i32.const 23
    i32.store offset=12
    local.get $inst
    i32.const 1
    i32.store offset=16
    local.get $inst
    i32.const 0
    i32.store offset=20
    local.get $inst
    i32.const 0
    i32.store offset=24
    local.get $inst
    global.set $__class_CharSequence
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 250
    i32.store offset=0
    local.get $inst
    i32.const 0
    i32.store offset=4
    local.get $inst
    i32.const 0
    i32.store offset=8
    local.get $inst
    i32.const 24
    i32.store offset=12
    local.get $inst
    i32.const 0
    i32.store offset=16
    local.get $inst
    i32.const 0
    i32.store offset=20
    local.get $inst
    i32.const 0
    i32.store offset=24
    local.get $inst
    global.set $__class_StringBuilder
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 250
    i32.store offset=0
    local.get $inst
    i32.const 0
    i32.store offset=4
    local.get $inst
    i32.const 0
    i32.store offset=8
    local.get $inst
    i32.const 25
    i32.store offset=12
    local.get $inst
    i32.const 1
    i32.store offset=16
    local.get $inst
    i32.const 0
    i32.store offset=20
    local.get $inst
    i32.const 0
    i32.store offset=24
    local.get $inst
    global.set $__class_AutoCloseable
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 250
    i32.store offset=0
    local.get $inst
    i32.const 0
    i32.store offset=4
    local.get $inst
    i32.const 0
    i32.store offset=8
    local.get $inst
    i32.const 26
    i32.store offset=12
    local.get $inst
    i32.const 1
    i32.store offset=16
    local.get $inst
    i32.const 0
    i32.store offset=20
    local.get $inst
    i32.const 0
    i32.store offset=24
    local.get $inst
    global.set $__class_Comparable
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 250
    i32.store offset=0
    local.get $inst
    i32.const 0
    i32.store offset=4
    local.get $inst
    i32.const 0
    i32.store offset=8
    local.get $inst
    i32.const 27
    i32.store offset=12
    local.get $inst
    i32.const 1
    i32.store offset=16
    local.get $inst
    i32.const 0
    i32.store offset=20
    local.get $inst
    i32.const 0
    i32.store offset=24
    local.get $inst
    global.set $__class_Iterable
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 250
    i32.store offset=0
    local.get $inst
    i32.const 0
    i32.store offset=4
    local.get $inst
    i32.const 0
    i32.store offset=8
    local.get $inst
    i32.const 28
    i32.store offset=12
    local.get $inst
    i32.const 1
    i32.store offset=16
    local.get $inst
    i32.const 0
    i32.store offset=20
    local.get $inst
    i32.const 0
    i32.store offset=24
    local.get $inst
    global.set $__class_Iterator
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 250
    i32.store offset=0
    local.get $inst
    i32.const 5770
    i32.store offset=4
    local.get $inst
    i32.const 5770
    i32.store offset=8
    local.get $inst
    i32.const 29
    i32.store offset=12
    local.get $inst
    i32.const 1
    i32.store offset=16
    local.get $inst
    i32.const 0
    i32.store offset=20
    local.get $inst
    i32.const 0
    i32.store offset=24
    local.get $inst
    global.set $__class_Runnable
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 250
    i32.store offset=0
    local.get $inst
    i32.const 5763
    i32.store offset=4
    local.get $inst
    i32.const 5763
    i32.store offset=8
    local.get $inst
    i32.const 30
    i32.store offset=12
    local.get $inst
    i32.const 0
    i32.store offset=16
    local.get $inst
    i32.const 0
    i32.store offset=20
    local.get $inst
    i32.const 0
    i32.store offset=24
    local.get $inst
    global.set $__class_Thread
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 250
    i32.store offset=0
    local.get $inst
    i32.const 5757
    i32.store offset=4
    local.get $inst
    i32.const 5757
    i32.store offset=8
    local.get $inst
    i32.const 31
    i32.store offset=12
    local.get $inst
    i32.const 0
    i32.store offset=16
    local.get $inst
    i32.const 0
    i32.store offset=20
    local.get $inst
    i32.const 0
    i32.store offset=24
    local.get $inst
    global.set $__class_Class
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 250
    i32.store offset=0
    local.get $inst
    i32.const 5784
    i32.store offset=4
    local.get $inst
    i32.const 5784
    i32.store offset=8
    local.get $inst
    i32.const 32
    i32.store offset=12
    local.get $inst
    i32.const 0
    i32.store offset=16
    local.get $inst
    i32.const 0
    i32.store offset=20
    local.get $inst
    i32.const 0
    i32.store offset=24
    local.get $inst
    global.set $__class_int
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 250
    i32.store offset=0
    local.get $inst
    i32.const 5788
    i32.store offset=4
    local.get $inst
    i32.const 5788
    i32.store offset=8
    local.get $inst
    i32.const 33
    i32.store offset=12
    local.get $inst
    i32.const 0
    i32.store offset=16
    local.get $inst
    i32.const 0
    i32.store offset=20
    local.get $inst
    i32.const 0
    i32.store offset=24
    local.get $inst
    global.set $__class_long
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 250
    i32.store offset=0
    local.get $inst
    i32.const 5793
    i32.store offset=4
    local.get $inst
    i32.const 5793
    i32.store offset=8
    local.get $inst
    i32.const 34
    i32.store offset=12
    local.get $inst
    i32.const 0
    i32.store offset=16
    local.get $inst
    i32.const 0
    i32.store offset=20
    local.get $inst
    i32.const 0
    i32.store offset=24
    local.get $inst
    global.set $__class_double
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 250
    i32.store offset=0
    local.get $inst
    i32.const 5800
    i32.store offset=4
    local.get $inst
    i32.const 5800
    i32.store offset=8
    local.get $inst
    i32.const 35
    i32.store offset=12
    local.get $inst
    i32.const 0
    i32.store offset=16
    local.get $inst
    i32.const 0
    i32.store offset=20
    local.get $inst
    i32.const 0
    i32.store offset=24
    local.get $inst
    global.set $__class_float
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 250
    i32.store offset=0
    local.get $inst
    i32.const 5193
    i32.store offset=4
    local.get $inst
    i32.const 5193
    i32.store offset=8
    local.get $inst
    i32.const 36
    i32.store offset=12
    local.get $inst
    i32.const 0
    i32.store offset=16
    local.get $inst
    i32.const 0
    i32.store offset=20
    local.get $inst
    i32.const 0
    i32.store offset=24
    local.get $inst
    global.set $__class_boolean
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 250
    i32.store offset=0
    local.get $inst
    i32.const 5806
    i32.store offset=4
    local.get $inst
    i32.const 5806
    i32.store offset=8
    local.get $inst
    i32.const 37
    i32.store offset=12
    local.get $inst
    i32.const 0
    i32.store offset=16
    local.get $inst
    i32.const 0
    i32.store offset=20
    local.get $inst
    i32.const 0
    i32.store offset=24
    local.get $inst
    global.set $__class_char
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 250
    i32.store offset=0
    local.get $inst
    i32.const 5811
    i32.store offset=4
    local.get $inst
    i32.const 5811
    i32.store offset=8
    local.get $inst
    i32.const 38
    i32.store offset=12
    local.get $inst
    i32.const 0
    i32.store offset=16
    local.get $inst
    i32.const 0
    i32.store offset=20
    local.get $inst
    i32.const 0
    i32.store offset=24
    local.get $inst
    global.set $__class_byte
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 250
    i32.store offset=0
    local.get $inst
    i32.const 5816
    i32.store offset=4
    local.get $inst
    i32.const 5816
    i32.store offset=8
    local.get $inst
    i32.const 39
    i32.store offset=12
    local.get $inst
    i32.const 0
    i32.store offset=16
    local.get $inst
    i32.const 0
    i32.store offset=20
    local.get $inst
    i32.const 0
    i32.store offset=24
    local.get $inst
    global.set $__class_short
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 250
    i32.store offset=0
    local.get $inst
    i32.const 5822
    i32.store offset=4
    local.get $inst
    i32.const 5822
    i32.store offset=8
    local.get $inst
    i32.const 40
    i32.store offset=12
    local.get $inst
    i32.const 0
    i32.store offset=16
    local.get $inst
    i32.const 0
    i32.store offset=20
    local.get $inst
    i32.const 0
    i32.store offset=24
    local.get $inst
    global.set $__class_void
    global.get $__class_CustomAppException
    global.get $__class_Exception
    i32.store offset=24
    global.get $__class_Circle
    global.get $__class_Shape
    i32.store offset=24
    global.get $__class_Rectangle
    global.get $__class_Shape
    i32.store offset=24
    global.get $__class_Exception
    global.get $__class_Throwable
    i32.store offset=24
    global.get $__class_RuntimeException
    global.get $__class_Exception
    i32.store offset=24
    global.get $__class_IllegalArgumentException
    global.get $__class_RuntimeException
    i32.store offset=24
    global.get $__class_IllegalStateException
    global.get $__class_RuntimeException
    i32.store offset=24
    global.get $__class_NullPointerException
    global.get $__class_RuntimeException
    i32.store offset=24
  )
  ;; Standard enum methods for Status
  (func $Status_name (export "Status_name") (param $this i32) (result i32)
    local.get $this
    i32.load offset=4
  )
  (func $Status_ordinal (export "Status_ordinal") (param $this i32) (result i32)
    local.get $this
    i32.load offset=8
  )
  (func $Status_compareTo (export "Status_compareTo") (param $this i32) (param $other i32) (result i32)
    local.get $this
    i32.load offset=8
    local.get $other
    i32.load offset=8
    i32.sub
  )
  (func $Status_values (export "Status_values") (result i32)
    (local $arr i32)
    i32.const 16
    call $__heap_alloc
    local.set $arr
    local.get $arr
    i32.const 3
    i32.store
    local.get $arr
    global.get $Status_PENDING
    i32.store offset=4
    local.get $arr
    global.get $Status_ACTIVE
    i32.store offset=8
    local.get $arr
    global.get $Status_COMPLETED
    i32.store offset=12
    local.get $arr
  )
  (func $Status_valueOf (export "Status_valueOf") (param $name i32) (result i32)
    local.get $name
    i32.const 5538
    call $__str_equals
    if
      global.get $Status_PENDING
      return
    end
    local.get $name
    i32.const 5546
    call $__str_equals
    if
      global.get $Status_ACTIVE
      return
    end
    local.get $name
    i32.const 5553
    call $__str_equals
    if
      global.get $Status_COMPLETED
      return
    end
    i32.const 0
  )
  ;; Standard enum methods for Priority
  (func $Priority_name (export "Priority_name") (param $this i32) (result i32)
    local.get $this
    i32.load offset=4
  )
  (func $Priority_ordinal (export "Priority_ordinal") (param $this i32) (result i32)
    local.get $this
    i32.load offset=8
  )
  (func $Priority_compareTo (export "Priority_compareTo") (param $this i32) (param $other i32) (result i32)
    local.get $this
    i32.load offset=8
    local.get $other
    i32.load offset=8
    i32.sub
  )
  (func $Priority_values (export "Priority_values") (result i32)
    (local $arr i32)
    i32.const 12
    call $__heap_alloc
    local.set $arr
    local.get $arr
    i32.const 2
    i32.store
    local.get $arr
    global.get $Priority_LOW
    i32.store offset=4
    local.get $arr
    global.get $Priority_HIGH
    i32.store offset=8
    local.get $arr
  )
  (func $Priority_valueOf (export "Priority_valueOf") (param $name i32) (result i32)
    local.get $name
    i32.const 5572
    call $__str_equals
    if
      global.get $Priority_LOW
      return
    end
    local.get $name
    i32.const 5576
    call $__str_equals
    if
      global.get $Priority_HIGH
      return
    end
    i32.const 0
  )
  ;; Enum Singleton Initializer
  (func $__init_enums (export "__init_enums")
    (local $inst i32)
    call $__init_classes
    i32.const 16
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 161
    i32.store offset=0
    local.get $inst
    i32.const 5538
    i32.store offset=4
    local.get $inst
    i32.const 0
    i32.store offset=8
    local.get $inst
    global.set $Status_PENDING
    i32.const 16
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 161
    i32.store offset=0
    local.get $inst
    i32.const 5546
    i32.store offset=4
    local.get $inst
    i32.const 1
    i32.store offset=8
    local.get $inst
    global.set $Status_ACTIVE
    i32.const 16
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 161
    i32.store offset=0
    local.get $inst
    i32.const 5553
    i32.store offset=4
    local.get $inst
    i32.const 2
    i32.store offset=8
    local.get $inst
    global.set $Status_COMPLETED
    i32.const 16
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 173
    i32.store offset=0
    local.get $inst
    i32.const 5572
    i32.store offset=4
    local.get $inst
    i32.const 0
    i32.store offset=8
    local.get $inst
    i32.const 10
    call $Priority__init
    local.get $inst
    global.set $Priority_LOW
    i32.const 16
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 173
    i32.store offset=0
    local.get $inst
    i32.const 5576
    i32.store offset=4
    local.get $inst
    i32.const 1
    i32.store offset=8
    local.get $inst
    i32.const 20
    call $Priority__init
    local.get $inst
    global.set $Priority_HIGH
  )
  (start $__init_enums)
)
