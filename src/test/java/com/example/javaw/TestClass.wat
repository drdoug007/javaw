(module
  (memory (export "memory") 2)
  (data (i32.const 4096) "Hello\00")
  (data (i32.const 4102) "WebAssembly\00")
  (data (i32.const 4114) ", \00")
  (data (i32.const 4117) "!\00")
  (data (i32.const 4119) "User: \00")
  (data (i32.const 4126) " (age: \00")
  (data (i32.const 4134) ")\00")
  (data (i32.const 4136) "Hello World\00")
  (data (i32.const 4148) "World\00")
  (data (i32.const 4154) "Java\00")
  (data (i32.const 4159) "Count: \00")
  (global $__heap_ptr (mut i32) (i32.const 65536))
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
  ;; Class/Interface: TestClass
  (func $sayHello (export "sayHello")
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
  )
  (func $addNumbers (export "addNumbers") (param $a i32) (param $b i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
    (local $__str_tmp i32)
    (local $__str_sub_start i32)
    (local $__sb_tmp i32)
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
    (if
      (then
    br $continue_for_2
      )
    )
    local.get $i
    i32.const 8
    i32.gt_s
    (if
      (then
    br $break_for_2
      )
    )
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
    (if
      (then
    br $continue_foreach_4
      )
    )
    local.get $n
    i32.const 8
    i32.gt_s
    (if
      (then
    br $break_foreach_4
      )
    )
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
    i32.const 0
    local.set $result
    local.get $code
    local.set $__switch_val_1
    (block $break_switch_8
      (block $case_8_3
      (block $case_8_2
      (block $case_8_1
      (block $case_8_0
        ;; Dispatch logic for switch cases
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
      )
    i32.const 100
    local.set $result
    br $break_switch_8
      )
    i32.const 200
    local.set $result
    br $break_switch_8
      )
    i32.const 300
    local.set $result
    br $break_switch_8
      )
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
    i32.const 0
    local.set $result
    local.get $code
    local.set $__switch_val_1
    (block $break_switch_9
      (block $case_9_3
      (block $case_9_2
      (block $case_9_1
      (block $case_9_0
        ;; Dispatch logic for switch cases
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
      )
    local.get $result
    i32.const 10
    i32.add
    local.set $result
      )
    local.get $result
    i32.const 20
    i32.add
    local.set $result
    br $break_switch_9
      )
    local.get $result
    i32.const 30
    i32.add
    local.set $result
    br $break_switch_9
      )
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
        ;; Dispatch logic for switch cases
        local.get $__switch_val_1
    i32.const 2
        i32.eq
        br_if $case_11_0
        local.get $__switch_val_1
    i32.const 4
        i32.eq
        br_if $case_11_1
        br $case_11_2
      )
    local.get $total
    i32.const 20
    i32.add
    local.set $total
    br $break_switch_11
      )
    local.get $total
    i32.const 40
    i32.add
    local.set $total
    br $break_switch_11
      )
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
    (if
      (then
    br $break_for_12
      )
    )
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
    (if
      (then
    br $continue_for_14
      )
    )
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
    i32.const 10
    local.set $res
    (block $break_label_16
    local.get $flag
    i32.const 1
    i32.eq
    (if
      (then
    local.get $res
    i32.const 5
    i32.add
    local.set $res
    br $break_label_16
      )
    )
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
    (if
      (then
    br $break_while_17
      )
    )
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
    i32.const 4096
    local.set $greeting
    i32.const 4102
    local.set $target
    local.get $greeting
    i32.const 4114
    call $__str_concat
    local.get $target
    call $__str_concat
    i32.const 4117
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
    i32.const 4119
    local.get $name
    call $__str_concat
    i32.const 4126
    call $__str_concat
    local.get $age
    call $__str_from_i32
    call $__str_concat
    i32.const 4134
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
    i32.const 4102
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
    i32.const 4102
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
    i32.const 4136
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
    local.get $a
    local.get $b
    call $__str_equals
    (if
      (then
    i32.const 1
    return
      )
    )
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
    i32.const 4136
    local.set $s
    local.get $s
    i32.const 4148
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
    i32.const 4136
    local.set $s
    local.get $s
    i32.const 4154
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
    i32.const 16
    call $__sb_new
    local.set $__sb_tmp
    local.get $__sb_tmp
    i32.const 4159
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
    i32.const 16
    call $__sb_new
    local.set $__sb_tmp
    local.get $__sb_tmp
    i32.const 4154
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
)
