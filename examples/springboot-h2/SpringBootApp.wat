(module
  (@custom "javaw_spring_web" "{\22routes\22:[{\22verb\22:\22GET\22,\22path\22:\22/api/products\22,\22controller\22:\22ProductController\22,\22export\22:\22ProductController_handleGetAll\22,\22result\22:\22Product[]\22,\22params\22:[]},{\22verb\22:\22GET\22,\22path\22:\22/api/products/{id}\22,\22controller\22:\22ProductController\22,\22export\22:\22ProductController_handleGetById\22,\22result\22:\22Product\22,\22params\22:[{\22name\22:\22id\22,\22type\22:\22int\22,\22path\22:true}]},{\22verb\22:\22POST\22,\22path\22:\22/api/products\22,\22controller\22:\22ProductController\22,\22export\22:\22ProductController_handleCreate\22,\22result\22:\22Product\22,\22params\22:[{\22name\22:\22name\22,\22type\22:\22String\22,\22path\22:false},{\22name\22:\22price\22,\22type\22:\22BigDecimal\22,\22path\22:false},{\22name\22:\22stock\22,\22type\22:\22int\22,\22path\22:false}]},{\22verb\22:\22PUT\22,\22path\22:\22/api/products/{id}\22,\22controller\22:\22ProductController\22,\22export\22:\22ProductController_handleUpdate\22,\22result\22:\22Product\22,\22params\22:[{\22name\22:\22id\22,\22type\22:\22int\22,\22path\22:true},{\22name\22:\22name\22,\22type\22:\22String\22,\22path\22:false},{\22name\22:\22price\22,\22type\22:\22BigDecimal\22,\22path\22:false},{\22name\22:\22stock\22,\22type\22:\22int\22,\22path\22:false}]},{\22verb\22:\22DELETE\22,\22path\22:\22/api/products/{id}\22,\22controller\22:\22ProductController\22,\22export\22:\22ProductController_handleDelete\22,\22result\22:\22void\22,\22params\22:[{\22name\22:\22id\22,\22type\22:\22int\22,\22path\22:true}]}],\22entities\22:{\22Product\22:[{\22name\22:\22id\22,\22type\22:\22Integer\22,\22offset\22:4,\22boxedOffset\22:8},{\22name\22:\22name\22,\22type\22:\22String\22,\22offset\22:8},{\22name\22:\22price\22,\22type\22:\22BigDecimal\22,\22offset\22:12},{\22name\22:\22stock\22,\22type\22:\22int\22,\22offset\22:16}]},\22boxInteger\22:\22Integer_valueOf__69_6e_74_\22,\22exception\22:{\22vtable\22:100,\22status\22:8,\22reason\22:12}}")
  (@custom "sourceMappingURL" "\16SpringBootApp.wasm.map")
  (import "dom" "save" (func $dom_save (param i32)))
  (tag $Exception (export "Exception") (param i32))
  (memory (export "memory") 2)
  (global $__repo_ProductRepository (mut i32) (i32.const 0))
  (global $__controller_ProductController (mut i32) (i32.const 0))
  (global $__application_SpringBootApp (mut i32) (i32.const 0))
  (global $__trueBoolean (mut i32) (i32.const 0))
  (global $__falseBoolean (mut i32) (i32.const 0))
  (global $__cache_Byte (mut i32) (i32.const 0))
  (global $__cache_Character (mut i32) (i32.const 0))
  (global $__cache_Integer (mut i32) (i32.const 0))
  (global $__cache_Long (mut i32) (i32.const 0))
  (global $__cache_Short (mut i32) (i32.const 0))
  (data (i32.const 4096) "products\00")
  (data (i32.const 4105) "MacBook Pro M3\00")
  (data (i32.const 4120) "1999\00")
  (data (i32.const 4125) "Dell XPS 15\00")
  (data (i32.const 4137) "11499\00")
  (data (i32.const 4143) "Sony WH-1000XM5\00")
  (data (i32.const 4159) "1399\00")
  (data (i32.const 4164) "/api/products\00")
  (data (i32.const 4178) "/{id}\00")
  (data (i32.const 4184) "Product not found with id \00")
  (data (i32.const 4211) "Product\00")
  (data (i32.const 4219) "SpringBootApp\00")
  (data (i32.const 4233) "ProductController\00")
  (data (i32.const 4251) "ProductRepository\00")
  (data (i32.const 4269) "__JavawRow_ProductRepository\00")
  (data (i32.const 4298) "ResponseStatusException\00")
  (data (i32.const 4322) "__JavawSpringContext\00")
  (data (i32.const 4343) "Number\00")
  (data (i32.const 4350) "Boolean\00")
  (data (i32.const 4358) "Byte\00")
  (data (i32.const 4363) "Character\00")
  (data (i32.const 4373) "Double\00")
  (data (i32.const 4380) "Float\00")
  (data (i32.const 4386) "Integer\00")
  (data (i32.const 4394) "Long\00")
  (data (i32.const 4399) "Short\00")
  (data (i32.const 4405) "@\00")
  (data (i32.const 4407) "true\00")
  (data (i32.const 4412) "false\00")
  (data (i32.const 4418) "class \00")
  (data (i32.const 4425) "interface \00")
  (data (i32.const 4436) "Object\00")
  (data (i32.const 4443) "String\00")
  (data (i32.const 4450) "Class\00")
  (data (i32.const 4456) "Thread\00")
  (data (i32.const 4463) "Runnable\00")
  (data (i32.const 4472) "Enum\00")
  (data (i32.const 4477) "int\00")
  (data (i32.const 4481) "long\00")
  (data (i32.const 4486) "double\00")
  (data (i32.const 4493) "float\00")
  (data (i32.const 4499) "boolean\00")
  (data (i32.const 4507) "char\00")
  (data (i32.const 4512) "byte\00")
  (data (i32.const 4517) "short\00")
  (data (i32.const 4523) "void\00")
  (global $__heap_ptr (mut i32) (i32.const 65536))
  (global $__class_Product (mut i32) (i32.const 0))
  (global $__class_SpringBootApp (mut i32) (i32.const 0))
  (global $__class_ProductController (mut i32) (i32.const 0))
  (global $__class_ProductRepository (mut i32) (i32.const 0))
  (global $__class___JavawRow_ProductRepository (mut i32) (i32.const 0))
  (global $__class_ResponseStatusException (mut i32) (i32.const 0))
  (global $__class___JavawSpringContext (mut i32) (i32.const 0))
  (global $__class_Number (mut i32) (i32.const 0))
  (global $__class_Boolean (mut i32) (i32.const 0))
  (global $__class_Byte (mut i32) (i32.const 0))
  (global $__class_Character (mut i32) (i32.const 0))
  (global $__class_Double (mut i32) (i32.const 0))
  (global $__class_Float (mut i32) (i32.const 0))
  (global $__class_Integer (mut i32) (i32.const 0))
  (global $__class_Long (mut i32) (i32.const 0))
  (global $__class_Short (mut i32) (i32.const 0))
  (global $__class_Throwable (mut i32) (i32.const 0))
  (global $__class_Exception (mut i32) (i32.const 0))
  (global $__class_RuntimeException (mut i32) (i32.const 0))
  (global $__class_IllegalArgumentException (mut i32) (i32.const 0))
  (global $__class_IllegalStateException (mut i32) (i32.const 0))
  (global $__class_NullPointerException (mut i32) (i32.const 0))
  (global $__class_ArithmeticException (mut i32) (i32.const 0))
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
  (type $t_sig_4 (func (param i32 i32)))
  (type $t_sig_5 (func (param i32 i32 i32 i32)))
  (type $t_sig_6 (func (param i32 i32 i32 i32) (result i32)))
  (type $t_sig_7 (func (param i32 i32 i32 i32 i32) (result i32)))
  (type $t_sig_8 (func (param i32) (result i64)))
  (type $t_sig_9 (func (param i32) (result f32)))
  (type $t_sig_10 (func (param i32) (result f64)))
  ;; --- WebAssembly Function Table (VTable) ---
  (table (export "__vtable") 327 funcref)
  (elem (i32.const 0) $Product_toString $Product_equals $Product_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Product_run $Product_getClass $Product_getName $Product_setName $Product_getPrice $Product_setPrice $Product_getStock $Product_setStock $SpringBootApp_toString $SpringBootApp_equals $SpringBootApp_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $SpringBootApp_run $SpringBootApp_getClass $SpringBootApp___javaw_runner_SpringBootApp_init $SpringBootApp_addProduct $ProductController_toString $ProductController_equals $ProductController_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $ProductController_run $ProductController_getClass $ProductController_handleGetAll $ProductController_handleGetById $ProductController_handleCreate $ProductController_handleUpdate $ProductController_handleDelete $ProductRepository_toString $ProductRepository_equals $ProductRepository_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $ProductRepository_run $ProductRepository_getClass $ProductRepository_findById $ProductRepository_findAll $ProductRepository_save $ProductRepository_deleteById $__JavawRow_ProductRepository_toString $__JavawRow_ProductRepository_equals $__JavawRow_ProductRepository_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $__JavawRow_ProductRepository_run $__JavawRow_ProductRepository_getClass $Throwable_toString $Throwable_equals $Throwable_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Throwable_run $Throwable_getClass $Throwable_getMessage $Throwable_toString $Throwable_equals $Throwable_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Throwable_run $Throwable_getClass $Exception_getMessage $Throwable_toString $Throwable_equals $Throwable_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Throwable_run $Throwable_getClass $RuntimeException_getMessage $Throwable_toString $Throwable_equals $Throwable_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Throwable_run $Throwable_getClass $RuntimeException_getMessage $__JavawSpringContext_toString $__JavawSpringContext_equals $__JavawSpringContext_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $__JavawSpringContext_run $__JavawSpringContext_getClass $Number_toString $Number_equals $Number_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Number_run $Number_getClass $Number_intValue $Number_longValue $Number_floatValue $Number_doubleValue $Boolean_toString $Boolean_equals $Boolean_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Boolean_run $Boolean_getClass $Boolean_booleanValue $Number_toString $Byte_equals $Byte_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Number_run $Number_getClass $Byte_intValue $Byte_longValue $Byte_floatValue $Byte_doubleValue $Byte_byteValue $Byte_shortValue $Character_toString $Character_equals $Character_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Character_run $Character_getClass $Character_charValue $Number_toString $Double_equals $Double_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Number_run $Number_getClass $Double_intValue $Double_longValue $Double_floatValue $Double_doubleValue $Double_byteValue $Double_shortValue $Number_toString $Float_equals $Float_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Number_run $Number_getClass $Float_intValue $Float_longValue $Float_floatValue $Float_doubleValue $Float_byteValue $Float_shortValue $Number_toString $Integer_equals $Integer_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Number_run $Number_getClass $Integer_intValue $Integer_longValue $Integer_floatValue $Integer_doubleValue $Integer_byteValue $Integer_shortValue $Number_toString $Long_equals $Long_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Number_run $Number_getClass $Long_intValue $Long_longValue $Long_floatValue $Long_doubleValue $Long_byteValue $Long_shortValue $Number_toString $Short_equals $Short_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Number_run $Number_getClass $Short_intValue $Short_longValue $Short_floatValue $Short_doubleValue $Short_byteValue $Short_shortValue $Throwable_toString $Throwable_equals $Throwable_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Throwable_run $Throwable_getClass $IllegalArgumentException_getMessage $Throwable_toString $Throwable_equals $Throwable_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Throwable_run $Throwable_getClass $IllegalStateException_getMessage $Throwable_toString $Throwable_equals $Throwable_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Throwable_run $Throwable_getClass $NullPointerException_getMessage $Throwable_toString $Throwable_equals $Throwable_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Throwable_run $Throwable_getClass $ArithmeticException_getMessage $WASIFile_toString $WASIFile_equals $WASIFile_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $WASIFile_run $WASIFile_getClass $Thread_toString $Thread_equals $Thread_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Thread_run $Thread_getClass $Class_toString $Class_equals $Class_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Class_run $Class_getClass)
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
      i32.const 4407
    else
      i32.const 4412
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
  (func $ArithmeticException__init (export "ArithmeticException__init") (param $this i32)
    local.get $this
    call $RuntimeException__init
  )
  (func $ArithmeticException__init_1 (export "ArithmeticException__init_1") (param $this i32) (param $message i32)
    local.get $this
    local.get $message
    call $RuntimeException__init_1
  )
  (func $ArithmeticException_getMessage (export "ArithmeticException_getMessage") (param $this i32) (result i32)
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
  (func $__system_getenv (export "__system_getenv") (param $key i32) (result i32)
    i32.const 0
  )
  ;; Class/Interface: Product
  (func $Product__init (export "Product__init") (param $this i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
  (func $Product__init_1 (export "Product__init_1") (param $this i32) (param $id i32) (param $name i32) (param $price i32) (param $stock i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    local.get $name
    i32.store offset=8
    local.get $this
    local.get $price
    i32.store offset=12
    local.get $this
    local.get $stock
    i32.store offset=16
  )
  (func $Product_getName (export "Product_getName") (param $this i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
  (func $Product_setName (export "Product_setName") (param $this i32) (param $name i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    i32.store offset=8
  )
  (func $Product_getPrice (export "Product_getPrice") (param $this i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    i32.load offset=12
    return
  )
  (func $Product_setPrice (export "Product_setPrice") (param $this i32) (param $price i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    local.get $price
    i32.store offset=12
  )
  (func $Product_getStock (export "Product_getStock") (param $this i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    i32.load offset=16
    return
  )
  (func $Product_setStock (export "Product_setStock") (param $this i32) (param $stock i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    local.get $stock
    i32.store offset=16
  )
  ;; Class/Interface: ProductRepository
  (func $ProductRepository__init (export "ProductRepository__init") (param $this i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    local.get $this
    i32.const 1
    i32.store offset=8
  )
  (func $ProductRepository_findById (export "ProductRepository_findById") (param $this i32) (param $id i32) (result i32)
    (local $row i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    local.set $row
    (block $break_while_1
      (loop $loop_while_1
    local.get $row
    i32.const 0
    i32.ne
        i32.eqz
        br_if $break_while_1
    local.get $row
    i32.load offset=4
    i32.load offset=4
    call $__require_nonnull
    i32.load offset=8
    local.get $id
    i32.eq
    if
    local.get $row
    i32.load offset=4
    return
    end
    local.get $row
    i32.load offset=8
    local.set $row
        br $loop_while_1
      )
    )
    i32.const 0
    return
  )
  (func $ProductRepository_findAll (export "ProductRepository_findAll") (param $this i32) (result i32)
    (local $size i32)
    (local $row i32)
    (local $values i32)
    (local $index i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    (local $__mutation$0$address i32)
    (local $__mutation$0$old i32)
    (local $__mutation$0$new i32)
    (local $__mutation$1$address i32)
    (local $__mutation$1$old i32)
    (local $__mutation$1$new i32)
    (local $__caught_exc i32)
    i32.const 0
    local.set $size
    local.get $this
    i32.load offset=4
    local.set $row
    (block $break_while_2
      (loop $loop_while_2
    local.get $row
    i32.const 0
    i32.ne
        i32.eqz
        br_if $break_while_2
    local.get $size
    local.tee $__mutation$0$old
    i32.const 1
    i32.add
    local.set $__mutation$0$new
    local.get $__mutation$0$new
    local.set $size
    local.get $__mutation$0$old
    drop
    local.get $row
    i32.load offset=8
    local.set $row
        br $loop_while_2
      )
    )
    local.get $size
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
    local.set $values
    local.get $this
    i32.load offset=4
    local.set $row
    i32.const 0
    local.set $index
    (block $break_while_3
      (loop $loop_while_3
    local.get $row
    i32.const 0
    i32.ne
        i32.eqz
        br_if $break_while_3
    local.get $values
    local.get $index
    i32.const 4
    i32.mul
    i32.add
    local.get $row
    i32.load offset=4
    i32.store offset=4
    local.get $index
    local.tee $__mutation$1$old
    i32.const 1
    i32.add
    local.set $__mutation$1$new
    local.get $__mutation$1$new
    local.set $index
    local.get $__mutation$1$old
    drop
    local.get $row
    i32.load offset=8
    local.set $row
        br $loop_while_3
      )
    )
    local.get $values
    return
  )
  (func $ProductRepository_save (export "ProductRepository_save") (param $this i32) (param $value i32) (result i32)
    (local $row i32)
    (local $previous i32)
    (local $added i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    (local $__mutation$2$address i32)
    (local $__mutation$2$old i32)
    (local $__mutation$2$new i32)
    (local $__caught_exc i32)
    local.get $value
    i32.load offset=4
    i32.const 0
    i32.eq
    if (result i32)
    i32.const 1
    else
    local.get $value
    i32.load offset=4
    call $__require_nonnull
    i32.load offset=8
    i32.const 0
    i32.eq
    end
    if
    local.get $value
    local.get $this
    i32.load offset=8
    call $Integer_valueOf__69_6e_74_
    i32.store offset=4
    local.get $this
    i32.const 8
    i32.add
    local.set $__mutation$2$address
    local.get $__mutation$2$address
    i32.load
    local.tee $__mutation$2$old
    i32.const 1
    i32.add
    local.set $__mutation$2$new
    local.get $__mutation$2$address
    local.get $__mutation$2$new
    i32.store
    local.get $__mutation$2$old
    drop
    end
    local.get $this
    i32.load offset=4
    local.set $row
    i32.const 0
    local.set $previous
    (block $break_while_4
      (loop $loop_while_4
    local.get $row
    i32.const 0
    i32.ne
        i32.eqz
        br_if $break_while_4
    local.get $row
    i32.load offset=4
    i32.load offset=4
    call $__require_nonnull
    i32.load offset=8
    local.get $value
    i32.load offset=4
    call $__require_nonnull
    i32.load offset=8
    i32.eq
    if
    local.get $row
    local.get $value
    i32.store offset=4
    local.get $value
    return
    end
    local.get $row
    local.set $previous
    local.get $row
    i32.load offset=8
    local.set $row
        br $loop_while_4
      )
    )
    i32.const 16
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 57
    i32.store offset=0
    local.get $__obj_alloc_ptr
    local.get $value
    i32.const 0
    call $__JavawRow_ProductRepository__init
    local.get $__obj_alloc_ptr
    local.set $added
    local.get $previous
    i32.const 0
    i32.eq
    if
    local.get $this
    local.get $added
    i32.store offset=4
    else
    local.get $previous
    local.get $added
    i32.store offset=8
    end
    local.get $value
    i32.load offset=4
    call $__require_nonnull
    i32.load offset=8
    local.get $this
    i32.load offset=8
    i32.ge_s
    if
    local.get $this
    local.get $value
    i32.load offset=4
    call $__require_nonnull
    i32.load offset=8
    i32.const 1
    i32.add
    i32.store offset=8
    end
    local.get $value
    return
  )
  (func $ProductRepository_deleteById (export "ProductRepository_deleteById") (param $this i32) (param $id i32)
    (local $row i32)
    (local $previous i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    local.set $row
    i32.const 0
    local.set $previous
    (block $break_while_5
      (loop $loop_while_5
    local.get $row
    i32.const 0
    i32.ne
        i32.eqz
        br_if $break_while_5
    local.get $row
    i32.load offset=4
    i32.load offset=4
    call $__require_nonnull
    i32.load offset=8
    local.get $id
    i32.eq
    if
    local.get $previous
    i32.const 0
    i32.eq
    if
    local.get $this
    local.get $row
    i32.load offset=8
    i32.store offset=4
    else
    local.get $previous
    local.get $row
    i32.load offset=8
    i32.store offset=8
    end
    return
    end
    local.get $row
    local.set $previous
    local.get $row
    i32.load offset=8
    local.set $row
        br $loop_while_5
      )
    )
  )
  ;; Class/Interface: SpringBootApp
  (func $SpringBootApp__init (export "SpringBootApp__init") (param $this i32) (param $productRepository i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    local.get $productRepository
    i32.store offset=4
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
    (local $__receiver$0 i32)
    (local $__caught_exc i32)
    call $__javaw_boot
  )
  (func $SpringBootApp___javaw_runner_SpringBootApp_init (export "SpringBootApp___javaw_runner_SpringBootApp_init") (param $this i32) (param $args i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    (local $__receiver$1 i32)
    (local $__receiver$2 i32)
    (local $__receiver$3 i32)
    (local $__caught_exc i32)
    local.get $this
    call $__require_nonnull
    local.tee $__receiver$1
    i32.const 4105
    i32.const 4120
    i32.const 25
    local.get $__receiver$1
    i32.load offset=0
    i32.const 11
    i32.add
    call_indirect (type $t_sig_5)
    local.get $this
    call $__require_nonnull
    local.tee $__receiver$2
    i32.const 4125
    i32.const 4137
    i32.const 18
    local.get $__receiver$2
    i32.load offset=0
    i32.const 11
    i32.add
    call_indirect (type $t_sig_5)
    local.get $this
    call $__require_nonnull
    local.tee $__receiver$3
    i32.const 4143
    i32.const 4159
    i32.const 50
    local.get $__receiver$3
    i32.load offset=0
    i32.const 11
    i32.add
    call_indirect (type $t_sig_5)
  )
  (func $SpringBootApp_addProduct (export "SpringBootApp_addProduct") (param $this i32) (param $name i32) (param $price i32) (param $stock i32)
    (local $product i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    (local $__receiver$4 i32)
    (local $__caught_exc i32)
    i32.const 24
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 0
    i32.store offset=0
    local.get $__obj_alloc_ptr
    i32.const 0
    local.get $name
    local.get $price
    local.get $stock
    call $Product__init_1
    local.get $__obj_alloc_ptr
    local.set $product
    local.get $this
    i32.load offset=4
    call $__require_nonnull
    local.tee $__receiver$4
    local.get $product
    local.get $__receiver$4
    i32.load offset=0
    i32.const 12
    i32.add
    call_indirect (type $t_sig_2)
    drop
  )
  ;; Class/Interface: ProductController
  (func $ProductController__init (export "ProductController__init") (param $this i32) (param $productRepository i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    local.get $productRepository
    i32.store offset=4
  )
  (func $ProductController_handleGetAll (export "ProductController_handleGetAll") (param $this i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    (local $__receiver$5 i32)
    (local $__caught_exc i32)
    local.get $this
    i32.load offset=4
    call $__require_nonnull
    local.tee $__receiver$5
    local.get $__receiver$5
    i32.load offset=0
    i32.const 11
    i32.add
    call_indirect (type $t_sig_1)
    return
  )
  (func $ProductController_handleGetById (export "ProductController_handleGetById") (param $this i32) (param $id i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    (local $__receiver$6 i32)
    (local $__receiver$7 i32)
    (local $__caught_exc i32)
    local.get $this
    i32.load offset=4
    call $__require_nonnull
    local.tee $__receiver$7
    local.get $id
    local.get $__receiver$7
    i32.load offset=0
    i32.const 10
    i32.add
    call_indirect (type $t_sig_2)
    local.get $id
    call $__javaw_require_0
    return
  )
  (func $ProductController_handleCreate (export "ProductController_handleCreate") (param $this i32) (param $name i32) (param $price i32) (param $stock i32) (result i32)
    (local $product i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    (local $__receiver$8 i32)
    (local $__caught_exc i32)
    i32.const 24
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 0
    i32.store offset=0
    local.get $__obj_alloc_ptr
    i32.const 0
    local.get $name
    local.get $price
    local.get $stock
    call $Product__init_1
    local.get $__obj_alloc_ptr
    local.set $product
    local.get $this
    i32.load offset=4
    call $__require_nonnull
    local.tee $__receiver$8
    local.get $product
    local.get $__receiver$8
    i32.load offset=0
    i32.const 12
    i32.add
    call_indirect (type $t_sig_2)
    return
  )
  (func $ProductController_handleUpdate (export "ProductController_handleUpdate") (param $this i32) (param $id i32) (param $name i32) (param $price i32) (param $stock i32) (result i32)
    (local $product i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    (local $__receiver$9 i32)
    (local $__receiver$10 i32)
    (local $__receiver$11 i32)
    (local $__receiver$12 i32)
    (local $__receiver$13 i32)
    (local $__receiver$14 i32)
    (local $__caught_exc i32)
    local.get $this
    i32.load offset=4
    call $__require_nonnull
    local.tee $__receiver$10
    local.get $id
    local.get $__receiver$10
    i32.load offset=0
    i32.const 10
    i32.add
    call_indirect (type $t_sig_2)
    local.get $id
    local.get $name
    local.get $price
    local.get $stock
    call $__javaw_require_1
    local.set $product
    local.get $product
    call $__require_nonnull
    local.tee $__receiver$11
    local.get $name
    local.get $__receiver$11
    i32.load offset=0
    i32.const 11
    i32.add
    call_indirect (type $t_sig_4)
    local.get $product
    call $__require_nonnull
    local.tee $__receiver$12
    local.get $price
    local.get $__receiver$12
    i32.load offset=0
    i32.const 13
    i32.add
    call_indirect (type $t_sig_4)
    local.get $product
    call $__require_nonnull
    local.tee $__receiver$13
    local.get $stock
    local.get $__receiver$13
    i32.load offset=0
    i32.const 15
    i32.add
    call_indirect (type $t_sig_4)
    local.get $this
    i32.load offset=4
    call $__require_nonnull
    local.tee $__receiver$14
    local.get $product
    local.get $__receiver$14
    i32.load offset=0
    i32.const 12
    i32.add
    call_indirect (type $t_sig_2)
    return
  )
  (func $ProductController_handleDelete (export "ProductController_handleDelete") (param $this i32) (param $id i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    (local $__receiver$15 i32)
    (local $__receiver$16 i32)
    (local $__receiver$17 i32)
    (local $__caught_exc i32)
    local.get $this
    i32.load offset=4
    call $__require_nonnull
    local.tee $__receiver$16
    local.get $id
    local.get $__receiver$16
    i32.load offset=0
    i32.const 10
    i32.add
    call_indirect (type $t_sig_2)
    local.get $id
    call $__javaw_require_2
    drop
    local.get $this
    i32.load offset=4
    call $__require_nonnull
    local.tee $__receiver$17
    local.get $id
    local.get $__receiver$17
    i32.load offset=0
    i32.const 13
    i32.add
    call_indirect (type $t_sig_4)
  )
  (func $__javaw_require_0 (export "__javaw_require_0") (param $__value i32) (param $id i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    local.get $__value
    i32.const 0
    i32.eq
    if
    i32.const 16
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 100
    i32.store offset=0
    local.get $__obj_alloc_ptr
    i32.const 404
    i32.const 4184
    local.get $id
    call $__str_from_i32
    call $__str_concat
    call $ResponseStatusException__init
    local.get $__obj_alloc_ptr
    throw $Exception
    end
    local.get $__value
    return
  )
  (func $__javaw_require_1 (export "__javaw_require_1") (param $__value i32) (param $id i32) (param $name i32) (param $price i32) (param $stock i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    local.get $__value
    i32.const 0
    i32.eq
    if
    i32.const 16
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 100
    i32.store offset=0
    local.get $__obj_alloc_ptr
    i32.const 404
    i32.const 4184
    local.get $id
    call $__str_from_i32
    call $__str_concat
    call $ResponseStatusException__init
    local.get $__obj_alloc_ptr
    throw $Exception
    end
    local.get $__value
    return
  )
  (func $__javaw_require_2 (export "__javaw_require_2") (param $__value i32) (param $id i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    local.get $__value
    i32.const 0
    i32.eq
    if
    i32.const 16
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 100
    i32.store offset=0
    local.get $__obj_alloc_ptr
    i32.const 404
    i32.const 4184
    local.get $id
    call $__str_from_i32
    call $__str_concat
    call $ResponseStatusException__init
    local.get $__obj_alloc_ptr
    throw $Exception
    end
    local.get $__value
    return
  )
  ;; Class/Interface: __JavawRow_ProductRepository
  (func $__JavawRow_ProductRepository__init (export "__JavawRow_ProductRepository__init") (param $this i32) (param $value i32) (param $next i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    local.get $value
    i32.store offset=4
    local.get $this
    local.get $next
    i32.store offset=8
  )
  ;; Class/Interface: ResponseStatusException
  (func $ResponseStatusException__init (export "ResponseStatusException__init") (param $this i32) (param $status i32) (param $reason i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    local.get $reason
    call $RuntimeException__init_1
    local.get $this
    local.get $status
    i32.store offset=8
    local.get $this
    local.get $reason
    i32.store offset=12
  )
  ;; Class/Interface: __JavawSpringContext
  (func $__JavawSpringContext__init (export "__JavawSpringContext__init") (param $this i32)
  )
  (func $__javaw_boot (export "__javaw_boot")
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    (local $__receiver$18 i32)
    (local $__caught_exc i32)
    global.get $__repo_ProductRepository
    i32.const 0
    i32.eq
    if
    i32.const 16
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 43
    i32.store offset=0
    local.get $__obj_alloc_ptr
    call $ProductRepository__init
    local.get $__obj_alloc_ptr
    global.set $__repo_ProductRepository
    end
    global.get $__controller_ProductController
    i32.const 0
    i32.eq
    if
    i32.const 8
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 28
    i32.store offset=0
    local.get $__obj_alloc_ptr
    global.get $__repo_ProductRepository
    call $ProductController__init
    local.get $__obj_alloc_ptr
    global.set $__controller_ProductController
    end
    global.get $__application_SpringBootApp
    i32.const 0
    i32.eq
    if
    i32.const 8
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 16
    i32.store offset=0
    local.get $__obj_alloc_ptr
    global.get $__repo_ProductRepository
    call $SpringBootApp__init
    local.get $__obj_alloc_ptr
    global.set $__application_SpringBootApp
    global.get $__application_SpringBootApp
    call $__require_nonnull
    local.tee $__receiver$18
    i32.const 0
    local.get $__receiver$18
    i32.load offset=0
    i32.const 10
    i32.add
    call_indirect (type $t_sig_4)
    end
  )
  (func $__javaw_controller_ProductController (export "__javaw_controller_ProductController") (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    global.get $__controller_ProductController
    return
  )
  ;; Class/Interface: Number
  (func $Number__init (export "Number__init") (param $this i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
  (func $Number_intValue (export "Number_intValue") (param $this i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    i32.const 264
    i32.store offset=0
    local.get $__obj_alloc_ptr
    call $IllegalStateException__init
    local.get $__obj_alloc_ptr
    throw $Exception
    i32.const 0
  )
  (func $Number_longValue (export "Number_longValue") (param $this i32) (result i64)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    i32.const 264
    i32.store offset=0
    local.get $__obj_alloc_ptr
    call $IllegalStateException__init
    local.get $__obj_alloc_ptr
    throw $Exception
    i64.const 0
  )
  (func $Number_floatValue (export "Number_floatValue") (param $this i32) (result f32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    i32.const 264
    i32.store offset=0
    local.get $__obj_alloc_ptr
    call $IllegalStateException__init
    local.get $__obj_alloc_ptr
    throw $Exception
    f32.const 0.0
  )
  (func $Number_doubleValue (export "Number_doubleValue") (param $this i32) (result f64)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    i32.const 264
    i32.store offset=0
    local.get $__obj_alloc_ptr
    call $IllegalStateException__init
    local.get $__obj_alloc_ptr
    throw $Exception
    f64.const 0.0
  )
  ;; Class/Interface: Boolean
  (func $Boolean__init (export "Boolean__init") (param $this i32) (param $value i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    local.get $value
    i32.store offset=8
  )
  (func $Boolean_valueOf__62_6f_6f_6c_65_61_6e_ (export "Boolean_valueOf__62_6f_6f_6c_65_61_6e_") (param $value i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    local.get $value
    if
    global.get $__trueBoolean
    i32.const 0
    i32.eq
    if
    i32.const 16
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 135
    i32.store offset=0
    local.get $__obj_alloc_ptr
    i32.const 1
    call $Boolean__init
    local.get $__obj_alloc_ptr
    global.set $__trueBoolean
    end
    global.get $__trueBoolean
    return
    end
    global.get $__falseBoolean
    i32.const 0
    i32.eq
    if
    i32.const 16
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 135
    i32.store offset=0
    local.get $__obj_alloc_ptr
    i32.const 0
    call $Boolean__init
    local.get $__obj_alloc_ptr
    global.set $__falseBoolean
    end
    global.get $__falseBoolean
    return
  )
  (func $Boolean_booleanValue (export "Boolean_booleanValue") (param $this i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
  (func $Boolean_equals (export "Boolean_equals") (param $this i32) (param $other i32) (result i32)
    (local $that i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
      i32.const 135
      i32.eq
      i32.or
    end
    i32.eqz
    if
    i32.const 0
    return
    end
    local.get $other
    local.set $that
    local.get $this
    i32.load offset=8
    local.get $that
    i32.load offset=8
    i32.eq
    return
  )
  (func $Boolean_hashCode (export "Boolean_hashCode") (param $this i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    if (result i32)
    i32.const 1231
    else
    i32.const 1237
    end
    return
  )
  ;; Class/Interface: Byte
  (func $Byte__init (export "Byte__init") (param $this i32) (param $value i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    call $Number__init
    local.get $this
    local.get $value
    i32.store offset=8
  )
  (func $Byte_valueOf__62_79_74_65_ (export "Byte_valueOf__62_79_74_65_") (param $value i32) (result i32)
    (local $index i32)
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
    (local $__caught_exc i32)
    local.get $value
    i32.const 0
    i32.const 128
    i32.sub
    i32.ge_s
    if (result i32)
    local.get $value
    i32.const 127
    i32.le_s
    else
    i32.const 0
    end
    if
    global.get $__cache_Byte
    i32.const 0
    i32.eq
    if
    i32.const 256
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
    global.set $__cache_Byte
    end
    local.get $value
    i32.const 128
    i32.add
    local.set $index
    global.get $__cache_Byte
    local.get $index
    i32.const 4
    i32.mul
    i32.add
    i32.load offset=4
    local.set $result
    local.get $result
    i32.const 0
    i32.eq
    if
    i32.const 16
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 146
    i32.store offset=0
    local.get $__obj_alloc_ptr
    local.get $value
    call $Byte__init
    local.get $__obj_alloc_ptr
    local.set $result
    global.get $__cache_Byte
    local.get $index
    i32.const 4
    i32.mul
    i32.add
    local.get $result
    i32.store offset=4
    end
    local.get $result
    return
    end
    i32.const 16
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 146
    i32.store offset=0
    local.get $__obj_alloc_ptr
    local.get $value
    call $Byte__init
    local.get $__obj_alloc_ptr
    return
  )
  (func $Byte_byteValue (export "Byte_byteValue") (param $this i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    i32.extend8_s
    return
  )
  (func $Byte_shortValue (export "Byte_shortValue") (param $this i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    i32.extend16_s
    return
  )
  (func $Byte_intValue (export "Byte_intValue") (param $this i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
  (func $Byte_longValue (export "Byte_longValue") (param $this i32) (result i64)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    i64.extend_i32_s
    return
  )
  (func $Byte_floatValue (export "Byte_floatValue") (param $this i32) (result f32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    f32.convert_i32_s
    return
  )
  (func $Byte_doubleValue (export "Byte_doubleValue") (param $this i32) (result f64)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    f64.convert_i32_s
    return
  )
  (func $Byte_equals (export "Byte_equals") (param $this i32) (param $other i32) (result i32)
    (local $that i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
      i32.const 146
      i32.eq
      i32.or
    end
    i32.eqz
    if
    i32.const 0
    return
    end
    local.get $other
    local.set $that
    local.get $this
    i32.load offset=8
    local.get $that
    i32.load offset=8
    i32.eq
    return
  )
  (func $Byte_hashCode (export "Byte_hashCode") (param $this i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
  ;; Class/Interface: Character
  (func $Character__init (export "Character__init") (param $this i32) (param $value i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    local.get $value
    i32.store offset=8
  )
  (func $Character_valueOf__63_68_61_72_ (export "Character_valueOf__63_68_61_72_") (param $value i32) (result i32)
    (local $index i32)
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
    (local $__caught_exc i32)
    local.get $value
    i32.const 0
    i32.ge_s
    if (result i32)
    local.get $value
    i32.const 127
    i32.le_s
    else
    i32.const 0
    end
    if
    global.get $__cache_Character
    i32.const 0
    i32.eq
    if
    i32.const 256
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
    global.set $__cache_Character
    end
    local.get $value
    i32.const 0
    i32.add
    local.set $index
    global.get $__cache_Character
    local.get $index
    i32.const 4
    i32.mul
    i32.add
    i32.load offset=4
    local.set $result
    local.get $result
    i32.const 0
    i32.eq
    if
    i32.const 16
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 162
    i32.store offset=0
    local.get $__obj_alloc_ptr
    local.get $value
    call $Character__init
    local.get $__obj_alloc_ptr
    local.set $result
    global.get $__cache_Character
    local.get $index
    i32.const 4
    i32.mul
    i32.add
    local.get $result
    i32.store offset=4
    end
    local.get $result
    return
    end
    i32.const 16
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 162
    i32.store offset=0
    local.get $__obj_alloc_ptr
    local.get $value
    call $Character__init
    local.get $__obj_alloc_ptr
    return
  )
  (func $Character_charValue (export "Character_charValue") (param $this i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
  (func $Character_equals (export "Character_equals") (param $this i32) (param $other i32) (result i32)
    (local $that i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
      i32.const 162
      i32.eq
      i32.or
    end
    i32.eqz
    if
    i32.const 0
    return
    end
    local.get $other
    local.set $that
    local.get $this
    i32.load offset=8
    local.get $that
    i32.load offset=8
    i32.eq
    return
  )
  (func $Character_hashCode (export "Character_hashCode") (param $this i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
  ;; Class/Interface: Double
  (func $Double__init (export "Double__init") (param $this i32) (param $value f64)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    call $Number__init
    local.get $this
    local.get $value
    f64.store offset=8
  )
  (func $Double_valueOf__64_6f_75_62_6c_65_ (export "Double_valueOf__64_6f_75_62_6c_65_") (param $value f64) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    i32.const 173
    i32.store offset=0
    local.get $__obj_alloc_ptr
    local.get $value
    call $Double__init
    local.get $__obj_alloc_ptr
    return
  )
  (func $Double_byteValue (export "Double_byteValue") (param $this i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    f64.load offset=8
    i32.trunc_sat_f64_s
    i32.extend8_s
    return
  )
  (func $Double_shortValue (export "Double_shortValue") (param $this i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    f64.load offset=8
    i32.trunc_sat_f64_s
    i32.extend16_s
    return
  )
  (func $Double_intValue (export "Double_intValue") (param $this i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    f64.load offset=8
    i32.trunc_sat_f64_s
    return
  )
  (func $Double_longValue (export "Double_longValue") (param $this i32) (result i64)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    f64.load offset=8
    i64.trunc_sat_f64_s
    return
  )
  (func $Double_floatValue (export "Double_floatValue") (param $this i32) (result f32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    f64.load offset=8
    f32.demote_f64
    return
  )
  (func $Double_doubleValue (export "Double_doubleValue") (param $this i32) (result f64)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    f64.load offset=8
    return
  )
  (func $Double_equals (export "Double_equals") (param $this i32) (param $other i32) (result i32)
    (local $that i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    (local $__receiver$19 i32)
    (local $__receiver$20 i32)
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
      i32.const 173
      i32.eq
      i32.or
    end
    i32.eqz
    if
    i32.const 0
    return
    end
    local.get $other
    local.set $that
    local.get $this
    f64.load offset=8
    local.get $this
    f64.load offset=8
    f64.ne
    if (result i32)
    local.get $that
    f64.load offset=8
    local.get $that
    f64.load offset=8
    f64.ne
    else
    i32.const 0
    end
    if (result i32)
    i32.const 1
    else
    local.get $this
    f64.load offset=8
    i64.reinterpret_f64
    local.get $that
    f64.load offset=8
    i64.reinterpret_f64
    i64.eq
    end
    return
  )
  (func $Double_hashCode (export "Double_hashCode") (param $this i32) (result i32)
    (local $bits i64)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    (local $__receiver$21 i32)
    (local $__caught_exc i32)
    local.get $this
    f64.load offset=8
    i64.reinterpret_f64
    local.set $bits
    local.get $bits
    local.get $bits
    i32.const 32
    i64.extend_i32_s
    i64.shr_u
    i64.xor
    i32.wrap_i64
    return
  )
  ;; Class/Interface: Float
  (func $Float__init (export "Float__init") (param $this i32) (param $value f32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    call $Number__init
    local.get $this
    local.get $value
    f32.store offset=8
  )
  (func $Float_valueOf__66_6c_6f_61_74_ (export "Float_valueOf__66_6c_6f_61_74_") (param $value f32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    i32.const 189
    i32.store offset=0
    local.get $__obj_alloc_ptr
    local.get $value
    call $Float__init
    local.get $__obj_alloc_ptr
    return
  )
  (func $Float_byteValue (export "Float_byteValue") (param $this i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    f32.load offset=8
    i32.trunc_sat_f32_s
    i32.extend8_s
    return
  )
  (func $Float_shortValue (export "Float_shortValue") (param $this i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    f32.load offset=8
    i32.trunc_sat_f32_s
    i32.extend16_s
    return
  )
  (func $Float_intValue (export "Float_intValue") (param $this i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    f32.load offset=8
    i32.trunc_sat_f32_s
    return
  )
  (func $Float_longValue (export "Float_longValue") (param $this i32) (result i64)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    f32.load offset=8
    i64.trunc_sat_f32_s
    return
  )
  (func $Float_floatValue (export "Float_floatValue") (param $this i32) (result f32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    f32.load offset=8
    return
  )
  (func $Float_doubleValue (export "Float_doubleValue") (param $this i32) (result f64)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    f32.load offset=8
    f64.promote_f32
    return
  )
  (func $Float_equals (export "Float_equals") (param $this i32) (param $other i32) (result i32)
    (local $that i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    (local $__receiver$22 i32)
    (local $__receiver$23 i32)
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
      i32.const 189
      i32.eq
      i32.or
    end
    i32.eqz
    if
    i32.const 0
    return
    end
    local.get $other
    local.set $that
    local.get $this
    f32.load offset=8
    local.get $this
    f32.load offset=8
    f32.ne
    if (result i32)
    local.get $that
    f32.load offset=8
    local.get $that
    f32.load offset=8
    f32.ne
    else
    i32.const 0
    end
    if (result i32)
    i32.const 1
    else
    local.get $this
    f32.load offset=8
    i32.reinterpret_f32
    local.get $that
    f32.load offset=8
    i32.reinterpret_f32
    i32.eq
    end
    return
  )
  (func $Float_hashCode (export "Float_hashCode") (param $this i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    (local $__receiver$24 i32)
    (local $__caught_exc i32)
    local.get $this
    f32.load offset=8
    i32.reinterpret_f32
    return
  )
  ;; Class/Interface: Integer
  (func $Integer__init (export "Integer__init") (param $this i32) (param $value i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    call $Number__init
    local.get $this
    local.get $value
    i32.store offset=8
  )
  (func $Integer_valueOf__69_6e_74_ (export "Integer_valueOf__69_6e_74_") (param $value i32) (result i32)
    (local $index i32)
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
    (local $__caught_exc i32)
    local.get $value
    i32.const 0
    i32.const 128
    i32.sub
    i32.ge_s
    if (result i32)
    local.get $value
    i32.const 127
    i32.le_s
    else
    i32.const 0
    end
    if
    global.get $__cache_Integer
    i32.const 0
    i32.eq
    if
    i32.const 256
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
    global.set $__cache_Integer
    end
    local.get $value
    i32.const 128
    i32.add
    local.set $index
    global.get $__cache_Integer
    local.get $index
    i32.const 4
    i32.mul
    i32.add
    i32.load offset=4
    local.set $result
    local.get $result
    i32.const 0
    i32.eq
    if
    i32.const 16
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 205
    i32.store offset=0
    local.get $__obj_alloc_ptr
    local.get $value
    call $Integer__init
    local.get $__obj_alloc_ptr
    local.set $result
    global.get $__cache_Integer
    local.get $index
    i32.const 4
    i32.mul
    i32.add
    local.get $result
    i32.store offset=4
    end
    local.get $result
    return
    end
    i32.const 16
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 205
    i32.store offset=0
    local.get $__obj_alloc_ptr
    local.get $value
    call $Integer__init
    local.get $__obj_alloc_ptr
    return
  )
  (func $Integer_byteValue (export "Integer_byteValue") (param $this i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    i32.extend8_s
    return
  )
  (func $Integer_shortValue (export "Integer_shortValue") (param $this i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    i32.extend16_s
    return
  )
  (func $Integer_intValue (export "Integer_intValue") (param $this i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
  (func $Integer_longValue (export "Integer_longValue") (param $this i32) (result i64)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    i64.extend_i32_s
    return
  )
  (func $Integer_floatValue (export "Integer_floatValue") (param $this i32) (result f32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    f32.convert_i32_s
    return
  )
  (func $Integer_doubleValue (export "Integer_doubleValue") (param $this i32) (result f64)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    f64.convert_i32_s
    return
  )
  (func $Integer_equals (export "Integer_equals") (param $this i32) (param $other i32) (result i32)
    (local $that i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
      i32.const 205
      i32.eq
      i32.or
    end
    i32.eqz
    if
    i32.const 0
    return
    end
    local.get $other
    local.set $that
    local.get $this
    i32.load offset=8
    local.get $that
    i32.load offset=8
    i32.eq
    return
  )
  (func $Integer_hashCode (export "Integer_hashCode") (param $this i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
  ;; Class/Interface: Long
  (func $Long__init (export "Long__init") (param $this i32) (param $value i64)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    call $Number__init
    local.get $this
    local.get $value
    i64.store offset=8
  )
  (func $Long_valueOf__6c_6f_6e_67_ (export "Long_valueOf__6c_6f_6e_67_") (param $value i64) (result i32)
    (local $index i32)
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
    (local $__caught_exc i32)
    local.get $value
    i32.const 0
    i32.const 128
    i32.sub
    i64.extend_i32_s
    i64.ge_s
    if (result i32)
    local.get $value
    i32.const 127
    i64.extend_i32_s
    i64.le_s
    else
    i32.const 0
    end
    if
    global.get $__cache_Long
    i32.const 0
    i32.eq
    if
    i32.const 256
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
    global.set $__cache_Long
    end
    local.get $value
    i32.wrap_i64
    i32.const 128
    i32.add
    local.set $index
    global.get $__cache_Long
    local.get $index
    i32.const 4
    i32.mul
    i32.add
    i32.load offset=4
    local.set $result
    local.get $result
    i32.const 0
    i32.eq
    if
    i32.const 16
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 221
    i32.store offset=0
    local.get $__obj_alloc_ptr
    local.get $value
    call $Long__init
    local.get $__obj_alloc_ptr
    local.set $result
    global.get $__cache_Long
    local.get $index
    i32.const 4
    i32.mul
    i32.add
    local.get $result
    i32.store offset=4
    end
    local.get $result
    return
    end
    i32.const 16
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 221
    i32.store offset=0
    local.get $__obj_alloc_ptr
    local.get $value
    call $Long__init
    local.get $__obj_alloc_ptr
    return
  )
  (func $Long_byteValue (export "Long_byteValue") (param $this i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    i64.load offset=8
    i32.wrap_i64
    i32.extend8_s
    return
  )
  (func $Long_shortValue (export "Long_shortValue") (param $this i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    i64.load offset=8
    i32.wrap_i64
    i32.extend16_s
    return
  )
  (func $Long_intValue (export "Long_intValue") (param $this i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    i64.load offset=8
    i32.wrap_i64
    return
  )
  (func $Long_longValue (export "Long_longValue") (param $this i32) (result i64)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    i64.load offset=8
    return
  )
  (func $Long_floatValue (export "Long_floatValue") (param $this i32) (result f32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    i64.load offset=8
    f32.convert_i64_s
    return
  )
  (func $Long_doubleValue (export "Long_doubleValue") (param $this i32) (result f64)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    i64.load offset=8
    f64.convert_i64_s
    return
  )
  (func $Long_equals (export "Long_equals") (param $this i32) (param $other i32) (result i32)
    (local $that i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
      i32.const 221
      i32.eq
      i32.or
    end
    i32.eqz
    if
    i32.const 0
    return
    end
    local.get $other
    local.set $that
    local.get $this
    i64.load offset=8
    local.get $that
    i64.load offset=8
    i64.eq
    return
  )
  (func $Long_hashCode (export "Long_hashCode") (param $this i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    i64.load offset=8
    local.get $this
    i64.load offset=8
    i32.const 32
    i64.extend_i32_s
    i64.shr_u
    i64.xor
    i32.wrap_i64
    return
  )
  ;; Class/Interface: Short
  (func $Short__init (export "Short__init") (param $this i32) (param $value i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    call $Number__init
    local.get $this
    local.get $value
    i32.store offset=8
  )
  (func $Short_valueOf__73_68_6f_72_74_ (export "Short_valueOf__73_68_6f_72_74_") (param $value i32) (result i32)
    (local $index i32)
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
    (local $__caught_exc i32)
    local.get $value
    i32.const 0
    i32.const 128
    i32.sub
    i32.ge_s
    if (result i32)
    local.get $value
    i32.const 127
    i32.le_s
    else
    i32.const 0
    end
    if
    global.get $__cache_Short
    i32.const 0
    i32.eq
    if
    i32.const 256
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
    global.set $__cache_Short
    end
    local.get $value
    i32.const 128
    i32.add
    local.set $index
    global.get $__cache_Short
    local.get $index
    i32.const 4
    i32.mul
    i32.add
    i32.load offset=4
    local.set $result
    local.get $result
    i32.const 0
    i32.eq
    if
    i32.const 16
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 237
    i32.store offset=0
    local.get $__obj_alloc_ptr
    local.get $value
    call $Short__init
    local.get $__obj_alloc_ptr
    local.set $result
    global.get $__cache_Short
    local.get $index
    i32.const 4
    i32.mul
    i32.add
    local.get $result
    i32.store offset=4
    end
    local.get $result
    return
    end
    i32.const 16
    call $__heap_alloc
    local.set $__obj_alloc_ptr
    local.get $__obj_alloc_ptr
    i32.const 237
    i32.store offset=0
    local.get $__obj_alloc_ptr
    local.get $value
    call $Short__init
    local.get $__obj_alloc_ptr
    return
  )
  (func $Short_byteValue (export "Short_byteValue") (param $this i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    i32.extend8_s
    return
  )
  (func $Short_shortValue (export "Short_shortValue") (param $this i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    i32.extend16_s
    return
  )
  (func $Short_intValue (export "Short_intValue") (param $this i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
  (func $Short_longValue (export "Short_longValue") (param $this i32) (result i64)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    i64.extend_i32_s
    return
  )
  (func $Short_floatValue (export "Short_floatValue") (param $this i32) (result f32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    f32.convert_i32_s
    return
  )
  (func $Short_doubleValue (export "Short_doubleValue") (param $this i32) (result f64)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
    f64.convert_i32_s
    return
  )
  (func $Short_equals (export "Short_equals") (param $this i32) (param $other i32) (result i32)
    (local $that i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
      i32.const 237
      i32.eq
      i32.or
    end
    i32.eqz
    if
    i32.const 0
    return
    end
    local.get $other
    local.set $that
    local.get $this
    i32.load offset=8
    local.get $that
    i32.load offset=8
    i32.eq
    return
  )
  (func $Short_hashCode (export "Short_hashCode") (param $this i32) (result i32)
    (local $__arr_alloc_ptr i32)
    (local $__arr_alloc_len i32)
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
  (func $__require_nonnull (param $value i32) (result i32) (local $e i32)
    local.get $value
    i32.eqz
    if
    i32.const 8
    call $__heap_alloc
    local.tee $e
    i32.const 275
    i32.store
    local.get $e
    throw $Exception
    end
    local.get $value
  )
  (func $__arithmetic_error (local $e i32)
    i32.const 8
    call $__heap_alloc
    local.tee $e
    i32.const 286
    i32.store
    local.get $e
    throw $Exception
  )
  (func $__java_i32_div (param $x i32) (param $y i32) (result i32)
    local.get $y
    i32.eqz
    if
    call $__arithmetic_error
    end
    local.get $x
    i32.const -2147483648
    i32.eq
    local.get $y
    i32.const -1
    i32.eq
    i32.and
    if
    i32.const -2147483648
    return
    end
    local.get $x
    local.get $y
    i32.div_s
  )
  (func $__java_i32_rem (param $x i32) (param $y i32) (result i32)
    local.get $y
    i32.eqz
    if
    call $__arithmetic_error
    end
    local.get $x
    local.get $y
    i32.rem_s
  )
  (func $__java_i64_div (param $x i64) (param $y i64) (result i64)
    local.get $y
    i64.eqz
    if
    call $__arithmetic_error
    end
    local.get $x
    i64.const -9223372036854775808
    i64.eq
    local.get $y
    i64.const -1
    i64.eq
    i32.and
    if
    i64.const -9223372036854775808
    return
    end
    local.get $x
    local.get $y
    i64.div_s
  )
  (func $__java_i64_rem (param $x i64) (param $y i64) (result i64)
    local.get $y
    i64.eqz
    if
    call $__arithmetic_error
    end
    local.get $x
    local.get $y
    i64.rem_s
  )
  (func $__java_f64_rem (param $x f64) (param $y f64) (result f64)
    (local $a f64) (local $b f64) (local $scaled f64)
    local.get $x
    f64.abs
    local.set $a
    local.get $y
    f64.abs
    local.set $b
    local.get $b
    f64.const 0
    f64.eq
    local.get $a
    f64.const inf
    f64.eq
    i32.or
    local.get $x
    local.get $x
    f64.ne
    i32.or
    local.get $y
    local.get $y
    f64.ne
    i32.or
    if
      f64.const nan
      return
    end
    local.get $a
    local.get $b
    f64.lt
    if
      local.get $x
      return
    end
    local.get $b
    local.set $scaled
    (block $scaled_up
      (loop $scale
        local.get $scaled
        f64.const 2
        f64.mul
        local.get $a
        f64.gt
        br_if $scaled_up
        local.get $scaled
        f64.const 2
        f64.mul
        local.set $scaled
        br $scale
      )
    )
    (block $done
      (loop $subtract
        local.get $a
        local.get $scaled
        f64.ge
        if
          local.get $a
          local.get $scaled
          f64.sub
          local.set $a
        end
        local.get $scaled
        local.get $b
        f64.le
        br_if $done
        local.get $scaled
        f64.const 2
        f64.div
        local.set $scaled
        br $subtract
      )
    )
    local.get $a
    local.get $x
    f64.copysign
  )
  (func $__java_f32_rem (param $x f32) (param $y f32) (result f32)
    local.get $x
    f64.promote_f32
    local.get $y
    f64.promote_f32
    call $__java_f64_rem
    f32.demote_f64
  )
  ;; Default toString() for Product
  (func $Product_toString (export "Product_toString") (param $this i32) (result i32)
    local.get $this
    call $__str_from_i32
  )
  ;; Default equals() for Product
  (func $Product_equals (export "Product_equals") (param $this i32) (param $other i32) (result i32)
    local.get $this
    local.get $other
    i32.eq
  )
  ;; Default hashCode() for Product
  (func $Product_hashCode (export "Product_hashCode") (param $this i32) (result i32)
    local.get $this
  )
  ;; Default run() for Product
  (func $Product_run (export "Product_run") (param $this i32)
  )
  ;; Default getClass() for Product
  (func $Product_getClass (export "Product_getClass") (param $this i32) (result i32)
    global.get $__class_Product
  )
  ;; Default toString() for SpringBootApp
  (func $SpringBootApp_toString (export "SpringBootApp_toString") (param $this i32) (result i32)
    local.get $this
    call $__str_from_i32
  )
  ;; Default equals() for SpringBootApp
  (func $SpringBootApp_equals (export "SpringBootApp_equals") (param $this i32) (param $other i32) (result i32)
    local.get $this
    local.get $other
    i32.eq
  )
  ;; Default hashCode() for SpringBootApp
  (func $SpringBootApp_hashCode (export "SpringBootApp_hashCode") (param $this i32) (result i32)
    local.get $this
  )
  ;; Default run() for SpringBootApp
  (func $SpringBootApp_run (export "SpringBootApp_run") (param $this i32)
  )
  ;; Default getClass() for SpringBootApp
  (func $SpringBootApp_getClass (export "SpringBootApp_getClass") (param $this i32) (result i32)
    global.get $__class_SpringBootApp
  )
  ;; Default toString() for ProductController
  (func $ProductController_toString (export "ProductController_toString") (param $this i32) (result i32)
    local.get $this
    call $__str_from_i32
  )
  ;; Default equals() for ProductController
  (func $ProductController_equals (export "ProductController_equals") (param $this i32) (param $other i32) (result i32)
    local.get $this
    local.get $other
    i32.eq
  )
  ;; Default hashCode() for ProductController
  (func $ProductController_hashCode (export "ProductController_hashCode") (param $this i32) (result i32)
    local.get $this
  )
  ;; Default run() for ProductController
  (func $ProductController_run (export "ProductController_run") (param $this i32)
  )
  ;; Default getClass() for ProductController
  (func $ProductController_getClass (export "ProductController_getClass") (param $this i32) (result i32)
    global.get $__class_ProductController
  )
  ;; Default toString() for ProductRepository
  (func $ProductRepository_toString (export "ProductRepository_toString") (param $this i32) (result i32)
    local.get $this
    call $__str_from_i32
  )
  ;; Default equals() for ProductRepository
  (func $ProductRepository_equals (export "ProductRepository_equals") (param $this i32) (param $other i32) (result i32)
    local.get $this
    local.get $other
    i32.eq
  )
  ;; Default hashCode() for ProductRepository
  (func $ProductRepository_hashCode (export "ProductRepository_hashCode") (param $this i32) (result i32)
    local.get $this
  )
  ;; Default run() for ProductRepository
  (func $ProductRepository_run (export "ProductRepository_run") (param $this i32)
  )
  ;; Default getClass() for ProductRepository
  (func $ProductRepository_getClass (export "ProductRepository_getClass") (param $this i32) (result i32)
    global.get $__class_ProductRepository
  )
  ;; Default toString() for __JavawRow_ProductRepository
  (func $__JavawRow_ProductRepository_toString (export "__JavawRow_ProductRepository_toString") (param $this i32) (result i32)
    local.get $this
    call $__str_from_i32
  )
  ;; Default equals() for __JavawRow_ProductRepository
  (func $__JavawRow_ProductRepository_equals (export "__JavawRow_ProductRepository_equals") (param $this i32) (param $other i32) (result i32)
    local.get $this
    local.get $other
    i32.eq
  )
  ;; Default hashCode() for __JavawRow_ProductRepository
  (func $__JavawRow_ProductRepository_hashCode (export "__JavawRow_ProductRepository_hashCode") (param $this i32) (result i32)
    local.get $this
  )
  ;; Default run() for __JavawRow_ProductRepository
  (func $__JavawRow_ProductRepository_run (export "__JavawRow_ProductRepository_run") (param $this i32)
  )
  ;; Default getClass() for __JavawRow_ProductRepository
  (func $__JavawRow_ProductRepository_getClass (export "__JavawRow_ProductRepository_getClass") (param $this i32) (result i32)
    global.get $__class___JavawRow_ProductRepository
  )
  ;; Default toString() for __JavawSpringContext
  (func $__JavawSpringContext_toString (export "__JavawSpringContext_toString") (param $this i32) (result i32)
    local.get $this
    call $__str_from_i32
  )
  ;; Default equals() for __JavawSpringContext
  (func $__JavawSpringContext_equals (export "__JavawSpringContext_equals") (param $this i32) (param $other i32) (result i32)
    local.get $this
    local.get $other
    i32.eq
  )
  ;; Default hashCode() for __JavawSpringContext
  (func $__JavawSpringContext_hashCode (export "__JavawSpringContext_hashCode") (param $this i32) (result i32)
    local.get $this
  )
  ;; Default run() for __JavawSpringContext
  (func $__JavawSpringContext_run (export "__JavawSpringContext_run") (param $this i32)
  )
  ;; Default getClass() for __JavawSpringContext
  (func $__JavawSpringContext_getClass (export "__JavawSpringContext_getClass") (param $this i32) (result i32)
    global.get $__class___JavawSpringContext
  )
  ;; Default toString() for Number
  (func $Number_toString (export "Number_toString") (param $this i32) (result i32)
    local.get $this
    call $__str_from_i32
  )
  ;; Default equals() for Number
  (func $Number_equals (export "Number_equals") (param $this i32) (param $other i32) (result i32)
    local.get $this
    local.get $other
    i32.eq
  )
  ;; Default hashCode() for Number
  (func $Number_hashCode (export "Number_hashCode") (param $this i32) (result i32)
    local.get $this
  )
  ;; Default run() for Number
  (func $Number_run (export "Number_run") (param $this i32)
  )
  ;; Default getClass() for Number
  (func $Number_getClass (export "Number_getClass") (param $this i32) (result i32)
    global.get $__class_Number
  )
  ;; Default toString() for Boolean
  (func $Boolean_toString (export "Boolean_toString") (param $this i32) (result i32)
    local.get $this
    call $__str_from_i32
  )
  ;; Default run() for Boolean
  (func $Boolean_run (export "Boolean_run") (param $this i32)
  )
  ;; Default getClass() for Boolean
  (func $Boolean_getClass (export "Boolean_getClass") (param $this i32) (result i32)
    global.get $__class_Boolean
  )
  ;; Default toString() for Character
  (func $Character_toString (export "Character_toString") (param $this i32) (result i32)
    local.get $this
    call $__str_from_i32
  )
  ;; Default run() for Character
  (func $Character_run (export "Character_run") (param $this i32)
  )
  ;; Default getClass() for Character
  (func $Character_getClass (export "Character_getClass") (param $this i32) (result i32)
    global.get $__class_Character
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
    i32.const 4211
    call $__str_equals
    if
      global.get $__class_Product
      return
    end
    local.get $name
    i32.const 4219
    call $__str_equals
    if
      global.get $__class_SpringBootApp
      return
    end
    local.get $name
    i32.const 4233
    call $__str_equals
    if
      global.get $__class_ProductController
      return
    end
    local.get $name
    i32.const 4251
    call $__str_equals
    if
      global.get $__class_ProductRepository
      return
    end
    local.get $name
    i32.const 4269
    call $__str_equals
    if
      global.get $__class___JavawRow_ProductRepository
      return
    end
    local.get $name
    i32.const 4298
    call $__str_equals
    if
      global.get $__class_ResponseStatusException
      return
    end
    local.get $name
    i32.const 4322
    call $__str_equals
    if
      global.get $__class___JavawSpringContext
      return
    end
    local.get $name
    i32.const 4343
    call $__str_equals
    if
      global.get $__class_Number
      return
    end
    local.get $name
    i32.const 4350
    call $__str_equals
    if
      global.get $__class_Boolean
      return
    end
    local.get $name
    i32.const 4358
    call $__str_equals
    if
      global.get $__class_Byte
      return
    end
    local.get $name
    i32.const 4363
    call $__str_equals
    if
      global.get $__class_Character
      return
    end
    local.get $name
    i32.const 4373
    call $__str_equals
    if
      global.get $__class_Double
      return
    end
    local.get $name
    i32.const 4380
    call $__str_equals
    if
      global.get $__class_Float
      return
    end
    local.get $name
    i32.const 4386
    call $__str_equals
    if
      global.get $__class_Integer
      return
    end
    local.get $name
    i32.const 4394
    call $__str_equals
    if
      global.get $__class_Long
      return
    end
    local.get $name
    i32.const 4399
    call $__str_equals
    if
      global.get $__class_Short
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
      global.get $__class_ArithmeticException
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
    i32.const 4463
    call $__str_equals
    if
      global.get $__class_Runnable
      return
    end
    local.get $name
    i32.const 4456
    call $__str_equals
    if
      global.get $__class_Thread
      return
    end
    local.get $name
    i32.const 4450
    call $__str_equals
    if
      global.get $__class_Class
      return
    end
    local.get $name
    i32.const 4477
    call $__str_equals
    if
      global.get $__class_int
      return
    end
    local.get $name
    i32.const 4481
    call $__str_equals
    if
      global.get $__class_long
      return
    end
    local.get $name
    i32.const 4486
    call $__str_equals
    if
      global.get $__class_double
      return
    end
    local.get $name
    i32.const 4493
    call $__str_equals
    if
      global.get $__class_float
      return
    end
    local.get $name
    i32.const 4499
    call $__str_equals
    if
      global.get $__class_boolean
      return
    end
    local.get $name
    i32.const 4507
    call $__str_equals
    if
      global.get $__class_char
      return
    end
    local.get $name
    i32.const 4512
    call $__str_equals
    if
      global.get $__class_byte
      return
    end
    local.get $name
    i32.const 4517
    call $__str_equals
    if
      global.get $__class_short
      return
    end
    local.get $name
    i32.const 4523
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
      global.get $__class_Product
      return
    end
    local.get $vtableBase
    i32.const 16
    i32.eq
    if
      global.get $__class_SpringBootApp
      return
    end
    local.get $vtableBase
    i32.const 28
    i32.eq
    if
      global.get $__class_ProductController
      return
    end
    local.get $vtableBase
    i32.const 43
    i32.eq
    if
      global.get $__class_ProductRepository
      return
    end
    local.get $vtableBase
    i32.const 57
    i32.eq
    if
      global.get $__class___JavawRow_ProductRepository
      return
    end
    local.get $vtableBase
    i32.const 100
    i32.eq
    if
      global.get $__class_ResponseStatusException
      return
    end
    local.get $vtableBase
    i32.const 111
    i32.eq
    if
      global.get $__class___JavawSpringContext
      return
    end
    local.get $vtableBase
    i32.const 121
    i32.eq
    if
      global.get $__class_Number
      return
    end
    local.get $vtableBase
    i32.const 135
    i32.eq
    if
      global.get $__class_Boolean
      return
    end
    local.get $vtableBase
    i32.const 146
    i32.eq
    if
      global.get $__class_Byte
      return
    end
    local.get $vtableBase
    i32.const 162
    i32.eq
    if
      global.get $__class_Character
      return
    end
    local.get $vtableBase
    i32.const 173
    i32.eq
    if
      global.get $__class_Double
      return
    end
    local.get $vtableBase
    i32.const 189
    i32.eq
    if
      global.get $__class_Float
      return
    end
    local.get $vtableBase
    i32.const 205
    i32.eq
    if
      global.get $__class_Integer
      return
    end
    local.get $vtableBase
    i32.const 221
    i32.eq
    if
      global.get $__class_Long
      return
    end
    local.get $vtableBase
    i32.const 237
    i32.eq
    if
      global.get $__class_Short
      return
    end
    local.get $vtableBase
    i32.const 67
    i32.eq
    if
      global.get $__class_Throwable
      return
    end
    local.get $vtableBase
    i32.const 78
    i32.eq
    if
      global.get $__class_Exception
      return
    end
    local.get $vtableBase
    i32.const 89
    i32.eq
    if
      global.get $__class_RuntimeException
      return
    end
    local.get $vtableBase
    i32.const 253
    i32.eq
    if
      global.get $__class_IllegalArgumentException
      return
    end
    local.get $vtableBase
    i32.const 264
    i32.eq
    if
      global.get $__class_IllegalStateException
      return
    end
    local.get $vtableBase
    i32.const 275
    i32.eq
    if
      global.get $__class_NullPointerException
      return
    end
    local.get $vtableBase
    i32.const 286
    i32.eq
    if
      global.get $__class_ArithmeticException
      return
    end
    local.get $vtableBase
    i32.const 297
    i32.eq
    if
      global.get $__class_WASIFile
      return
    end
    local.get $vtableBase
    i32.const 307
    i32.eq
    if
      global.get $__class_Thread
      return
    end
    local.get $vtableBase
    i32.const 317
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
    i32.const 307
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
    i32.const 317
    i32.store offset=0
    local.get $inst
    i32.const 4211
    i32.store offset=4
    local.get $inst
    i32.const 4211
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
    global.set $__class_Product
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 317
    i32.store offset=0
    local.get $inst
    i32.const 4219
    i32.store offset=4
    local.get $inst
    i32.const 4219
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
    global.set $__class_SpringBootApp
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 317
    i32.store offset=0
    local.get $inst
    i32.const 4233
    i32.store offset=4
    local.get $inst
    i32.const 4233
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
    global.set $__class_ProductController
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 317
    i32.store offset=0
    local.get $inst
    i32.const 4251
    i32.store offset=4
    local.get $inst
    i32.const 4251
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
    global.set $__class_ProductRepository
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 317
    i32.store offset=0
    local.get $inst
    i32.const 4269
    i32.store offset=4
    local.get $inst
    i32.const 4269
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
    global.set $__class___JavawRow_ProductRepository
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 317
    i32.store offset=0
    local.get $inst
    i32.const 4298
    i32.store offset=4
    local.get $inst
    i32.const 4298
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
    global.set $__class_ResponseStatusException
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 317
    i32.store offset=0
    local.get $inst
    i32.const 4322
    i32.store offset=4
    local.get $inst
    i32.const 4322
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
    global.set $__class___JavawSpringContext
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 317
    i32.store offset=0
    local.get $inst
    i32.const 4343
    i32.store offset=4
    local.get $inst
    i32.const 4343
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
    global.set $__class_Number
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 317
    i32.store offset=0
    local.get $inst
    i32.const 4350
    i32.store offset=4
    local.get $inst
    i32.const 4350
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
    global.set $__class_Boolean
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 317
    i32.store offset=0
    local.get $inst
    i32.const 4358
    i32.store offset=4
    local.get $inst
    i32.const 4358
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
    global.set $__class_Byte
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 317
    i32.store offset=0
    local.get $inst
    i32.const 4363
    i32.store offset=4
    local.get $inst
    i32.const 4363
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
    global.set $__class_Character
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 317
    i32.store offset=0
    local.get $inst
    i32.const 4373
    i32.store offset=4
    local.get $inst
    i32.const 4373
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
    global.set $__class_Double
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 317
    i32.store offset=0
    local.get $inst
    i32.const 4380
    i32.store offset=4
    local.get $inst
    i32.const 4380
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
    global.set $__class_Float
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 317
    i32.store offset=0
    local.get $inst
    i32.const 4386
    i32.store offset=4
    local.get $inst
    i32.const 4386
    i32.store offset=8
    local.get $inst
    i32.const 14
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
    global.set $__class_Integer
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 317
    i32.store offset=0
    local.get $inst
    i32.const 4394
    i32.store offset=4
    local.get $inst
    i32.const 4394
    i32.store offset=8
    local.get $inst
    i32.const 15
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
    global.set $__class_Long
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 317
    i32.store offset=0
    local.get $inst
    i32.const 4399
    i32.store offset=4
    local.get $inst
    i32.const 4399
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
    global.set $__class_Short
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 317
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
    global.set $__class_Throwable
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 317
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
    global.set $__class_Exception
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 317
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
    global.set $__class_RuntimeException
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 317
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
    global.set $__class_IllegalArgumentException
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 317
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
    global.set $__class_IllegalStateException
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 317
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
    global.set $__class_NullPointerException
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 317
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
    i32.const 0
    i32.store offset=16
    local.get $inst
    i32.const 0
    i32.store offset=20
    local.get $inst
    i32.const 0
    i32.store offset=24
    local.get $inst
    global.set $__class_ArithmeticException
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 317
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
    global.set $__class_WASIFile
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 317
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
    global.set $__class_CharSequence
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 317
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
    i32.const 317
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
    global.set $__class_AutoCloseable
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 317
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
    global.set $__class_Comparable
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 317
    i32.store offset=0
    local.get $inst
    i32.const 0
    i32.store offset=4
    local.get $inst
    i32.const 0
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
    global.set $__class_Iterable
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 317
    i32.store offset=0
    local.get $inst
    i32.const 0
    i32.store offset=4
    local.get $inst
    i32.const 0
    i32.store offset=8
    local.get $inst
    i32.const 30
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
    i32.const 317
    i32.store offset=0
    local.get $inst
    i32.const 4463
    i32.store offset=4
    local.get $inst
    i32.const 4463
    i32.store offset=8
    local.get $inst
    i32.const 31
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
    i32.const 317
    i32.store offset=0
    local.get $inst
    i32.const 4456
    i32.store offset=4
    local.get $inst
    i32.const 4456
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
    global.set $__class_Thread
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 317
    i32.store offset=0
    local.get $inst
    i32.const 4450
    i32.store offset=4
    local.get $inst
    i32.const 4450
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
    global.set $__class_Class
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 317
    i32.store offset=0
    local.get $inst
    i32.const 4477
    i32.store offset=4
    local.get $inst
    i32.const 4477
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
    global.set $__class_int
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 317
    i32.store offset=0
    local.get $inst
    i32.const 4481
    i32.store offset=4
    local.get $inst
    i32.const 4481
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
    global.set $__class_long
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 317
    i32.store offset=0
    local.get $inst
    i32.const 4486
    i32.store offset=4
    local.get $inst
    i32.const 4486
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
    global.set $__class_double
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 317
    i32.store offset=0
    local.get $inst
    i32.const 4493
    i32.store offset=4
    local.get $inst
    i32.const 4493
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
    global.set $__class_float
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 317
    i32.store offset=0
    local.get $inst
    i32.const 4499
    i32.store offset=4
    local.get $inst
    i32.const 4499
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
    global.set $__class_boolean
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 317
    i32.store offset=0
    local.get $inst
    i32.const 4507
    i32.store offset=4
    local.get $inst
    i32.const 4507
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
    global.set $__class_char
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 317
    i32.store offset=0
    local.get $inst
    i32.const 4512
    i32.store offset=4
    local.get $inst
    i32.const 4512
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
    global.set $__class_byte
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 317
    i32.store offset=0
    local.get $inst
    i32.const 4517
    i32.store offset=4
    local.get $inst
    i32.const 4517
    i32.store offset=8
    local.get $inst
    i32.const 41
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
    i32.const 317
    i32.store offset=0
    local.get $inst
    i32.const 4523
    i32.store offset=4
    local.get $inst
    i32.const 4523
    i32.store offset=8
    local.get $inst
    i32.const 42
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
    global.get $__class_ResponseStatusException
    global.get $__class_RuntimeException
    i32.store offset=24
    global.get $__class_Byte
    global.get $__class_Number
    i32.store offset=24
    global.get $__class_Double
    global.get $__class_Number
    i32.store offset=24
    global.get $__class_Float
    global.get $__class_Number
    i32.store offset=24
    global.get $__class_Integer
    global.get $__class_Number
    i32.store offset=24
    global.get $__class_Long
    global.get $__class_Number
    i32.store offset=24
    global.get $__class_Short
    global.get $__class_Number
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
    global.get $__class_ArithmeticException
    global.get $__class_RuntimeException
    i32.store offset=24
  )
  (func $__init_enums (export "__init_enums")
  )
  (start $__init_classes)
)
