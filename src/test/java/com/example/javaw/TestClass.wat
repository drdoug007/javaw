(module
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
  (import "dom" "thread_sleep" (func $dom_thread_sleep (param i32)))
  (import "dom" "thread_current_id" (func $dom_thread_current_id (result i32)))
  (import "dom" "thread_hardware_concurrency" (func $dom_thread_hardware_concurrency (result i32)))
  ;; --- WASI Host Imports (wasi_snapshot_preview1) ---
  (import "wasi_snapshot_preview1" "fd_write" (func $__wasi_fd_write (param i32 i32 i32 i32) (result i32)))
  (memory (export "memory") 2 20 shared)
  (global $asyncCounter (mut i32) (i32.const 0))
  (global $asyncFetchedData (mut i32) (i32.const 0))
  (data (i32.const 4096) "\00")
  (data (i32.const 4097) "onTimerTick\00")
  (data (i32.const 4109) "onFrameUpdate\00")
  (data (i32.const 4123) "onFetchSuccess\00")
  (data (i32.const 4138) "Hello\00")
  (data (i32.const 4144) "WebAssembly\00")
  (data (i32.const 4156) ", \00")
  (data (i32.const 4159) "!\00")
  (data (i32.const 4161) "User: \00")
  (data (i32.const 4168) " (age: \00")
  (data (i32.const 4176) ")\00")
  (data (i32.const 4178) "Hello World\00")
  (data (i32.const 4190) "World\00")
  (data (i32.const 4196) "Java\00")
  (data (i32.const 4201) "Count: \00")
  (data (i32.const 4209) "testCanvas\00")
  (data (i32.const 4220) "#ff0000\00")
  (data (i32.const 4228) "glCanvas\00")
  (data (i32.const 4237) "({ title: 'Dynamic JS', count: 42, active: true })\00")
  (data (i32.const 4288) "title\00")
  (data (i32.const 4294) "count\00")
  (data (i32.const 4300) "active\00")
  (data (i32.const 4307) "user\00")
  (data (i32.const 4312) "Alice\00")
  (data (i32.const 4318) "score\00")
  (data (i32.const 4324) "rating\00")
  (data (i32.const 4331) "premium\00")
  (data (i32.const 4339) "item1\00")
  (data (i32.const 4345) "item2\00")
  (data (i32.const 4351) "updated\00")
  (data (i32.const 4359) "Math\00")
  (data (i32.const 4364) "max\00")
  (data (i32.const 4368) "Testing WASI standard output from Java!\00")
  (data (i32.const 4408) "Progress: \00")
  (data (i32.const 4419) "WASI stderr stream message\00")
  (data (i32.const 4446) "WASI Facade message: \00")
  (data (i32.const 4468) "Active\00")
  (data (i32.const 4475) "WASI Facade Error\00")
  (global $__heap_ptr (mut i32) (i32.const 65536))
  ;; --- WebAssembly Function Type Signatures ---
  (type $t_sig_1 (func (param i32)))
  (type $t_sig_2 (func (param i32 f64)))
  (type $t_sig_3 (func (param i32 i32)))
  (type $t_sig_4 (func (param i32 i32) (result i32)))
  (type $t_sig_5 (func (param i32) (result i32)))
  (type $t_sig_6 (func (param i32 i32 i32) (result i32)))
  (type $t_sig_7 (func (param i32) (result f64)))
  (type $t_sig_8 (func (param i32 i32) (result f64)))
  (type $t_sig_9 (func (param i32 i32 i32 i32) (result i32)))
  (type $t_sig_10 (func (param i32 i32 i32 i32 i32) (result i32)))
  (type $t_sig_11 (func (param i32 i32 i32)))
  ;; --- WebAssembly Function Table (VTable) ---
  (table (export "__vtable") 63 funcref)
  (elem (i32.const 0) $onTimerTick $onFrameUpdate $onFetchSuccess $testScheduleTimer $testScheduleInterval $testRequestAnimationFrame $testFetchData $testConfirm $sayHello $addNumbers $getPi $calculateFactorial $sumRangeWithSkips $sumArray $sumArrayWithSkips $sumDoubleArray $sumInlineArray $createAndSumArray $evaluateSwitch $switchFallthrough $switchInsideLoop $labeledBreakNestedLoops $labeledContinueNestedLoops $labeledBlockBreak $labeledWhileBreak $concatStrings $formatUser $getStringLength $getCharAtIndex $getSubString $compareStrings $findSubstringIndex $findMissingSubstringIndex $buildCustomString $manipulateStringBuilder $testPointObject $testDirectFieldAccess $testPolymorphismArea $testInheritedColor $testInstanceOf $testObjectArray $testCanvasDraw $testWebGLInit $testJsEvalAndGet $testJsCreateAndSet $testJsArrayOperations $testJsGlobalMathCall $testWasiOutput $testWasiFacade $testAtomicIntegerOperations $testAtomicsArrayOperations $testWasmThread $Point_getX $Point_getY $Point_setLocation $Point_distanceSq $Shape_getColor $Shape_area $Shape_getColor $Circle_area $Circle_getRadius $Shape_getColor $Rectangle_area)
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
    i32.const 0
    local.set $i
    (block $break_zero
      (loop $loop_zero
        local.get $i
        local.get $aligned_size
        i32.ge_s
        br_if $break_zero
        local.get $ptr
        local.get $i
        i32.add
        i32.const 0
        i32.store8
        local.get $i
        i32.const 1
        i32.add
        local.set $i
        br $loop_zero
      )
    )
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
    (local $i i32)
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
    i32.const 0
    local.set $i
    (block $break_copy1
      (loop $loop_copy1
        local.get $i
        local.get $len1
        i32.ge_s
        br_if $break_copy1
        local.get $res
        local.get $i
        i32.add
        local.get $s1
        local.get $i
        i32.add
        i32.load8_u
        i32.store8
        local.get $i
        i32.const 1
        i32.add
        local.set $i
        br $loop_copy1
      )
    )
    i32.const 0
    local.set $i
    (block $break_copy2
      (loop $loop_copy2
        local.get $i
        local.get $len2
        i32.ge_s
        br_if $break_copy2
        local.get $res
        local.get $len1
        i32.add
        local.get $i
        i32.add
        local.get $s2
        local.get $i
        i32.add
        i32.load8_u
        i32.store8
        local.get $i
        i32.const 1
        i32.add
        local.set $i
        br $loop_copy2
      )
    )
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
    (local $i i32)
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
    i32.const 0
    local.set $i
    (block $break_sub
      (loop $loop_sub
        local.get $i
        local.get $subLen
        i32.ge_s
        br_if $break_sub
        local.get $res
        local.get $i
        i32.add
        local.get $s
        local.get $start
        i32.add
        local.get $i
        i32.add
        i32.load8_u
        i32.store8
        local.get $i
        i32.const 1
        i32.add
        local.set $i
        br $loop_sub
      )
    )
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
    (local $i i32)
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
    i32.const 0
    local.set $i
    (block $break_sb_copy
      (loop $loop_sb_copy
        local.get $i
        local.get $str_len
        i32.ge_s
        br_if $break_sb_copy
        local.get $sb
        i32.const 8
        i32.add
        local.get $cur_len
        i32.add
        local.get $i
        i32.add
        local.get $str
        local.get $i
        i32.add
        i32.load8_u
        i32.store8
        local.get $i
        i32.const 1
        i32.add
        local.set $i
        br $loop_sb_copy
      )
    )
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
    (local $i i32)
    local.get $sb
    i32.const 4
    i32.add
    i32.load
    local.set $cur_len
    local.get $cur_len
    call $__str_alloc
    local.set $res
    i32.const 0
    local.set $i
    (block $break_sb_tostr
      (loop $loop_sb_tostr
        local.get $i
        local.get $cur_len
        i32.ge_s
        br_if $break_sb_tostr
        local.get $res
        local.get $i
        i32.add
        local.get $sb
        i32.const 8
        i32.add
        local.get $i
        i32.add
        i32.load8_u
        i32.store8
        local.get $i
        i32.const 1
        i32.add
        local.set $i
        br $loop_sb_tostr
      )
    )
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
    i32.const 4097
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
    i32.const 4097
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
    i32.const 4109
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
    local.get $url
    local.set $__str_arg_ptr
    local.get $__str_arg_ptr
    local.get $__str_arg_ptr
    call $__str_length
    i32.const 4123
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
    i32.const 4138
    local.set $greeting
    i32.const 4144
    local.set $target
    local.get $greeting
    i32.const 4156
    call $__str_concat
    local.get $target
    call $__str_concat
    i32.const 4159
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
    i32.const 4161
    local.get $name
    call $__str_concat
    i32.const 4168
    call $__str_concat
    local.get $age
    call $__str_from_i32
    call $__str_concat
    i32.const 4176
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
    i32.const 4144
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
    i32.const 4144
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
    i32.const 4178
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
    i32.const 4178
    local.set $s
    local.get $s
    i32.const 4190
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
    i32.const 4178
    local.set $s
    local.get $s
    i32.const 4196
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
    i32.const 16
    call $__sb_new
    local.set $__sb_tmp
    local.get $__sb_tmp
    i32.const 4201
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
    i32.const 16
    call $__sb_new
    local.set $__sb_tmp
    local.get $__sb_tmp
    i32.const 4196
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
    i32.const 16
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 52
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
    call_indirect (type $t_sig_11)
    local.get $p
    local.set $__obj_tmp
    local.get $__obj_tmp
    local.get $__obj_tmp
    i32.load offset=0
    i32.const 3
    i32.add
    call_indirect (type $t_sig_5)
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
    i32.const 16
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 52
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
    local.get $type
    i32.const 1
    i32.eq
    if
    i32.const 16
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 58
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
    i32.const 61
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
    call_indirect (type $t_sig_5)
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
    local.get $type
    i32.const 1
    i32.eq
    if
    i32.const 16
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 58
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
    i32.const 61
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
    call_indirect (type $t_sig_5)
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
    local.get $type
    i32.const 1
    i32.eq
    if
    i32.const 16
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 58
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
    i32.const 61
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
      i32.const 58
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
      i32.const 61
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
    i32.const 52
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
    i32.const 52
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
    i32.const 52
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
    call_indirect (type $t_sig_5)
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
    i32.const 4209
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
    i32.const 4220
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
    i32.const 4228
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
    i32.const 4237
    i32.const 50
    call $dom_js_eval
    local.set $obj
    local.get $obj
    i32.const 4288
    i32.const 5
    call $dom_js_getString
    local.set $title
    local.get $obj
    i32.const 4294
    i32.const 5
    call $dom_js_getInt
    local.set $count
    local.get $obj
    i32.const 4300
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
    call $dom_js_createObject
    local.set $obj
    local.get $obj
    i32.const 4307
    i32.const 4
    i32.const 4312
    i32.const 5
    call $dom_js_setString
    local.get $obj
    i32.const 4318
    i32.const 5
    i32.const 95
    call $dom_js_setInt
    local.get $obj
    i32.const 4324
    i32.const 6
    f64.const 4.8
    call $dom_js_setDouble
    local.get $obj
    i32.const 4331
    i32.const 7
    i32.const 1
    call $dom_js_setBoolean
    local.get $obj
    i32.const 4307
    i32.const 4
    call $dom_js_getString
    local.set $user
    local.get $obj
    i32.const 4318
    i32.const 5
    call $dom_js_getInt
    local.set $score
    local.get $obj
    i32.const 4331
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
    call $dom_js_createArray
    local.set $arr
    local.get $arr
    i32.const 4339
    i32.const 5
    call $dom_js_pushStr
    local.get $arr
    i32.const 4345
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
    i32.const 4351
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
    call $dom_js_global
    i32.const 4359
    i32.const 4
    call $dom_js_get
    local.set $math
    local.get $math
    i32.const 4364
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
    i32.const 1
    i32.const 4368
    i32.const 39
    i32.const 1
    call $__wasi_print_str
    i32.const 1
    i32.const 4408
    i32.const 10
    i32.const 0
    call $__wasi_print_str
    i32.const 1
    i32.const 100
    i32.const 1
    call $__wasi_print_int
    i32.const 2
    i32.const 4419
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
    i32.const 1
    i32.const 4446
    i32.const 21
    i32.const 0
    call $__wasi_print_str
    i32.const 1
    i32.const 4468
    i32.const 6
    i32.const 1
    call $__wasi_print_str
    i32.const 1
    i32.const 42
    i32.const 1
    call $__wasi_print_int
    i32.const 2
    i32.const 4475
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
    local.get $this
    i32.load offset=8
    local.get $this
    i32.load offset=12
    i32.mul
    return
  )
)
