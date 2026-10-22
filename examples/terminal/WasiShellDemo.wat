(module
  (@custom "sourceMappingURL" "\16WasiShellDemo.wasm.map")
  (import "dom" "js_length" (func $dom_js_length (param i32) (result i32)))
  (import "dom" "terminal_clear" (func $dom_terminal_clear))
  (import "dom" "terminal_get_columns" (func $dom_terminal_get_columns (result i32)))
  (import "dom" "terminal_get_rows" (func $dom_terminal_get_rows (result i32)))
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
  (memory (export "memory") 2)
  (data (i32.const 4096) "=============================================\00")
  (data (i32.const 4142) "   Javaw Interactive WASI Browser Terminal   \00")
  (data (i32.const 4188) "Type \00")
  (data (i32.const 4194) "help\00")
  (data (i32.const 4199) " for available shell commands.\00")
  (data (i32.const 4230) "\00")
  (data (i32.const 4231) "welcome.txt\00")
  (data (i32.const 4243) "Welcome to the WebAssembly Virtual File System in Java!\00")
  (data (i32.const 4299) "wasi-sh> \00")
  (data (i32.const 4309) "exit\00")
  (data (i32.const 4314) "quit\00")
  (data (i32.const 4319) "Shell terminated. Goodbye!\00")
  (data (i32.const 4346) "clear\00")
  (data (i32.const 4352) "cls\00")
  (data (i32.const 4356) "info\00")
  (data (i32.const 4361) "Terminal Dimensions: \00")
  (data (i32.const 4383) " columns x \00")
  (data (i32.const 4395) " rows\00")
  (data (i32.const 4401) "cat\00")
  (data (i32.const 4405) "cat welcome.txt\00")
  (data (i32.const 4421) "Error: File not found.\00")
  (data (i32.const 4444) "echo \00")
  (data (i32.const 4450) "color\00")
  (data (i32.const 4456) "colors\00")
  (data (i32.const 4463) "Command not recognized: '\00")
  (data (i32.const 4489) "'. Type 'help' for command list.\00")
  (data (i32.const 4522) "Available Built-in Commands:\00")
  (data (i32.const 4551) "  \00")
  (data (i32.const 4554) "          - Display this help message\00")
  (data (i32.const 4592) "         - Clear terminal screen\00")
  (data (i32.const 4625) "          - Show terminal size and configuration\00")
  (data (i32.const 4674) "cat <file>\00")
  (data (i32.const 4685) "    - Display contents of virtual file\00")
  (data (i32.const 4724) "echo <msg>\00")
  (data (i32.const 4735) "    - Print text to standard output\00")
  (data (i32.const 4771) "        - Display ANSI 16-color test palette\00")
  (data (i32.const 4816) "          - Terminate the shell session\00")
  (data (i32.const 4856) "ANSI Color Test Palette:\00")
  (data (i32.const 4881) "\e2\96\a0 Red   \00")
  (data (i32.const 4892) "\e2\96\a0 Green   \00")
  (data (i32.const 4905) "\e2\96\a0 Yellow   \00")
  (data (i32.const 4919) "\e2\96\a0 Blue   \00")
  (data (i32.const 4931) "\e2\96\a0 Magenta   \00")
  (data (i32.const 4946) "\e2\96\a0 Cyan\00")
  (data (i32.const 4955) "\e2\96\a0 B.Red \00")
  (data (i32.const 4966) "\e2\96\a0 B.Green \00")
  (data (i32.const 4979) "\e2\96\a0 B.Yellow \00")
  (data (i32.const 4993) "\e2\96\a0 B.Blue \00")
  (data (i32.const 5005) "\e2\96\a0 B.Mag   \00")
  (data (i32.const 5018) "\e2\96\a0 B.Cyan\00")
  (data (i32.const 5029) "\1b[96m\00")
  (data (i32.const 5035) "\1b[1m\00")
  (data (i32.const 5040) "\1b[0m\00")
  (data (i32.const 5045) "\1b[2m\00")
  (data (i32.const 5050) "\1b[93m\00")
  (data (i32.const 5056) "\1b[92m\00")
  (data (i32.const 5062) "\1b[33m\00")
  (data (i32.const 5068) "\1b[94m\00")
  (data (i32.const 5074) "\1b[97m\00")
  (data (i32.const 5080) "\1b[31m\00")
  (data (i32.const 5086) "\1b[36m\00")
  (data (i32.const 5092) "\1b[32m\00")
  (data (i32.const 5098) "\1b[34m\00")
  (data (i32.const 5104) "\1b[35m\00")
  (data (i32.const 5110) "\1b[91m\00")
  (data (i32.const 5116) "\1b[95m\00")
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
  ;; Class/Interface: WasiShellDemo
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
    call $runInteractiveShell
  )
  (func $runInteractiveShell (export "runInteractiveShell")
    (local $welcome i32)
    (local $active i32)
    (local $input i32)
    (local $f i32)
    (local $msg i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
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
    i32.const 1
    i32.const 5029
    i32.const 5035
    call $__str_concat
    i32.const 4096
    call $__str_concat
    i32.const 5040
    call $__str_concat
    local.set $__str_arg_ptr
    local.get $__str_arg_ptr
    local.get $__str_arg_ptr
    call $__str_length
    i32.const 1
    call $__wasi_print_str
    i32.const 1
    i32.const 5029
    i32.const 5035
    call $__str_concat
    i32.const 4142
    call $__str_concat
    i32.const 5040
    call $__str_concat
    local.set $__str_arg_ptr
    local.get $__str_arg_ptr
    local.get $__str_arg_ptr
    call $__str_length
    i32.const 1
    call $__wasi_print_str
    i32.const 1
    i32.const 5029
    i32.const 5035
    call $__str_concat
    i32.const 4096
    call $__str_concat
    i32.const 5040
    call $__str_concat
    local.set $__str_arg_ptr
    local.get $__str_arg_ptr
    local.get $__str_arg_ptr
    call $__str_length
    i32.const 1
    call $__wasi_print_str
    i32.const 1
    i32.const 5045
    i32.const 4188
    call $__str_concat
    i32.const 5050
    call $__str_concat
    i32.const 4194
    call $__str_concat
    i32.const 5045
    call $__str_concat
    i32.const 4199
    call $__str_concat
    i32.const 5040
    call $__str_concat
    local.set $__str_arg_ptr
    local.get $__str_arg_ptr
    local.get $__str_arg_ptr
    call $__str_length
    i32.const 1
    call $__wasi_print_str
    i32.const 1
    i32.const 4230
    i32.const 0
    i32.const 1
    call $__wasi_print_str
    i32.const 8
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 4231
    call $WASIFile__init
    local.get $__obj_alloc_ptr
    local.set $welcome
    local.get $welcome
    i32.load offset=4
    call $__wasi_file_exists
    i32.eqz
    if
    local.get $welcome
    i32.load offset=4
    i32.const 4243
    i32.const 0
    call $__wasi_write_file_str
    drop
    end
    i32.const 1
    local.set $active
    (block $break_while_1
      (loop $loop_while_1
    local.get $active
        i32.eqz
        br_if $break_while_1
    i32.const 5056
    i32.const 4299
    call $__str_concat
    i32.const 5040
    call $__str_concat
    local.set $__str_arg_ptr
    local.get $__str_arg_ptr
    local.get $__str_arg_ptr
    call $__str_length
    call $__wasi_read_line
    local.set $input
    local.get $input
    i32.const 0
    i32.eq
    if
    br $break_while_1
    end
    local.get $input
    i32.const 4309
    call $__str_equals
    local.get $input
    i32.const 4314
    call $__str_equals
    i32.or
    if
    i32.const 0
    local.set $active
    i32.const 1
    i32.const 5062
    i32.const 4319
    call $__str_concat
    i32.const 5040
    call $__str_concat
    local.set $__str_arg_ptr
    local.get $__str_arg_ptr
    local.get $__str_arg_ptr
    call $__str_length
    i32.const 1
    call $__wasi_print_str
    else
    local.get $input
    i32.const 4194
    call $__str_equals
    if
    call $printHelp
    else
    local.get $input
    i32.const 4346
    call $__str_equals
    local.get $input
    i32.const 4352
    call $__str_equals
    i32.or
    if
    call $dom_terminal_clear
    else
    local.get $input
    i32.const 4356
    call $__str_equals
    if
    i32.const 1
    i32.const 5068
    i32.const 4361
    call $__str_concat
    i32.const 5040
    call $__str_concat
    call $dom_terminal_get_columns
    call $__str_from_i32
    call $__str_concat
    i32.const 4383
    call $__str_concat
    call $dom_terminal_get_rows
    call $__str_from_i32
    call $__str_concat
    i32.const 4395
    call $__str_concat
    local.set $__str_arg_ptr
    local.get $__str_arg_ptr
    local.get $__str_arg_ptr
    call $__str_length
    i32.const 1
    call $__wasi_print_str
    else
    local.get $input
    i32.const 4401
    call $__str_equals
    local.get $input
    i32.const 4405
    call $__str_equals
    i32.or
    if
    i32.const 8
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 4231
    call $WASIFile__init
    local.get $__obj_alloc_ptr
    local.set $f
    local.get $f
    i32.load offset=4
    call $__wasi_file_exists
    if
    i32.const 1
    i32.const 5074
    local.get $f
    i32.load offset=4
    call $__wasi_read_file_str
    call $__str_concat
    i32.const 5040
    call $__str_concat
    local.set $__str_arg_ptr
    local.get $__str_arg_ptr
    local.get $__str_arg_ptr
    call $__str_length
    i32.const 1
    call $__wasi_print_str
    else
    i32.const 1
    i32.const 5080
    i32.const 4421
    call $__str_concat
    i32.const 5040
    call $__str_concat
    local.set $__str_arg_ptr
    local.get $__str_arg_ptr
    local.get $__str_arg_ptr
    call $__str_length
    i32.const 1
    call $__wasi_print_str
    end
    else
    local.get $input
    i32.const 4444
    call $__str_startsWith
    if
    local.get $input
    i32.const 5
    local.get $input
    call $__str_length
    call $__str_substring
    local.set $msg
    i32.const 1
    local.get $msg
    local.set $__str_arg_ptr
    local.get $__str_arg_ptr
    local.get $__str_arg_ptr
    call $__str_length
    i32.const 1
    call $__wasi_print_str
    else
    local.get $input
    i32.const 4450
    call $__str_equals
    local.get $input
    i32.const 4456
    call $__str_equals
    i32.or
    if
    call $printColorPalette
    else
    local.get $input
    call $__str_length
    i32.eqz
    i32.eqz
    if
    i32.const 1
    i32.const 5080
    i32.const 4463
    call $__str_concat
    local.get $input
    call $__str_concat
    i32.const 4489
    call $__str_concat
    i32.const 5040
    call $__str_concat
    local.set $__str_arg_ptr
    local.get $__str_arg_ptr
    local.get $__str_arg_ptr
    call $__str_length
    i32.const 1
    call $__wasi_print_str
    end
    end
    end
    end
    end
    end
    end
    end
        br $loop_while_1
      )
    )
  )
  (func $printHelp (export "printHelp")
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
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
    i32.const 5035
    i32.const 4522
    call $__str_concat
    i32.const 5040
    call $__str_concat
    local.set $__str_arg_ptr
    local.get $__str_arg_ptr
    local.get $__str_arg_ptr
    call $__str_length
    i32.const 1
    call $__wasi_print_str
    i32.const 1
    i32.const 4551
    i32.const 5086
    call $__str_concat
    i32.const 4194
    call $__str_concat
    i32.const 5040
    call $__str_concat
    i32.const 4554
    call $__str_concat
    local.set $__str_arg_ptr
    local.get $__str_arg_ptr
    local.get $__str_arg_ptr
    call $__str_length
    i32.const 1
    call $__wasi_print_str
    i32.const 1
    i32.const 4551
    i32.const 5086
    call $__str_concat
    i32.const 4346
    call $__str_concat
    i32.const 5040
    call $__str_concat
    i32.const 4592
    call $__str_concat
    local.set $__str_arg_ptr
    local.get $__str_arg_ptr
    local.get $__str_arg_ptr
    call $__str_length
    i32.const 1
    call $__wasi_print_str
    i32.const 1
    i32.const 4551
    i32.const 5086
    call $__str_concat
    i32.const 4356
    call $__str_concat
    i32.const 5040
    call $__str_concat
    i32.const 4625
    call $__str_concat
    local.set $__str_arg_ptr
    local.get $__str_arg_ptr
    local.get $__str_arg_ptr
    call $__str_length
    i32.const 1
    call $__wasi_print_str
    i32.const 1
    i32.const 4551
    i32.const 5086
    call $__str_concat
    i32.const 4674
    call $__str_concat
    i32.const 5040
    call $__str_concat
    i32.const 4685
    call $__str_concat
    local.set $__str_arg_ptr
    local.get $__str_arg_ptr
    local.get $__str_arg_ptr
    call $__str_length
    i32.const 1
    call $__wasi_print_str
    i32.const 1
    i32.const 4551
    i32.const 5086
    call $__str_concat
    i32.const 4724
    call $__str_concat
    i32.const 5040
    call $__str_concat
    i32.const 4735
    call $__str_concat
    local.set $__str_arg_ptr
    local.get $__str_arg_ptr
    local.get $__str_arg_ptr
    call $__str_length
    i32.const 1
    call $__wasi_print_str
    i32.const 1
    i32.const 4551
    i32.const 5086
    call $__str_concat
    i32.const 4456
    call $__str_concat
    i32.const 5040
    call $__str_concat
    i32.const 4771
    call $__str_concat
    local.set $__str_arg_ptr
    local.get $__str_arg_ptr
    local.get $__str_arg_ptr
    call $__str_length
    i32.const 1
    call $__wasi_print_str
    i32.const 1
    i32.const 4551
    i32.const 5086
    call $__str_concat
    i32.const 4309
    call $__str_concat
    i32.const 5040
    call $__str_concat
    i32.const 4816
    call $__str_concat
    local.set $__str_arg_ptr
    local.get $__str_arg_ptr
    local.get $__str_arg_ptr
    call $__str_length
    i32.const 1
    call $__wasi_print_str
  )
  (func $printColorPalette (export "printColorPalette")
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
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
    i32.const 5035
    i32.const 4856
    call $__str_concat
    i32.const 5040
    call $__str_concat
    local.set $__str_arg_ptr
    local.get $__str_arg_ptr
    local.get $__str_arg_ptr
    call $__str_length
    i32.const 1
    call $__wasi_print_str
    i32.const 1
    i32.const 5080
    i32.const 4881
    call $__str_concat
    i32.const 5092
    call $__str_concat
    i32.const 4892
    call $__str_concat
    i32.const 5062
    call $__str_concat
    i32.const 4905
    call $__str_concat
    i32.const 5098
    call $__str_concat
    i32.const 4919
    call $__str_concat
    i32.const 5104
    call $__str_concat
    i32.const 4931
    call $__str_concat
    i32.const 5086
    call $__str_concat
    i32.const 4946
    call $__str_concat
    i32.const 5040
    call $__str_concat
    local.set $__str_arg_ptr
    local.get $__str_arg_ptr
    local.get $__str_arg_ptr
    call $__str_length
    i32.const 1
    call $__wasi_print_str
    i32.const 1
    i32.const 5110
    i32.const 4955
    call $__str_concat
    i32.const 5056
    call $__str_concat
    i32.const 4966
    call $__str_concat
    i32.const 5050
    call $__str_concat
    i32.const 4979
    call $__str_concat
    i32.const 5068
    call $__str_concat
    i32.const 4993
    call $__str_concat
    i32.const 5116
    call $__str_concat
    i32.const 5005
    call $__str_concat
    i32.const 5029
    call $__str_concat
    i32.const 5018
    call $__str_concat
    i32.const 5040
    call $__str_concat
    local.set $__str_arg_ptr
    local.get $__str_arg_ptr
    local.get $__str_arg_ptr
    call $__str_length
    i32.const 1
    call $__wasi_print_str
  )
)
