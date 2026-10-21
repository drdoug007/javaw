(module
  (memory (export "memory") 1)
  ;; Class/Interface: TestClass
  (func $sayHello (export "sayHello")
  )
  (func $addNumbers (export "addNumbers") (param $a i32) (param $b i32) (result i32)
    local.get $a
    local.get $b
    i32.add
  )
  (func $getPi (export "getPi") (result f64)
    f64.const 3.14159
  )
)
