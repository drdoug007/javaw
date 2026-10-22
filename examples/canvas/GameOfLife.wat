(module
  (@custom "sourceMappingURL" "\13GameOfLife.wasm.map")
  (import "dom" "getElementById" (func $dom_getElementById (param i32 i32) (result i32)))
  (import "dom" "createElement" (func $dom_createElement (param i32 i32) (result i32)))
  (import "dom" "getBody" (func $dom_getBody (result i32)))
  (import "dom" "getCanvasContext2D" (func $dom_getCanvasContext2D (param i32 i32) (result i32)))
  (import "dom" "setInnerText" (func $dom_setInnerText (param i32 i32 i32)))
  (import "dom" "setAttribute" (func $dom_setAttribute (param i32 i32 i32 i32 i32)))
  (import "dom" "appendChild" (func $dom_appendChild (param i32 i32)))
  (import "dom" "setStyle" (func $dom_setStyle (param i32 i32 i32 i32 i32)))
  (import "dom" "setFillStyle" (func $dom_setFillStyle (param i32 i32 i32)))
  (import "dom" "fillRect" (func $dom_fillRect (param i32 f64 f64 f64 f64)))
  (import "dom" "addEventListener" (func $dom_addEventListener (param i32 i32 i32 i32 i32)))
  (tag $Exception (export "Exception") (param i32))
  (memory (export "memory") 2)
  (global $COLS (mut i32) (i32.const 40))
  (global $ROWS (mut i32) (i32.const 30))
  (global $CELL_SIZE (mut i32) (i32.const 12))
  (global $grid (mut i32) (i32.const 0))
  (global $nextGrid (mut i32) (i32.const 0))
  (global $generation (mut i32) (i32.const 0))
  (global $isRunning (mut i32) (i32.const 0))
  (data (i32.const 4096) "game-container\00")
  (data (i32.const 4111) "h2\00")
  (data (i32.const 4114) "Conway's Game of Life (HTML5 Canvas 2D)\00")
  (data (i32.const 4154) "color\00")
  (data (i32.const 4160) "#34d399\00")
  (data (i32.const 4168) "canvas\00")
  (data (i32.const 4175) "id\00")
  (data (i32.const 4178) "life-canvas\00")
  (data (i32.const 4190) "width\00")
  (data (i32.const 4196) "480\00")
  (data (i32.const 4200) "height\00")
  (data (i32.const 4207) "360\00")
  (data (i32.const 4211) "background\00")
  (data (i32.const 4222) "#020617\00")
  (data (i32.const 4230) "border\00")
  (data (i32.const 4237) "1px solid #1e293b\00")
  (data (i32.const 4255) "border-radius\00")
  (data (i32.const 4269) "8px\00")
  (data (i32.const 4273) "display\00")
  (data (i32.const 4281) "block\00")
  (data (i32.const 4287) "margin-bottom\00")
  (data (i32.const 4301) "12px\00")
  (data (i32.const 4306) "div\00")
  (data (i32.const 4310) "flex\00")
  (data (i32.const 4315) "gap\00")
  (data (i32.const 4319) "10px\00")
  (data (i32.const 4324) "align-items\00")
  (data (i32.const 4336) "center\00")
  (data (i32.const 4343) "button\00")
  (data (i32.const 4350) "Next Step\00")
  (data (i32.const 4360) "padding\00")
  (data (i32.const 4368) "6px 14px\00")
  (data (i32.const 4377) "#10b981\00")
  (data (i32.const 4385) "white\00")
  (data (i32.const 4391) "none\00")
  (data (i32.const 4396) "4px\00")
  (data (i32.const 4400) "cursor\00")
  (data (i32.const 4407) "pointer\00")
  (data (i32.const 4415) "click\00")
  (data (i32.const 4421) "step\00")
  (data (i32.const 4426) "Randomize\00")
  (data (i32.const 4436) "#6366f1\00")
  (data (i32.const 4444) "randomize\00")
  (data (i32.const 4454) "Clear\00")
  (data (i32.const 4460) "#ef4444\00")
  (data (i32.const 4468) "clearGrid\00")
  (data (i32.const 4478) "span\00")
  (data (i32.const 4483) "gen-info\00")
  (data (i32.const 4492) "#94a3b8\00")
  (data (i32.const 4500) "font-size\00")
  (data (i32.const 4510) "14px\00")
  (data (i32.const 4515) "Generation: 0\00")
  (data (i32.const 4529) "Generation: \00")
  (global $__heap_ptr (mut i32) (i32.const 65536))
  ;; --- WebAssembly Function Type Signatures ---
  (type $t_sig_1 (func (param i32) (result i32)))
  ;; --- WebAssembly Function Table (VTable) ---
  (table (export "__vtable") 6 funcref)
  (elem (i32.const 0) $Throwable_getMessage $Exception_getMessage $RuntimeException_getMessage $IllegalArgumentException_getMessage $IllegalStateException_getMessage $NullPointerException_getMessage)
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
  ;; Class/Interface: GameOfLife
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
    call $initGame
  )
  (func $initGame (export "initGame")
    (local $container i32)
    (local $title i32)
    (local $canvas i32)
    (local $controls i32)
    (local $stepBtn i32)
    (local $randomBtn i32)
    (local $clearBtn i32)
    (local $info i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
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
    i32.const 4096
    i32.const 14
    call $dom_getElementById
    local.set $container
    local.get $container
    i32.const 0
    i32.eq
    if
    call $dom_getBody
    local.set $container
    end
    i32.const 4111
    i32.const 2
    call $dom_createElement
    local.set $title
    local.get $title
    i32.const 4114
    i32.const 39
    call $dom_setInnerText
    local.get $title
    i32.const 4154
    i32.const 5
    i32.const 4160
    i32.const 7
    call $dom_setStyle
    local.get $container
    local.get $title
    call $dom_appendChild
    i32.const 4168
    i32.const 6
    call $dom_createElement
    local.set $canvas
    local.get $canvas
    i32.const 4175
    i32.const 2
    i32.const 4178
    i32.const 11
    call $dom_setAttribute
    local.get $canvas
    i32.const 4190
    i32.const 5
    i32.const 4196
    i32.const 3
    call $dom_setAttribute
    local.get $canvas
    i32.const 4200
    i32.const 6
    i32.const 4207
    i32.const 3
    call $dom_setAttribute
    local.get $canvas
    i32.const 4211
    i32.const 10
    i32.const 4222
    i32.const 7
    call $dom_setStyle
    local.get $canvas
    i32.const 4230
    i32.const 6
    i32.const 4237
    i32.const 17
    call $dom_setStyle
    local.get $canvas
    i32.const 4255
    i32.const 13
    i32.const 4269
    i32.const 3
    call $dom_setStyle
    local.get $canvas
    i32.const 4273
    i32.const 7
    i32.const 4281
    i32.const 5
    call $dom_setStyle
    local.get $canvas
    i32.const 4287
    i32.const 13
    i32.const 4301
    i32.const 4
    call $dom_setStyle
    local.get $container
    local.get $canvas
    call $dom_appendChild
    i32.const 4306
    i32.const 3
    call $dom_createElement
    local.set $controls
    local.get $controls
    i32.const 4273
    i32.const 7
    i32.const 4310
    i32.const 4
    call $dom_setStyle
    local.get $controls
    i32.const 4315
    i32.const 3
    i32.const 4319
    i32.const 4
    call $dom_setStyle
    local.get $controls
    i32.const 4324
    i32.const 11
    i32.const 4336
    i32.const 6
    call $dom_setStyle
    i32.const 4343
    i32.const 6
    call $dom_createElement
    local.set $stepBtn
    local.get $stepBtn
    i32.const 4350
    i32.const 9
    call $dom_setInnerText
    local.get $stepBtn
    i32.const 4360
    i32.const 7
    i32.const 4368
    i32.const 8
    call $dom_setStyle
    local.get $stepBtn
    i32.const 4211
    i32.const 10
    i32.const 4377
    i32.const 7
    call $dom_setStyle
    local.get $stepBtn
    i32.const 4154
    i32.const 5
    i32.const 4385
    i32.const 5
    call $dom_setStyle
    local.get $stepBtn
    i32.const 4230
    i32.const 6
    i32.const 4391
    i32.const 4
    call $dom_setStyle
    local.get $stepBtn
    i32.const 4255
    i32.const 13
    i32.const 4396
    i32.const 3
    call $dom_setStyle
    local.get $stepBtn
    i32.const 4400
    i32.const 6
    i32.const 4407
    i32.const 7
    call $dom_setStyle
    local.get $stepBtn
    i32.const 4415
    i32.const 5
    i32.const 4421
    i32.const 4
    call $dom_addEventListener
    local.get $controls
    local.get $stepBtn
    call $dom_appendChild
    i32.const 4343
    i32.const 6
    call $dom_createElement
    local.set $randomBtn
    local.get $randomBtn
    i32.const 4426
    i32.const 9
    call $dom_setInnerText
    local.get $randomBtn
    i32.const 4360
    i32.const 7
    i32.const 4368
    i32.const 8
    call $dom_setStyle
    local.get $randomBtn
    i32.const 4211
    i32.const 10
    i32.const 4436
    i32.const 7
    call $dom_setStyle
    local.get $randomBtn
    i32.const 4154
    i32.const 5
    i32.const 4385
    i32.const 5
    call $dom_setStyle
    local.get $randomBtn
    i32.const 4230
    i32.const 6
    i32.const 4391
    i32.const 4
    call $dom_setStyle
    local.get $randomBtn
    i32.const 4255
    i32.const 13
    i32.const 4396
    i32.const 3
    call $dom_setStyle
    local.get $randomBtn
    i32.const 4400
    i32.const 6
    i32.const 4407
    i32.const 7
    call $dom_setStyle
    local.get $randomBtn
    i32.const 4415
    i32.const 5
    i32.const 4444
    i32.const 9
    call $dom_addEventListener
    local.get $controls
    local.get $randomBtn
    call $dom_appendChild
    i32.const 4343
    i32.const 6
    call $dom_createElement
    local.set $clearBtn
    local.get $clearBtn
    i32.const 4454
    i32.const 5
    call $dom_setInnerText
    local.get $clearBtn
    i32.const 4360
    i32.const 7
    i32.const 4368
    i32.const 8
    call $dom_setStyle
    local.get $clearBtn
    i32.const 4211
    i32.const 10
    i32.const 4460
    i32.const 7
    call $dom_setStyle
    local.get $clearBtn
    i32.const 4154
    i32.const 5
    i32.const 4385
    i32.const 5
    call $dom_setStyle
    local.get $clearBtn
    i32.const 4230
    i32.const 6
    i32.const 4391
    i32.const 4
    call $dom_setStyle
    local.get $clearBtn
    i32.const 4255
    i32.const 13
    i32.const 4396
    i32.const 3
    call $dom_setStyle
    local.get $clearBtn
    i32.const 4400
    i32.const 6
    i32.const 4407
    i32.const 7
    call $dom_setStyle
    local.get $clearBtn
    i32.const 4415
    i32.const 5
    i32.const 4468
    i32.const 9
    call $dom_addEventListener
    local.get $controls
    local.get $clearBtn
    call $dom_appendChild
    i32.const 4478
    i32.const 4
    call $dom_createElement
    local.set $info
    local.get $info
    i32.const 4175
    i32.const 2
    i32.const 4483
    i32.const 8
    call $dom_setAttribute
    local.get $info
    i32.const 4154
    i32.const 5
    i32.const 4492
    i32.const 7
    call $dom_setStyle
    local.get $info
    i32.const 4500
    i32.const 9
    i32.const 4510
    i32.const 4
    call $dom_setStyle
    local.get $info
    i32.const 4515
    i32.const 13
    call $dom_setInnerText
    local.get $controls
    local.get $info
    call $dom_appendChild
    local.get $container
    local.get $controls
    call $dom_appendChild
    i32.const 2
    i32.const 2
    call $seedGlider
    i32.const 15
    i32.const 10
    call $seedBlinker
    i32.const 20
    i32.const 15
    call $seedGlider
    call $render
  )
  (func $seedGlider (export "seedGlider") (param $x i32) (param $y i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
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
    local.get $x
    i32.const 1
    i32.add
    local.get $y
    i32.const 1
    call $setCell
    local.get $x
    i32.const 2
    i32.add
    local.get $y
    i32.const 1
    i32.add
    i32.const 1
    call $setCell
    local.get $x
    local.get $y
    i32.const 2
    i32.add
    i32.const 1
    call $setCell
    local.get $x
    i32.const 1
    i32.add
    local.get $y
    i32.const 2
    i32.add
    i32.const 1
    call $setCell
    local.get $x
    i32.const 2
    i32.add
    local.get $y
    i32.const 2
    i32.add
    i32.const 1
    call $setCell
  )
  (func $seedBlinker (export "seedBlinker") (param $x i32) (param $y i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
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
    local.get $x
    local.get $y
    i32.const 1
    call $setCell
    local.get $x
    i32.const 1
    i32.add
    local.get $y
    i32.const 1
    call $setCell
    local.get $x
    i32.const 2
    i32.add
    local.get $y
    i32.const 1
    call $setCell
  )
  (func $getCell (export "getCell") (param $x i32) (param $y i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
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
    local.get $x
    i32.const 0
    i32.lt_s
    local.get $x
    global.get $COLS
    i32.ge_s
    i32.or
    local.get $y
    i32.const 0
    i32.lt_s
    i32.or
    local.get $y
    global.get $ROWS
    i32.ge_s
    i32.or
    if
    i32.const 0
    return
    end
    global.get $grid
    local.get $y
    global.get $COLS
    i32.mul
    local.get $x
    i32.add
    i32.const 4
    i32.mul
    i32.add
    i32.load offset=4
    return
  )
  (func $setCell (export "setCell") (param $x i32) (param $y i32) (param $val i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
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
    local.get $x
    i32.const 0
    i32.ge_s
    local.get $x
    global.get $COLS
    i32.lt_s
    i32.and
    local.get $y
    i32.const 0
    i32.ge_s
    i32.and
    local.get $y
    global.get $ROWS
    i32.lt_s
    i32.and
    if
    global.get $grid
    local.get $y
    global.get $COLS
    i32.mul
    local.get $x
    i32.add
    i32.const 4
    i32.mul
    i32.add
    local.get $val
    i32.store offset=4
    end
  )
  (func $countNeighbors (export "countNeighbors") (param $x i32) (param $y i32) (result i32)
    (local $count i32)
    (local $dy i32)
    (local $dx i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
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
    i32.const 1
    i32.sub
    local.set $dy
    (block $break_for_1
      (loop $loop_for_1
    local.get $dy
    i32.const 1
    i32.le_s
        i32.eqz
        br_if $break_for_1
        (block $continue_for_1
    i32.const 0
    i32.const 1
    i32.sub
    local.set $dx
    (block $break_for_2
      (loop $loop_for_2
    local.get $dx
    i32.const 1
    i32.le_s
        i32.eqz
        br_if $break_for_2
        (block $continue_for_2
    local.get $dx
    i32.const 0
    i32.eq
    local.get $dy
    i32.const 0
    i32.eq
    i32.and
    if
    br $continue_for_2
    end
    local.get $count
    local.get $x
    local.get $dx
    i32.add
    local.get $y
    local.get $dy
    i32.add
    call $getCell
    i32.add
    local.set $count
        )
    local.get $dx
    i32.const 1
    i32.add
    local.set $dx
        br $loop_for_2
      )
    )
        )
    local.get $dy
    i32.const 1
    i32.add
    local.set $dy
        br $loop_for_1
      )
    )
    local.get $count
    return
  )
  (func $step (export "step")
    (local $y i32)
    (local $x i32)
    (local $neighbors i32)
    (local $current i32)
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
    local.set $y
    (block $break_for_3
      (loop $loop_for_3
    local.get $y
    global.get $ROWS
    i32.lt_s
        i32.eqz
        br_if $break_for_3
        (block $continue_for_3
    i32.const 0
    local.set $x
    (block $break_for_4
      (loop $loop_for_4
    local.get $x
    global.get $COLS
    i32.lt_s
        i32.eqz
        br_if $break_for_4
        (block $continue_for_4
    local.get $x
    local.get $y
    call $countNeighbors
    local.set $neighbors
    local.get $x
    local.get $y
    call $getCell
    local.set $current
    local.get $current
    i32.const 1
    i32.eq
    if
    local.get $neighbors
    i32.const 2
    i32.eq
    local.get $neighbors
    i32.const 3
    i32.eq
    i32.or
    if
    global.get $nextGrid
    local.get $y
    global.get $COLS
    i32.mul
    local.get $x
    i32.add
    i32.const 4
    i32.mul
    i32.add
    i32.const 1
    i32.store offset=4
    else
    global.get $nextGrid
    local.get $y
    global.get $COLS
    i32.mul
    local.get $x
    i32.add
    i32.const 4
    i32.mul
    i32.add
    i32.const 0
    i32.store offset=4
    end
    else
    local.get $neighbors
    i32.const 3
    i32.eq
    if
    global.get $nextGrid
    local.get $y
    global.get $COLS
    i32.mul
    local.get $x
    i32.add
    i32.const 4
    i32.mul
    i32.add
    i32.const 1
    i32.store offset=4
    else
    global.get $nextGrid
    local.get $y
    global.get $COLS
    i32.mul
    local.get $x
    i32.add
    i32.const 4
    i32.mul
    i32.add
    i32.const 0
    i32.store offset=4
    end
    end
        )
    local.get $x
    i32.const 1
    i32.add
    local.set $x
        br $loop_for_4
      )
    )
        )
    local.get $y
    i32.const 1
    i32.add
    local.set $y
        br $loop_for_3
      )
    )
    i32.const 0
    local.set $i
    (block $break_for_5
      (loop $loop_for_5
    local.get $i
    global.get $COLS
    global.get $ROWS
    i32.mul
    i32.lt_s
        i32.eqz
        br_if $break_for_5
        (block $continue_for_5
    global.get $grid
    local.get $i
    i32.const 4
    i32.mul
    i32.add
    global.get $nextGrid
    local.get $i
    i32.const 4
    i32.mul
    i32.add
    i32.load offset=4
    i32.store offset=4
        )
    local.get $i
    i32.const 1
    i32.add
    local.set $i
        br $loop_for_5
      )
    )
    global.get $generation
    i32.const 1
    i32.add
    global.set $generation
    call $updateInfo
    call $render
  )
  (func $randomize (export "randomize")
    (local $i i32)
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
    (local $__caught_exc i32)
    i32.const 0
    local.set $i
    (block $break_for_6
      (loop $loop_for_6
    local.get $i
    global.get $COLS
    global.get $ROWS
    i32.mul
    i32.lt_s
        i32.eqz
        br_if $break_for_6
        (block $continue_for_6
    local.get $i
    i32.const 1103515245
    i32.mul
    i32.const 12345
    i32.add
    i32.const 0x7fffffff
    i32.and
    local.set $r
    global.get $grid
    local.get $i
    i32.const 4
    i32.mul
    i32.add
    local.get $r
    i32.const 5
    i32.rem_s
    i32.const 0
    i32.eq
    if (result i32)
    i32.const 1
    else
    i32.const 0
    end
    i32.store offset=4
        )
    local.get $i
    i32.const 1
    i32.add
    local.set $i
        br $loop_for_6
      )
    )
    i32.const 0
    global.set $generation
    call $updateInfo
    call $render
  )
  (func $clearGrid (export "clearGrid")
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
    local.set $i
    (block $break_for_7
      (loop $loop_for_7
    local.get $i
    global.get $COLS
    global.get $ROWS
    i32.mul
    i32.lt_s
        i32.eqz
        br_if $break_for_7
        (block $continue_for_7
    global.get $grid
    local.get $i
    i32.const 4
    i32.mul
    i32.add
    i32.const 0
    i32.store offset=4
        )
    local.get $i
    i32.const 1
    i32.add
    local.set $i
        br $loop_for_7
      )
    )
    i32.const 0
    global.set $generation
    call $updateInfo
    call $render
  )
  (func $updateInfo (export "updateInfo")
    (local $info i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
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
    i32.const 4483
    i32.const 8
    call $dom_getElementById
    local.set $info
    local.get $info
    i32.const 0
    i32.ne
    if
    local.get $info
    i32.const 4529
    global.get $generation
    call $__str_from_i32
    call $__str_concat
    local.set $__str_arg_ptr
    local.get $__str_arg_ptr
    local.get $__str_arg_ptr
    call $__str_length
    call $dom_setInnerText
    end
  )
  (func $render (export "render")
    (local $ctx i32)
    (local $y i32)
    (local $x i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
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
    i32.const 4178
    i32.const 11
    call $dom_getCanvasContext2D
    local.set $ctx
    local.get $ctx
    i32.const 0
    i32.eq
    if
    return
    end
    local.get $ctx
    i32.const 4222
    i32.const 7
    call $dom_setFillStyle
    local.get $ctx
    f64.const 0.0
    f64.const 0.0
    global.get $COLS
    global.get $CELL_SIZE
    i32.mul
    f64.convert_i32_s
    global.get $ROWS
    global.get $CELL_SIZE
    i32.mul
    f64.convert_i32_s
    call $dom_fillRect
    local.get $ctx
    i32.const 4377
    i32.const 7
    call $dom_setFillStyle
    i32.const 0
    local.set $y
    (block $break_for_8
      (loop $loop_for_8
    local.get $y
    global.get $ROWS
    i32.lt_s
        i32.eqz
        br_if $break_for_8
        (block $continue_for_8
    i32.const 0
    local.set $x
    (block $break_for_9
      (loop $loop_for_9
    local.get $x
    global.get $COLS
    i32.lt_s
        i32.eqz
        br_if $break_for_9
        (block $continue_for_9
    global.get $grid
    local.get $y
    global.get $COLS
    i32.mul
    local.get $x
    i32.add
    i32.const 4
    i32.mul
    i32.add
    i32.load offset=4
    i32.const 1
    i32.eq
    if
    local.get $ctx
    local.get $x
    global.get $CELL_SIZE
    i32.mul
    i32.const 1
    i32.add
    f64.convert_i32_s
    local.get $y
    global.get $CELL_SIZE
    i32.mul
    i32.const 1
    i32.add
    f64.convert_i32_s
    global.get $CELL_SIZE
    i32.const 2
    i32.sub
    f64.convert_i32_s
    global.get $CELL_SIZE
    i32.const 2
    i32.sub
    f64.convert_i32_s
    call $dom_fillRect
    end
        )
    local.get $x
    i32.const 1
    i32.add
    local.set $x
        br $loop_for_9
      )
    )
        )
    local.get $y
    i32.const 1
    i32.add
    local.set $y
        br $loop_for_8
      )
    )
  )
)
