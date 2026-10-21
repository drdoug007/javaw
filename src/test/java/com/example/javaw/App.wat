(module
  (import "dom" "getElementById" (func $dom_getElementById (param i32 i32) (result i32)))
  (import "dom" "createElement" (func $dom_createElement (param i32 i32) (result i32)))
  (import "dom" "setInnerText" (func $dom_setInnerText (param i32 i32 i32)))
  (import "dom" "setAttribute" (func $dom_setAttribute (param i32 i32 i32 i32 i32)))
  (import "dom" "appendChild" (func $dom_appendChild (param i32 i32)))
  (import "dom" "addEventListener" (func $dom_addEventListener (param i32 i32 i32 i32 i32)))
  (memory (export "memory") 2)
  (global $counter (mut i32) (i32.const 0))
  (data (i32.const 4096) "app\00")
  (data (i32.const 4100) "h1\00")
  (data (i32.const 4103) "class\00")
  (data (i32.const 4109) "app-title\00")
  (data (i32.const 4119) "Java to WebAssembly DOM App\00")
  (data (i32.const 4147) "p\00")
  (data (i32.const 4149) "id\00")
  (data (i32.const 4152) "status-text\00")
  (data (i32.const 4164) "Count: 0\00")
  (data (i32.const 4173) "button\00")
  (data (i32.const 4180) "btn-increment\00")
  (data (i32.const 4194) "Increment Counter\00")
  (data (i32.const 4212) "click\00")
  (data (i32.const 4218) "increment\00")
  (data (i32.const 4228) "Count: 1 click\00")
  (data (i32.const 4243) "Count: 2 clicks\00")
  (data (i32.const 4259) "Count: multiple clicks\00")
  (global $__heap_ptr (mut i32) (i32.const 65536))
  ;; Class/Interface: App
  (func $start (export "start")
    (local $app i32)
    (local $heading i32)
    (local $status i32)
    (local $btn i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    local.get $app
    local.get $heading
    call $dom_appendChild
    i32.const 4147
    i32.const 1
    call $dom_createElement
    local.set $status
    local.get $status
    i32.const 4149
    i32.const 2
    i32.const 4152
    i32.const 11
    call $dom_setAttribute
    local.get $status
    i32.const 4164
    i32.const 8
    call $dom_setInnerText
    local.get $app
    local.get $status
    call $dom_appendChild
    i32.const 4173
    i32.const 6
    call $dom_createElement
    local.set $btn
    local.get $btn
    i32.const 4149
    i32.const 2
    i32.const 4180
    i32.const 13
    call $dom_setAttribute
    local.get $btn
    i32.const 4194
    i32.const 17
    call $dom_setInnerText
    local.get $btn
    i32.const 4212
    i32.const 5
    i32.const 4218
    i32.const 9
    call $dom_addEventListener
    local.get $app
    local.get $btn
    call $dom_appendChild
  )
  (func $increment (export "increment")
    (local $status i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
    global.get $counter
    i32.const 1
    i32.add
    global.set $counter
    i32.const 4152
    i32.const 11
    call $dom_getElementById
    local.set $status
    global.get $counter
    i32.const 1
    i32.eq
    (if
      (then
    local.get $status
    i32.const 4228
    i32.const 14
    call $dom_setInnerText
      )
      (else
    global.get $counter
    i32.const 2
    i32.eq
    (if
      (then
    local.get $status
    i32.const 4243
    i32.const 15
    call $dom_setInnerText
      )
      (else
    local.get $status
    i32.const 4259
    i32.const 22
    call $dom_setInnerText
      )
    )
      )
    )
  )
)
