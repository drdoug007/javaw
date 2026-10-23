(module
  (import "dom" "getElementById" (func $dom_getElementById (param i32 i32) (result i32)))
  (import "dom" "createElement" (func $dom_createElement (param i32 i32) (result i32)))
  (import "dom" "setInnerText" (func $dom_setInnerText (param i32 i32 i32)))
  (import "dom" "setAttribute" (func $dom_setAttribute (param i32 i32 i32 i32 i32)))
  (import "dom" "appendChild" (func $dom_appendChild (param i32 i32)))
  (import "dom" "setStyle" (func $dom_setStyle (param i32 i32 i32 i32 i32)))
  (import "dom" "addEventListener" (func $dom_addEventListener (param i32 i32 i32 i32 i32)))
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
  (memory (export "memory") 2 20 shared)
  (global $appLogger (mut i32) (i32.const 0))
  (global $authLogger (mut i32) (i32.const 0))
  (global $dbLogger (mut i32) (i32.const 0))
  (global $paymentLogger (mut i32) (i32.const 0))
  (global $requestSequence (mut i32) (i32.const 100))
  (data (i32.const 4096) "AuthService\00")
  (data (i32.const 4108) "DatabasePool\00")
  (data (i32.const 4121) "PaymentGateway\00")
  (data (i32.const 4136) "env\00")
  (data (i32.const 4140) "wasm-edge\00")
  (data (i32.const 4150) "region\00")
  (data (i32.const 4157) "us-east-1\00")
  (data (i32.const 4167) "traceId\00")
  (data (i32.const 4175) "req-8f4b-\00")
  (data (i32.const 4185) "Log4j2 logging engine initialized in WebAssembly runtime\00")
  (data (i32.const 4242) "Validating bearer authentication token for user: {}\00")
  (data (i32.const 4294) "doug_admin\00")
  (data (i32.const 4305) "User session established for userId: {}\00")
  (data (i32.const 4345) "usr_49201\00")
  (data (i32.const 4355) "Connection leased from pool [active={}, idle={}]\00")
  (data (i32.const 4404) "Payment provider response delayed (latency: {} ms)\00")
  (data (i32.const 4455) "Query timeout while scanning index on partition: {}\00")
  (data (i32.const 4507) "sh_europe_04\00")
  (data (i32.const 4520) "Payment gateway connection reset by peer! Triggering circuit breaker\00")
  (data (i32.const 4589) "auth-\00")
  (data (i32.const 4595) "Authentication request received from IP {}\00")
  (data (i32.const 4638) "192.168.1.105\00")
  (data (i32.const 4652) "Checking credentials against identity store...\00")
  (data (i32.const 4699) "User {} successfully authenticated with roles: [ADMIN, OPERATOR]\00")
  (data (i32.const 4764) "admin_user\00")
  (data (i32.const 4775) "db-\00")
  (data (i32.const 4779) "Acquiring lock on table '{}' for batch update\00")
  (data (i32.const 4825) "orders\00")
  (data (i32.const 4832) "Executed batch update across {} records in {} ms\00")
  (data (i32.const 4881) "Connection returned to pool. Health check: OK\00")
  (data (i32.const 4927) "warn-\00")
  (data (i32.const 4933) "High memory pressure detected: {} MB heap allocated\00")
  (data (i32.const 4985) "Retrying transaction tx_{} (attempt {} of {})\00")
  (data (i32.const 5031) "9938a\00")
  (data (i32.const 5037) "err-\00")
  (data (i32.const 5042) "Transaction rollback triggered due to optimistic lock conflict on order_{}\00")
  (data (i32.const 5117) "Unhandled exception downstream: HTTP 503 Service Unavailable\00")
  (data (i32.const 5178) "log4j-app\00")
  (data (i32.const 5188) "div\00")
  (data (i32.const 5192) "background\00")
  (data (i32.const 5203) "#1e293b\00")
  (data (i32.const 5211) "padding\00")
  (data (i32.const 5219) "24px\00")
  (data (i32.const 5224) "borderRadius\00")
  (data (i32.const 5237) "10px\00")
  (data (i32.const 5242) "color\00")
  (data (i32.const 5248) "#f8fafc\00")
  (data (i32.const 5256) "boxShadow\00")
  (data (i32.const 5266) "0 4px 12px rgba(0,0,0,0.3)\00")
  (data (i32.const 5293) "h3\00")
  (data (i32.const 5296) "Log4j2 Enterprise Logging Engine\00")
  (data (i32.const 5329) "#38bdf8\00")
  (data (i32.const 5337) "marginBottom\00")
  (data (i32.const 5350) "8px\00")
  (data (i32.const 5354) "p\00")
  (data (i32.const 5356) "Structured Log4j2 logging in WebAssembly with Mapped Diagnostic Context (MDC), parameter substitution, and severity filtering.\00")
  (data (i32.const 5483) "#94a3b8\00")
  (data (i32.const 5491) "fontSize\00")
  (data (i32.const 5500) "13px\00")
  (data (i32.const 5505) "16px\00")
  (data (i32.const 5510) "display\00")
  (data (i32.const 5518) "flex\00")
  (data (i32.const 5523) "flexWrap\00")
  (data (i32.const 5532) "wrap\00")
  (data (i32.const 5537) "gap\00")
  (data (i32.const 5541) "Run Full Lifecycle\00")
  (data (i32.const 5560) "#2563eb\00")
  (data (i32.const 5568) "runLog4jSimulation\00")
  (data (i32.const 5587) "Auth Event\00")
  (data (i32.const 5598) "#059669\00")
  (data (i32.const 5606) "simulateAuthEvent\00")
  (data (i32.const 5624) "DB Metrics\00")
  (data (i32.const 5635) "#0284c7\00")
  (data (i32.const 5643) "simulateDbEvent\00")
  (data (i32.const 5659) "Simulate Warning\00")
  (data (i32.const 5676) "#d97706\00")
  (data (i32.const 5684) "simulateWarnEvent\00")
  (data (i32.const 5702) "Simulate Error\00")
  (data (i32.const 5717) "#dc2626\00")
  (data (i32.const 5725) "simulateErrEvent\00")
  (data (i32.const 5742) "Clear Console\00")
  (data (i32.const 5756) "#475569\00")
  (data (i32.const 5764) "clearLogConsole\00")
  (data (i32.const 5780) "#020617\00")
  (data (i32.const 5788) "border\00")
  (data (i32.const 5795) "1px solid #334155\00")
  (data (i32.const 5813) "12px\00")
  (data (i32.const 5818) "height\00")
  (data (i32.const 5825) "260px\00")
  (data (i32.const 5831) "overflowY\00")
  (data (i32.const 5841) "auto\00")
  (data (i32.const 5846) "fontFamily\00")
  (data (i32.const 5857) "monospace\00")
  (data (i32.const 5867) "id\00")
  (data (i32.const 5870) "log4j-console-stream\00")
  (data (i32.const 5891) "3px 0\00")
  (data (i32.const 5897) "borderBottom\00")
  (data (i32.const 5910) "1px solid #0f172a\00")
  (data (i32.const 5928) "alignItems\00")
  (data (i32.const 5939) "center\00")
  (data (i32.const 5946) "span\00")
  (data (i32.const 5951) "[\00")
  (data (i32.const 5953) "]\00")
  (data (i32.const 5955) "fontWeight\00")
  (data (i32.const 5966) "bold\00")
  (data (i32.const 5971) "11px\00")
  (data (i32.const 5976) "2px 6px\00")
  (data (i32.const 5984) "4px\00")
  (data (i32.const 5988) "#7f1d1d\00")
  (data (i32.const 5996) "#fecaca\00")
  (data (i32.const 6004) "#991b1b\00")
  (data (i32.const 6012) "#fee2e2\00")
  (data (i32.const 6020) "#78350f\00")
  (data (i32.const 6028) "#fef3c7\00")
  (data (i32.const 6036) "#064e3b\00")
  (data (i32.const 6044) "#a7f3d0\00")
  (data (i32.const 6052) "#1e1b4b\00")
  (data (i32.const 6060) "#c7d2fe\00")
  (data (i32.const 6068) " - \00")
  (data (i32.const 6072) "#e2e8f0\00")
  (data (i32.const 6080) "button\00")
  (data (i32.const 6087) "#ffffff\00")
  (data (i32.const 6095) "none\00")
  (data (i32.const 6100) "6px 12px\00")
  (data (i32.const 6109) "6px\00")
  (data (i32.const 6113) "cursor\00")
  (data (i32.const 6120) "pointer\00")
  (data (i32.const 6128) "500\00")
  (data (i32.const 6132) "click\00")
  (data (i32.const 6138) "\00")
  (data (i32.const 6139) "Log4j2Demo\00")
  (data (i32.const 6150) "@\00")
  (data (i32.const 6152) "true\00")
  (data (i32.const 6157) "false\00")
  (data (i32.const 6163) "class \00")
  (data (i32.const 6170) "interface \00")
  (data (i32.const 6181) "Object\00")
  (data (i32.const 6188) "String\00")
  (data (i32.const 6195) "Class\00")
  (data (i32.const 6201) "Thread\00")
  (data (i32.const 6208) "Runnable\00")
  (data (i32.const 6217) "Enum\00")
  (data (i32.const 6222) "int\00")
  (data (i32.const 6226) "long\00")
  (data (i32.const 6231) "double\00")
  (data (i32.const 6238) "float\00")
  (data (i32.const 6244) "boolean\00")
  (data (i32.const 6252) "char\00")
  (data (i32.const 6257) "byte\00")
  (data (i32.const 6262) "short\00")
  (data (i32.const 6268) "void\00")
  (global $__heap_ptr (mut i32) (i32.const 65536))
  (global $__class_Log4j2Demo (mut i32) (i32.const 0))
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
  ;; --- WebAssembly Function Table (VTable) ---
  (table (export "__vtable") 96 funcref)
  (elem (i32.const 0) $Throwable_toString $Throwable_equals $Throwable_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Throwable_run $Throwable_getClass $Throwable_getMessage $Throwable_toString $Throwable_equals $Throwable_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Throwable_run $Throwable_getClass $Exception_getMessage $Throwable_toString $Throwable_equals $Throwable_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Throwable_run $Throwable_getClass $RuntimeException_getMessage $Throwable_toString $Throwable_equals $Throwable_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Throwable_run $Throwable_getClass $IllegalArgumentException_getMessage $Throwable_toString $Throwable_equals $Throwable_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Throwable_run $Throwable_getClass $IllegalStateException_getMessage $Throwable_toString $Throwable_equals $Throwable_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Throwable_run $Throwable_getClass $NullPointerException_getMessage $WASIFile_toString $WASIFile_equals $WASIFile_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $WASIFile_run $WASIFile_getClass $Thread_toString $Thread_equals $Thread_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Thread_run $Thread_getClass $Class_toString $Class_equals $Class_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Class_run $Class_getClass)
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
      i32.const 6152
    else
      i32.const 6157
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
  ;; Class/Interface: Log4j2Demo
  (func $runSimulation (export "runSimulation")
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
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
    i32.const 0
    i32.const 0
    i32.const 0
    call $__wasi_print_str
    i32.const 1
    i32.const 4185
    i32.const 56
    i32.const 1
    call $__wasi_print_str
    i32.const 1
    i32.const 0
    i32.const 0
    i32.const 0
    call $__wasi_print_str
    i32.const 1
    i32.const 0
    i32.const 0
    i32.const 0
    call $__wasi_print_str
    i32.const 1
    i32.const 4294
    i32.const 10
    i32.const 1
    call $__wasi_print_str
    i32.const 1
    i32.const 0
    i32.const 0
    i32.const 0
    call $__wasi_print_str
    i32.const 1
    i32.const 0
    i32.const 0
    i32.const 0
    call $__wasi_print_str
    i32.const 1
    i32.const 4345
    i32.const 9
    i32.const 1
    call $__wasi_print_str
    i32.const 1
    i32.const 0
    i32.const 0
    i32.const 0
    call $__wasi_print_str
    i32.const 1
    i32.const 0
    i32.const 0
    i32.const 0
    call $__wasi_print_str
    i32.const 1
    i32.const 12
    i32.const 0
    call $__wasi_print_int
    i32.const 1
    i32.const 0
    i32.const 0
    i32.const 0
    call $__wasi_print_str
    i32.const 1
    i32.const 8
    i32.const 0
    call $__wasi_print_int
    i32.const 1
    i32.const 5953
    i32.const 1
    i32.const 1
    call $__wasi_print_str
    i32.const 1
    i32.const 0
    i32.const 0
    i32.const 0
    call $__wasi_print_str
    i32.const 1
    i32.const 0
    i32.const 0
    i32.const 0
    call $__wasi_print_str
    i32.const 1
    i32.const 480
    i32.const 0
    call $__wasi_print_int
    i32.const 1
    i32.const 0
    i32.const 0
    i32.const 1
    call $__wasi_print_str
    i32.const 2
    i32.const 0
    i32.const 0
    i32.const 0
    call $__wasi_print_str
    i32.const 2
    i32.const 0
    i32.const 0
    i32.const 0
    call $__wasi_print_str
    i32.const 2
    i32.const 4507
    i32.const 12
    i32.const 1
    call $__wasi_print_str
    i32.const 2
    i32.const 0
    i32.const 0
    i32.const 0
    call $__wasi_print_str
    i32.const 2
    i32.const 4520
    i32.const 68
    i32.const 0
    call $__wasi_print_str
    i32.const 2
    i32.const 0
    i32.const 0
    i32.const 0
    call $__wasi_print_str
    i32.const 2
    i32.const 0
    i32.const 1
    call $__wasi_print_int
  )
  (func $simulateAuth (export "simulateAuth")
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
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
    i32.const 0
    i32.const 0
    i32.const 0
    call $__wasi_print_str
    i32.const 1
    i32.const 0
    i32.const 0
    i32.const 0
    call $__wasi_print_str
    i32.const 1
    i32.const 4638
    i32.const 13
    i32.const 1
    call $__wasi_print_str
    i32.const 1
    i32.const 0
    i32.const 0
    i32.const 0
    call $__wasi_print_str
    i32.const 1
    i32.const 4652
    i32.const 46
    i32.const 1
    call $__wasi_print_str
    i32.const 1
    i32.const 0
    i32.const 0
    i32.const 0
    call $__wasi_print_str
    i32.const 1
    i32.const 0
    i32.const 0
    i32.const 0
    call $__wasi_print_str
    i32.const 1
    i32.const 4764
    i32.const 10
    i32.const 0
    call $__wasi_print_str
    i32.const 1
    i32.const 0
    i32.const 0
    i32.const 1
    call $__wasi_print_str
  )
  (func $simulateDatabase (export "simulateDatabase")
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
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
    i32.const 0
    i32.const 0
    i32.const 0
    call $__wasi_print_str
    i32.const 1
    i32.const 0
    i32.const 0
    i32.const 0
    call $__wasi_print_str
    i32.const 1
    i32.const 4825
    i32.const 6
    i32.const 0
    call $__wasi_print_str
    i32.const 1
    i32.const 0
    i32.const 0
    i32.const 1
    call $__wasi_print_str
    i32.const 1
    i32.const 0
    i32.const 0
    i32.const 0
    call $__wasi_print_str
    i32.const 1
    i32.const 0
    i32.const 0
    i32.const 0
    call $__wasi_print_str
    i32.const 1
    i32.const 1540
    i32.const 0
    call $__wasi_print_int
    i32.const 1
    i32.const 0
    i32.const 0
    i32.const 0
    call $__wasi_print_str
    i32.const 1
    i32.const 24
    i32.const 0
    call $__wasi_print_int
    i32.const 1
    i32.const 0
    i32.const 0
    i32.const 1
    call $__wasi_print_str
    i32.const 1
    i32.const 0
    i32.const 0
    i32.const 0
    call $__wasi_print_str
    i32.const 1
    i32.const 4881
    i32.const 45
    i32.const 0
    call $__wasi_print_str
    i32.const 1
    i32.const 0
    i32.const 0
    i32.const 0
    call $__wasi_print_str
    i32.const 1
    i32.const 0
    i32.const 1
    call $__wasi_print_int
  )
  (func $simulateWarning (export "simulateWarning")
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
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
    i32.const 0
    i32.const 0
    i32.const 0
    call $__wasi_print_str
    i32.const 1
    i32.const 0
    i32.const 0
    i32.const 0
    call $__wasi_print_str
    i32.const 1
    i32.const 890
    i32.const 0
    call $__wasi_print_int
    i32.const 1
    i32.const 0
    i32.const 0
    i32.const 1
    call $__wasi_print_str
    i32.const 1
    i32.const 0
    i32.const 0
    i32.const 0
    call $__wasi_print_str
    i32.const 1
    i32.const 0
    i32.const 0
    i32.const 0
    call $__wasi_print_str
    i32.const 1
    i32.const 5031
    i32.const 5
    i32.const 0
    call $__wasi_print_str
    i32.const 1
    i32.const 0
    i32.const 0
    i32.const 0
    call $__wasi_print_str
    i32.const 1
    i32.const 2
    i32.const 0
    call $__wasi_print_int
    i32.const 1
    i32.const 0
    i32.const 0
    i32.const 0
    call $__wasi_print_str
    i32.const 1
    i32.const 3
    i32.const 0
    call $__wasi_print_int
    i32.const 1
    i32.const 0
    i32.const 0
    i32.const 1
    call $__wasi_print_str
  )
  (func $simulateError (export "simulateError")
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
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
    i32.const 2
    i32.const 0
    i32.const 0
    i32.const 0
    call $__wasi_print_str
    i32.const 2
    i32.const 0
    i32.const 0
    i32.const 0
    call $__wasi_print_str
    i32.const 2
    i32.const 88219
    i32.const 1
    call $__wasi_print_int
    i32.const 2
    i32.const 0
    i32.const 0
    i32.const 0
    call $__wasi_print_str
    i32.const 2
    i32.const 5117
    i32.const 60
    i32.const 0
    call $__wasi_print_str
    i32.const 2
    i32.const 0
    i32.const 0
    i32.const 0
    call $__wasi_print_str
    i32.const 2
    i32.const 0
    i32.const 1
    call $__wasi_print_int
  )
  (func $start (export "start")
    (local $root i32)
    (local $card i32)
    (local $title i32)
    (local $desc i32)
    (local $toolbar i32)
    (local $consoleContainer i32)
    (local $stream i32)
    (local $entry i32)
    (local $badge i32)
    (local $text i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
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
    i32.const 5178
    i32.const 9
    call $dom_getElementById
    local.set $root
    local.get $root
    i32.const 0
    i32.eq
    if
    return
    end
    i32.const 5188
    i32.const 3
    call $dom_createElement
    local.set $card
    local.get $card
    i32.const 5192
    i32.const 10
    i32.const 5203
    i32.const 7
    call $dom_setStyle
    local.get $card
    i32.const 5211
    i32.const 7
    i32.const 5219
    i32.const 4
    call $dom_setStyle
    local.get $card
    i32.const 5224
    i32.const 12
    i32.const 5237
    i32.const 4
    call $dom_setStyle
    local.get $card
    i32.const 5242
    i32.const 5
    i32.const 5248
    i32.const 7
    call $dom_setStyle
    local.get $card
    i32.const 5256
    i32.const 9
    i32.const 5266
    i32.const 26
    call $dom_setStyle
    i32.const 5293
    i32.const 2
    call $dom_createElement
    local.set $title
    local.get $title
    i32.const 5296
    i32.const 32
    call $dom_setInnerText
    local.get $title
    i32.const 5242
    i32.const 5
    i32.const 5329
    i32.const 7
    call $dom_setStyle
    local.get $title
    i32.const 5337
    i32.const 12
    i32.const 5350
    i32.const 3
    call $dom_setStyle
    local.get $card
    local.get $title
    call $dom_appendChild
    i32.const 5354
    i32.const 1
    call $dom_createElement
    local.set $desc
    local.get $desc
    i32.const 5356
    i32.const 126
    call $dom_setInnerText
    local.get $desc
    i32.const 5242
    i32.const 5
    i32.const 5483
    i32.const 7
    call $dom_setStyle
    local.get $desc
    i32.const 5491
    i32.const 8
    i32.const 5500
    i32.const 4
    call $dom_setStyle
    local.get $desc
    i32.const 5337
    i32.const 12
    i32.const 5505
    i32.const 4
    call $dom_setStyle
    local.get $card
    local.get $desc
    call $dom_appendChild
    i32.const 5188
    i32.const 3
    call $dom_createElement
    local.set $toolbar
    local.get $toolbar
    i32.const 5510
    i32.const 7
    i32.const 5518
    i32.const 4
    call $dom_setStyle
    local.get $toolbar
    i32.const 5523
    i32.const 8
    i32.const 5532
    i32.const 4
    call $dom_setStyle
    local.get $toolbar
    i32.const 5537
    i32.const 3
    i32.const 5350
    i32.const 3
    call $dom_setStyle
    local.get $toolbar
    i32.const 5337
    i32.const 12
    i32.const 5505
    i32.const 4
    call $dom_setStyle
    local.get $toolbar
    i32.const 5541
    i32.const 5560
    i32.const 5568
    call $createBtn
    call $dom_appendChild
    local.get $toolbar
    i32.const 5587
    i32.const 5598
    i32.const 5606
    call $createBtn
    call $dom_appendChild
    local.get $toolbar
    i32.const 5624
    i32.const 5635
    i32.const 5643
    call $createBtn
    call $dom_appendChild
    local.get $toolbar
    i32.const 5659
    i32.const 5676
    i32.const 5684
    call $createBtn
    call $dom_appendChild
    local.get $toolbar
    i32.const 5702
    i32.const 5717
    i32.const 5725
    call $createBtn
    call $dom_appendChild
    local.get $toolbar
    i32.const 5742
    i32.const 5756
    i32.const 5764
    call $createBtn
    call $dom_appendChild
    local.get $card
    local.get $toolbar
    call $dom_appendChild
    i32.const 5188
    i32.const 3
    call $dom_createElement
    local.set $consoleContainer
    local.get $consoleContainer
    i32.const 5192
    i32.const 10
    i32.const 5780
    i32.const 7
    call $dom_setStyle
    local.get $consoleContainer
    i32.const 5788
    i32.const 6
    i32.const 5795
    i32.const 17
    call $dom_setStyle
    local.get $consoleContainer
    i32.const 5224
    i32.const 12
    i32.const 5350
    i32.const 3
    call $dom_setStyle
    local.get $consoleContainer
    i32.const 5211
    i32.const 7
    i32.const 5813
    i32.const 4
    call $dom_setStyle
    local.get $consoleContainer
    i32.const 5818
    i32.const 6
    i32.const 5825
    i32.const 5
    call $dom_setStyle
    local.get $consoleContainer
    i32.const 5831
    i32.const 9
    i32.const 5841
    i32.const 4
    call $dom_setStyle
    local.get $consoleContainer
    i32.const 5846
    i32.const 10
    i32.const 5857
    i32.const 9
    call $dom_setStyle
    local.get $consoleContainer
    i32.const 5491
    i32.const 8
    i32.const 5500
    i32.const 4
    call $dom_setStyle
    local.get $consoleContainer
    i32.const 5867
    i32.const 2
    i32.const 5870
    i32.const 20
    call $dom_setAttribute
    local.get $card
    local.get $consoleContainer
    call $dom_appendChild
    local.get $root
    local.get $card
    call $dom_appendChild
    call $runSimulation
  )
  (func $createBtn (export "createBtn") (param $label i32) (param $bgColor i32) (param $eventName i32) (result i32)
    (local $btn i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
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
    i32.const 6080
    i32.const 6
    call $dom_createElement
    local.set $btn
    local.get $btn
    local.get $label
    local.set $__str_arg_ptr
    local.get $__str_arg_ptr
    local.get $__str_arg_ptr
    call $__str_length
    call $dom_setInnerText
    local.get $btn
    i32.const 5192
    i32.const 10
    local.get $bgColor
    local.set $__str_arg_ptr
    local.get $__str_arg_ptr
    local.get $__str_arg_ptr
    call $__str_length
    call $dom_setStyle
    local.get $btn
    i32.const 5242
    i32.const 5
    i32.const 6087
    i32.const 7
    call $dom_setStyle
    local.get $btn
    i32.const 5788
    i32.const 6
    i32.const 6095
    i32.const 4
    call $dom_setStyle
    local.get $btn
    i32.const 5211
    i32.const 7
    i32.const 6100
    i32.const 8
    call $dom_setStyle
    local.get $btn
    i32.const 5224
    i32.const 12
    i32.const 6109
    i32.const 3
    call $dom_setStyle
    local.get $btn
    i32.const 6113
    i32.const 6
    i32.const 6120
    i32.const 7
    call $dom_setStyle
    local.get $btn
    i32.const 5491
    i32.const 8
    i32.const 5813
    i32.const 4
    call $dom_setStyle
    local.get $btn
    i32.const 5955
    i32.const 10
    i32.const 6128
    i32.const 3
    call $dom_setStyle
    local.get $btn
    i32.const 6132
    i32.const 5
    local.get $eventName
    local.set $__str_arg_ptr
    local.get $__str_arg_ptr
    local.get $__str_arg_ptr
    call $__str_length
    call $dom_addEventListener
    local.get $btn
    return
  )
  (func $clearConsole (export "clearConsole")
    (local $stream i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    (local $__str_arg_ptr i32)
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
    i32.const 5870
    i32.const 20
    call $dom_getElementById
    local.set $stream
    local.get $stream
    i32.const 0
    i32.ne
    if
    local.get $stream
    i32.const 6138
    i32.const 0
    call $dom_setInnerText
    end
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__caught_exc i32)
    call $runSimulation
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
    i32.const 6139
    call $__str_equals
    if
      global.get $__class_Log4j2Demo
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
    i32.const 6208
    call $__str_equals
    if
      global.get $__class_Runnable
      return
    end
    local.get $name
    i32.const 6201
    call $__str_equals
    if
      global.get $__class_Thread
      return
    end
    local.get $name
    i32.const 6195
    call $__str_equals
    if
      global.get $__class_Class
      return
    end
    local.get $name
    i32.const 6222
    call $__str_equals
    if
      global.get $__class_int
      return
    end
    local.get $name
    i32.const 6226
    call $__str_equals
    if
      global.get $__class_long
      return
    end
    local.get $name
    i32.const 6231
    call $__str_equals
    if
      global.get $__class_double
      return
    end
    local.get $name
    i32.const 6238
    call $__str_equals
    if
      global.get $__class_float
      return
    end
    local.get $name
    i32.const 6244
    call $__str_equals
    if
      global.get $__class_boolean
      return
    end
    local.get $name
    i32.const 6252
    call $__str_equals
    if
      global.get $__class_char
      return
    end
    local.get $name
    i32.const 6257
    call $__str_equals
    if
      global.get $__class_byte
      return
    end
    local.get $name
    i32.const 6262
    call $__str_equals
    if
      global.get $__class_short
      return
    end
    local.get $name
    i32.const 6268
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
      global.get $__class_Throwable
      return
    end
    local.get $vtableBase
    i32.const 11
    i32.eq
    if
      global.get $__class_Exception
      return
    end
    local.get $vtableBase
    i32.const 22
    i32.eq
    if
      global.get $__class_RuntimeException
      return
    end
    local.get $vtableBase
    i32.const 33
    i32.eq
    if
      global.get $__class_IllegalArgumentException
      return
    end
    local.get $vtableBase
    i32.const 44
    i32.eq
    if
      global.get $__class_IllegalStateException
      return
    end
    local.get $vtableBase
    i32.const 55
    i32.eq
    if
      global.get $__class_NullPointerException
      return
    end
    local.get $vtableBase
    i32.const 66
    i32.eq
    if
      global.get $__class_WASIFile
      return
    end
    local.get $vtableBase
    i32.const 76
    i32.eq
    if
      global.get $__class_Thread
      return
    end
    local.get $vtableBase
    i32.const 86
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
    i32.const 76
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
    i32.const 86
    i32.store offset=0
    local.get $inst
    i32.const 6139
    i32.store offset=4
    local.get $inst
    i32.const 6139
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
    global.set $__class_Log4j2Demo
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 86
    i32.store offset=0
    local.get $inst
    i32.const 0
    i32.store offset=4
    local.get $inst
    i32.const 0
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
    global.set $__class_Throwable
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 86
    i32.store offset=0
    local.get $inst
    i32.const 0
    i32.store offset=4
    local.get $inst
    i32.const 0
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
    global.set $__class_Exception
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 86
    i32.store offset=0
    local.get $inst
    i32.const 0
    i32.store offset=4
    local.get $inst
    i32.const 0
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
    global.set $__class_RuntimeException
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 86
    i32.store offset=0
    local.get $inst
    i32.const 0
    i32.store offset=4
    local.get $inst
    i32.const 0
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
    global.set $__class_IllegalArgumentException
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 86
    i32.store offset=0
    local.get $inst
    i32.const 0
    i32.store offset=4
    local.get $inst
    i32.const 0
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
    global.set $__class_IllegalStateException
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 86
    i32.store offset=0
    local.get $inst
    i32.const 0
    i32.store offset=4
    local.get $inst
    i32.const 0
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
    global.set $__class_NullPointerException
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 86
    i32.store offset=0
    local.get $inst
    i32.const 0
    i32.store offset=4
    local.get $inst
    i32.const 0
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
    global.set $__class_WASIFile
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 86
    i32.store offset=0
    local.get $inst
    i32.const 0
    i32.store offset=4
    local.get $inst
    i32.const 0
    i32.store offset=8
    local.get $inst
    i32.const 9
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
    i32.const 86
    i32.store offset=0
    local.get $inst
    i32.const 0
    i32.store offset=4
    local.get $inst
    i32.const 0
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
    global.set $__class_StringBuilder
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 86
    i32.store offset=0
    local.get $inst
    i32.const 0
    i32.store offset=4
    local.get $inst
    i32.const 0
    i32.store offset=8
    local.get $inst
    i32.const 11
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
    i32.const 86
    i32.store offset=0
    local.get $inst
    i32.const 0
    i32.store offset=4
    local.get $inst
    i32.const 0
    i32.store offset=8
    local.get $inst
    i32.const 12
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
    i32.const 86
    i32.store offset=0
    local.get $inst
    i32.const 0
    i32.store offset=4
    local.get $inst
    i32.const 0
    i32.store offset=8
    local.get $inst
    i32.const 13
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
    i32.const 86
    i32.store offset=0
    local.get $inst
    i32.const 0
    i32.store offset=4
    local.get $inst
    i32.const 0
    i32.store offset=8
    local.get $inst
    i32.const 14
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
    i32.const 86
    i32.store offset=0
    local.get $inst
    i32.const 6208
    i32.store offset=4
    local.get $inst
    i32.const 6208
    i32.store offset=8
    local.get $inst
    i32.const 15
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
    i32.const 86
    i32.store offset=0
    local.get $inst
    i32.const 6201
    i32.store offset=4
    local.get $inst
    i32.const 6201
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
    global.set $__class_Thread
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 86
    i32.store offset=0
    local.get $inst
    i32.const 6195
    i32.store offset=4
    local.get $inst
    i32.const 6195
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
    global.set $__class_Class
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 86
    i32.store offset=0
    local.get $inst
    i32.const 6222
    i32.store offset=4
    local.get $inst
    i32.const 6222
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
    global.set $__class_int
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 86
    i32.store offset=0
    local.get $inst
    i32.const 6226
    i32.store offset=4
    local.get $inst
    i32.const 6226
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
    global.set $__class_long
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 86
    i32.store offset=0
    local.get $inst
    i32.const 6231
    i32.store offset=4
    local.get $inst
    i32.const 6231
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
    global.set $__class_double
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 86
    i32.store offset=0
    local.get $inst
    i32.const 6238
    i32.store offset=4
    local.get $inst
    i32.const 6238
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
    global.set $__class_float
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 86
    i32.store offset=0
    local.get $inst
    i32.const 6244
    i32.store offset=4
    local.get $inst
    i32.const 6244
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
    global.set $__class_boolean
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 86
    i32.store offset=0
    local.get $inst
    i32.const 6252
    i32.store offset=4
    local.get $inst
    i32.const 6252
    i32.store offset=8
    local.get $inst
    i32.const 23
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
    i32.const 86
    i32.store offset=0
    local.get $inst
    i32.const 6257
    i32.store offset=4
    local.get $inst
    i32.const 6257
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
    global.set $__class_byte
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 86
    i32.store offset=0
    local.get $inst
    i32.const 6262
    i32.store offset=4
    local.get $inst
    i32.const 6262
    i32.store offset=8
    local.get $inst
    i32.const 25
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
    i32.const 86
    i32.store offset=0
    local.get $inst
    i32.const 6268
    i32.store offset=4
    local.get $inst
    i32.const 6268
    i32.store offset=8
    local.get $inst
    i32.const 26
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
  (func $__init_enums (export "__init_enums")
  )
  (start $__init_classes)
)
