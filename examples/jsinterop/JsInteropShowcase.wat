(module
  (@custom "sourceMappingURL" "\1aJsInteropShowcase.wasm.map")
  (import "dom" "getElementById" (func $dom_getElementById (param i32 i32) (result i32)))
  (import "dom" "createElement" (func $dom_createElement (param i32 i32) (result i32)))
  (import "dom" "getBody" (func $dom_getBody (result i32)))
  (import "dom" "setInnerText" (func $dom_setInnerText (param i32 i32 i32)))
  (import "dom" "appendChild" (func $dom_appendChild (param i32 i32)))
  (import "dom" "setStyle" (func $dom_setStyle (param i32 i32 i32 i32 i32)))
  (@custom "javaw_js_snippets" "[{\22className\22:\22JSDate\22,\22methodName\22:\22create\22,\22importName\22:\22js_inline_JSDate_create\22,\22jsCode\22:\22return new Date();\22,\22returnType\22:\22JSDate\22,\22returnWasmType\22:\22i32\22,\22isStatic\22:true,\22isCoerced\22:false,\22parameters\22:[]},{\22className\22:\22JSDate\22,\22methodName\22:\22toISOString\22,\22importName\22:\22js_inline_JSDate_toISOString\22,\22jsCode\22:\22return this.toISOString();\22,\22returnType\22:\22String\22,\22returnWasmType\22:\22i32\22,\22isStatic\22:false,\22isCoerced\22:false,\22parameters\22:[{\22name\22:\22this\22,\22javaType\22:\22JSDate\22,\22wasmType\22:\22i32\22,\22coerced\22:false}]},{\22className\22:\22JSJSON\22,\22methodName\22:\22stringify\22,\22importName\22:\22js_inline_JSJSON_stringify\22,\22jsCode\22:\22return JSON.stringify(val);\22,\22returnType\22:\22String\22,\22returnWasmType\22:\22i32\22,\22isStatic\22:true,\22isCoerced\22:false,\22parameters\22:[{\22name\22:\22val\22,\22javaType\22:\22JSValue\22,\22wasmType\22:\22i32\22,\22coerced\22:false}]},{\22className\22:\22JSStorage\22,\22methodName\22:\22setItem\22,\22importName\22:\22js_inline_JSStorage_setItem\22,\22jsCode\22:\22localStorage.setItem(k, v);\22,\22returnType\22:\22void\22,\22returnWasmType\22:null,\22isStatic\22:true,\22isCoerced\22:false,\22parameters\22:[{\22name\22:\22k\22,\22javaType\22:\22String\22,\22wasmType\22:\22i32\22,\22coerced\22:false},{\22name\22:\22v\22,\22javaType\22:\22String\22,\22wasmType\22:\22i32\22,\22coerced\22:false}]},{\22className\22:\22JSStorage\22,\22methodName\22:\22getItem\22,\22importName\22:\22js_inline_JSStorage_getItem\22,\22jsCode\22:\22return localStorage.getItem(k);\22,\22returnType\22:\22String\22,\22returnWasmType\22:\22i32\22,\22isStatic\22:true,\22isCoerced\22:false,\22parameters\22:[{\22name\22:\22k\22,\22javaType\22:\22String\22,\22wasmType\22:\22i32\22,\22coerced\22:false}]},{\22className\22:\22JSConsole\22,\22methodName\22:\22log\22,\22importName\22:\22js_inline_JSConsole_log\22,\22jsCode\22:\22console.log(msg);\22,\22returnType\22:\22void\22,\22returnWasmType\22:null,\22isStatic\22:true,\22isCoerced\22:false,\22parameters\22:[{\22name\22:\22msg\22,\22javaType\22:\22String\22,\22wasmType\22:\22i32\22,\22coerced\22:false}]},{\22className\22:\22JsInteropShowcase\22,\22methodName\22:\22getUserAgent\22,\22importName\22:\22js_inline_JsInteropShowcase_getUserAgent\22,\22jsCode\22:\22return window.navigator ? window.navigator.userAgent : 'Node.js Runtime';\22,\22returnType\22:\22String\22,\22returnWasmType\22:\22i32\22,\22isStatic\22:true,\22isCoerced\22:false,\22parameters\22:[]},{\22className\22:\22JsInteropShowcase\22,\22methodName\22:\22calculateHypotenuse\22,\22importName\22:\22js_inline_JsInteropShowcase_calculateHypotenuse\22,\22jsCode\22:\22return Math.hypot(a, b);\22,\22returnType\22:\22double\22,\22returnWasmType\22:\22f64\22,\22isStatic\22:true,\22isCoerced\22:false,\22parameters\22:[{\22name\22:\22a\22,\22javaType\22:\22double\22,\22wasmType\22:\22f64\22,\22coerced\22:true},{\22name\22:\22b\22,\22javaType\22:\22double\22,\22wasmType\22:\22f64\22,\22coerced\22:true}]}]")
  (import "dom" "js_inline_JSDate_create" (func $dom_js_inline_JSDate_create (result i32)))
  (import "dom" "js_inline_JSDate_toISOString" (func $dom_js_inline_JSDate_toISOString (param i32) (result i32)))
  (import "dom" "js_inline_JSJSON_stringify" (func $dom_js_inline_JSJSON_stringify (param i32) (result i32)))
  (import "dom" "js_inline_JSStorage_setItem" (func $dom_js_inline_JSStorage_setItem (param i32 i32 i32 i32)))
  (import "dom" "js_inline_JSStorage_getItem" (func $dom_js_inline_JSStorage_getItem (param i32 i32) (result i32)))
  (import "dom" "js_inline_JSConsole_log" (func $dom_js_inline_JSConsole_log (param i32 i32)))
  (import "dom" "js_inline_JsInteropShowcase_getUserAgent" (func $dom_js_inline_JsInteropShowcase_getUserAgent (result i32)))
  (import "dom" "js_inline_JsInteropShowcase_calculateHypotenuse" (func $dom_js_inline_JsInteropShowcase_calculateHypotenuse (param f64 f64) (result f64)))
  (@custom "javaw_js_exports" "[{\22className\22:\22JsInteropShowcase\22,\22exportName\22:\22JavaCalculator\22,\22structSize\22:8,\22vtableBase\22:0,\22hasVtable\22:true,\22constructors\22:[{\22exportFunctionName\22:\22JsInteropShowcase__init\22,\22params\22:[]}],\22methods\22:[{\22methodName\22:\22main\22,\22exportName\22:\22main\22,\22wasmFunctionName\22:\22main\22,\22returnType\22:\22void\22,\22returnWasmType\22:\22\22,\22isStatic\22:true,\22isCoerced\22:false,\22params\22:[{\22name\22:\22args\22,\22javaType\22:\22String[]\22,\22wasmType\22:\22i32\22,\22coerced\22:false}]},{\22methodName\22:\22getUserAgent\22,\22exportName\22:\22getUserAgent\22,\22wasmFunctionName\22:\22getUserAgent\22,\22returnType\22:\22String\22,\22returnWasmType\22:\22i32\22,\22isStatic\22:true,\22isCoerced\22:false,\22params\22:[]},{\22methodName\22:\22calculateHypotenuse\22,\22exportName\22:\22calculateHypotenuse\22,\22wasmFunctionName\22:\22calculateHypotenuse\22,\22returnType\22:\22double\22,\22returnWasmType\22:\22f64\22,\22isStatic\22:true,\22isCoerced\22:false,\22params\22:[{\22name\22:\22a\22,\22javaType\22:\22double\22,\22wasmType\22:\22f64\22,\22coerced\22:true},{\22name\22:\22b\22,\22javaType\22:\22double\22,\22wasmType\22:\22f64\22,\22coerced\22:true}]},{\22methodName\22:\22start\22,\22exportName\22:\22start\22,\22wasmFunctionName\22:\22start\22,\22returnType\22:\22void\22,\22returnWasmType\22:\22\22,\22isStatic\22:true,\22isCoerced\22:false,\22params\22:[]},{\22methodName\22:\22add\22,\22exportName\22:\22add\22,\22wasmFunctionName\22:\22JsInteropShowcase_add\22,\22returnType\22:\22int\22,\22returnWasmType\22:\22i32\22,\22isStatic\22:false,\22isCoerced\22:false,\22params\22:[{\22name\22:\22a\22,\22javaType\22:\22int\22,\22wasmType\22:\22i32\22,\22coerced\22:false},{\22name\22:\22b\22,\22javaType\22:\22int\22,\22wasmType\22:\22i32\22,\22coerced\22:false}]},{\22methodName\22:\22getMemory\22,\22exportName\22:\22getMemory\22,\22wasmFunctionName\22:\22JsInteropShowcase_getMemory\22,\22returnType\22:\22int\22,\22returnWasmType\22:\22i32\22,\22isStatic\22:false,\22isCoerced\22:false,\22params\22:[]}],\22fields\22:[{\22name\22:\22memoryValue\22,\22javaType\22:\22int\22,\22wasmType\22:\22i32\22,\22offset\22:4,\22size\22:4,\22isStatic\22:false}]}]")
  (tag $Exception (export "Exception") (param i32))
  (memory (export "memory") 2)
  (data (i32.const 4096) "JavaCalculator\00")
  (data (i32.const 4111) "return window.navigator ? window.navigator.userAgent : 'Node.js Runtime';\00")
  (data (i32.const 4185) "return Math.hypot(a, b);\00")
  (data (i32.const 4210) "js-interop-app\00")
  (data (i32.const 4225) "h2\00")
  (data (i32.const 4228) "GraalVM Web Image-Style JavaScript Interop\00")
  (data (i32.const 4271) "color\00")
  (data (i32.const 4277) "#f59e0b\00")
  (data (i32.const 4285) "p\00")
  (data (i32.const 4287) "#cbd5e1\00")
  (data (i32.const 4295) "font-size\00")
  (data (i32.const 4305) "14px\00")
  (data (i32.const 4310) "Host User Agent: \00")
  (data (i32.const 4328) "#a3e635\00")
  (data (i32.const 4336) "Hypotenuse of (3.0, 4.0) via @JS: \00")
  (data (i32.const 4371) "#38bdf8\00")
  (data (i32.const 4379) "Current Date/Time via JSDate: \00")
  (data (i32.const 4410) "#e879f9\00")
  (data (i32.const 4418) "JSON Stringify demo: \00")
  (data (i32.const 4440) "{\5c\22engine\5c\22:\5c\22javaw\5c\22,\5c\22version\5c\22:\5c\221.0\5c\22}\00")
  (data (i32.const 4483) "javaw_last_access\00")
  (data (i32.const 4501) "#fbbf24\00")
  (data (i32.const 4509) "Saved to localStorage: \00")
  (data (i32.const 4533) "JsInteropShowcase successfully initialized!\00")
  (global $__heap_ptr (mut i32) (i32.const 65536))
  ;; --- WebAssembly Function Type Signatures ---
  (type $t_sig_1 (func (param i32 i32 i32) (result i32)))
  (type $t_sig_2 (func (param i32) (result i32)))
  ;; --- WebAssembly Function Table (VTable) ---
  (table (export "__vtable") 8 funcref)
  (elem (i32.const 0) $JsInteropShowcase_add $JsInteropShowcase_getMemory $Throwable_getMessage $Exception_getMessage $RuntimeException_getMessage $IllegalArgumentException_getMessage $IllegalStateException_getMessage $NullPointerException_getMessage)
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
  ;; Class/Interface: JsInteropShowcase
  (func $JsInteropShowcase__init (export "JsInteropShowcase__init") (param $this i32)
  )
  (func $main (export "main") (param $args i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
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
    call $start
  )
  (func $getUserAgent (export "getUserAgent") (result i32)
    call $dom_js_inline_JsInteropShowcase_getUserAgent
  )
  (func $calculateHypotenuse (export "calculateHypotenuse") (param $a f64) (param $b f64) (result f64)
    local.get $a
    local.get $b
    call $dom_js_inline_JsInteropShowcase_calculateHypotenuse
  )
  (func $start (export "start")
    (local $root i32)
    (local $title i32)
    (local $uaInfo i32)
    (local $hyp f64)
    (local $mathInfo i32)
    (local $now i32)
    (local $dateInfo i32)
    (local $jsonInfo i32)
    (local $stored i32)
    (local $storeInfo i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
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
    i32.const 4210
    i32.const 14
    call $dom_getElementById
    local.set $root
    local.get $root
    i32.const 0
    i32.eq
    if
    call $dom_getBody
    local.set $root
    end
    i32.const 4225
    i32.const 2
    call $dom_createElement
    local.set $title
    local.get $title
    i32.const 4228
    i32.const 42
    call $dom_setInnerText
    local.get $title
    i32.const 4271
    i32.const 5
    i32.const 4277
    i32.const 7
    call $dom_setStyle
    local.get $root
    local.get $title
    call $dom_appendChild
    i32.const 4285
    i32.const 1
    call $dom_createElement
    local.set $uaInfo
    local.get $uaInfo
    i32.const 4271
    i32.const 5
    i32.const 4287
    i32.const 7
    call $dom_setStyle
    local.get $uaInfo
    i32.const 4295
    i32.const 9
    i32.const 4305
    i32.const 4
    call $dom_setStyle
    local.get $uaInfo
    i32.const 4310
    call $getUserAgent
    call $__str_concat
    local.set $__str_arg_ptr
    local.get $__str_arg_ptr
    local.get $__str_arg_ptr
    call $__str_length
    call $dom_setInnerText
    local.get $root
    local.get $uaInfo
    call $dom_appendChild
    f64.const 3.0
    f64.const 4.0
    call $calculateHypotenuse
    local.set $hyp
    i32.const 4285
    i32.const 1
    call $dom_createElement
    local.set $mathInfo
    local.get $mathInfo
    i32.const 4271
    i32.const 5
    i32.const 4328
    i32.const 7
    call $dom_setStyle
    local.get $mathInfo
    i32.const 4336
    local.get $hyp
    call $__str_from_f64
    call $__str_concat
    local.set $__str_arg_ptr
    local.get $__str_arg_ptr
    local.get $__str_arg_ptr
    call $__str_length
    call $dom_setInnerText
    local.get $root
    local.get $mathInfo
    call $dom_appendChild
    call $dom_js_inline_JSDate_create
    local.set $now
    i32.const 4285
    i32.const 1
    call $dom_createElement
    local.set $dateInfo
    local.get $dateInfo
    i32.const 4271
    i32.const 5
    i32.const 4371
    i32.const 7
    call $dom_setStyle
    local.get $dateInfo
    i32.const 4379
    local.get $now
    call $dom_js_inline_JSDate_toISOString
    call $__str_from_i32
    call $__str_concat
    local.set $__str_arg_ptr
    local.get $__str_arg_ptr
    local.get $__str_arg_ptr
    call $__str_length
    call $dom_setInnerText
    local.get $root
    local.get $dateInfo
    call $dom_appendChild
    i32.const 4285
    i32.const 1
    call $dom_createElement
    local.set $jsonInfo
    local.get $jsonInfo
    i32.const 4271
    i32.const 5
    i32.const 4410
    i32.const 7
    call $dom_setStyle
    local.get $jsonInfo
    i32.const 4418
    i32.const 4440
    call $dom_js_inline_JSJSON_stringify
    call $__str_concat
    local.set $__str_arg_ptr
    local.get $__str_arg_ptr
    local.get $__str_arg_ptr
    call $__str_length
    call $dom_setInnerText
    local.get $root
    local.get $jsonInfo
    call $dom_appendChild
    i32.const 4483
    i32.const 17
    local.get $now
    call $dom_js_inline_JSDate_toISOString
    local.set $__str_arg_ptr
    local.get $__str_arg_ptr
    local.get $__str_arg_ptr
    call $__str_length
    call $dom_js_inline_JSStorage_setItem
    i32.const 4483
    i32.const 17
    call $dom_js_inline_JSStorage_getItem
    local.set $stored
    i32.const 4285
    i32.const 1
    call $dom_createElement
    local.set $storeInfo
    local.get $storeInfo
    i32.const 4271
    i32.const 5
    i32.const 4501
    i32.const 7
    call $dom_setStyle
    local.get $storeInfo
    i32.const 4509
    local.get $stored
    call $__str_concat
    local.set $__str_arg_ptr
    local.get $__str_arg_ptr
    local.get $__str_arg_ptr
    call $__str_length
    call $dom_setInnerText
    local.get $root
    local.get $storeInfo
    call $dom_appendChild
    i32.const 4533
    i32.const 43
    call $dom_js_inline_JSConsole_log
  )
  (func $JsInteropShowcase_add (export "JsInteropShowcase_add") (param $this i32) (param $a i32) (param $b i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
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
    local.get $a
    local.get $b
    i32.add
    i32.store offset=4
    local.get $this
    i32.load offset=4
    return
  )
  (func $JsInteropShowcase_getMemory (export "JsInteropShowcase_getMemory") (param $this i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
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
)
