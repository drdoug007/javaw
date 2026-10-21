(module
  (import "dom" "getElementById" (func $dom_getElementById (param i32 i32) (result i32)))
  (import "dom" "createElement" (func $dom_createElement (param i32 i32) (result i32)))
  (import "dom" "querySelector" (func $dom_querySelector (param i32 i32) (result i32)))
  (import "dom" "querySelectorAll" (func $dom_querySelectorAll (param i32 i32) (result i32)))
  (import "dom" "setInnerText" (func $dom_setInnerText (param i32 i32 i32)))
  (import "dom" "getInnerText" (func $dom_getInnerText (param i32) (result i32)))
  (import "dom" "setAttribute" (func $dom_setAttribute (param i32 i32 i32 i32 i32)))
  (import "dom" "appendChild" (func $dom_appendChild (param i32 i32)))
  (import "dom" "removeChild" (func $dom_removeChild (param i32 i32)))
  (import "dom" "getValue" (func $dom_getValue (param i32) (result i32)))
  (import "dom" "setValue" (func $dom_setValue (param i32 i32 i32)))
  (import "dom" "isChecked" (func $dom_isChecked (param i32) (result i32)))
  (import "dom" "setChecked" (func $dom_setChecked (param i32 i32)))
  (import "dom" "setStyle" (func $dom_setStyle (param i32 i32 i32 i32 i32)))
  (import "dom" "addClass" (func $dom_addClass (param i32 i32 i32)))
  (import "dom" "toggleClass" (func $dom_toggleClass (param i32 i32 i32)))
  (import "dom" "addEventListener" (func $dom_addEventListener (param i32 i32 i32 i32 i32)))
  (memory (export "memory") 2)
  (global $counter (mut i32) (i32.const 0))
  (data (i32.const 4096) "app\00")
  (data (i32.const 4100) "h1\00")
  (data (i32.const 4103) "class\00")
  (data (i32.const 4109) "app-title\00")
  (data (i32.const 4119) "Java to WebAssembly DOM App\00")
  (data (i32.const 4147) "color\00")
  (data (i32.const 4153) "blue\00")
  (data (i32.const 4158) "p\00")
  (data (i32.const 4160) "id\00")
  (data (i32.const 4163) "status-text\00")
  (data (i32.const 4175) "Count: 0\00")
  (data (i32.const 4184) "status-badge\00")
  (data (i32.const 4197) "button\00")
  (data (i32.const 4204) "btn-increment\00")
  (data (i32.const 4218) "Increment Counter\00")
  (data (i32.const 4236) "click\00")
  (data (i32.const 4242) "increment\00")
  (data (i32.const 4252) "input\00")
  (data (i32.const 4258) "user-input\00")
  (data (i32.const 4269) "type\00")
  (data (i32.const 4274) "text\00")
  (data (i32.const 4279) "Initial text\00")
  (data (i32.const 4292) "user-check\00")
  (data (i32.const 4303) "checkbox\00")
  (data (i32.const 4312) "div\00")
  (data (i32.const 4316) "item-container\00")
  (data (i32.const 4331) "span\00")
  (data (i32.const 4336) "item-tag\00")
  (data (i32.const 4345) "Item \00")
  (data (i32.const 4351) "Count: 1 click\00")
  (data (i32.const 4366) "Count: 2 clicks\00")
  (data (i32.const 4382) "Count: multiple clicks\00")
  (data (i32.const 4405) "active\00")
  (data (i32.const 4412) "#status-text\00")
  (data (i32.const 4425) ".item-tag\00")
  (global $__heap_ptr (mut i32) (i32.const 65536))
  ;; --- WebAssembly Function Type Signatures ---
  (type $t_sig_1 (func (param i32)))
  (type $t_sig_2 (func (param i32) (result i32)))
  ;; --- WebAssembly Function Table (VTable) ---
  (table (export "__vtable") 7 funcref)
  (elem (i32.const 0) $start $increment $readStatusText $readInputValue $readCheckboxState $countItemTags $removeFirstItem)
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
  ;; Class/Interface: App
  (func $start (export "start")
    (local $app i32)
    (local $heading i32)
    (local $status i32)
    (local $btn i32)
    (local $input i32)
    (local $checkbox i32)
    (local $container i32)
    (local $i i32)
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
    i32.const 4096
    i32.const 3
    call $dom_getElementById
    local.set $app
    i32.const 4100
    i32.const 2
    call $dom_createElement
    local.set $heading
    local.get $heading
    i32.const 4103
    i32.const 5
    i32.const 4109
    i32.const 9
    call $dom_setAttribute
    local.get $heading
    i32.const 4119
    i32.const 27
    call $dom_setInnerText
    local.get $heading
    i32.const 4147
    i32.const 5
    i32.const 4153
    i32.const 4
    call $dom_setStyle
    local.get $app
    local.get $heading
    call $dom_appendChild
    i32.const 4158
    i32.const 1
    call $dom_createElement
    local.set $status
    local.get $status
    i32.const 4160
    i32.const 2
    i32.const 4163
    i32.const 11
    call $dom_setAttribute
    local.get $status
    i32.const 4175
    i32.const 8
    call $dom_setInnerText
    local.get $status
    i32.const 4184
    i32.const 12
    call $dom_addClass
    local.get $app
    local.get $status
    call $dom_appendChild
    i32.const 4197
    i32.const 6
    call $dom_createElement
    local.set $btn
    local.get $btn
    i32.const 4160
    i32.const 2
    i32.const 4204
    i32.const 13
    call $dom_setAttribute
    local.get $btn
    i32.const 4218
    i32.const 17
    call $dom_setInnerText
    local.get $btn
    i32.const 4236
    i32.const 5
    i32.const 4242
    i32.const 9
    call $dom_addEventListener
    local.get $app
    local.get $btn
    call $dom_appendChild
    i32.const 4252
    i32.const 5
    call $dom_createElement
    local.set $input
    local.get $input
    i32.const 4160
    i32.const 2
    i32.const 4258
    i32.const 10
    call $dom_setAttribute
    local.get $input
    i32.const 4269
    i32.const 4
    i32.const 4274
    i32.const 4
    call $dom_setAttribute
    local.get $input
    i32.const 4279
    i32.const 12
    call $dom_setValue
    local.get $app
    local.get $input
    call $dom_appendChild
    i32.const 4252
    i32.const 5
    call $dom_createElement
    local.set $checkbox
    local.get $checkbox
    i32.const 4160
    i32.const 2
    i32.const 4292
    i32.const 10
    call $dom_setAttribute
    local.get $checkbox
    i32.const 4269
    i32.const 4
    i32.const 4303
    i32.const 8
    call $dom_setAttribute
    local.get $checkbox
    i32.const 1
    call $dom_setChecked
    local.get $app
    local.get $checkbox
    call $dom_appendChild
    i32.const 4312
    i32.const 3
    call $dom_createElement
    local.set $container
    local.get $container
    i32.const 4160
    i32.const 2
    i32.const 4316
    i32.const 14
    call $dom_setAttribute
    i32.const 0
    local.set $i
    (block $break_for_1
      (loop $loop_for_1
    local.get $i
    i32.const 3
    i32.lt_s
        i32.eqz
        br_if $break_for_1
        (block $continue_for_1
    i32.const 4331
    i32.const 4
    call $dom_createElement
    local.set $item
    local.get $item
    i32.const 4336
    i32.const 8
    call $dom_addClass
    local.get $item
    i32.const 4345
    local.get $i
    call $__str_from_i32
    call $__str_concat
    local.set $__str_arg_ptr
    local.get $__str_arg_ptr
    local.get $__str_arg_ptr
    call $__str_length
    call $dom_setInnerText
    local.get $container
    local.get $item
    call $dom_appendChild
        )
    local.get $i
    i32.const 1
    i32.add
    local.set $i
        br $loop_for_1
      )
    )
    local.get $app
    local.get $container
    call $dom_appendChild
  )
  (func $increment (export "increment")
    (local $status i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    global.get $counter
    i32.const 1
    i32.add
    global.set $counter
    i32.const 4163
    i32.const 11
    call $dom_getElementById
    local.set $status
    global.get $counter
    i32.const 1
    i32.eq
    if
    local.get $status
    i32.const 4351
    i32.const 14
    call $dom_setInnerText
    else
    global.get $counter
    i32.const 2
    i32.eq
    if
    local.get $status
    i32.const 4366
    i32.const 15
    call $dom_setInnerText
    else
    local.get $status
    i32.const 4382
    i32.const 22
    call $dom_setInnerText
    end
    end
    local.get $status
    i32.const 4405
    i32.const 6
    call $dom_toggleClass
  )
  (func $readStatusText (export "readStatusText") (result i32)
    (local $status i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    i32.const 4412
    i32.const 12
    call $dom_querySelector
    local.set $status
    local.get $status
    call $dom_getInnerText
    return
  )
  (func $readInputValue (export "readInputValue") (result i32)
    (local $input i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    i32.const 4258
    i32.const 10
    call $dom_getElementById
    local.set $input
    local.get $input
    call $dom_getValue
    return
  )
  (func $readCheckboxState (export "readCheckboxState") (result i32)
    (local $check i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    i32.const 4292
    i32.const 10
    call $dom_getElementById
    local.set $check
    local.get $check
    call $dom_isChecked
    return
  )
  (func $countItemTags (export "countItemTags") (result i32)
    (local $items i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    i32.const 4425
    i32.const 9
    call $dom_querySelectorAll
    local.set $items
    local.get $items
    i32.load
    return
  )
  (func $removeFirstItem (export "removeFirstItem")
    (local $container i32)
    (local $firstItem i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
    (local $__obj_alloc_ptr i32)
    (local $__obj_tmp i32)
    (local $__vtable_tmp i32)
    i32.const 4316
    i32.const 14
    call $dom_getElementById
    local.set $container
    i32.const 4425
    i32.const 9
    call $dom_querySelector
    local.set $firstItem
    local.get $container
    local.get $firstItem
    call $dom_removeChild
  )
)
