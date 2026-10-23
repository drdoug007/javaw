(module
  (@custom "javaw_spring_web" "{\22routes\22:[{\22verb\22:\22GET\22,\22path\22:\22/api/products\22,\22controller\22:\22ProductController\22,\22export\22:\22ProductController_handleGetAll\22,\22result\22:\22Product[]\22,\22params\22:[]},{\22verb\22:\22GET\22,\22path\22:\22/api/products/{id}\22,\22controller\22:\22ProductController\22,\22export\22:\22ProductController_handleGetById\22,\22result\22:\22Product\22,\22params\22:[{\22name\22:\22id\22,\22type\22:\22int\22,\22path\22:true}]},{\22verb\22:\22POST\22,\22path\22:\22/api/products\22,\22controller\22:\22ProductController\22,\22export\22:\22ProductController_handleCreate\22,\22result\22:\22Product\22,\22params\22:[{\22name\22:\22name\22,\22type\22:\22String\22,\22path\22:false},{\22name\22:\22price\22,\22type\22:\22BigDecimal\22,\22path\22:false},{\22name\22:\22stock\22,\22type\22:\22int\22,\22path\22:false}]},{\22verb\22:\22PUT\22,\22path\22:\22/api/products/{id}\22,\22controller\22:\22ProductController\22,\22export\22:\22ProductController_handleUpdate\22,\22result\22:\22Product\22,\22params\22:[{\22name\22:\22id\22,\22type\22:\22int\22,\22path\22:true},{\22name\22:\22name\22,\22type\22:\22String\22,\22path\22:false},{\22name\22:\22price\22,\22type\22:\22BigDecimal\22,\22path\22:false},{\22name\22:\22stock\22,\22type\22:\22int\22,\22path\22:false}]},{\22verb\22:\22DELETE\22,\22path\22:\22/api/products/{id}\22,\22controller\22:\22ProductController\22,\22export\22:\22ProductController_handleDelete\22,\22result\22:\22void\22,\22params\22:[{\22name\22:\22id\22,\22type\22:\22int\22,\22path\22:true}]}],\22entities\22:{\22Product\22:[{\22name\22:\22id\22,\22type\22:\22Integer\22,\22offset\22:4,\22boxedOffset\22:8},{\22name\22:\22name\22,\22type\22:\22String\22,\22offset\22:8},{\22name\22:\22price\22,\22type\22:\22BigDecimal\22,\22offset\22:12},{\22name\22:\22stock\22,\22type\22:\22int\22,\22offset\22:16}]},\22boxInteger\22:\22Integer_valueOf__69_6e_74_\22,\22exception\22:{\22vtable\22:340,\22status\22:8,\22reason\22:12}}")
  (@custom "sourceMappingURL" "\16SpringBootApp.wasm.map")
  (import "dom" "remove" (func $dom_remove (param i32)))
  (import "dom" "save" (func $dom_save (param i32)))
  (import "dom" "js_get" (func $dom_js_get (param i32 i32 i32) (result i32)))
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
  (data (i32.const 4105) "/api/products\00")
  (data (i32.const 4119) "/{id}\00")
  (data (i32.const 4125) "Product not found with id \00")
  (data (i32.const 4152) "MacBook Pro M3\00")
  (data (i32.const 4167) "1999\00")
  (data (i32.const 4172) "Dell XPS 15\00")
  (data (i32.const 4184) "11499\00")
  (data (i32.const 4190) "Sony WH-1000XM5\00")
  (data (i32.const 4206) "1399\00")
  (data (i32.const 4211) "Product\00")
  (data (i32.const 4219) "SpringBootApp\00")
  (data (i32.const 4233) "ProductController\00")
  (data (i32.const 4251) "ProductRepository\00")
  (data (i32.const 4269) "__JavawRow_ProductRepository\00")
  (data (i32.const 4298) "ResponseStatusException\00")
  (data (i32.const 4322) "ApplicationRunner\00")
  (data (i32.const 4340) "__JavawSpringContext\00")
  (data (i32.const 4361) "Supplier\00")
  (data (i32.const 4370) "Function\00")
  (data (i32.const 4379) "Predicate\00")
  (data (i32.const 4389) "Consumer\00")
  (data (i32.const 4398) "UnaryOperator\00")
  (data (i32.const 4412) "BinaryOperator\00")
  (data (i32.const 4427) "Optional\00")
  (data (i32.const 4436) "Iterable\00")
  (data (i32.const 4445) "Iterator\00")
  (data (i32.const 4454) "Collection\00")
  (data (i32.const 4465) "List\00")
  (data (i32.const 4470) "Set\00")
  (data (i32.const 4474) "Map\00")
  (data (i32.const 4478) "__JavawObjects\00")
  (data (i32.const 4493) "ArrayList\00")
  (data (i32.const 4503) "__JavawListIterator\00")
  (data (i32.const 4523) "__JavawMapEntry\00")
  (data (i32.const 4539) "HashMap\00")
  (data (i32.const 4547) "__JavawKeyIterator\00")
  (data (i32.const 4566) "__JavawKeySet\00")
  (data (i32.const 4580) "HashSet\00")
  (data (i32.const 4588) "Comparator\00")
  (data (i32.const 4599) "__JavawArrayIterator\00")
  (data (i32.const 4620) "__JavawGenerateIterator\00")
  (data (i32.const 4644) "__JavawIterateIterator\00")
  (data (i32.const 4667) "__JavawReadOnlyList\00")
  (data (i32.const 4687) "Collector\00")
  (data (i32.const 4697) "__JavawListCollector\00")
  (data (i32.const 4718) "__JavawSetCollector\00")
  (data (i32.const 4738) "Collectors\00")
  (data (i32.const 4749) "Stream\00")
  (data (i32.const 4756) "Number\00")
  (data (i32.const 4763) "Boolean\00")
  (data (i32.const 4771) "Byte\00")
  (data (i32.const 4776) "Character\00")
  (data (i32.const 4786) "Double\00")
  (data (i32.const 4793) "Float\00")
  (data (i32.const 4799) "Integer\00")
  (data (i32.const 4807) "Long\00")
  (data (i32.const 4812) "Short\00")
  (data (i32.const 4818) "__JavawLambda0\00")
  (data (i32.const 4833) "@\00")
  (data (i32.const 4835) "true\00")
  (data (i32.const 4840) "false\00")
  (data (i32.const 4846) "class \00")
  (data (i32.const 4853) "interface \00")
  (data (i32.const 4864) "Object\00")
  (data (i32.const 4871) "String\00")
  (data (i32.const 4878) "Class\00")
  (data (i32.const 4884) "Thread\00")
  (data (i32.const 4891) "Runnable\00")
  (data (i32.const 4900) "Enum\00")
  (data (i32.const 4905) "int\00")
  (data (i32.const 4909) "long\00")
  (data (i32.const 4914) "double\00")
  (data (i32.const 4921) "float\00")
  (data (i32.const 4927) "boolean\00")
  (data (i32.const 4935) "char\00")
  (data (i32.const 4940) "byte\00")
  (data (i32.const 4945) "short\00")
  (data (i32.const 4951) "void\00")
  (global $__heap_ptr (mut i32) (i32.const 65536))
  (global $__class_Product (mut i32) (i32.const 0))
  (global $__class_SpringBootApp (mut i32) (i32.const 0))
  (global $__class_ProductController (mut i32) (i32.const 0))
  (global $__class_ProductRepository (mut i32) (i32.const 0))
  (global $__class___JavawRow_ProductRepository (mut i32) (i32.const 0))
  (global $__class_ResponseStatusException (mut i32) (i32.const 0))
  (global $__class_ApplicationRunner (mut i32) (i32.const 0))
  (global $__class___JavawSpringContext (mut i32) (i32.const 0))
  (global $__class_Supplier (mut i32) (i32.const 0))
  (global $__class_Function (mut i32) (i32.const 0))
  (global $__class_Predicate (mut i32) (i32.const 0))
  (global $__class_Consumer (mut i32) (i32.const 0))
  (global $__class_UnaryOperator (mut i32) (i32.const 0))
  (global $__class_BinaryOperator (mut i32) (i32.const 0))
  (global $__class_Optional (mut i32) (i32.const 0))
  (global $__class_Iterable (mut i32) (i32.const 0))
  (global $__class_Iterator (mut i32) (i32.const 0))
  (global $__class_Collection (mut i32) (i32.const 0))
  (global $__class_List (mut i32) (i32.const 0))
  (global $__class_Set (mut i32) (i32.const 0))
  (global $__class_Map (mut i32) (i32.const 0))
  (global $__class___JavawObjects (mut i32) (i32.const 0))
  (global $__class_ArrayList (mut i32) (i32.const 0))
  (global $__class___JavawListIterator (mut i32) (i32.const 0))
  (global $__class___JavawMapEntry (mut i32) (i32.const 0))
  (global $__class_HashMap (mut i32) (i32.const 0))
  (global $__class___JavawKeyIterator (mut i32) (i32.const 0))
  (global $__class___JavawKeySet (mut i32) (i32.const 0))
  (global $__class_HashSet (mut i32) (i32.const 0))
  (global $__class_Comparator (mut i32) (i32.const 0))
  (global $__class___JavawArrayIterator (mut i32) (i32.const 0))
  (global $__class___JavawGenerateIterator (mut i32) (i32.const 0))
  (global $__class___JavawIterateIterator (mut i32) (i32.const 0))
  (global $__class___JavawReadOnlyList (mut i32) (i32.const 0))
  (global $__class_Collector (mut i32) (i32.const 0))
  (global $__class___JavawListCollector (mut i32) (i32.const 0))
  (global $__class___JavawSetCollector (mut i32) (i32.const 0))
  (global $__class_Collectors (mut i32) (i32.const 0))
  (global $__class_Stream (mut i32) (i32.const 0))
  (global $__class_Number (mut i32) (i32.const 0))
  (global $__class_Boolean (mut i32) (i32.const 0))
  (global $__class_Byte (mut i32) (i32.const 0))
  (global $__class_Character (mut i32) (i32.const 0))
  (global $__class_Double (mut i32) (i32.const 0))
  (global $__class_Float (mut i32) (i32.const 0))
  (global $__class_Integer (mut i32) (i32.const 0))
  (global $__class_Long (mut i32) (i32.const 0))
  (global $__class_Short (mut i32) (i32.const 0))
  (global $__class___JavawLambda0 (mut i32) (i32.const 0))
  (global $__class_Throwable (mut i32) (i32.const 0))
  (global $__class_Exception (mut i32) (i32.const 0))
  (global $__class_RuntimeException (mut i32) (i32.const 0))
  (global $__class_IllegalArgumentException (mut i32) (i32.const 0))
  (global $__class_IllegalStateException (mut i32) (i32.const 0))
  (global $__class_NullPointerException (mut i32) (i32.const 0))
  (global $__class_NoSuchElementException (mut i32) (i32.const 0))
  (global $__class_IndexOutOfBoundsException (mut i32) (i32.const 0))
  (global $__class_ConcurrentModificationException (mut i32) (i32.const 0))
  (global $__class_UnsupportedOperationException (mut i32) (i32.const 0))
  (global $__class_ArithmeticException (mut i32) (i32.const 0))
  (global $__class_WASIFile (mut i32) (i32.const 0))
  (global $__class_CharSequence (mut i32) (i32.const 0))
  (global $__class_StringBuilder (mut i32) (i32.const 0))
  (global $__class_AutoCloseable (mut i32) (i32.const 0))
  (global $__class_Comparable (mut i32) (i32.const 0))
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
  (type $t_sig_1 (func (param i32 i32)))
  (type $t_sig_2 (func (param i32) (result i32)))
  (type $t_sig_3 (func (param i32 i32) (result i32)))
  (type $t_sig_4 (func (param i32 i32 i32) (result i32)))
  (type $t_sig_5 (func (param i32)))
  (type $t_sig_6 (func (param i32 i32 i32 i32) (result i32)))
  (type $t_sig_7 (func (param i32 i32 i32 i32 i32) (result i32)))
  (type $t_sig_8 (func (param i32 i64) (result i32)))
  (type $t_sig_9 (func (param i32) (result i64)))
  (type $t_sig_10 (func (param i32) (result f32)))
  (type $t_sig_11 (func (param i32) (result f64)))
  ;; --- WebAssembly Function Table (VTable) ---
  (table (export "__vtable") 1950 funcref)
  (elem (i32.const 0) $Product_toString $Product_equals $Product_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Product_run $Product_getClass $ApplicationRunner_run $Supplier_get $Function_apply $Predicate_test $Consumer_accept $UnaryOperator_apply $BinaryOperator_apply $Iterable_iterator $Iterator_hasNext $Iterator_next $Collection_size $Collection_isEmpty $Collection_add $Collection_contains $Collection_remove $Collection_clear $Collection_stream $List_get $List_set $List_remove $Map_size $Map_isEmpty $Map_put $Map_get $Map_containsKey $Map_remove $Map_clear $Map_keySet $Comparator_compare $Collector_collect $Product_getName $Product_setName $Product_getPrice $Product_setPrice $Product_getStock $Product_setStock $SpringBootApp_toString $SpringBootApp_equals $SpringBootApp_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $SpringBootApp_run $SpringBootApp_getClass $ApplicationRunner_run $Supplier_get $Function_apply $Predicate_test $Consumer_accept $UnaryOperator_apply $BinaryOperator_apply $Iterable_iterator $Iterator_hasNext $Iterator_next $Collection_size $Collection_isEmpty $Collection_add $Collection_contains $Collection_remove $Collection_clear $Collection_stream $List_get $List_set $List_remove $Map_size $Map_isEmpty $Map_put $Map_get $Map_containsKey $Map_remove $Map_clear $Map_keySet $Comparator_compare $Collector_collect $SpringBootApp_init $SpringBootApp___javaw_runner_SpringBootApp_init $ProductController_toString $ProductController_equals $ProductController_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $ProductController_run $ProductController_getClass $ApplicationRunner_run $Supplier_get $Function_apply $Predicate_test $Consumer_accept $UnaryOperator_apply $BinaryOperator_apply $Iterable_iterator $Iterator_hasNext $Iterator_next $Collection_size $Collection_isEmpty $Collection_add $Collection_contains $Collection_remove $Collection_clear $Collection_stream $List_get $List_set $List_remove $Map_size $Map_isEmpty $Map_put $Map_get $Map_containsKey $Map_remove $Map_clear $Map_keySet $Comparator_compare $Collector_collect $ProductController_handleGetAll $ProductController_handleGetById $ProductController_handleCreate $ProductController_handleUpdate $ProductController_handleDelete $ProductRepository_toString $ProductRepository_equals $ProductRepository_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $ProductRepository_run $ProductRepository_getClass $ApplicationRunner_run $Supplier_get $Function_apply $Predicate_test $Consumer_accept $UnaryOperator_apply $BinaryOperator_apply $Iterable_iterator $Iterator_hasNext $Iterator_next $Collection_size $Collection_isEmpty $Collection_add $Collection_contains $Collection_remove $Collection_clear $Collection_stream $List_get $List_set $List_remove $Map_size $Map_isEmpty $Map_put $Map_get $Map_containsKey $Map_remove $Map_clear $Map_keySet $Comparator_compare $Collector_collect $ProductRepository_findById $ProductRepository_findAll $ProductRepository_save $ProductRepository_deleteById $__JavawRow_ProductRepository_toString $__JavawRow_ProductRepository_equals $__JavawRow_ProductRepository_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $__JavawRow_ProductRepository_run $__JavawRow_ProductRepository_getClass $ApplicationRunner_run $Supplier_get $Function_apply $Predicate_test $Consumer_accept $UnaryOperator_apply $BinaryOperator_apply $Iterable_iterator $Iterator_hasNext $Iterator_next $Collection_size $Collection_isEmpty $Collection_add $Collection_contains $Collection_remove $Collection_clear $Collection_stream $List_get $List_set $List_remove $Map_size $Map_isEmpty $Map_put $Map_get $Map_containsKey $Map_remove $Map_clear $Map_keySet $Comparator_compare $Collector_collect $Throwable_toString $Throwable_equals $Throwable_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Throwable_run $Throwable_getClass $ApplicationRunner_run $Supplier_get $Function_apply $Predicate_test $Consumer_accept $UnaryOperator_apply $BinaryOperator_apply $Iterable_iterator $Iterator_hasNext $Iterator_next $Collection_size $Collection_isEmpty $Collection_add $Collection_contains $Collection_remove $Collection_clear $Collection_stream $List_get $List_set $List_remove $Map_size $Map_isEmpty $Map_put $Map_get $Map_containsKey $Map_remove $Map_clear $Map_keySet $Comparator_compare $Collector_collect $Throwable_getMessage $Throwable_toString $Throwable_equals $Throwable_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Throwable_run $Throwable_getClass $ApplicationRunner_run $Supplier_get $Function_apply $Predicate_test $Consumer_accept $UnaryOperator_apply $BinaryOperator_apply $Iterable_iterator $Iterator_hasNext $Iterator_next $Collection_size $Collection_isEmpty $Collection_add $Collection_contains $Collection_remove $Collection_clear $Collection_stream $List_get $List_set $List_remove $Map_size $Map_isEmpty $Map_put $Map_get $Map_containsKey $Map_remove $Map_clear $Map_keySet $Comparator_compare $Collector_collect $Exception_getMessage $Throwable_toString $Throwable_equals $Throwable_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Throwable_run $Throwable_getClass $ApplicationRunner_run $Supplier_get $Function_apply $Predicate_test $Consumer_accept $UnaryOperator_apply $BinaryOperator_apply $Iterable_iterator $Iterator_hasNext $Iterator_next $Collection_size $Collection_isEmpty $Collection_add $Collection_contains $Collection_remove $Collection_clear $Collection_stream $List_get $List_set $List_remove $Map_size $Map_isEmpty $Map_put $Map_get $Map_containsKey $Map_remove $Map_clear $Map_keySet $Comparator_compare $Collector_collect $RuntimeException_getMessage $Throwable_toString $Throwable_equals $Throwable_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Throwable_run $Throwable_getClass $ApplicationRunner_run $Supplier_get $Function_apply $Predicate_test $Consumer_accept $UnaryOperator_apply $BinaryOperator_apply $Iterable_iterator $Iterator_hasNext $Iterator_next $Collection_size $Collection_isEmpty $Collection_add $Collection_contains $Collection_remove $Collection_clear $Collection_stream $List_get $List_set $List_remove $Map_size $Map_isEmpty $Map_put $Map_get $Map_containsKey $Map_remove $Map_clear $Map_keySet $Comparator_compare $Collector_collect $RuntimeException_getMessage $__JavawSpringContext_toString $__JavawSpringContext_equals $__JavawSpringContext_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $__JavawSpringContext_run $__JavawSpringContext_getClass $ApplicationRunner_run $Supplier_get $Function_apply $Predicate_test $Consumer_accept $UnaryOperator_apply $BinaryOperator_apply $Iterable_iterator $Iterator_hasNext $Iterator_next $Collection_size $Collection_isEmpty $Collection_add $Collection_contains $Collection_remove $Collection_clear $Collection_stream $List_get $List_set $List_remove $Map_size $Map_isEmpty $Map_put $Map_get $Map_containsKey $Map_remove $Map_clear $Map_keySet $Comparator_compare $Collector_collect $Optional_toString $Optional_equals $Optional_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Optional_run $Optional_getClass $ApplicationRunner_run $Optional_get $Function_apply $Predicate_test $Consumer_accept $UnaryOperator_apply $BinaryOperator_apply $Iterable_iterator $Iterator_hasNext $Iterator_next $Collection_size $Optional_isEmpty $Collection_add $Collection_contains $Collection_remove $Collection_clear $Collection_stream $List_get $List_set $List_remove $Map_size $Optional_isEmpty $Map_put $Map_get $Map_containsKey $Map_remove $Map_clear $Map_keySet $Comparator_compare $Collector_collect $Optional_isPresent $Optional_orElse $Optional_orElseGet $Optional_orElseThrow__noargs $Optional_orElseThrow__53_75_70_70_6c_69_65_72_3c_52_75_6e_74_69_6d_65_45_78_63_65_70_74_69_6f_6e_3e_ $Optional_map $Optional_flatMap $Optional_filter $Optional_ifPresent $ArrayList_toString $ArrayList_equals $ArrayList_hashCode $AutoCloseable_close $Comparable_compareTo $ArrayList_iterator $Iterator_hasNext $Iterator_next $ArrayList_run $ArrayList_getClass $ApplicationRunner_run $Supplier_get $Function_apply $Predicate_test $Consumer_accept $UnaryOperator_apply $BinaryOperator_apply $ArrayList_iterator $Iterator_hasNext $Iterator_next $ArrayList_size $ArrayList_isEmpty $ArrayList_add $ArrayList_contains $ArrayList_remove__4f_62_6a_65_63_74_ $ArrayList_clear $ArrayList_stream $ArrayList_get $ArrayList_set $ArrayList_remove__69_6e_74_ $ArrayList_size $ArrayList_isEmpty $Map_put $Map_get $Map_containsKey $ArrayList_remove__4f_62_6a_65_63_74_ $ArrayList_clear $Map_keySet $Comparator_compare $Collector_collect $ArrayList_indexOf $ArrayList_toArray $__JavawListIterator_toString $__JavawListIterator_equals $__JavawListIterator_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $__JavawListIterator_hasNext $__JavawListIterator_next $__JavawListIterator_run $__JavawListIterator_getClass $ApplicationRunner_run $Supplier_get $Function_apply $Predicate_test $Consumer_accept $UnaryOperator_apply $BinaryOperator_apply $Iterable_iterator $__JavawListIterator_hasNext $__JavawListIterator_next $Collection_size $Collection_isEmpty $Collection_add $Collection_contains $Collection_remove $Collection_clear $Collection_stream $List_get $List_set $List_remove $Map_size $Map_isEmpty $Map_put $Map_get $Map_containsKey $Map_remove $Map_clear $Map_keySet $Comparator_compare $Collector_collect $__JavawMapEntry_toString $__JavawMapEntry_equals $__JavawMapEntry_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $__JavawMapEntry_run $__JavawMapEntry_getClass $ApplicationRunner_run $Supplier_get $Function_apply $Predicate_test $Consumer_accept $UnaryOperator_apply $BinaryOperator_apply $Iterable_iterator $Iterator_hasNext $Iterator_next $Collection_size $Collection_isEmpty $Collection_add $Collection_contains $Collection_remove $Collection_clear $Collection_stream $List_get $List_set $List_remove $Map_size $Map_isEmpty $Map_put $Map_get $Map_containsKey $Map_remove $Map_clear $Map_keySet $Comparator_compare $Collector_collect $HashMap_toString $HashMap_equals $HashMap_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $HashMap_run $HashMap_getClass $ApplicationRunner_run $Supplier_get $Function_apply $Predicate_test $Consumer_accept $UnaryOperator_apply $BinaryOperator_apply $Iterable_iterator $Iterator_hasNext $Iterator_next $HashMap_size $HashMap_isEmpty $Collection_add $Collection_contains $HashMap_remove $HashMap_clear $Collection_stream $List_get $List_set $List_remove $HashMap_size $HashMap_isEmpty $HashMap_put $HashMap_get $HashMap_containsKey $HashMap_remove $HashMap_clear $HashMap_keySet $Comparator_compare $Collector_collect $HashMap_keyIterator $__JavawKeyIterator_toString $__JavawKeyIterator_equals $__JavawKeyIterator_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $__JavawKeyIterator_hasNext $__JavawKeyIterator_next $__JavawKeyIterator_run $__JavawKeyIterator_getClass $ApplicationRunner_run $Supplier_get $Function_apply $Predicate_test $Consumer_accept $UnaryOperator_apply $BinaryOperator_apply $Iterable_iterator $__JavawKeyIterator_hasNext $__JavawKeyIterator_next $Collection_size $Collection_isEmpty $Collection_add $Collection_contains $Collection_remove $Collection_clear $Collection_stream $List_get $List_set $List_remove $Map_size $Map_isEmpty $Map_put $Map_get $Map_containsKey $Map_remove $Map_clear $Map_keySet $Comparator_compare $Collector_collect $__JavawKeySet_toString $__JavawKeySet_equals $__JavawKeySet_hashCode $AutoCloseable_close $Comparable_compareTo $__JavawKeySet_iterator $Iterator_hasNext $Iterator_next $__JavawKeySet_run $__JavawKeySet_getClass $ApplicationRunner_run $Supplier_get $Function_apply $Predicate_test $Consumer_accept $UnaryOperator_apply $BinaryOperator_apply $__JavawKeySet_iterator $Iterator_hasNext $Iterator_next $__JavawKeySet_size $__JavawKeySet_isEmpty $__JavawKeySet_add $__JavawKeySet_contains $__JavawKeySet_remove $__JavawKeySet_clear $__JavawKeySet_stream $List_get $List_set $List_remove $__JavawKeySet_size $__JavawKeySet_isEmpty $Map_put $Map_get $Map_containsKey $__JavawKeySet_remove $__JavawKeySet_clear $Map_keySet $Comparator_compare $Collector_collect $HashSet_toString $HashSet_equals $HashSet_hashCode $AutoCloseable_close $Comparable_compareTo $HashSet_iterator $Iterator_hasNext $Iterator_next $HashSet_run $HashSet_getClass $ApplicationRunner_run $Supplier_get $Function_apply $Predicate_test $Consumer_accept $UnaryOperator_apply $BinaryOperator_apply $HashSet_iterator $Iterator_hasNext $Iterator_next $HashSet_size $HashSet_isEmpty $HashSet_add $HashSet_contains $HashSet_remove $HashSet_clear $HashSet_stream $List_get $List_set $List_remove $HashSet_size $HashSet_isEmpty $Map_put $Map_get $Map_containsKey $HashSet_remove $HashSet_clear $Map_keySet $Comparator_compare $Collector_collect $__JavawArrayIterator_toString $__JavawArrayIterator_equals $__JavawArrayIterator_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $__JavawArrayIterator_hasNext $__JavawArrayIterator_next $__JavawArrayIterator_run $__JavawArrayIterator_getClass $ApplicationRunner_run $Supplier_get $Function_apply $Predicate_test $Consumer_accept $UnaryOperator_apply $BinaryOperator_apply $Iterable_iterator $__JavawArrayIterator_hasNext $__JavawArrayIterator_next $Collection_size $Collection_isEmpty $Collection_add $Collection_contains $Collection_remove $Collection_clear $Collection_stream $List_get $List_set $List_remove $Map_size $Map_isEmpty $Map_put $Map_get $Map_containsKey $Map_remove $Map_clear $Map_keySet $Comparator_compare $Collector_collect $__JavawGenerateIterator_toString $__JavawGenerateIterator_equals $__JavawGenerateIterator_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $__JavawGenerateIterator_hasNext $__JavawGenerateIterator_next $__JavawGenerateIterator_run $__JavawGenerateIterator_getClass $ApplicationRunner_run $Supplier_get $Function_apply $Predicate_test $Consumer_accept $UnaryOperator_apply $BinaryOperator_apply $Iterable_iterator $__JavawGenerateIterator_hasNext $__JavawGenerateIterator_next $Collection_size $Collection_isEmpty $Collection_add $Collection_contains $Collection_remove $Collection_clear $Collection_stream $List_get $List_set $List_remove $Map_size $Map_isEmpty $Map_put $Map_get $Map_containsKey $Map_remove $Map_clear $Map_keySet $Comparator_compare $Collector_collect $__JavawIterateIterator_toString $__JavawIterateIterator_equals $__JavawIterateIterator_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $__JavawIterateIterator_hasNext $__JavawIterateIterator_next $__JavawIterateIterator_run $__JavawIterateIterator_getClass $ApplicationRunner_run $Supplier_get $Function_apply $Predicate_test $Consumer_accept $UnaryOperator_apply $BinaryOperator_apply $Iterable_iterator $__JavawIterateIterator_hasNext $__JavawIterateIterator_next $Collection_size $Collection_isEmpty $Collection_add $Collection_contains $Collection_remove $Collection_clear $Collection_stream $List_get $List_set $List_remove $Map_size $Map_isEmpty $Map_put $Map_get $Map_containsKey $Map_remove $Map_clear $Map_keySet $Comparator_compare $Collector_collect $ArrayList_toString $ArrayList_equals $ArrayList_hashCode $AutoCloseable_close $Comparable_compareTo $ArrayList_iterator $Iterator_hasNext $Iterator_next $ArrayList_run $ArrayList_getClass $ApplicationRunner_run $Supplier_get $Function_apply $Predicate_test $Consumer_accept $UnaryOperator_apply $BinaryOperator_apply $ArrayList_iterator $Iterator_hasNext $Iterator_next $ArrayList_size $ArrayList_isEmpty $__JavawReadOnlyList_add $ArrayList_contains $__JavawReadOnlyList_remove__4f_62_6a_65_63_74_ $__JavawReadOnlyList_clear $ArrayList_stream $ArrayList_get $__JavawReadOnlyList_set $__JavawReadOnlyList_remove__69_6e_74_ $ArrayList_size $ArrayList_isEmpty $Map_put $Map_get $Map_containsKey $__JavawReadOnlyList_remove__4f_62_6a_65_63_74_ $__JavawReadOnlyList_clear $Map_keySet $Comparator_compare $Collector_collect $ArrayList_indexOf $ArrayList_toArray $__JavawListCollector_toString $__JavawListCollector_equals $__JavawListCollector_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $__JavawListCollector_run $__JavawListCollector_getClass $ApplicationRunner_run $Supplier_get $Function_apply $Predicate_test $Consumer_accept $UnaryOperator_apply $BinaryOperator_apply $Iterable_iterator $Iterator_hasNext $Iterator_next $Collection_size $Collection_isEmpty $Collection_add $Collection_contains $Collection_remove $Collection_clear $Collection_stream $List_get $List_set $List_remove $Map_size $Map_isEmpty $Map_put $Map_get $Map_containsKey $Map_remove $Map_clear $Map_keySet $Comparator_compare $__JavawListCollector_collect $__JavawSetCollector_toString $__JavawSetCollector_equals $__JavawSetCollector_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $__JavawSetCollector_run $__JavawSetCollector_getClass $ApplicationRunner_run $Supplier_get $Function_apply $Predicate_test $Consumer_accept $UnaryOperator_apply $BinaryOperator_apply $Iterable_iterator $Iterator_hasNext $Iterator_next $Collection_size $Collection_isEmpty $Collection_add $Collection_contains $Collection_remove $Collection_clear $Collection_stream $List_get $List_set $List_remove $Map_size $Map_isEmpty $Map_put $Map_get $Map_containsKey $Map_remove $Map_clear $Map_keySet $Comparator_compare $__JavawSetCollector_collect $Stream_toString $Stream_equals $Stream_hashCode $Stream_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Stream_run $Stream_getClass $ApplicationRunner_run $Supplier_get $Function_apply $Predicate_test $Consumer_accept $UnaryOperator_apply $BinaryOperator_apply $Iterable_iterator $Iterator_hasNext $Iterator_next $Collection_size $Collection_isEmpty $Collection_add $Collection_contains $Collection_remove $Collection_clear $Collection_stream $List_get $List_set $List_remove $Map_size $Map_isEmpty $Map_put $Map_get $Map_containsKey $Map_remove $Map_clear $Map_keySet $Comparator_compare $Collector_collect $Stream_map $Stream_filter $Stream_peek $Stream_limit $Stream_skip $Stream_distinct $Stream_flatMap $Stream_count $Stream_findFirst $Stream_findAny $Stream_anyMatch $Stream_allMatch $Stream_noneMatch $Stream_forEach $Stream_forEachOrdered $Stream_reduce__4f_62_6a_65_63_74___42_69_6e_61_72_79_4f_70_65_72_61_74_6f_72_3c_4f_62_6a_65_63_74_3e_ $Stream_reduce__42_69_6e_61_72_79_4f_70_65_72_61_74_6f_72_3c_4f_62_6a_65_63_74_3e_ $Stream_min $Stream_max $Stream_toArray $Stream_collect $Stream_toList $Stream_isParallel $Stream_sequential $Stream_parallel $Number_toString $Number_equals $Number_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Number_run $Number_getClass $ApplicationRunner_run $Supplier_get $Function_apply $Predicate_test $Consumer_accept $UnaryOperator_apply $BinaryOperator_apply $Iterable_iterator $Iterator_hasNext $Iterator_next $Collection_size $Collection_isEmpty $Collection_add $Collection_contains $Collection_remove $Collection_clear $Collection_stream $List_get $List_set $List_remove $Map_size $Map_isEmpty $Map_put $Map_get $Map_containsKey $Map_remove $Map_clear $Map_keySet $Comparator_compare $Collector_collect $Number_intValue $Number_longValue $Number_floatValue $Number_doubleValue $Boolean_toString $Boolean_equals $Boolean_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Boolean_run $Boolean_getClass $ApplicationRunner_run $Supplier_get $Function_apply $Predicate_test $Consumer_accept $UnaryOperator_apply $BinaryOperator_apply $Iterable_iterator $Iterator_hasNext $Iterator_next $Collection_size $Collection_isEmpty $Collection_add $Collection_contains $Collection_remove $Collection_clear $Collection_stream $List_get $List_set $List_remove $Map_size $Map_isEmpty $Map_put $Map_get $Map_containsKey $Map_remove $Map_clear $Map_keySet $Comparator_compare $Collector_collect $Boolean_booleanValue $Number_toString $Byte_equals $Byte_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Number_run $Number_getClass $ApplicationRunner_run $Supplier_get $Function_apply $Predicate_test $Consumer_accept $UnaryOperator_apply $BinaryOperator_apply $Iterable_iterator $Iterator_hasNext $Iterator_next $Collection_size $Collection_isEmpty $Collection_add $Collection_contains $Collection_remove $Collection_clear $Collection_stream $List_get $List_set $List_remove $Map_size $Map_isEmpty $Map_put $Map_get $Map_containsKey $Map_remove $Map_clear $Map_keySet $Comparator_compare $Collector_collect $Byte_intValue $Byte_longValue $Byte_floatValue $Byte_doubleValue $Byte_byteValue $Byte_shortValue $Character_toString $Character_equals $Character_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Character_run $Character_getClass $ApplicationRunner_run $Supplier_get $Function_apply $Predicate_test $Consumer_accept $UnaryOperator_apply $BinaryOperator_apply $Iterable_iterator $Iterator_hasNext $Iterator_next $Collection_size $Collection_isEmpty $Collection_add $Collection_contains $Collection_remove $Collection_clear $Collection_stream $List_get $List_set $List_remove $Map_size $Map_isEmpty $Map_put $Map_get $Map_containsKey $Map_remove $Map_clear $Map_keySet $Comparator_compare $Collector_collect $Character_charValue $Number_toString $Double_equals $Double_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Number_run $Number_getClass $ApplicationRunner_run $Supplier_get $Function_apply $Predicate_test $Consumer_accept $UnaryOperator_apply $BinaryOperator_apply $Iterable_iterator $Iterator_hasNext $Iterator_next $Collection_size $Collection_isEmpty $Collection_add $Collection_contains $Collection_remove $Collection_clear $Collection_stream $List_get $List_set $List_remove $Map_size $Map_isEmpty $Map_put $Map_get $Map_containsKey $Map_remove $Map_clear $Map_keySet $Comparator_compare $Collector_collect $Double_intValue $Double_longValue $Double_floatValue $Double_doubleValue $Double_byteValue $Double_shortValue $Number_toString $Float_equals $Float_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Number_run $Number_getClass $ApplicationRunner_run $Supplier_get $Function_apply $Predicate_test $Consumer_accept $UnaryOperator_apply $BinaryOperator_apply $Iterable_iterator $Iterator_hasNext $Iterator_next $Collection_size $Collection_isEmpty $Collection_add $Collection_contains $Collection_remove $Collection_clear $Collection_stream $List_get $List_set $List_remove $Map_size $Map_isEmpty $Map_put $Map_get $Map_containsKey $Map_remove $Map_clear $Map_keySet $Comparator_compare $Collector_collect $Float_intValue $Float_longValue $Float_floatValue $Float_doubleValue $Float_byteValue $Float_shortValue $Number_toString $Integer_equals $Integer_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Number_run $Number_getClass $ApplicationRunner_run $Supplier_get $Function_apply $Predicate_test $Consumer_accept $UnaryOperator_apply $BinaryOperator_apply $Iterable_iterator $Iterator_hasNext $Iterator_next $Collection_size $Collection_isEmpty $Collection_add $Collection_contains $Collection_remove $Collection_clear $Collection_stream $List_get $List_set $List_remove $Map_size $Map_isEmpty $Map_put $Map_get $Map_containsKey $Map_remove $Map_clear $Map_keySet $Comparator_compare $Collector_collect $Integer_intValue $Integer_longValue $Integer_floatValue $Integer_doubleValue $Integer_byteValue $Integer_shortValue $Number_toString $Long_equals $Long_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Number_run $Number_getClass $ApplicationRunner_run $Supplier_get $Function_apply $Predicate_test $Consumer_accept $UnaryOperator_apply $BinaryOperator_apply $Iterable_iterator $Iterator_hasNext $Iterator_next $Collection_size $Collection_isEmpty $Collection_add $Collection_contains $Collection_remove $Collection_clear $Collection_stream $List_get $List_set $List_remove $Map_size $Map_isEmpty $Map_put $Map_get $Map_containsKey $Map_remove $Map_clear $Map_keySet $Comparator_compare $Collector_collect $Long_intValue $Long_longValue $Long_floatValue $Long_doubleValue $Long_byteValue $Long_shortValue $Number_toString $Short_equals $Short_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Number_run $Number_getClass $ApplicationRunner_run $Supplier_get $Function_apply $Predicate_test $Consumer_accept $UnaryOperator_apply $BinaryOperator_apply $Iterable_iterator $Iterator_hasNext $Iterator_next $Collection_size $Collection_isEmpty $Collection_add $Collection_contains $Collection_remove $Collection_clear $Collection_stream $List_get $List_set $List_remove $Map_size $Map_isEmpty $Map_put $Map_get $Map_containsKey $Map_remove $Map_clear $Map_keySet $Comparator_compare $Collector_collect $Short_intValue $Short_longValue $Short_floatValue $Short_doubleValue $Short_byteValue $Short_shortValue $__JavawLambda0_toString $__JavawLambda0_equals $__JavawLambda0_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $__JavawLambda0_run $__JavawLambda0_getClass $__JavawLambda0_run $Supplier_get $Function_apply $Predicate_test $Consumer_accept $UnaryOperator_apply $BinaryOperator_apply $Iterable_iterator $Iterator_hasNext $Iterator_next $Collection_size $Collection_isEmpty $Collection_add $Collection_contains $Collection_remove $Collection_clear $Collection_stream $List_get $List_set $List_remove $Map_size $Map_isEmpty $Map_put $Map_get $Map_containsKey $Map_remove $Map_clear $Map_keySet $Comparator_compare $Collector_collect $Throwable_toString $Throwable_equals $Throwable_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Throwable_run $Throwable_getClass $ApplicationRunner_run $Supplier_get $Function_apply $Predicate_test $Consumer_accept $UnaryOperator_apply $BinaryOperator_apply $Iterable_iterator $Iterator_hasNext $Iterator_next $Collection_size $Collection_isEmpty $Collection_add $Collection_contains $Collection_remove $Collection_clear $Collection_stream $List_get $List_set $List_remove $Map_size $Map_isEmpty $Map_put $Map_get $Map_containsKey $Map_remove $Map_clear $Map_keySet $Comparator_compare $Collector_collect $IllegalArgumentException_getMessage $Throwable_toString $Throwable_equals $Throwable_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Throwable_run $Throwable_getClass $ApplicationRunner_run $Supplier_get $Function_apply $Predicate_test $Consumer_accept $UnaryOperator_apply $BinaryOperator_apply $Iterable_iterator $Iterator_hasNext $Iterator_next $Collection_size $Collection_isEmpty $Collection_add $Collection_contains $Collection_remove $Collection_clear $Collection_stream $List_get $List_set $List_remove $Map_size $Map_isEmpty $Map_put $Map_get $Map_containsKey $Map_remove $Map_clear $Map_keySet $Comparator_compare $Collector_collect $IllegalStateException_getMessage $Throwable_toString $Throwable_equals $Throwable_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Throwable_run $Throwable_getClass $ApplicationRunner_run $Supplier_get $Function_apply $Predicate_test $Consumer_accept $UnaryOperator_apply $BinaryOperator_apply $Iterable_iterator $Iterator_hasNext $Iterator_next $Collection_size $Collection_isEmpty $Collection_add $Collection_contains $Collection_remove $Collection_clear $Collection_stream $List_get $List_set $List_remove $Map_size $Map_isEmpty $Map_put $Map_get $Map_containsKey $Map_remove $Map_clear $Map_keySet $Comparator_compare $Collector_collect $NullPointerException_getMessage $Throwable_toString $Throwable_equals $Throwable_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Throwable_run $Throwable_getClass $ApplicationRunner_run $Supplier_get $Function_apply $Predicate_test $Consumer_accept $UnaryOperator_apply $BinaryOperator_apply $Iterable_iterator $Iterator_hasNext $Iterator_next $Collection_size $Collection_isEmpty $Collection_add $Collection_contains $Collection_remove $Collection_clear $Collection_stream $List_get $List_set $List_remove $Map_size $Map_isEmpty $Map_put $Map_get $Map_containsKey $Map_remove $Map_clear $Map_keySet $Comparator_compare $Collector_collect $NoSuchElementException_getMessage $Throwable_toString $Throwable_equals $Throwable_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Throwable_run $Throwable_getClass $ApplicationRunner_run $Supplier_get $Function_apply $Predicate_test $Consumer_accept $UnaryOperator_apply $BinaryOperator_apply $Iterable_iterator $Iterator_hasNext $Iterator_next $Collection_size $Collection_isEmpty $Collection_add $Collection_contains $Collection_remove $Collection_clear $Collection_stream $List_get $List_set $List_remove $Map_size $Map_isEmpty $Map_put $Map_get $Map_containsKey $Map_remove $Map_clear $Map_keySet $Comparator_compare $Collector_collect $IndexOutOfBoundsException_getMessage $Throwable_toString $Throwable_equals $Throwable_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Throwable_run $Throwable_getClass $ApplicationRunner_run $Supplier_get $Function_apply $Predicate_test $Consumer_accept $UnaryOperator_apply $BinaryOperator_apply $Iterable_iterator $Iterator_hasNext $Iterator_next $Collection_size $Collection_isEmpty $Collection_add $Collection_contains $Collection_remove $Collection_clear $Collection_stream $List_get $List_set $List_remove $Map_size $Map_isEmpty $Map_put $Map_get $Map_containsKey $Map_remove $Map_clear $Map_keySet $Comparator_compare $Collector_collect $ConcurrentModificationException_getMessage $Throwable_toString $Throwable_equals $Throwable_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Throwable_run $Throwable_getClass $ApplicationRunner_run $Supplier_get $Function_apply $Predicate_test $Consumer_accept $UnaryOperator_apply $BinaryOperator_apply $Iterable_iterator $Iterator_hasNext $Iterator_next $Collection_size $Collection_isEmpty $Collection_add $Collection_contains $Collection_remove $Collection_clear $Collection_stream $List_get $List_set $List_remove $Map_size $Map_isEmpty $Map_put $Map_get $Map_containsKey $Map_remove $Map_clear $Map_keySet $Comparator_compare $Collector_collect $UnsupportedOperationException_getMessage $Throwable_toString $Throwable_equals $Throwable_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Throwable_run $Throwable_getClass $ApplicationRunner_run $Supplier_get $Function_apply $Predicate_test $Consumer_accept $UnaryOperator_apply $BinaryOperator_apply $Iterable_iterator $Iterator_hasNext $Iterator_next $Collection_size $Collection_isEmpty $Collection_add $Collection_contains $Collection_remove $Collection_clear $Collection_stream $List_get $List_set $List_remove $Map_size $Map_isEmpty $Map_put $Map_get $Map_containsKey $Map_remove $Map_clear $Map_keySet $Comparator_compare $Collector_collect $ArithmeticException_getMessage $WASIFile_toString $WASIFile_equals $WASIFile_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $WASIFile_run $WASIFile_getClass $ApplicationRunner_run $Supplier_get $Function_apply $Predicate_test $Consumer_accept $UnaryOperator_apply $BinaryOperator_apply $Iterable_iterator $Iterator_hasNext $Iterator_next $Collection_size $Collection_isEmpty $Collection_add $Collection_contains $Collection_remove $Collection_clear $Collection_stream $List_get $List_set $List_remove $Map_size $Map_isEmpty $Map_put $Map_get $Map_containsKey $Map_remove $Map_clear $Map_keySet $Comparator_compare $Collector_collect $Thread_toString $Thread_equals $Thread_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Thread_run $Thread_getClass $ApplicationRunner_run $Supplier_get $Function_apply $Predicate_test $Consumer_accept $UnaryOperator_apply $BinaryOperator_apply $Iterable_iterator $Iterator_hasNext $Iterator_next $Collection_size $Collection_isEmpty $Collection_add $Collection_contains $Collection_remove $Collection_clear $Collection_stream $List_get $List_set $List_remove $Map_size $Map_isEmpty $Map_put $Map_get $Map_containsKey $Map_remove $Map_clear $Map_keySet $Comparator_compare $Collector_collect $Class_toString $Class_equals $Class_hashCode $AutoCloseable_close $Comparable_compareTo $Iterable_iterator $Iterator_hasNext $Iterator_next $Class_run $Class_getClass $ApplicationRunner_run $Supplier_get $Function_apply $Predicate_test $Consumer_accept $UnaryOperator_apply $BinaryOperator_apply $Iterable_iterator $Iterator_hasNext $Iterator_next $Collection_size $Collection_isEmpty $Collection_add $Collection_contains $Collection_remove $Collection_clear $Collection_stream $List_get $List_set $List_remove $Map_size $Map_isEmpty $Map_put $Map_get $Map_containsKey $Map_remove $Map_clear $Map_keySet $Comparator_compare $Collector_collect)
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
      i32.const 4835
    else
      i32.const 4840
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
  (func $NoSuchElementException__init (export "NoSuchElementException__init") (param $this i32)
    local.get $this
    call $RuntimeException__init
  )
  (func $NoSuchElementException__init_1 (export "NoSuchElementException__init_1") (param $this i32) (param $message i32)
    local.get $this
    local.get $message
    call $RuntimeException__init_1
  )
  (func $NoSuchElementException_getMessage (export "NoSuchElementException_getMessage") (param $this i32) (result i32)
    local.get $this
    call $RuntimeException_getMessage
  )
  (func $IndexOutOfBoundsException__init (export "IndexOutOfBoundsException__init") (param $this i32)
    local.get $this
    call $RuntimeException__init
  )
  (func $IndexOutOfBoundsException__init_1 (export "IndexOutOfBoundsException__init_1") (param $this i32) (param $message i32)
    local.get $this
    local.get $message
    call $RuntimeException__init_1
  )
  (func $IndexOutOfBoundsException_getMessage (export "IndexOutOfBoundsException_getMessage") (param $this i32) (result i32)
    local.get $this
    call $RuntimeException_getMessage
  )
  (func $ConcurrentModificationException__init (export "ConcurrentModificationException__init") (param $this i32)
    local.get $this
    call $RuntimeException__init
  )
  (func $ConcurrentModificationException__init_1 (export "ConcurrentModificationException__init_1") (param $this i32) (param $message i32)
    local.get $this
    local.get $message
    call $RuntimeException__init_1
  )
  (func $ConcurrentModificationException_getMessage (export "ConcurrentModificationException_getMessage") (param $this i32) (result i32)
    local.get $this
    call $RuntimeException_getMessage
  )
  (func $UnsupportedOperationException__init (export "UnsupportedOperationException__init") (param $this i32)
    local.get $this
    call $RuntimeException__init
  )
  (func $UnsupportedOperationException__init_1 (export "UnsupportedOperationException__init_1") (param $this i32) (param $message i32)
    local.get $this
    local.get $message
    call $RuntimeException__init_1
  )
  (func $UnsupportedOperationException_getMessage (export "UnsupportedOperationException_getMessage") (param $this i32) (result i32)
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
    (local $__allocation$0 i32)
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
    call $__javaw_object_alloc
    local.set $__allocation$0
    local.get $__allocation$0
    i32.const 177
    i32.store offset=0
    local.get $__allocation$0
    local.get $value
    i32.const 0
    call $__JavawRow_ProductRepository__init
    local.get $__allocation$0
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
  (func $SpringBootApp_init (export "SpringBootApp_init") (param $this i32) (result i32)
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
    (local $__allocation$1 i32)
    (local $__caught_exc i32)
    i32.const 8
    call $__javaw_object_alloc
    local.set $__allocation$1
    local.get $__allocation$1
    i32.const 1462
    i32.store offset=0
    local.get $__allocation$1
    local.get $this
    call $__JavawLambda0__init
    local.get $__allocation$1
    return
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
    (local $__allocation$2 i32)
    (local $__receiver$1 i32)
    (local $__caught_exc i32)
    i32.const 24
    call $__javaw_object_alloc
    local.set $__allocation$2
    local.get $__allocation$2
    i32.const 0
    i32.store offset=0
    local.get $__allocation$2
    i32.const 0
    local.get $name
    local.get $price
    local.get $stock
    call $Product__init_1
    local.get $__allocation$2
    local.set $product
    local.get $this
    i32.load offset=4
    call $__require_nonnull
    local.tee $__receiver$1
    local.get $product
    local.get $__receiver$1
    i32.load offset=0
    i32.const 42
    i32.add
    call_indirect (type $t_sig_3)
    drop
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
    (local $__receiver$2 i32)
    (local $__receiver$3 i32)
    (local $__caught_exc i32)
    local.get $this
    call $__require_nonnull
    local.tee $__receiver$3
    local.get $__receiver$3
    i32.load offset=0
    i32.const 40
    i32.add
    call_indirect (type $t_sig_2)
    call $__require_nonnull
    local.tee $__receiver$2
    local.get $args
    local.get $__receiver$2
    i32.load offset=0
    i32.const 10
    i32.add
    call_indirect (type $t_sig_1)
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
    (local $__receiver$4 i32)
    (local $__caught_exc i32)
    local.get $this
    i32.load offset=4
    call $__require_nonnull
    local.tee $__receiver$4
    local.get $__receiver$4
    i32.load offset=0
    i32.const 41
    i32.add
    call_indirect (type $t_sig_2)
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
    (local $__receiver$5 i32)
    (local $__receiver$6 i32)
    (local $__caught_exc i32)
    local.get $this
    i32.load offset=4
    call $__require_nonnull
    local.tee $__receiver$6
    local.get $id
    local.get $__receiver$6
    i32.load offset=0
    i32.const 40
    i32.add
    call_indirect (type $t_sig_3)
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
    (local $__allocation$3 i32)
    (local $__receiver$7 i32)
    (local $__caught_exc i32)
    i32.const 24
    call $__javaw_object_alloc
    local.set $__allocation$3
    local.get $__allocation$3
    i32.const 0
    i32.store offset=0
    local.get $__allocation$3
    i32.const 0
    local.get $name
    local.get $price
    local.get $stock
    call $Product__init_1
    local.get $__allocation$3
    local.set $product
    local.get $this
    i32.load offset=4
    call $__require_nonnull
    local.tee $__receiver$7
    local.get $product
    local.get $__receiver$7
    i32.load offset=0
    i32.const 42
    i32.add
    call_indirect (type $t_sig_3)
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
    (local $__receiver$8 i32)
    (local $__receiver$9 i32)
    (local $__receiver$10 i32)
    (local $__receiver$11 i32)
    (local $__receiver$12 i32)
    (local $__receiver$13 i32)
    (local $__caught_exc i32)
    local.get $this
    i32.load offset=4
    call $__require_nonnull
    local.tee $__receiver$9
    local.get $id
    local.get $__receiver$9
    i32.load offset=0
    i32.const 40
    i32.add
    call_indirect (type $t_sig_3)
    local.get $id
    local.get $name
    local.get $price
    local.get $stock
    call $__javaw_require_1
    local.set $product
    local.get $product
    call $__require_nonnull
    local.tee $__receiver$10
    local.get $name
    local.get $__receiver$10
    i32.load offset=0
    i32.const 41
    i32.add
    call_indirect (type $t_sig_1)
    local.get $product
    call $__require_nonnull
    local.tee $__receiver$11
    local.get $price
    local.get $__receiver$11
    i32.load offset=0
    i32.const 43
    i32.add
    call_indirect (type $t_sig_1)
    local.get $product
    call $__require_nonnull
    local.tee $__receiver$12
    local.get $stock
    local.get $__receiver$12
    i32.load offset=0
    i32.const 45
    i32.add
    call_indirect (type $t_sig_1)
    local.get $this
    i32.load offset=4
    call $__require_nonnull
    local.tee $__receiver$13
    local.get $product
    local.get $__receiver$13
    i32.load offset=0
    i32.const 42
    i32.add
    call_indirect (type $t_sig_3)
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
    (local $__receiver$14 i32)
    (local $__receiver$15 i32)
    (local $__receiver$16 i32)
    (local $__caught_exc i32)
    local.get $this
    i32.load offset=4
    call $__require_nonnull
    local.tee $__receiver$15
    local.get $id
    local.get $__receiver$15
    i32.load offset=0
    i32.const 40
    i32.add
    call_indirect (type $t_sig_3)
    local.get $id
    call $__javaw_require_2
    drop
    local.get $this
    i32.load offset=4
    call $__require_nonnull
    local.tee $__receiver$16
    local.get $id
    local.get $__receiver$16
    i32.load offset=0
    i32.const 43
    i32.add
    call_indirect (type $t_sig_1)
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
    (local $__allocation$4 i32)
    (local $__caught_exc i32)
    local.get $__value
    i32.const 0
    i32.eq
    if
    i32.const 16
    call $__javaw_object_alloc
    local.set $__allocation$4
    local.get $__allocation$4
    i32.const 340
    i32.store offset=0
    local.get $__allocation$4
    i32.const 404
    i32.const 4125
    local.get $id
    call $__str_from_i32
    call $__str_concat
    call $ResponseStatusException__init
    local.get $__allocation$4
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
    (local $__allocation$5 i32)
    (local $__caught_exc i32)
    local.get $__value
    i32.const 0
    i32.eq
    if
    i32.const 16
    call $__javaw_object_alloc
    local.set $__allocation$5
    local.get $__allocation$5
    i32.const 340
    i32.store offset=0
    local.get $__allocation$5
    i32.const 404
    i32.const 4125
    local.get $id
    call $__str_from_i32
    call $__str_concat
    call $ResponseStatusException__init
    local.get $__allocation$5
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
    (local $__allocation$6 i32)
    (local $__caught_exc i32)
    local.get $__value
    i32.const 0
    i32.eq
    if
    i32.const 16
    call $__javaw_object_alloc
    local.set $__allocation$6
    local.get $__allocation$6
    i32.const 340
    i32.store offset=0
    local.get $__allocation$6
    i32.const 404
    i32.const 4125
    local.get $id
    call $__str_from_i32
    call $__str_concat
    call $ResponseStatusException__init
    local.get $__allocation$6
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
  ;; Class/Interface: ApplicationRunner
  (func $ApplicationRunner_run (export "ApplicationRunner_run") (param $this i32) (param $args i32)
    unreachable
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
    (local $__allocation$7 i32)
    (local $__allocation$8 i32)
    (local $__allocation$9 i32)
    (local $__receiver$17 i32)
    (local $__caught_exc i32)
    global.get $__repo_ProductRepository
    i32.const 0
    i32.eq
    if
    i32.const 16
    call $__javaw_object_alloc
    local.set $__allocation$7
    local.get $__allocation$7
    i32.const 133
    i32.store offset=0
    local.get $__allocation$7
    call $ProductRepository__init
    local.get $__allocation$7
    global.set $__repo_ProductRepository
    end
    global.get $__controller_ProductController
    i32.const 0
    i32.eq
    if
    i32.const 8
    call $__javaw_object_alloc
    local.set $__allocation$8
    local.get $__allocation$8
    i32.const 88
    i32.store offset=0
    local.get $__allocation$8
    global.get $__repo_ProductRepository
    call $ProductController__init
    local.get $__allocation$8
    global.set $__controller_ProductController
    end
    global.get $__application_SpringBootApp
    i32.const 0
    i32.eq
    if
    i32.const 8
    call $__javaw_object_alloc
    local.set $__allocation$9
    local.get $__allocation$9
    i32.const 46
    i32.store offset=0
    local.get $__allocation$9
    global.get $__repo_ProductRepository
    call $SpringBootApp__init
    local.get $__allocation$9
    global.set $__application_SpringBootApp
    global.get $__application_SpringBootApp
    call $__require_nonnull
    local.tee $__receiver$17
    i32.const 0
    local.get $__receiver$17
    i32.load offset=0
    i32.const 41
    i32.add
    call_indirect (type $t_sig_1)
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
  ;; Class/Interface: Supplier
  (func $Supplier_get (export "Supplier_get") (param $this i32) (result i32)
    unreachable
  )
  ;; Class/Interface: Function
  (func $Function_apply (export "Function_apply") (param $this i32) (param $value i32) (result i32)
    unreachable
  )
  ;; Class/Interface: Predicate
  (func $Predicate_test (export "Predicate_test") (param $this i32) (param $value i32) (result i32)
    unreachable
  )
  ;; Class/Interface: Consumer
  (func $Consumer_accept (export "Consumer_accept") (param $this i32) (param $value i32)
    unreachable
  )
  ;; Class/Interface: UnaryOperator
  (func $UnaryOperator_apply (export "UnaryOperator_apply") (param $this i32) (param $value i32) (result i32)
    unreachable
  )
  ;; Class/Interface: BinaryOperator
  (func $BinaryOperator_apply (export "BinaryOperator_apply") (param $this i32) (param $left i32) (param $right i32) (result i32)
    unreachable
  )
  ;; Class/Interface: Optional
  (func $Optional__init (export "Optional__init") (param $this i32) (param $value i32)
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
  )
  (func $Optional_empty (export "Optional_empty") (result i32)
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
    (local $__allocation$10 i32)
    (local $__caught_exc i32)
    i32.const 8
    call $__javaw_object_alloc
    local.set $__allocation$10
    local.get $__allocation$10
    i32.const 421
    i32.store offset=0
    local.get $__allocation$10
    i32.const 0
    call $Optional__init
    local.get $__allocation$10
    return
  )
  (func $Optional_of (export "Optional_of") (param $value i32) (result i32)
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
    (local $__allocation$11 i32)
    (local $__allocation$12 i32)
    (local $__caught_exc i32)
    local.get $value
    i32.const 0
    i32.eq
    if
    i32.const 8
    call $__javaw_object_alloc
    local.set $__allocation$11
    local.get $__allocation$11
    i32.const 1584
    i32.store offset=0
    local.get $__allocation$11
    call $NullPointerException__init
    local.get $__allocation$11
    throw $Exception
    end
    i32.const 8
    call $__javaw_object_alloc
    local.set $__allocation$12
    local.get $__allocation$12
    i32.const 421
    i32.store offset=0
    local.get $__allocation$12
    local.get $value
    call $Optional__init
    local.get $__allocation$12
    return
  )
  (func $Optional_ofNullable (export "Optional_ofNullable") (param $value i32) (result i32)
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
    (local $__allocation$13 i32)
    (local $__caught_exc i32)
    i32.const 8
    call $__javaw_object_alloc
    local.set $__allocation$13
    local.get $__allocation$13
    i32.const 421
    i32.store offset=0
    local.get $__allocation$13
    local.get $value
    call $Optional__init
    local.get $__allocation$13
    return
  )
  (func $Optional_isPresent (export "Optional_isPresent") (param $this i32) (result i32)
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
    i32.const 0
    i32.ne
    return
  )
  (func $Optional_isEmpty (export "Optional_isEmpty") (param $this i32) (result i32)
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
    i32.const 0
    i32.eq
    return
  )
  (func $Optional_get (export "Optional_get") (param $this i32) (result i32)
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
    (local $__allocation$14 i32)
    (local $__caught_exc i32)
    local.get $this
    i32.load offset=4
    i32.const 0
    i32.eq
    if
    i32.const 8
    call $__javaw_object_alloc
    local.set $__allocation$14
    local.get $__allocation$14
    i32.const 1625
    i32.store offset=0
    local.get $__allocation$14
    call $NoSuchElementException__init
    local.get $__allocation$14
    throw $Exception
    end
    local.get $this
    i32.load offset=4
    return
  )
  (func $Optional_orElse (export "Optional_orElse") (param $this i32) (param $fallback i32) (result i32)
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
    i32.const 0
    i32.eq
    if (result i32)
    local.get $fallback
    else
    local.get $this
    i32.load offset=4
    end
    return
  )
  (func $Optional_orElseGet (export "Optional_orElseGet") (param $this i32) (param $supplier i32) (result i32)
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
    local.get $this
    i32.load offset=4
    i32.const 0
    i32.eq
    if (result i32)
    local.get $supplier
    call $__require_nonnull
    local.tee $__receiver$18
    local.get $__receiver$18
    i32.load offset=0
    i32.const 11
    i32.add
    call_indirect (type $t_sig_2)
    else
    local.get $this
    i32.load offset=4
    end
    return
  )
  (func $Optional_orElseThrow__noargs (export "Optional_orElseThrow__noargs") (param $this i32) (result i32)
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
    (local $__caught_exc i32)
    local.get $this
    call $__require_nonnull
    local.tee $__receiver$19
    local.get $__receiver$19
    i32.load offset=0
    i32.const 11
    i32.add
    call_indirect (type $t_sig_2)
    return
  )
  (func $Optional_orElseThrow__53_75_70_70_6c_69_65_72_3c_52_75_6e_74_69_6d_65_45_78_63_65_70_74_69_6f_6e_3e_ (export "Optional_orElseThrow__53_75_70_70_6c_69_65_72_3c_52_75_6e_74_69_6d_65_45_78_63_65_70_74_69_6f_6e_3e_") (param $this i32) (param $supplier i32) (result i32)
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
    (local $__receiver$20 i32)
    (local $__caught_exc i32)
    local.get $this
    i32.load offset=4
    i32.const 0
    i32.eq
    if
    local.get $supplier
    call $__require_nonnull
    local.tee $__receiver$20
    local.get $__receiver$20
    i32.load offset=0
    i32.const 11
    i32.add
    call_indirect (type $t_sig_2)
    throw $Exception
    end
    local.get $this
    i32.load offset=4
    return
  )
  (func $Optional_map (export "Optional_map") (param $this i32) (param $mapper i32) (result i32)
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
    (local $__allocation$15 i32)
    (local $__receiver$21 i32)
    (local $__receiver$22 i32)
    (local $__receiver$23 i32)
    (local $__caught_exc i32)
    local.get $mapper
    i32.const 0
    i32.eq
    if
    i32.const 8
    call $__javaw_object_alloc
    local.set $__allocation$15
    local.get $__allocation$15
    i32.const 1584
    i32.store offset=0
    local.get $__allocation$15
    call $NullPointerException__init
    local.get $__allocation$15
    throw $Exception
    end
    local.get $this
    i32.load offset=4
    i32.const 0
    i32.eq
    if
    call $Optional_empty
    return
    end
    local.get $mapper
    call $__require_nonnull
    local.tee $__receiver$23
    local.get $this
    i32.load offset=4
    local.get $__receiver$23
    i32.load offset=0
    i32.const 12
    i32.add
    call_indirect (type $t_sig_3)
    call $Optional_ofNullable
    return
  )
  (func $Optional_flatMap (export "Optional_flatMap") (param $this i32) (param $mapper i32) (result i32)
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
    (local $__allocation$16 i32)
    (local $__allocation$17 i32)
    (local $__receiver$24 i32)
    (local $__receiver$25 i32)
    (local $__caught_exc i32)
    local.get $mapper
    i32.const 0
    i32.eq
    if
    i32.const 8
    call $__javaw_object_alloc
    local.set $__allocation$16
    local.get $__allocation$16
    i32.const 1584
    i32.store offset=0
    local.get $__allocation$16
    call $NullPointerException__init
    local.get $__allocation$16
    throw $Exception
    end
    local.get $this
    i32.load offset=4
    i32.const 0
    i32.eq
    if
    call $Optional_empty
    return
    end
    local.get $mapper
    call $__require_nonnull
    local.tee $__receiver$25
    local.get $this
    i32.load offset=4
    local.get $__receiver$25
    i32.load offset=0
    i32.const 12
    i32.add
    call_indirect (type $t_sig_3)
    local.set $result
    local.get $result
    i32.const 0
    i32.eq
    if
    i32.const 8
    call $__javaw_object_alloc
    local.set $__allocation$17
    local.get $__allocation$17
    i32.const 1584
    i32.store offset=0
    local.get $__allocation$17
    call $NullPointerException__init
    local.get $__allocation$17
    throw $Exception
    end
    local.get $result
    return
  )
  (func $Optional_filter (export "Optional_filter") (param $this i32) (param $predicate i32) (result i32)
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
    (local $__allocation$18 i32)
    (local $__receiver$26 i32)
    (local $__receiver$27 i32)
    (local $__caught_exc i32)
    local.get $predicate
    i32.const 0
    i32.eq
    if
    i32.const 8
    call $__javaw_object_alloc
    local.set $__allocation$18
    local.get $__allocation$18
    i32.const 1584
    i32.store offset=0
    local.get $__allocation$18
    call $NullPointerException__init
    local.get $__allocation$18
    throw $Exception
    end
    local.get $this
    i32.load offset=4
    i32.const 0
    i32.eq
    if (result i32)
    i32.const 1
    else
    local.get $predicate
    call $__require_nonnull
    local.tee $__receiver$26
    local.get $this
    i32.load offset=4
    local.get $__receiver$26
    i32.load offset=0
    i32.const 13
    i32.add
    call_indirect (type $t_sig_3)
    end
    if
    local.get $this
    return
    end
    call $Optional_empty
    return
  )
  (func $Optional_ifPresent (export "Optional_ifPresent") (param $this i32) (param $action i32)
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
    (local $__receiver$28 i32)
    (local $__caught_exc i32)
    local.get $this
    i32.load offset=4
    i32.const 0
    i32.ne
    if
    local.get $action
    call $__require_nonnull
    local.tee $__receiver$28
    local.get $this
    i32.load offset=4
    local.get $__receiver$28
    i32.load offset=0
    i32.const 14
    i32.add
    call_indirect (type $t_sig_1)
    end
  )
  ;; Class/Interface: Iterable
  (func $__source_Iterable_iterator (export "__source_Iterable_iterator") (param $this i32) (result i32)
    unreachable
  )
  ;; Class/Interface: Iterator
  (func $__source_Iterator_hasNext (export "__source_Iterator_hasNext") (param $this i32) (result i32)
    unreachable
  )
  (func $__source_Iterator_next (export "__source_Iterator_next") (param $this i32) (result i32)
    unreachable
  )
  ;; Class/Interface: Collection
  (func $Collection_size (export "Collection_size") (param $this i32) (result i32)
    unreachable
  )
  (func $Collection_isEmpty (export "Collection_isEmpty") (param $this i32) (result i32)
    unreachable
  )
  (func $Collection_add (export "Collection_add") (param $this i32) (param $value i32) (result i32)
    unreachable
  )
  (func $Collection_contains (export "Collection_contains") (param $this i32) (param $value i32) (result i32)
    unreachable
  )
  (func $Collection_remove (export "Collection_remove") (param $this i32) (param $value i32) (result i32)
    unreachable
  )
  (func $Collection_clear (export "Collection_clear") (param $this i32)
    unreachable
  )
  (func $Collection_stream (export "Collection_stream") (param $this i32) (result i32)
    unreachable
  )
  ;; Class/Interface: List
  (func $List_get (export "List_get") (param $this i32) (param $index i32) (result i32)
    unreachable
  )
  (func $List_set (export "List_set") (param $this i32) (param $index i32) (param $value i32) (result i32)
    unreachable
  )
  (func $List_remove (export "List_remove") (param $this i32) (param $index i32) (result i32)
    unreachable
  )
  ;; Class/Interface: Set
  ;; Class/Interface: Map
  (func $Map_size (export "Map_size") (param $this i32) (result i32)
    unreachable
  )
  (func $Map_isEmpty (export "Map_isEmpty") (param $this i32) (result i32)
    unreachable
  )
  (func $Map_put (export "Map_put") (param $this i32) (param $key i32) (param $value i32) (result i32)
    unreachable
  )
  (func $Map_get (export "Map_get") (param $this i32) (param $key i32) (result i32)
    unreachable
  )
  (func $Map_containsKey (export "Map_containsKey") (param $this i32) (param $key i32) (result i32)
    unreachable
  )
  (func $Map_remove (export "Map_remove") (param $this i32) (param $key i32) (result i32)
    unreachable
  )
  (func $Map_clear (export "Map_clear") (param $this i32)
    unreachable
  )
  (func $Map_keySet (export "Map_keySet") (param $this i32) (result i32)
    unreachable
  )
  ;; Class/Interface: __JavawObjects
  (func $__JavawObjects_same (export "__JavawObjects_same") (param $a i32) (param $b i32) (result i32)
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
    i32.const 0
    return
  )
  (func $__JavawObjects_hash (export "__JavawObjects_hash") (param $value i32) (result i32)
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
    i32.const 0
    return
  )
  ;; Class/Interface: ArrayList
  (func $ArrayList__init (export "ArrayList__init") (param $this i32)
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
    i32.const 8
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
    i32.store offset=4
  )
  (func $ArrayList__init_1 (export "ArrayList__init_1") (param $this i32) (param $capacity i32)
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
    (local $__allocation$19 i32)
    (local $__caught_exc i32)
    local.get $capacity
    i32.const 0
    i32.lt_s
    if
    i32.const 8
    call $__javaw_object_alloc
    local.set $__allocation$19
    local.get $__allocation$19
    i32.const 1502
    i32.store offset=0
    local.get $__allocation$19
    call $IllegalArgumentException__init
    local.get $__allocation$19
    throw $Exception
    end
    local.get $this
    local.get $capacity
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
    i32.store offset=4
  )
  (func $ArrayList_size (export "ArrayList_size") (param $this i32) (result i32)
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
  (func $ArrayList_isEmpty (export "ArrayList_isEmpty") (param $this i32) (result i32)
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
    i32.const 0
    i32.eq
    return
  )
  (func $ArrayList_add (export "ArrayList_add") (param $this i32) (param $value i32) (result i32)
    (local $next i32)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__mutation$3$address i32)
    (local $__mutation$3$old i32)
    (local $__mutation$3$new i32)
    (local $__mutation$4$address i32)
    (local $__mutation$4$old i32)
    (local $__mutation$4$new i32)
    (local $__mutation$5$address i32)
    (local $__mutation$5$old i32)
    (local $__mutation$5$new i32)
    (local $__caught_exc i32)
    local.get $this
    i32.load offset=8
    local.get $this
    i32.load offset=4
    i32.load
    i32.eq
    if
    local.get $this
    i32.load offset=8
    i32.const 2
    i32.mul
    i32.const 8
    i32.add
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
    local.set $next
    i32.const 0
    local.set $i
    (block $break_for_6
      (loop $loop_for_6
    local.get $i
    local.get $this
    i32.load offset=8
    i32.lt_s
        i32.eqz
        br_if $break_for_6
        (block $continue_for_6
    local.get $next
    local.get $i
    i32.const 4
    i32.mul
    i32.add
    local.get $this
    i32.load offset=4
    local.get $i
    i32.const 4
    i32.mul
    i32.add
    i32.load offset=4
    i32.store offset=4
        )
    local.get $i
    local.tee $__mutation$3$old
    i32.const 1
    i32.add
    local.set $__mutation$3$new
    local.get $__mutation$3$new
    local.set $i
    local.get $__mutation$3$old
    drop
        br $loop_for_6
      )
    )
    local.get $this
    local.get $next
    i32.store offset=4
    end
    local.get $this
    i32.load offset=4
    local.get $this
    i32.load offset=8
    i32.const 4
    i32.mul
    i32.add
    local.get $value
    i32.store offset=4
    local.get $this
    i32.const 8
    i32.add
    local.set $__mutation$4$address
    local.get $__mutation$4$address
    i32.load
    local.tee $__mutation$4$old
    i32.const 1
    i32.add
    local.set $__mutation$4$new
    local.get $__mutation$4$address
    local.get $__mutation$4$new
    i32.store
    local.get $__mutation$4$old
    drop
    local.get $this
    i32.const 12
    i32.add
    local.set $__mutation$5$address
    local.get $__mutation$5$address
    i32.load
    local.tee $__mutation$5$old
    i32.const 1
    i32.add
    local.set $__mutation$5$new
    local.get $__mutation$5$address
    local.get $__mutation$5$new
    i32.store
    local.get $__mutation$5$old
    drop
    i32.const 1
    return
  )
  (func $ArrayList_check (export "ArrayList_check") (param $this i32) (param $index i32)
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
    (local $__allocation$20 i32)
    (local $__caught_exc i32)
    local.get $index
    i32.const 0
    i32.lt_s
    if (result i32)
    i32.const 1
    else
    local.get $index
    local.get $this
    i32.load offset=8
    i32.ge_s
    end
    if
    i32.const 8
    call $__javaw_object_alloc
    local.set $__allocation$20
    local.get $__allocation$20
    i32.const 1666
    i32.store offset=0
    local.get $__allocation$20
    call $IndexOutOfBoundsException__init
    local.get $__allocation$20
    throw $Exception
    end
  )
  (func $ArrayList_get (export "ArrayList_get") (param $this i32) (param $index i32) (result i32)
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
    (local $__receiver$29 i32)
    (local $__caught_exc i32)
    local.get $this
    call $__require_nonnull
    local.get $index
    call $ArrayList_check
    local.get $this
    i32.load offset=4
    local.get $index
    i32.const 4
    i32.mul
    i32.add
    i32.load offset=4
    return
  )
  (func $ArrayList_set (export "ArrayList_set") (param $this i32) (param $index i32) (param $value i32) (result i32)
    (local $old i32)
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
    (local $__receiver$30 i32)
    (local $__caught_exc i32)
    local.get $this
    call $__require_nonnull
    local.get $index
    call $ArrayList_check
    local.get $this
    i32.load offset=4
    local.get $index
    i32.const 4
    i32.mul
    i32.add
    i32.load offset=4
    local.set $old
    local.get $this
    i32.load offset=4
    local.get $index
    i32.const 4
    i32.mul
    i32.add
    local.get $value
    i32.store offset=4
    local.get $old
    return
  )
  (func $ArrayList_remove__69_6e_74_ (export "ArrayList_remove__69_6e_74_") (param $this i32) (param $index i32) (result i32)
    (local $old i32)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__mutation$6$address i32)
    (local $__mutation$6$old i32)
    (local $__mutation$6$new i32)
    (local $__mutation$7$address i32)
    (local $__mutation$7$old i32)
    (local $__mutation$7$new i32)
    (local $__mutation$8$address i32)
    (local $__mutation$8$old i32)
    (local $__mutation$8$new i32)
    (local $__receiver$31 i32)
    (local $__caught_exc i32)
    local.get $this
    call $__require_nonnull
    local.get $index
    call $ArrayList_check
    local.get $this
    i32.load offset=4
    local.get $index
    i32.const 4
    i32.mul
    i32.add
    i32.load offset=4
    local.set $old
    local.get $index
    local.set $i
    (block $break_for_7
      (loop $loop_for_7
    local.get $i
    local.get $this
    i32.load offset=8
    i32.const 1
    i32.sub
    i32.lt_s
        i32.eqz
        br_if $break_for_7
        (block $continue_for_7
    local.get $this
    i32.load offset=4
    local.get $i
    i32.const 4
    i32.mul
    i32.add
    local.get $this
    i32.load offset=4
    local.get $i
    i32.const 1
    i32.add
    i32.const 4
    i32.mul
    i32.add
    i32.load offset=4
    i32.store offset=4
        )
    local.get $i
    local.tee $__mutation$6$old
    i32.const 1
    i32.add
    local.set $__mutation$6$new
    local.get $__mutation$6$new
    local.set $i
    local.get $__mutation$6$old
    drop
        br $loop_for_7
      )
    )
    local.get $this
    i32.const 8
    i32.add
    local.set $__mutation$7$address
    local.get $__mutation$7$address
    i32.load
    local.tee $__mutation$7$old
    i32.const 1
    i32.sub
    local.set $__mutation$7$new
    local.get $__mutation$7$address
    local.get $__mutation$7$new
    i32.store
    local.get $__mutation$7$old
    drop
    local.get $this
    i32.load offset=4
    local.get $this
    i32.load offset=8
    i32.const 4
    i32.mul
    i32.add
    i32.const 0
    i32.store offset=4
    local.get $this
    i32.const 12
    i32.add
    local.set $__mutation$8$address
    local.get $__mutation$8$address
    i32.load
    local.tee $__mutation$8$old
    i32.const 1
    i32.add
    local.set $__mutation$8$new
    local.get $__mutation$8$address
    local.get $__mutation$8$new
    i32.store
    local.get $__mutation$8$old
    drop
    local.get $old
    return
  )
  (func $ArrayList_indexOf (export "ArrayList_indexOf") (param $this i32) (param $value i32) (result i32)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__mutation$9$address i32)
    (local $__mutation$9$old i32)
    (local $__mutation$9$new i32)
    (local $__receiver$32 i32)
    (local $__caught_exc i32)
    i32.const 0
    local.set $i
    (block $break_for_8
      (loop $loop_for_8
    local.get $i
    local.get $this
    i32.load offset=8
    i32.lt_s
        i32.eqz
        br_if $break_for_8
        (block $continue_for_8
    local.get $value
    local.get $this
    i32.load offset=4
    local.get $i
    i32.const 4
    i32.mul
    i32.add
    i32.load offset=4
    call $__javaw_same
    if
    local.get $i
    return
    end
        )
    local.get $i
    local.tee $__mutation$9$old
    i32.const 1
    i32.add
    local.set $__mutation$9$new
    local.get $__mutation$9$new
    local.set $i
    local.get $__mutation$9$old
    drop
        br $loop_for_8
      )
    )
    i32.const 0
    i32.const 1
    i32.sub
    return
  )
  (func $ArrayList_contains (export "ArrayList_contains") (param $this i32) (param $value i32) (result i32)
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
    (local $__receiver$33 i32)
    (local $__caught_exc i32)
    local.get $this
    call $__require_nonnull
    local.tee $__receiver$33
    local.get $value
    local.get $__receiver$33
    i32.load offset=0
    i32.const 40
    i32.add
    call_indirect (type $t_sig_3)
    i32.const 0
    i32.ge_s
    return
  )
  (func $ArrayList_remove__4f_62_6a_65_63_74_ (export "ArrayList_remove__4f_62_6a_65_63_74_") (param $this i32) (param $value i32) (result i32)
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
    (local $__receiver$34 i32)
    (local $__receiver$35 i32)
    (local $__caught_exc i32)
    local.get $this
    call $__require_nonnull
    local.tee $__receiver$34
    local.get $value
    local.get $__receiver$34
    i32.load offset=0
    i32.const 40
    i32.add
    call_indirect (type $t_sig_3)
    local.set $index
    local.get $index
    i32.const 0
    i32.lt_s
    if
    i32.const 0
    return
    end
    local.get $this
    call $__require_nonnull
    local.tee $__receiver$35
    local.get $index
    local.get $__receiver$35
    i32.load offset=0
    i32.const 29
    i32.add
    call_indirect (type $t_sig_3)
    drop
    i32.const 1
    return
  )
  (func $ArrayList_clear (export "ArrayList_clear") (param $this i32)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__mutation$10$address i32)
    (local $__mutation$10$old i32)
    (local $__mutation$10$new i32)
    (local $__mutation$11$address i32)
    (local $__mutation$11$old i32)
    (local $__mutation$11$new i32)
    (local $__caught_exc i32)
    i32.const 0
    local.set $i
    (block $break_for_9
      (loop $loop_for_9
    local.get $i
    local.get $this
    i32.load offset=8
    i32.lt_s
        i32.eqz
        br_if $break_for_9
        (block $continue_for_9
    local.get $this
    i32.load offset=4
    local.get $i
    i32.const 4
    i32.mul
    i32.add
    i32.const 0
    i32.store offset=4
        )
    local.get $i
    local.tee $__mutation$10$old
    i32.const 1
    i32.add
    local.set $__mutation$10$new
    local.get $__mutation$10$new
    local.set $i
    local.get $__mutation$10$old
    drop
        br $loop_for_9
      )
    )
    local.get $this
    i32.const 0
    i32.store offset=8
    local.get $this
    i32.const 12
    i32.add
    local.set $__mutation$11$address
    local.get $__mutation$11$address
    i32.load
    local.tee $__mutation$11$old
    i32.const 1
    i32.add
    local.set $__mutation$11$new
    local.get $__mutation$11$address
    local.get $__mutation$11$new
    i32.store
    local.get $__mutation$11$old
    drop
  )
  (func $ArrayList_iterator (export "ArrayList_iterator") (param $this i32) (result i32)
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
    (local $__allocation$21 i32)
    (local $__caught_exc i32)
    i32.const 16
    call $__javaw_object_alloc
    local.set $__allocation$21
    local.get $__allocation$21
    i32.const 512
    i32.store offset=0
    local.get $__allocation$21
    local.get $this
    call $__JavawListIterator__init
    local.get $__allocation$21
    return
  )
  (func $ArrayList_stream (export "ArrayList_stream") (param $this i32) (result i32)
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
    (local $__receiver$36 i32)
    (local $__caught_exc i32)
    local.get $this
    call $Stream_fromCollection
    return
  )
  (func $ArrayList_toArray (export "ArrayList_toArray") (param $this i32) (result i32)
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
    (local $__simd_tmp v128)
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__mutation$12$address i32)
    (local $__mutation$12$old i32)
    (local $__mutation$12$new i32)
    (local $__caught_exc i32)
    local.get $this
    i32.load offset=8
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
    local.set $result
    i32.const 0
    local.set $i
    (block $break_for_10
      (loop $loop_for_10
    local.get $i
    local.get $this
    i32.load offset=8
    i32.lt_s
        i32.eqz
        br_if $break_for_10
        (block $continue_for_10
    local.get $result
    local.get $i
    i32.const 4
    i32.mul
    i32.add
    local.get $this
    i32.load offset=4
    local.get $i
    i32.const 4
    i32.mul
    i32.add
    i32.load offset=4
    i32.store offset=4
        )
    local.get $i
    local.tee $__mutation$12$old
    i32.const 1
    i32.add
    local.set $__mutation$12$new
    local.get $__mutation$12$new
    local.set $i
    local.get $__mutation$12$old
    drop
        br $loop_for_10
      )
    )
    local.get $result
    return
  )
  ;; Class/Interface: __JavawListIterator
  (func $__JavawListIterator__init (export "__JavawListIterator__init") (param $this i32) (param $list i32)
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
    local.get $list
    i32.store offset=4
    local.get $this
    local.get $list
    i32.load offset=12
    i32.store offset=12
  )
  (func $__JavawListIterator_hasNext (export "__JavawListIterator_hasNext") (param $this i32) (result i32)
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
    (local $__receiver$37 i32)
    (local $__caught_exc i32)
    local.get $this
    i32.load offset=8
    local.get $this
    i32.load offset=4
    call $__require_nonnull
    local.tee $__receiver$37
    local.get $__receiver$37
    i32.load offset=0
    i32.const 20
    i32.add
    call_indirect (type $t_sig_2)
    i32.lt_s
    return
  )
  (func $__JavawListIterator_next (export "__JavawListIterator_next") (param $this i32) (result i32)
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
    (local $__mutation$13$address i32)
    (local $__mutation$13$old i32)
    (local $__mutation$13$new i32)
    (local $__allocation$22 i32)
    (local $__allocation$23 i32)
    (local $__receiver$38 i32)
    (local $__receiver$39 i32)
    (local $__caught_exc i32)
    local.get $this
    i32.load offset=12
    local.get $this
    i32.load offset=4
    i32.load offset=12
    i32.ne
    if
    i32.const 8
    call $__javaw_object_alloc
    local.set $__allocation$22
    local.get $__allocation$22
    i32.const 1707
    i32.store offset=0
    local.get $__allocation$22
    call $ConcurrentModificationException__init
    local.get $__allocation$22
    throw $Exception
    end
    local.get $this
    call $__require_nonnull
    local.tee $__receiver$38
    local.get $__receiver$38
    i32.load offset=0
    i32.const 6
    i32.add
    call_indirect (type $t_sig_2)
    i32.eqz
    if
    i32.const 8
    call $__javaw_object_alloc
    local.set $__allocation$23
    local.get $__allocation$23
    i32.const 1625
    i32.store offset=0
    local.get $__allocation$23
    call $NoSuchElementException__init
    local.get $__allocation$23
    throw $Exception
    end
    local.get $this
    i32.load offset=4
    call $__require_nonnull
    local.tee $__receiver$39
    local.get $this
    i32.load offset=8
    local.get $__receiver$39
    i32.load offset=0
    i32.const 27
    i32.add
    call_indirect (type $t_sig_3)
    local.set $result
    local.get $this
    i32.const 8
    i32.add
    local.set $__mutation$13$address
    local.get $__mutation$13$address
    i32.load
    local.tee $__mutation$13$old
    i32.const 1
    i32.add
    local.set $__mutation$13$new
    local.get $__mutation$13$address
    local.get $__mutation$13$new
    i32.store
    local.get $__mutation$13$old
    drop
    local.get $result
    return
  )
  ;; Class/Interface: __JavawMapEntry
  (func $__JavawMapEntry__init (export "__JavawMapEntry__init") (param $this i32) (param $key i32) (param $value i32) (param $hash i32) (param $next i32)
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
    local.get $key
    i32.store offset=4
    local.get $this
    local.get $value
    i32.store offset=8
    local.get $this
    local.get $hash
    i32.store offset=12
    local.get $this
    local.get $next
    i32.store offset=16
  )
  ;; Class/Interface: HashMap
  (func $HashMap__init (export "HashMap__init") (param $this i32)
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
    i32.const 32
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
    i32.store offset=4
  )
  (func $HashMap_size (export "HashMap_size") (param $this i32) (result i32)
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
  (func $HashMap_isEmpty (export "HashMap_isEmpty") (param $this i32) (result i32)
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
    i32.const 0
    i32.eq
    return
  )
  (func $HashMap_bucket (export "HashMap_bucket") (param $this i32) (param $hash i32) (result i32)
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
    local.get $hash
    local.get $this
    i32.load offset=4
    i32.load
    i32.const 1
    i32.sub
    i32.and
    return
  )
  (func $HashMap_entry (export "HashMap_entry") (param $this i32) (param $key i32) (param $hash i32) (result i32)
    (local $e i32)
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
    (local $__receiver$40 i32)
    (local $__receiver$41 i32)
    (local $__caught_exc i32)
    local.get $this
    i32.load offset=4
    local.get $this
    call $__require_nonnull
    local.get $hash
    call $HashMap_bucket
    i32.const 4
    i32.mul
    i32.add
    i32.load offset=4
    local.set $e
    (block $break_while_11
      (loop $loop_while_11
    local.get $e
    i32.const 0
    i32.ne
        i32.eqz
        br_if $break_while_11
    local.get $e
    i32.load offset=12
    local.get $hash
    i32.eq
    if (result i32)
    local.get $key
    local.get $e
    i32.load offset=4
    call $__javaw_same
    else
    i32.const 0
    end
    if
    local.get $e
    return
    end
    local.get $e
    i32.load offset=16
    local.set $e
        br $loop_while_11
      )
    )
    i32.const 0
    return
  )
  (func $HashMap_get (export "HashMap_get") (param $this i32) (param $key i32) (result i32)
    (local $e i32)
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
    (local $__receiver$42 i32)
    (local $__receiver$43 i32)
    (local $__caught_exc i32)
    local.get $this
    call $__require_nonnull
    local.get $key
    local.get $key
    call $__javaw_hash
    call $HashMap_entry
    local.set $e
    local.get $e
    i32.const 0
    i32.eq
    if (result i32)
    i32.const 0
    else
    local.get $e
    i32.load offset=8
    end
    return
  )
  (func $HashMap_containsKey (export "HashMap_containsKey") (param $this i32) (param $key i32) (result i32)
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
    (local $__receiver$44 i32)
    (local $__receiver$45 i32)
    (local $__caught_exc i32)
    local.get $this
    call $__require_nonnull
    local.get $key
    local.get $key
    call $__javaw_hash
    call $HashMap_entry
    i32.const 0
    i32.ne
    return
  )
  (func $HashMap_put (export "HashMap_put") (param $this i32) (param $key i32) (param $value i32) (result i32)
    (local $hash i32)
    (local $e i32)
    (local $old i32)
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
    (local $__mutation$14$address i32)
    (local $__mutation$14$old i32)
    (local $__mutation$14$new i32)
    (local $__mutation$15$address i32)
    (local $__mutation$15$old i32)
    (local $__mutation$15$new i32)
    (local $__allocation$24 i32)
    (local $__receiver$46 i32)
    (local $__receiver$47 i32)
    (local $__receiver$48 i32)
    (local $__receiver$49 i32)
    (local $__caught_exc i32)
    local.get $key
    call $__javaw_hash
    local.set $hash
    local.get $this
    call $__require_nonnull
    local.get $key
    local.get $hash
    call $HashMap_entry
    local.set $e
    local.get $e
    i32.const 0
    i32.ne
    if
    local.get $e
    i32.load offset=8
    local.set $old
    local.get $e
    local.get $value
    i32.store offset=8
    local.get $old
    return
    end
    local.get $this
    call $__require_nonnull
    local.get $hash
    call $HashMap_bucket
    local.set $index
    local.get $this
    i32.load offset=4
    local.get $index
    i32.const 4
    i32.mul
    i32.add
    i32.const 24
    call $__javaw_object_alloc
    local.set $__allocation$24
    local.get $__allocation$24
    i32.const 552
    i32.store offset=0
    local.get $__allocation$24
    local.get $key
    local.get $value
    local.get $hash
    local.get $this
    i32.load offset=4
    local.get $index
    i32.const 4
    i32.mul
    i32.add
    i32.load offset=4
    call $__JavawMapEntry__init
    local.get $__allocation$24
    i32.store offset=4
    local.get $this
    i32.const 8
    i32.add
    local.set $__mutation$14$address
    local.get $__mutation$14$address
    i32.load
    local.tee $__mutation$14$old
    i32.const 1
    i32.add
    local.set $__mutation$14$new
    local.get $__mutation$14$address
    local.get $__mutation$14$new
    i32.store
    local.get $__mutation$14$old
    drop
    local.get $this
    i32.const 12
    i32.add
    local.set $__mutation$15$address
    local.get $__mutation$15$address
    i32.load
    local.tee $__mutation$15$old
    i32.const 1
    i32.add
    local.set $__mutation$15$new
    local.get $__mutation$15$address
    local.get $__mutation$15$new
    i32.store
    local.get $__mutation$15$old
    drop
    local.get $this
    i32.load offset=8
    local.get $this
    i32.load offset=4
    i32.load
    i32.const 3
    i32.mul
    i32.const 4
    call $__java_i32_div
    i32.gt_s
    if
    local.get $this
    call $__require_nonnull
    call $HashMap_grow
    end
    i32.const 0
    return
  )
  (func $HashMap_grow (export "HashMap_grow") (param $this i32)
    (local $old i32)
    (local $i i32)
    (local $e i32)
    (local $next i32)
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
    (local $__mutation$16$address i32)
    (local $__mutation$16$old i32)
    (local $__mutation$16$new i32)
    (local $__receiver$50 i32)
    (local $__caught_exc i32)
    local.get $this
    i32.load offset=4
    local.set $old
    local.get $this
    local.get $old
    i32.load
    i32.const 2
    i32.mul
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
    i32.store offset=4
    i32.const 0
    local.set $i
    (block $break_for_12
      (loop $loop_for_12
    local.get $i
    local.get $old
    i32.load
    i32.lt_s
        i32.eqz
        br_if $break_for_12
        (block $continue_for_12
    local.get $old
    local.get $i
    i32.const 4
    i32.mul
    i32.add
    i32.load offset=4
    local.set $e
    (block $break_while_13
      (loop $loop_while_13
    local.get $e
    i32.const 0
    i32.ne
        i32.eqz
        br_if $break_while_13
    local.get $e
    i32.load offset=16
    local.set $next
    local.get $this
    call $__require_nonnull
    local.get $e
    i32.load offset=12
    call $HashMap_bucket
    local.set $index
    local.get $e
    local.get $this
    i32.load offset=4
    local.get $index
    i32.const 4
    i32.mul
    i32.add
    i32.load offset=4
    i32.store offset=16
    local.get $this
    i32.load offset=4
    local.get $index
    i32.const 4
    i32.mul
    i32.add
    local.get $e
    i32.store offset=4
    local.get $next
    local.set $e
        br $loop_while_13
      )
    )
        )
    local.get $i
    local.tee $__mutation$16$old
    i32.const 1
    i32.add
    local.set $__mutation$16$new
    local.get $__mutation$16$new
    local.set $i
    local.get $__mutation$16$old
    drop
        br $loop_for_12
      )
    )
  )
  (func $HashMap_remove (export "HashMap_remove") (param $this i32) (param $key i32) (result i32)
    (local $hash i32)
    (local $index i32)
    (local $e i32)
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
    (local $__mutation$17$address i32)
    (local $__mutation$17$old i32)
    (local $__mutation$17$new i32)
    (local $__mutation$18$address i32)
    (local $__mutation$18$old i32)
    (local $__mutation$18$new i32)
    (local $__receiver$51 i32)
    (local $__receiver$52 i32)
    (local $__receiver$53 i32)
    (local $__caught_exc i32)
    local.get $key
    call $__javaw_hash
    local.set $hash
    local.get $this
    call $__require_nonnull
    local.get $hash
    call $HashMap_bucket
    local.set $index
    local.get $this
    i32.load offset=4
    local.get $index
    i32.const 4
    i32.mul
    i32.add
    i32.load offset=4
    local.set $e
    i32.const 0
    local.set $previous
    (block $break_while_14
      (loop $loop_while_14
    local.get $e
    i32.const 0
    i32.ne
        i32.eqz
        br_if $break_while_14
    local.get $e
    i32.load offset=12
    local.get $hash
    i32.eq
    if (result i32)
    local.get $key
    local.get $e
    i32.load offset=4
    call $__javaw_same
    else
    i32.const 0
    end
    if
    local.get $previous
    i32.const 0
    i32.eq
    if
    local.get $this
    i32.load offset=4
    local.get $index
    i32.const 4
    i32.mul
    i32.add
    local.get $e
    i32.load offset=16
    i32.store offset=4
    else
    local.get $previous
    local.get $e
    i32.load offset=16
    i32.store offset=16
    end
    local.get $this
    i32.const 8
    i32.add
    local.set $__mutation$17$address
    local.get $__mutation$17$address
    i32.load
    local.tee $__mutation$17$old
    i32.const 1
    i32.sub
    local.set $__mutation$17$new
    local.get $__mutation$17$address
    local.get $__mutation$17$new
    i32.store
    local.get $__mutation$17$old
    drop
    local.get $this
    i32.const 12
    i32.add
    local.set $__mutation$18$address
    local.get $__mutation$18$address
    i32.load
    local.tee $__mutation$18$old
    i32.const 1
    i32.add
    local.set $__mutation$18$new
    local.get $__mutation$18$address
    local.get $__mutation$18$new
    i32.store
    local.get $__mutation$18$old
    drop
    local.get $e
    i32.load offset=8
    return
    end
    local.get $e
    local.set $previous
    local.get $e
    i32.load offset=16
    local.set $e
        br $loop_while_14
      )
    )
    i32.const 0
    return
  )
  (func $HashMap_clear (export "HashMap_clear") (param $this i32)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__mutation$19$address i32)
    (local $__mutation$19$old i32)
    (local $__mutation$19$new i32)
    (local $__mutation$20$address i32)
    (local $__mutation$20$old i32)
    (local $__mutation$20$new i32)
    (local $__caught_exc i32)
    i32.const 0
    local.set $i
    (block $break_for_15
      (loop $loop_for_15
    local.get $i
    local.get $this
    i32.load offset=4
    i32.load
    i32.lt_s
        i32.eqz
        br_if $break_for_15
        (block $continue_for_15
    local.get $this
    i32.load offset=4
    local.get $i
    i32.const 4
    i32.mul
    i32.add
    i32.const 0
    i32.store offset=4
        )
    local.get $i
    local.tee $__mutation$19$old
    i32.const 1
    i32.add
    local.set $__mutation$19$new
    local.get $__mutation$19$new
    local.set $i
    local.get $__mutation$19$old
    drop
        br $loop_for_15
      )
    )
    local.get $this
    i32.const 0
    i32.store offset=8
    local.get $this
    i32.const 12
    i32.add
    local.set $__mutation$20$address
    local.get $__mutation$20$address
    i32.load
    local.tee $__mutation$20$old
    i32.const 1
    i32.add
    local.set $__mutation$20$new
    local.get $__mutation$20$address
    local.get $__mutation$20$new
    i32.store
    local.get $__mutation$20$old
    drop
  )
  (func $HashMap_keySet (export "HashMap_keySet") (param $this i32) (result i32)
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
    (local $__allocation$25 i32)
    (local $__caught_exc i32)
    i32.const 8
    call $__javaw_object_alloc
    local.set $__allocation$25
    local.get $__allocation$25
    i32.const 673
    i32.store offset=0
    local.get $__allocation$25
    local.get $this
    call $__JavawKeySet__init
    local.get $__allocation$25
    return
  )
  (func $HashMap_keyIterator (export "HashMap_keyIterator") (param $this i32) (result i32)
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
    (local $__allocation$26 i32)
    (local $__caught_exc i32)
    i32.const 24
    call $__javaw_object_alloc
    local.set $__allocation$26
    local.get $__allocation$26
    i32.const 633
    i32.store offset=0
    local.get $__allocation$26
    local.get $this
    local.get $this
    i32.load offset=4
    call $__JavawKeyIterator__init
    local.get $__allocation$26
    return
  )
  ;; Class/Interface: __JavawKeyIterator
  (func $__JavawKeyIterator__init (export "__JavawKeyIterator__init") (param $this i32) (param $map i32) (param $buckets i32)
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
    (local $__receiver$54 i32)
    (local $__caught_exc i32)
    local.get $this
    local.get $map
    i32.store offset=4
    local.get $this
    local.get $buckets
    i32.store offset=8
    local.get $this
    local.get $map
    i32.load offset=12
    i32.store offset=16
    local.get $this
    call $__require_nonnull
    call $__JavawKeyIterator_seek
  )
  (func $__JavawKeyIterator_seek (export "__JavawKeyIterator_seek") (param $this i32)
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
    (local $__mutation$21$address i32)
    (local $__mutation$21$old i32)
    (local $__mutation$21$new i32)
    (local $__caught_exc i32)
    (block $break_while_16
      (loop $loop_while_16
    local.get $this
    i32.load offset=20
    i32.const 0
    i32.eq
    if (result i32)
    local.get $this
    i32.load offset=12
    local.get $this
    i32.load offset=8
    i32.load
    i32.lt_s
    else
    i32.const 0
    end
        i32.eqz
        br_if $break_while_16
    local.get $this
    local.get $this
    i32.load offset=8
    local.get $this
    i32.load offset=12
    i32.const 4
    i32.mul
    i32.add
    i32.load offset=4
    i32.store offset=20
    local.get $this
    i32.const 12
    i32.add
    local.set $__mutation$21$address
    local.get $__mutation$21$address
    i32.load
    local.tee $__mutation$21$old
    i32.const 1
    i32.add
    local.set $__mutation$21$new
    local.get $__mutation$21$address
    local.get $__mutation$21$new
    i32.store
    local.get $__mutation$21$old
    drop
        br $loop_while_16
      )
    )
  )
  (func $__JavawKeyIterator_hasNext (export "__JavawKeyIterator_hasNext") (param $this i32) (result i32)
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
    i32.load offset=20
    i32.const 0
    i32.ne
    return
  )
  (func $__JavawKeyIterator_next (export "__JavawKeyIterator_next") (param $this i32) (result i32)
    (local $key i32)
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
    (local $__allocation$27 i32)
    (local $__allocation$28 i32)
    (local $__receiver$55 i32)
    (local $__caught_exc i32)
    local.get $this
    i32.load offset=16
    local.get $this
    i32.load offset=4
    i32.load offset=12
    i32.ne
    if
    i32.const 8
    call $__javaw_object_alloc
    local.set $__allocation$27
    local.get $__allocation$27
    i32.const 1707
    i32.store offset=0
    local.get $__allocation$27
    call $ConcurrentModificationException__init
    local.get $__allocation$27
    throw $Exception
    end
    local.get $this
    i32.load offset=20
    i32.const 0
    i32.eq
    if
    i32.const 8
    call $__javaw_object_alloc
    local.set $__allocation$28
    local.get $__allocation$28
    i32.const 1625
    i32.store offset=0
    local.get $__allocation$28
    call $NoSuchElementException__init
    local.get $__allocation$28
    throw $Exception
    end
    local.get $this
    i32.load offset=20
    i32.load offset=4
    local.set $key
    local.get $this
    local.get $this
    i32.load offset=20
    i32.load offset=16
    i32.store offset=20
    local.get $this
    call $__require_nonnull
    call $__JavawKeyIterator_seek
    local.get $key
    return
  )
  ;; Class/Interface: __JavawKeySet
  (func $__JavawKeySet__init (export "__JavawKeySet__init") (param $this i32) (param $map i32)
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
    local.get $map
    i32.store offset=4
  )
  (func $__JavawKeySet_size (export "__JavawKeySet_size") (param $this i32) (result i32)
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
    (local $__receiver$56 i32)
    (local $__caught_exc i32)
    local.get $this
    i32.load offset=4
    call $__require_nonnull
    local.tee $__receiver$56
    local.get $__receiver$56
    i32.load offset=0
    i32.const 20
    i32.add
    call_indirect (type $t_sig_2)
    return
  )
  (func $__JavawKeySet_isEmpty (export "__JavawKeySet_isEmpty") (param $this i32) (result i32)
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
    (local $__receiver$57 i32)
    (local $__caught_exc i32)
    local.get $this
    i32.load offset=4
    call $__require_nonnull
    local.tee $__receiver$57
    local.get $__receiver$57
    i32.load offset=0
    i32.const 21
    i32.add
    call_indirect (type $t_sig_2)
    return
  )
  (func $__JavawKeySet_contains (export "__JavawKeySet_contains") (param $this i32) (param $key i32) (result i32)
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
    (local $__receiver$58 i32)
    (local $__caught_exc i32)
    local.get $this
    i32.load offset=4
    call $__require_nonnull
    local.tee $__receiver$58
    local.get $key
    local.get $__receiver$58
    i32.load offset=0
    i32.const 34
    i32.add
    call_indirect (type $t_sig_3)
    return
  )
  (func $__JavawKeySet_remove (export "__JavawKeySet_remove") (param $this i32) (param $key i32) (result i32)
    (local $exists i32)
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
    (local $__receiver$59 i32)
    (local $__receiver$60 i32)
    (local $__caught_exc i32)
    local.get $this
    i32.load offset=4
    call $__require_nonnull
    local.tee $__receiver$59
    local.get $key
    local.get $__receiver$59
    i32.load offset=0
    i32.const 34
    i32.add
    call_indirect (type $t_sig_3)
    local.set $exists
    local.get $this
    i32.load offset=4
    call $__require_nonnull
    local.tee $__receiver$60
    local.get $key
    local.get $__receiver$60
    i32.load offset=0
    i32.const 24
    i32.add
    call_indirect (type $t_sig_3)
    drop
    local.get $exists
    return
  )
  (func $__JavawKeySet_add (export "__JavawKeySet_add") (param $this i32) (param $key i32) (result i32)
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
    (local $__allocation$29 i32)
    (local $__caught_exc i32)
    i32.const 8
    call $__javaw_object_alloc
    local.set $__allocation$29
    local.get $__allocation$29
    i32.const 1748
    i32.store offset=0
    local.get $__allocation$29
    call $UnsupportedOperationException__init
    local.get $__allocation$29
    throw $Exception
    i32.const 0
  )
  (func $__JavawKeySet_clear (export "__JavawKeySet_clear") (param $this i32)
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
    (local $__receiver$61 i32)
    (local $__caught_exc i32)
    local.get $this
    i32.load offset=4
    call $__require_nonnull
    local.tee $__receiver$61
    local.get $__receiver$61
    i32.load offset=0
    i32.const 25
    i32.add
    call_indirect (type $t_sig_5)
  )
  (func $__JavawKeySet_iterator (export "__JavawKeySet_iterator") (param $this i32) (result i32)
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
    (local $__receiver$62 i32)
    (local $__caught_exc i32)
    local.get $this
    i32.load offset=4
    call $__require_nonnull
    local.tee $__receiver$62
    local.get $__receiver$62
    i32.load offset=0
    i32.const 40
    i32.add
    call_indirect (type $t_sig_2)
    return
  )
  (func $__JavawKeySet_stream (export "__JavawKeySet_stream") (param $this i32) (result i32)
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
    (local $__receiver$63 i32)
    (local $__caught_exc i32)
    local.get $this
    call $Stream_fromCollection
    return
  )
  ;; Class/Interface: HashSet
  (func $HashSet__init (export "HashSet__init") (param $this i32)
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
    (local $__allocation$30 i32)
    (local $__caught_exc i32)
    local.get $this
    i32.const 16
    call $__javaw_object_alloc
    local.set $__allocation$30
    local.get $__allocation$30
    i32.const 592
    i32.store offset=0
    local.get $__allocation$30
    call $HashMap__init
    local.get $__allocation$30
    i32.store offset=4
  )
  (func $HashSet_size (export "HashSet_size") (param $this i32) (result i32)
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
    (local $__receiver$64 i32)
    (local $__caught_exc i32)
    local.get $this
    i32.load offset=4
    call $__require_nonnull
    local.tee $__receiver$64
    local.get $__receiver$64
    i32.load offset=0
    i32.const 20
    i32.add
    call_indirect (type $t_sig_2)
    return
  )
  (func $HashSet_isEmpty (export "HashSet_isEmpty") (param $this i32) (result i32)
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
    (local $__receiver$65 i32)
    (local $__caught_exc i32)
    local.get $this
    i32.load offset=4
    call $__require_nonnull
    local.tee $__receiver$65
    local.get $__receiver$65
    i32.load offset=0
    i32.const 21
    i32.add
    call_indirect (type $t_sig_2)
    return
  )
  (func $HashSet_contains (export "HashSet_contains") (param $this i32) (param $value i32) (result i32)
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
    (local $__receiver$66 i32)
    (local $__caught_exc i32)
    local.get $this
    i32.load offset=4
    call $__require_nonnull
    local.tee $__receiver$66
    local.get $value
    local.get $__receiver$66
    i32.load offset=0
    i32.const 34
    i32.add
    call_indirect (type $t_sig_3)
    return
  )
  (func $HashSet_add (export "HashSet_add") (param $this i32) (param $value i32) (result i32)
    (local $exists i32)
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
    (local $__receiver$67 i32)
    (local $__receiver$68 i32)
    (local $__caught_exc i32)
    local.get $this
    i32.load offset=4
    call $__require_nonnull
    local.tee $__receiver$67
    local.get $value
    local.get $__receiver$67
    i32.load offset=0
    i32.const 34
    i32.add
    call_indirect (type $t_sig_3)
    local.set $exists
    local.get $this
    i32.load offset=4
    call $__require_nonnull
    local.tee $__receiver$68
    local.get $value
    i32.const 1
    call $Boolean_valueOf__62_6f_6f_6c_65_61_6e_
    local.get $__receiver$68
    i32.load offset=0
    i32.const 32
    i32.add
    call_indirect (type $t_sig_4)
    drop
    local.get $exists
    i32.eqz
    return
  )
  (func $HashSet_remove (export "HashSet_remove") (param $this i32) (param $value i32) (result i32)
    (local $exists i32)
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
    (local $__receiver$69 i32)
    (local $__receiver$70 i32)
    (local $__caught_exc i32)
    local.get $this
    i32.load offset=4
    call $__require_nonnull
    local.tee $__receiver$69
    local.get $value
    local.get $__receiver$69
    i32.load offset=0
    i32.const 34
    i32.add
    call_indirect (type $t_sig_3)
    local.set $exists
    local.get $this
    i32.load offset=4
    call $__require_nonnull
    local.tee $__receiver$70
    local.get $value
    local.get $__receiver$70
    i32.load offset=0
    i32.const 24
    i32.add
    call_indirect (type $t_sig_3)
    drop
    local.get $exists
    return
  )
  (func $HashSet_clear (export "HashSet_clear") (param $this i32)
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
    (local $__receiver$71 i32)
    (local $__caught_exc i32)
    local.get $this
    i32.load offset=4
    call $__require_nonnull
    local.tee $__receiver$71
    local.get $__receiver$71
    i32.load offset=0
    i32.const 25
    i32.add
    call_indirect (type $t_sig_5)
  )
  (func $HashSet_iterator (export "HashSet_iterator") (param $this i32) (result i32)
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
    (local $__receiver$72 i32)
    (local $__caught_exc i32)
    local.get $this
    i32.load offset=4
    call $__require_nonnull
    local.tee $__receiver$72
    local.get $__receiver$72
    i32.load offset=0
    i32.const 40
    i32.add
    call_indirect (type $t_sig_2)
    return
  )
  (func $HashSet_stream (export "HashSet_stream") (param $this i32) (result i32)
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
    (local $__receiver$73 i32)
    (local $__caught_exc i32)
    local.get $this
    call $Stream_fromCollection
    return
  )
  ;; Class/Interface: Comparator
  (func $Comparator_compare (export "Comparator_compare") (param $this i32) (param $left i32) (param $right i32) (result i32)
    unreachable
  )
  ;; Class/Interface: __JavawArrayIterator
  (func $__JavawArrayIterator__init (export "__JavawArrayIterator__init") (param $this i32) (param $values i32)
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
    local.get $values
    i32.store offset=4
  )
  (func $__JavawArrayIterator_hasNext (export "__JavawArrayIterator_hasNext") (param $this i32) (result i32)
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
    local.get $this
    i32.load offset=4
    i32.load
    i32.lt_s
    return
  )
  (func $__JavawArrayIterator_next (export "__JavawArrayIterator_next") (param $this i32) (result i32)
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
    (local $__mutation$22$address i32)
    (local $__mutation$22$old i32)
    (local $__mutation$22$new i32)
    (local $__allocation$31 i32)
    (local $__receiver$74 i32)
    (local $__caught_exc i32)
    local.get $this
    call $__require_nonnull
    local.tee $__receiver$74
    local.get $__receiver$74
    i32.load offset=0
    i32.const 6
    i32.add
    call_indirect (type $t_sig_2)
    i32.eqz
    if
    i32.const 8
    call $__javaw_object_alloc
    local.set $__allocation$31
    local.get $__allocation$31
    i32.const 1625
    i32.store offset=0
    local.get $__allocation$31
    call $NoSuchElementException__init
    local.get $__allocation$31
    throw $Exception
    end
    local.get $this
    i32.load offset=4
    local.get $this
    i32.load offset=8
    i32.const 4
    i32.mul
    i32.add
    i32.load offset=4
    local.set $result
    local.get $this
    i32.const 8
    i32.add
    local.set $__mutation$22$address
    local.get $__mutation$22$address
    i32.load
    local.tee $__mutation$22$old
    i32.const 1
    i32.add
    local.set $__mutation$22$new
    local.get $__mutation$22$address
    local.get $__mutation$22$new
    i32.store
    local.get $__mutation$22$old
    drop
    local.get $result
    return
  )
  ;; Class/Interface: __JavawGenerateIterator
  (func $__JavawGenerateIterator__init (export "__JavawGenerateIterator__init") (param $this i32) (param $supplier i32)
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
    local.get $supplier
    i32.store offset=4
  )
  (func $__JavawGenerateIterator_hasNext (export "__JavawGenerateIterator_hasNext") (param $this i32) (result i32)
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
    return
  )
  (func $__JavawGenerateIterator_next (export "__JavawGenerateIterator_next") (param $this i32) (result i32)
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
    (local $__receiver$75 i32)
    (local $__caught_exc i32)
    local.get $this
    i32.load offset=4
    call $__require_nonnull
    local.tee $__receiver$75
    local.get $__receiver$75
    i32.load offset=0
    i32.const 11
    i32.add
    call_indirect (type $t_sig_2)
    return
  )
  ;; Class/Interface: __JavawIterateIterator
  (func $__JavawIterateIterator__init (export "__JavawIterateIterator__init") (param $this i32) (param $seed i32) (param $nextValue i32)
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
    local.get $seed
    i32.store offset=4
    local.get $this
    local.get $nextValue
    i32.store offset=8
    local.get $this
    i32.const 1
    i32.store offset=12
  )
  (func $__JavawIterateIterator_hasNext (export "__JavawIterateIterator_hasNext") (param $this i32) (result i32)
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
    return
  )
  (func $__JavawIterateIterator_next (export "__JavawIterateIterator_next") (param $this i32) (result i32)
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
    (local $__receiver$76 i32)
    (local $__caught_exc i32)
    local.get $this
    i32.load offset=12
    if
    local.get $this
    i32.const 0
    i32.store offset=12
    else
    local.get $this
    local.get $this
    i32.load offset=8
    call $__require_nonnull
    local.tee $__receiver$76
    local.get $this
    i32.load offset=4
    local.get $__receiver$76
    i32.load offset=0
    i32.const 15
    i32.add
    call_indirect (type $t_sig_3)
    i32.store offset=4
    end
    local.get $this
    i32.load offset=4
    return
  )
  ;; Class/Interface: __JavawReadOnlyList
  (func $__JavawReadOnlyList__init (export "__JavawReadOnlyList__init") (param $this i32) (param $values i32)
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
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__mutation$23$address i32)
    (local $__mutation$23$old i32)
    (local $__mutation$23$new i32)
    (local $__receiver$77 i32)
    (local $__caught_exc i32)
    local.get $this
    call $ArrayList__init
    i32.const 0
    local.set $i
    (block $break_for_17
      (loop $loop_for_17
    local.get $i
    local.get $values
    i32.load
    i32.lt_s
        i32.eqz
        br_if $break_for_17
        (block $continue_for_17
    local.get $this
    call $__require_nonnull
    local.get $values
    local.get $i
    i32.const 4
    i32.mul
    i32.add
    i32.load offset=4
    call $ArrayList_add
    drop
        )
    local.get $i
    local.tee $__mutation$23$old
    i32.const 1
    i32.add
    local.set $__mutation$23$new
    local.get $__mutation$23$new
    local.set $i
    local.get $__mutation$23$old
    drop
        br $loop_for_17
      )
    )
  )
  (func $__JavawReadOnlyList_add (export "__JavawReadOnlyList_add") (param $this i32) (param $value i32) (result i32)
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
    (local $__allocation$32 i32)
    (local $__caught_exc i32)
    i32.const 8
    call $__javaw_object_alloc
    local.set $__allocation$32
    local.get $__allocation$32
    i32.const 1748
    i32.store offset=0
    local.get $__allocation$32
    call $UnsupportedOperationException__init
    local.get $__allocation$32
    throw $Exception
    i32.const 0
  )
  (func $__JavawReadOnlyList_set (export "__JavawReadOnlyList_set") (param $this i32) (param $index i32) (param $value i32) (result i32)
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
    (local $__allocation$33 i32)
    (local $__caught_exc i32)
    i32.const 8
    call $__javaw_object_alloc
    local.set $__allocation$33
    local.get $__allocation$33
    i32.const 1748
    i32.store offset=0
    local.get $__allocation$33
    call $UnsupportedOperationException__init
    local.get $__allocation$33
    throw $Exception
    i32.const 0
  )
  (func $__JavawReadOnlyList_remove__69_6e_74_ (export "__JavawReadOnlyList_remove__69_6e_74_") (param $this i32) (param $index i32) (result i32)
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
    (local $__allocation$34 i32)
    (local $__caught_exc i32)
    i32.const 8
    call $__javaw_object_alloc
    local.set $__allocation$34
    local.get $__allocation$34
    i32.const 1748
    i32.store offset=0
    local.get $__allocation$34
    call $UnsupportedOperationException__init
    local.get $__allocation$34
    throw $Exception
    i32.const 0
  )
  (func $__JavawReadOnlyList_remove__4f_62_6a_65_63_74_ (export "__JavawReadOnlyList_remove__4f_62_6a_65_63_74_") (param $this i32) (param $value i32) (result i32)
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
    (local $__allocation$35 i32)
    (local $__caught_exc i32)
    i32.const 8
    call $__javaw_object_alloc
    local.set $__allocation$35
    local.get $__allocation$35
    i32.const 1748
    i32.store offset=0
    local.get $__allocation$35
    call $UnsupportedOperationException__init
    local.get $__allocation$35
    throw $Exception
    i32.const 0
  )
  (func $__JavawReadOnlyList_clear (export "__JavawReadOnlyList_clear") (param $this i32)
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
    (local $__allocation$36 i32)
    (local $__caught_exc i32)
    i32.const 8
    call $__javaw_object_alloc
    local.set $__allocation$36
    local.get $__allocation$36
    i32.const 1748
    i32.store offset=0
    local.get $__allocation$36
    call $UnsupportedOperationException__init
    local.get $__allocation$36
    throw $Exception
  )
  ;; Class/Interface: Collector
  (func $Collector_collect (export "Collector_collect") (param $this i32) (param $stream i32) (result i32)
    unreachable
  )
  ;; Class/Interface: __JavawListCollector
  (func $__JavawListCollector__init (export "__JavawListCollector__init") (param $this i32)
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
  (func $__JavawListCollector_collect (export "__JavawListCollector_collect") (param $this i32) (param $stream i32) (result i32)
    (local $values i32)
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
    (local $__simd_tmp v128)
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__mutation$24$address i32)
    (local $__mutation$24$old i32)
    (local $__mutation$24$new i32)
    (local $__allocation$37 i32)
    (local $__receiver$78 i32)
    (local $__receiver$79 i32)
    (local $__caught_exc i32)
    local.get $stream
    call $__require_nonnull
    local.tee $__receiver$78
    local.get $__receiver$78
    i32.load offset=0
    i32.const 59
    i32.add
    call_indirect (type $t_sig_2)
    local.set $values
    i32.const 16
    call $__javaw_object_alloc
    local.set $__allocation$37
    local.get $__allocation$37
    i32.const 470
    i32.store offset=0
    local.get $__allocation$37
    call $ArrayList__init
    local.get $__allocation$37
    local.set $result
    i32.const 0
    local.set $i
    (block $break_for_18
      (loop $loop_for_18
    local.get $i
    local.get $values
    i32.load
    i32.lt_s
        i32.eqz
        br_if $break_for_18
        (block $continue_for_18
    local.get $result
    call $__require_nonnull
    local.tee $__receiver$79
    local.get $values
    local.get $i
    i32.const 4
    i32.mul
    i32.add
    i32.load offset=4
    local.get $__receiver$79
    i32.load offset=0
    i32.const 22
    i32.add
    call_indirect (type $t_sig_3)
    drop
        )
    local.get $i
    local.tee $__mutation$24$old
    i32.const 1
    i32.add
    local.set $__mutation$24$new
    local.get $__mutation$24$new
    local.set $i
    local.get $__mutation$24$old
    drop
        br $loop_for_18
      )
    )
    local.get $result
    return
  )
  ;; Class/Interface: __JavawSetCollector
  (func $__JavawSetCollector__init (export "__JavawSetCollector__init") (param $this i32)
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
  (func $__JavawSetCollector_collect (export "__JavawSetCollector_collect") (param $this i32) (param $stream i32) (result i32)
    (local $values i32)
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
    (local $__simd_tmp v128)
    (local $__f64_tmp f64)
    (local $__f32_tmp f32)
    (local $__i64_tmp i64)
    (local $__mutation$25$address i32)
    (local $__mutation$25$old i32)
    (local $__mutation$25$new i32)
    (local $__allocation$38 i32)
    (local $__receiver$80 i32)
    (local $__receiver$81 i32)
    (local $__caught_exc i32)
    local.get $stream
    call $__require_nonnull
    local.tee $__receiver$80
    local.get $__receiver$80
    i32.load offset=0
    i32.const 59
    i32.add
    call_indirect (type $t_sig_2)
    local.set $values
    i32.const 8
    call $__javaw_object_alloc
    local.set $__allocation$38
    local.get $__allocation$38
    i32.const 713
    i32.store offset=0
    local.get $__allocation$38
    call $HashSet__init
    local.get $__allocation$38
    local.set $result
    i32.const 0
    local.set $i
    (block $break_for_19
      (loop $loop_for_19
    local.get $i
    local.get $values
    i32.load
    i32.lt_s
        i32.eqz
        br_if $break_for_19
        (block $continue_for_19
    local.get $result
    call $__require_nonnull
    local.tee $__receiver$81
    local.get $values
    local.get $i
    i32.const 4
    i32.mul
    i32.add
    i32.load offset=4
    local.get $__receiver$81
    i32.load offset=0
    i32.const 22
    i32.add
    call_indirect (type $t_sig_3)
    drop
        )
    local.get $i
    local.tee $__mutation$25$old
    i32.const 1
    i32.add
    local.set $__mutation$25$new
    local.get $__mutation$25$new
    local.set $i
    local.get $__mutation$25$old
    drop
        br $loop_for_19
      )
    )
    local.get $result
    return
  )
  ;; Class/Interface: Collectors
  (func $Collectors_toList (export "Collectors_toList") (result i32)
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
    (local $__allocation$39 i32)
    (local $__caught_exc i32)
    i32.const 8
    call $__javaw_object_alloc
    local.set $__allocation$39
    local.get $__allocation$39
    i32.const 915
    i32.store offset=0
    local.get $__allocation$39
    call $__JavawListCollector__init
    local.get $__allocation$39
    return
  )
  (func $Collectors_toSet (export "Collectors_toSet") (result i32)
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
    (local $__allocation$40 i32)
    (local $__caught_exc i32)
    i32.const 8
    call $__javaw_object_alloc
    local.set $__allocation$40
    local.get $__allocation$40
    i32.const 955
    i32.store offset=0
    local.get $__allocation$40
    call $__JavawSetCollector__init
    local.get $__allocation$40
    return
  )
  ;; Class/Interface: Stream
  (func $Stream__init (export "Stream__init") (param $this i32) (param $source i32)
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
    local.get $source
    i32.store offset=4
  )
  (func $Stream__init_1 (export "Stream__init_1") (param $this i32) (param $parent i32) (param $stage i32)
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
    local.get $parent
    i32.store offset=12
    local.get $this
    local.get $stage
    i32.store offset=16
  )
  (func $Stream_of (export "Stream_of") (param $values i32) (result i32)
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
    (local $__allocation$41 i32)
    (local $__allocation$42 i32)
    (local $__allocation$43 i32)
    (local $__caught_exc i32)
    local.get $values
    i32.const 0
    i32.eq
    if
    i32.const 8
    call $__javaw_object_alloc
    local.set $__allocation$41
    local.get $__allocation$41
    i32.const 1584
    i32.store offset=0
    local.get $__allocation$41
    call $NullPointerException__init
    local.get $__allocation$41
    throw $Exception
    end
    i32.const 64
    call $__javaw_object_alloc
    local.set $__allocation$42
    local.get $__allocation$42
    i32.const 995
    i32.store offset=0
    local.get $__allocation$42
    i32.const 16
    call $__javaw_object_alloc
    local.set $__allocation$43
    local.get $__allocation$43
    i32.const 753
    i32.store offset=0
    local.get $__allocation$43
    local.get $values
    call $__JavawArrayIterator__init
    local.get $__allocation$43
    call $Stream__init
    local.get $__allocation$42
    return
  )
  (func $Stream_empty (export "Stream_empty") (result i32)
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
    (local $__receiver$82 i32)
    (local $__caught_exc i32)
    global.get $__heap_ptr
    local.set $__arr_alloc_ptr
    global.get $__heap_ptr
    i32.const 8
    i32.add
    global.set $__heap_ptr
    local.get $__arr_alloc_ptr
    i32.const 0
    i32.store
    local.get $__arr_alloc_ptr
    call $Stream_of
    return
  )
  (func $Stream_from (export "Stream_from") (param $iterator i32) (result i32)
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
    (local $__allocation$44 i32)
    (local $__caught_exc i32)
    i32.const 64
    call $__javaw_object_alloc
    local.set $__allocation$44
    local.get $__allocation$44
    i32.const 995
    i32.store offset=0
    local.get $__allocation$44
    local.get $iterator
    call $Stream__init
    local.get $__allocation$44
    return
  )
  (func $Stream_fromCollection (export "Stream_fromCollection") (param $collection i32) (result i32)
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
    (local $__allocation$45 i32)
    (local $__caught_exc i32)
    i32.const 64
    call $__javaw_object_alloc
    local.set $__allocation$45
    local.get $__allocation$45
    i32.const 995
    i32.store offset=0
    local.get $__allocation$45
    i32.const 0
    call $Stream__init
    local.get $__allocation$45
    local.set $result
    local.get $result
    local.get $collection
    i32.store offset=8
    local.get $result
    return
  )
  (func $Stream_generate (export "Stream_generate") (param $supplier i32) (result i32)
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
    (local $__allocation$46 i32)
    (local $__allocation$47 i32)
    (local $__allocation$48 i32)
    (local $__caught_exc i32)
    local.get $supplier
    i32.const 0
    i32.eq
    if
    i32.const 8
    call $__javaw_object_alloc
    local.set $__allocation$46
    local.get $__allocation$46
    i32.const 1584
    i32.store offset=0
    local.get $__allocation$46
    call $NullPointerException__init
    local.get $__allocation$46
    throw $Exception
    end
    i32.const 64
    call $__javaw_object_alloc
    local.set $__allocation$47
    local.get $__allocation$47
    i32.const 995
    i32.store offset=0
    local.get $__allocation$47
    i32.const 8
    call $__javaw_object_alloc
    local.set $__allocation$48
    local.get $__allocation$48
    i32.const 793
    i32.store offset=0
    local.get $__allocation$48
    local.get $supplier
    call $__JavawGenerateIterator__init
    local.get $__allocation$48
    call $Stream__init
    local.get $__allocation$47
    return
  )
  (func $Stream_iterate (export "Stream_iterate") (param $seed i32) (param $nextValue i32) (result i32)
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
    (local $__allocation$49 i32)
    (local $__allocation$50 i32)
    (local $__allocation$51 i32)
    (local $__caught_exc i32)
    local.get $nextValue
    i32.const 0
    i32.eq
    if
    i32.const 8
    call $__javaw_object_alloc
    local.set $__allocation$49
    local.get $__allocation$49
    i32.const 1584
    i32.store offset=0
    local.get $__allocation$49
    call $NullPointerException__init
    local.get $__allocation$49
    throw $Exception
    end
    i32.const 64
    call $__javaw_object_alloc
    local.set $__allocation$50
    local.get $__allocation$50
    i32.const 995
    i32.store offset=0
    local.get $__allocation$50
    i32.const 16
    call $__javaw_object_alloc
    local.set $__allocation$51
    local.get $__allocation$51
    i32.const 833
    i32.store offset=0
    local.get $__allocation$51
    local.get $seed
    local.get $nextValue
    call $__JavawIterateIterator__init
    local.get $__allocation$51
    call $Stream__init
    local.get $__allocation$50
    return
  )
  (func $Stream_available (export "Stream_available") (param $this i32)
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
    (local $__allocation$52 i32)
    (local $__caught_exc i32)
    local.get $this
    i32.load offset=52
    if (result i32)
    i32.const 1
    else
    local.get $this
    i32.load offset=56
    end
    if
    i32.const 8
    call $__javaw_object_alloc
    local.set $__allocation$52
    local.get $__allocation$52
    i32.const 1543
    i32.store offset=0
    local.get $__allocation$52
    call $IllegalStateException__init
    local.get $__allocation$52
    throw $Exception
    end
  )
  (func $Stream_start (export "Stream_start") (param $this i32)
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
    (local $__receiver$83 i32)
    (local $__caught_exc i32)
    local.get $this
    call $__require_nonnull
    call $Stream_available
    local.get $this
    i32.const 1
    i32.store offset=52
  )
  (func $Stream_link (export "Stream_link") (param $this i32) (param $stage i32) (result i32)
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
    (local $__allocation$53 i32)
    (local $__receiver$84 i32)
    (local $__caught_exc i32)
    local.get $this
    call $__require_nonnull
    call $Stream_start
    i32.const 64
    call $__javaw_object_alloc
    local.set $__allocation$53
    local.get $__allocation$53
    i32.const 995
    i32.store offset=0
    local.get $__allocation$53
    local.get $this
    local.get $stage
    call $Stream__init_1
    local.get $__allocation$53
    return
  )
  (func $Stream_map (export "Stream_map") (param $this i32) (param $mapper i32) (result i32)
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
    (local $__allocation$54 i32)
    (local $__allocation$55 i32)
    (local $__receiver$85 i32)
    (local $__caught_exc i32)
    local.get $mapper
    i32.const 0
    i32.eq
    if
    i32.const 8
    call $__javaw_object_alloc
    local.set $__allocation$54
    local.get $__allocation$54
    i32.const 1584
    i32.store offset=0
    local.get $__allocation$54
    call $NullPointerException__init
    local.get $__allocation$54
    throw $Exception
    end
    local.get $this
    call $__require_nonnull
    call $Stream_start
    i32.const 64
    call $__javaw_object_alloc
    local.set $__allocation$55
    local.get $__allocation$55
    i32.const 995
    i32.store offset=0
    local.get $__allocation$55
    local.get $this
    i32.const 1
    call $Stream__init_1
    local.get $__allocation$55
    local.set $result
    local.get $result
    local.get $mapper
    i32.store offset=20
    local.get $result
    return
  )
  (func $Stream_filter (export "Stream_filter") (param $this i32) (param $predicate i32) (result i32)
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
    (local $__allocation$56 i32)
    (local $__receiver$86 i32)
    (local $__caught_exc i32)
    local.get $predicate
    i32.const 0
    i32.eq
    if
    i32.const 8
    call $__javaw_object_alloc
    local.set $__allocation$56
    local.get $__allocation$56
    i32.const 1584
    i32.store offset=0
    local.get $__allocation$56
    call $NullPointerException__init
    local.get $__allocation$56
    throw $Exception
    end
    local.get $this
    call $__require_nonnull
    i32.const 2
    call $Stream_link
    local.set $result
    local.get $result
    local.get $predicate
    i32.store offset=24
    local.get $result
    return
  )
  (func $Stream_peek (export "Stream_peek") (param $this i32) (param $observer i32) (result i32)
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
    (local $__allocation$57 i32)
    (local $__receiver$87 i32)
    (local $__caught_exc i32)
    local.get $observer
    i32.const 0
    i32.eq
    if
    i32.const 8
    call $__javaw_object_alloc
    local.set $__allocation$57
    local.get $__allocation$57
    i32.const 1584
    i32.store offset=0
    local.get $__allocation$57
    call $NullPointerException__init
    local.get $__allocation$57
    throw $Exception
    end
    local.get $this
    call $__require_nonnull
    i32.const 3
    call $Stream_link
    local.set $result
    local.get $result
    local.get $observer
    i32.store offset=28
    local.get $result
    return
  )
  (func $Stream_limit (export "Stream_limit") (param $this i32) (param $count i64) (result i32)
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
    (local $__allocation$58 i32)
    (local $__receiver$88 i32)
    (local $__caught_exc i32)
    local.get $count
    i32.const 0
    i64.extend_i32_s
    i64.lt_s
    if
    i32.const 8
    call $__javaw_object_alloc
    local.set $__allocation$58
    local.get $__allocation$58
    i32.const 1502
    i32.store offset=0
    local.get $__allocation$58
    call $IllegalArgumentException__init
    local.get $__allocation$58
    throw $Exception
    end
    local.get $this
    call $__require_nonnull
    i32.const 4
    call $Stream_link
    local.set $result
    local.get $result
    local.get $count
    i64.store offset=40
    local.get $result
    return
  )
  (func $Stream_skip (export "Stream_skip") (param $this i32) (param $count i64) (result i32)
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
    (local $__allocation$59 i32)
    (local $__receiver$89 i32)
    (local $__caught_exc i32)
    local.get $count
    i32.const 0
    i64.extend_i32_s
    i64.lt_s
    if
    i32.const 8
    call $__javaw_object_alloc
    local.set $__allocation$59
    local.get $__allocation$59
    i32.const 1502
    i32.store offset=0
    local.get $__allocation$59
    call $IllegalArgumentException__init
    local.get $__allocation$59
    throw $Exception
    end
    local.get $this
    call $__require_nonnull
    i32.const 5
    call $Stream_link
    local.set $result
    local.get $result
    local.get $count
    i64.store offset=40
    local.get $result
    return
  )
  (func $Stream_distinct (export "Stream_distinct") (param $this i32) (result i32)
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
    (local $__allocation$60 i32)
    (local $__receiver$90 i32)
    (local $__caught_exc i32)
    local.get $this
    call $__require_nonnull
    i32.const 6
    call $Stream_link
    local.set $result
    local.get $result
    i32.const 8
    call $__javaw_object_alloc
    local.set $__allocation$60
    local.get $__allocation$60
    i32.const 713
    i32.store offset=0
    local.get $__allocation$60
    call $HashSet__init
    local.get $__allocation$60
    i32.store offset=36
    local.get $result
    return
  )
  (func $Stream_flatMap (export "Stream_flatMap") (param $this i32) (param $mapper i32) (result i32)
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
    (local $__allocation$61 i32)
    (local $__allocation$62 i32)
    (local $__receiver$91 i32)
    (local $__caught_exc i32)
    local.get $mapper
    i32.const 0
    i32.eq
    if
    i32.const 8
    call $__javaw_object_alloc
    local.set $__allocation$61
    local.get $__allocation$61
    i32.const 1584
    i32.store offset=0
    local.get $__allocation$61
    call $NullPointerException__init
    local.get $__allocation$61
    throw $Exception
    end
    local.get $this
    call $__require_nonnull
    call $Stream_start
    i32.const 64
    call $__javaw_object_alloc
    local.set $__allocation$62
    local.get $__allocation$62
    i32.const 995
    i32.store offset=0
    local.get $__allocation$62
    local.get $this
    i32.const 7
    call $Stream__init_1
    local.get $__allocation$62
    local.set $result
    local.get $result
    local.get $mapper
    i32.store offset=20
    local.get $result
    return
  )
  (func $Stream_advance (export "Stream_advance") (param $this i32) (result i32)
    (local $value i32)
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
    (local $__mutation$26$address i32)
    (local $__mutation$26$old i64)
    (local $__mutation$26$new i64)
    (local $__mutation$27$address i32)
    (local $__mutation$27$old i64)
    (local $__mutation$27$new i64)
    (local $__allocation$63 i32)
    (local $__receiver$92 i32)
    (local $__receiver$93 i32)
    (local $__receiver$94 i32)
    (local $__receiver$95 i32)
    (local $__receiver$96 i32)
    (local $__receiver$97 i32)
    (local $__receiver$98 i32)
    (local $__receiver$99 i32)
    (local $__receiver$100 i32)
    (local $__receiver$101 i32)
    (local $__receiver$102 i32)
    (local $__receiver$103 i32)
    (local $__receiver$104 i32)
    (local $__caught_exc i32)
    local.get $this
    i32.load offset=56
    if
    i32.const 8
    call $__javaw_object_alloc
    local.set $__allocation$63
    local.get $__allocation$63
    i32.const 1543
    i32.store offset=0
    local.get $__allocation$63
    call $IllegalStateException__init
    local.get $__allocation$63
    throw $Exception
    end
    local.get $this
    i32.load offset=16
    i32.const 0
    i32.eq
    if
    local.get $this
    i32.load offset=4
    i32.const 0
    i32.eq
    if
    local.get $this
    local.get $this
    i32.load offset=8
    call $__require_nonnull
    local.tee $__receiver$92
    local.get $__receiver$92
    i32.load offset=0
    i32.const 17
    i32.add
    call_indirect (type $t_sig_2)
    i32.store offset=4
    end
    local.get $this
    i32.load offset=4
    call $__require_nonnull
    local.tee $__receiver$93
    local.get $__receiver$93
    i32.load offset=0
    i32.const 18
    i32.add
    call_indirect (type $t_sig_2)
    i32.eqz
    if
    i32.const 0
    return
    end
    local.get $this
    local.get $this
    i32.load offset=4
    call $__require_nonnull
    local.tee $__receiver$94
    local.get $__receiver$94
    i32.load offset=0
    i32.const 19
    i32.add
    call_indirect (type $t_sig_2)
    i32.store offset=48
    i32.const 1
    return
    end
    local.get $this
    i32.load offset=16
    i32.const 4
    i32.eq
    if (result i32)
    local.get $this
    i64.load offset=40
    i32.const 0
    i64.extend_i32_s
    i64.eq
    else
    i32.const 0
    end
    if
    i32.const 0
    return
    end
    local.get $this
    i32.load offset=16
    i32.const 7
    i32.eq
    if
    (block $break_while_20
      (loop $loop_while_20
    i32.const 1
        i32.eqz
        br_if $break_while_20
    local.get $this
    i32.load offset=32
    i32.const 0
    i32.ne
    if (result i32)
    local.get $this
    i32.load offset=32
    call $__require_nonnull
    call $Stream_advance
    else
    i32.const 0
    end
    if
    local.get $this
    local.get $this
    i32.load offset=32
    i32.load offset=48
    i32.store offset=48
    i32.const 1
    return
    end
    local.get $this
    i32.load offset=32
    i32.const 0
    i32.ne
    if
    local.get $this
    i32.load offset=32
    call $__require_nonnull
    local.tee $__receiver$96
    local.get $__receiver$96
    i32.load offset=0
    i32.const 3
    i32.add
    call_indirect (type $t_sig_5)
    local.get $this
    i32.const 0
    i32.store offset=32
    end
    local.get $this
    i32.load offset=12
    call $__require_nonnull
    call $Stream_advance
    i32.eqz
    if
    i32.const 0
    return
    end
    local.get $this
    local.get $this
    i32.load offset=20
    call $__require_nonnull
    local.tee $__receiver$98
    local.get $this
    i32.load offset=12
    i32.load offset=48
    local.get $__receiver$98
    i32.load offset=0
    i32.const 12
    i32.add
    call_indirect (type $t_sig_3)
    i32.store offset=32
    local.get $this
    i32.load offset=32
    i32.const 0
    i32.ne
    if
    local.get $this
    i32.load offset=32
    call $__require_nonnull
    call $Stream_start
    end
        br $loop_while_20
      )
    )
    end
    (block $break_while_21
      (loop $loop_while_21
    local.get $this
    i32.load offset=12
    call $__require_nonnull
    call $Stream_advance
        i32.eqz
        br_if $break_while_21
    local.get $this
    i32.load offset=12
    i32.load offset=48
    local.set $value
    local.get $this
    i32.load offset=16
    i32.const 2
    i32.eq
    if (result i32)
    local.get $this
    i32.load offset=24
    call $__require_nonnull
    local.tee $__receiver$101
    local.get $value
    local.get $__receiver$101
    i32.load offset=0
    i32.const 13
    i32.add
    call_indirect (type $t_sig_3)
    i32.eqz
    else
    i32.const 0
    end
    if
    br $loop_while_21
    end
    local.get $this
    i32.load offset=16
    i32.const 5
    i32.eq
    if (result i32)
    local.get $this
    i64.load offset=40
    i32.const 0
    i64.extend_i32_s
    i64.gt_s
    else
    i32.const 0
    end
    if
    local.get $this
    i32.const 40
    i32.add
    local.set $__mutation$26$address
    local.get $__mutation$26$address
    i64.load
    local.tee $__mutation$26$old
    i64.const 1
    i64.sub
    local.set $__mutation$26$new
    local.get $__mutation$26$address
    local.get $__mutation$26$new
    i64.store
    local.get $__mutation$26$old
    drop
    br $loop_while_21
    end
    local.get $this
    i32.load offset=16
    i32.const 6
    i32.eq
    if (result i32)
    local.get $this
    i32.load offset=36
    call $__require_nonnull
    local.tee $__receiver$102
    local.get $value
    local.get $__receiver$102
    i32.load offset=0
    i32.const 22
    i32.add
    call_indirect (type $t_sig_3)
    i32.eqz
    else
    i32.const 0
    end
    if
    br $loop_while_21
    end
    local.get $this
    i32.load offset=16
    i32.const 1
    i32.eq
    if
    local.get $this
    i32.load offset=20
    call $__require_nonnull
    local.tee $__receiver$103
    local.get $value
    local.get $__receiver$103
    i32.load offset=0
    i32.const 12
    i32.add
    call_indirect (type $t_sig_3)
    local.set $value
    end
    local.get $this
    i32.load offset=16
    i32.const 3
    i32.eq
    if
    local.get $this
    i32.load offset=28
    call $__require_nonnull
    local.tee $__receiver$104
    local.get $value
    local.get $__receiver$104
    i32.load offset=0
    i32.const 14
    i32.add
    call_indirect (type $t_sig_1)
    end
    local.get $this
    i32.load offset=16
    i32.const 4
    i32.eq
    if
    local.get $this
    i32.const 40
    i32.add
    local.set $__mutation$27$address
    local.get $__mutation$27$address
    i64.load
    local.tee $__mutation$27$old
    i64.const 1
    i64.sub
    local.set $__mutation$27$new
    local.get $__mutation$27$address
    local.get $__mutation$27$new
    i64.store
    local.get $__mutation$27$old
    drop
    end
    local.get $this
    local.get $value
    i32.store offset=48
    i32.const 1
    return
        br $loop_while_21
      )
    )
    i32.const 0
    return
  )
  (func $Stream_count (export "Stream_count") (param $this i32) (result i64)
    (local $count i64)
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
    (local $__mutation$28$address i32)
    (local $__mutation$28$old i64)
    (local $__mutation$28$new i64)
    (local $__receiver$105 i32)
    (local $__receiver$106 i32)
    (local $__caught_exc i32)
    local.get $this
    call $__require_nonnull
    call $Stream_start
    i32.const 0
    i64.extend_i32_s
    local.set $count
    (block $break_while_22
      (loop $loop_while_22
    local.get $this
    call $__require_nonnull
    call $Stream_advance
        i32.eqz
        br_if $break_while_22
    local.get $count
    local.tee $__mutation$28$old
    i64.const 1
    i64.add
    local.set $__mutation$28$new
    local.get $__mutation$28$new
    local.set $count
    local.get $__mutation$28$old
    drop
        br $loop_while_22
      )
    )
    local.get $count
    return
  )
  (func $Stream_findFirst (export "Stream_findFirst") (param $this i32) (result i32)
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
    (local $__receiver$107 i32)
    (local $__receiver$108 i32)
    (local $__receiver$109 i32)
    (local $__receiver$110 i32)
    (local $__caught_exc i32)
    local.get $this
    call $__require_nonnull
    call $Stream_start
    local.get $this
    call $__require_nonnull
    call $Stream_advance
    if
    local.get $this
    i32.load offset=48
    call $Optional_of
    return
    end
    call $Optional_empty
    return
  )
  (func $Stream_findAny (export "Stream_findAny") (param $this i32) (result i32)
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
    (local $__receiver$111 i32)
    (local $__caught_exc i32)
    local.get $this
    call $__require_nonnull
    local.tee $__receiver$111
    local.get $__receiver$111
    i32.load offset=0
    i32.const 48
    i32.add
    call_indirect (type $t_sig_2)
    return
  )
  (func $Stream_anyMatch (export "Stream_anyMatch") (param $this i32) (param $predicate i32) (result i32)
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
    (local $__allocation$64 i32)
    (local $__receiver$112 i32)
    (local $__receiver$113 i32)
    (local $__receiver$114 i32)
    (local $__caught_exc i32)
    local.get $predicate
    i32.const 0
    i32.eq
    if
    i32.const 8
    call $__javaw_object_alloc
    local.set $__allocation$64
    local.get $__allocation$64
    i32.const 1584
    i32.store offset=0
    local.get $__allocation$64
    call $NullPointerException__init
    local.get $__allocation$64
    throw $Exception
    end
    local.get $this
    call $__require_nonnull
    call $Stream_start
    (block $break_while_23
      (loop $loop_while_23
    local.get $this
    call $__require_nonnull
    call $Stream_advance
        i32.eqz
        br_if $break_while_23
    local.get $predicate
    call $__require_nonnull
    local.tee $__receiver$114
    local.get $this
    i32.load offset=48
    local.get $__receiver$114
    i32.load offset=0
    i32.const 13
    i32.add
    call_indirect (type $t_sig_3)
    if
    i32.const 1
    return
    end
        br $loop_while_23
      )
    )
    i32.const 0
    return
  )
  (func $Stream_allMatch (export "Stream_allMatch") (param $this i32) (param $predicate i32) (result i32)
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
    (local $__allocation$65 i32)
    (local $__receiver$115 i32)
    (local $__receiver$116 i32)
    (local $__receiver$117 i32)
    (local $__caught_exc i32)
    local.get $predicate
    i32.const 0
    i32.eq
    if
    i32.const 8
    call $__javaw_object_alloc
    local.set $__allocation$65
    local.get $__allocation$65
    i32.const 1584
    i32.store offset=0
    local.get $__allocation$65
    call $NullPointerException__init
    local.get $__allocation$65
    throw $Exception
    end
    local.get $this
    call $__require_nonnull
    call $Stream_start
    (block $break_while_24
      (loop $loop_while_24
    local.get $this
    call $__require_nonnull
    call $Stream_advance
        i32.eqz
        br_if $break_while_24
    local.get $predicate
    call $__require_nonnull
    local.tee $__receiver$117
    local.get $this
    i32.load offset=48
    local.get $__receiver$117
    i32.load offset=0
    i32.const 13
    i32.add
    call_indirect (type $t_sig_3)
    i32.eqz
    if
    i32.const 0
    return
    end
        br $loop_while_24
      )
    )
    i32.const 1
    return
  )
  (func $Stream_noneMatch (export "Stream_noneMatch") (param $this i32) (param $predicate i32) (result i32)
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
    (local $__allocation$66 i32)
    (local $__receiver$118 i32)
    (local $__receiver$119 i32)
    (local $__receiver$120 i32)
    (local $__caught_exc i32)
    local.get $predicate
    i32.const 0
    i32.eq
    if
    i32.const 8
    call $__javaw_object_alloc
    local.set $__allocation$66
    local.get $__allocation$66
    i32.const 1584
    i32.store offset=0
    local.get $__allocation$66
    call $NullPointerException__init
    local.get $__allocation$66
    throw $Exception
    end
    local.get $this
    call $__require_nonnull
    call $Stream_start
    (block $break_while_25
      (loop $loop_while_25
    local.get $this
    call $__require_nonnull
    call $Stream_advance
        i32.eqz
        br_if $break_while_25
    local.get $predicate
    call $__require_nonnull
    local.tee $__receiver$120
    local.get $this
    i32.load offset=48
    local.get $__receiver$120
    i32.load offset=0
    i32.const 13
    i32.add
    call_indirect (type $t_sig_3)
    if
    i32.const 0
    return
    end
        br $loop_while_25
      )
    )
    i32.const 1
    return
  )
  (func $Stream_forEach (export "Stream_forEach") (param $this i32) (param $action i32)
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
    (local $__allocation$67 i32)
    (local $__receiver$121 i32)
    (local $__receiver$122 i32)
    (local $__receiver$123 i32)
    (local $__caught_exc i32)
    local.get $action
    i32.const 0
    i32.eq
    if
    i32.const 8
    call $__javaw_object_alloc
    local.set $__allocation$67
    local.get $__allocation$67
    i32.const 1584
    i32.store offset=0
    local.get $__allocation$67
    call $NullPointerException__init
    local.get $__allocation$67
    throw $Exception
    end
    local.get $this
    call $__require_nonnull
    call $Stream_start
    (block $break_while_26
      (loop $loop_while_26
    local.get $this
    call $__require_nonnull
    call $Stream_advance
        i32.eqz
        br_if $break_while_26
    local.get $action
    call $__require_nonnull
    local.tee $__receiver$123
    local.get $this
    i32.load offset=48
    local.get $__receiver$123
    i32.load offset=0
    i32.const 14
    i32.add
    call_indirect (type $t_sig_1)
        br $loop_while_26
      )
    )
  )
  (func $Stream_forEachOrdered (export "Stream_forEachOrdered") (param $this i32) (param $action i32)
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
    (local $__receiver$124 i32)
    (local $__caught_exc i32)
    local.get $this
    call $__require_nonnull
    local.tee $__receiver$124
    local.get $action
    local.get $__receiver$124
    i32.load offset=0
    i32.const 53
    i32.add
    call_indirect (type $t_sig_1)
  )
  (func $Stream_reduce__4f_62_6a_65_63_74___42_69_6e_61_72_79_4f_70_65_72_61_74_6f_72_3c_4f_62_6a_65_63_74_3e_ (export "Stream_reduce__4f_62_6a_65_63_74___42_69_6e_61_72_79_4f_70_65_72_61_74_6f_72_3c_4f_62_6a_65_63_74_3e_") (param $this i32) (param $identity i32) (param $accumulator i32) (result i32)
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
    (local $__allocation$68 i32)
    (local $__receiver$125 i32)
    (local $__receiver$126 i32)
    (local $__receiver$127 i32)
    (local $__caught_exc i32)
    local.get $accumulator
    i32.const 0
    i32.eq
    if
    i32.const 8
    call $__javaw_object_alloc
    local.set $__allocation$68
    local.get $__allocation$68
    i32.const 1584
    i32.store offset=0
    local.get $__allocation$68
    call $NullPointerException__init
    local.get $__allocation$68
    throw $Exception
    end
    local.get $this
    call $__require_nonnull
    call $Stream_start
    local.get $identity
    local.set $result
    (block $break_while_27
      (loop $loop_while_27
    local.get $this
    call $__require_nonnull
    call $Stream_advance
        i32.eqz
        br_if $break_while_27
    local.get $accumulator
    call $__require_nonnull
    local.tee $__receiver$127
    local.get $result
    local.get $this
    i32.load offset=48
    local.get $__receiver$127
    i32.load offset=0
    i32.const 16
    i32.add
    call_indirect (type $t_sig_4)
    local.set $result
        br $loop_while_27
      )
    )
    local.get $result
    return
  )
  (func $Stream_reduce__42_69_6e_61_72_79_4f_70_65_72_61_74_6f_72_3c_4f_62_6a_65_63_74_3e_ (export "Stream_reduce__42_69_6e_61_72_79_4f_70_65_72_61_74_6f_72_3c_4f_62_6a_65_63_74_3e_") (param $this i32) (param $accumulator i32) (result i32)
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
    (local $__allocation$69 i32)
    (local $__receiver$128 i32)
    (local $__receiver$129 i32)
    (local $__receiver$130 i32)
    (local $__receiver$131 i32)
    (local $__receiver$132 i32)
    (local $__receiver$133 i32)
    (local $__caught_exc i32)
    local.get $accumulator
    i32.const 0
    i32.eq
    if
    i32.const 8
    call $__javaw_object_alloc
    local.set $__allocation$69
    local.get $__allocation$69
    i32.const 1584
    i32.store offset=0
    local.get $__allocation$69
    call $NullPointerException__init
    local.get $__allocation$69
    throw $Exception
    end
    local.get $this
    call $__require_nonnull
    call $Stream_start
    local.get $this
    call $__require_nonnull
    call $Stream_advance
    i32.eqz
    if
    call $Optional_empty
    return
    end
    local.get $this
    i32.load offset=48
    local.set $result
    (block $break_while_28
      (loop $loop_while_28
    local.get $this
    call $__require_nonnull
    call $Stream_advance
        i32.eqz
        br_if $break_while_28
    local.get $accumulator
    call $__require_nonnull
    local.tee $__receiver$132
    local.get $result
    local.get $this
    i32.load offset=48
    local.get $__receiver$132
    i32.load offset=0
    i32.const 16
    i32.add
    call_indirect (type $t_sig_4)
    local.set $result
        br $loop_while_28
      )
    )
    local.get $result
    call $Optional_of
    return
  )
  (func $Stream_min (export "Stream_min") (param $this i32) (param $comparator i32) (result i32)
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
    (local $__allocation$70 i32)
    (local $__receiver$134 i32)
    (local $__receiver$135 i32)
    (local $__receiver$136 i32)
    (local $__receiver$137 i32)
    (local $__receiver$138 i32)
    (local $__receiver$139 i32)
    (local $__caught_exc i32)
    local.get $comparator
    i32.const 0
    i32.eq
    if
    i32.const 8
    call $__javaw_object_alloc
    local.set $__allocation$70
    local.get $__allocation$70
    i32.const 1584
    i32.store offset=0
    local.get $__allocation$70
    call $NullPointerException__init
    local.get $__allocation$70
    throw $Exception
    end
    local.get $this
    call $__require_nonnull
    call $Stream_start
    local.get $this
    call $__require_nonnull
    call $Stream_advance
    i32.eqz
    if
    call $Optional_empty
    return
    end
    local.get $this
    i32.load offset=48
    local.set $result
    (block $break_while_29
      (loop $loop_while_29
    local.get $this
    call $__require_nonnull
    call $Stream_advance
        i32.eqz
        br_if $break_while_29
    local.get $comparator
    call $__require_nonnull
    local.tee $__receiver$138
    local.get $this
    i32.load offset=48
    local.get $result
    local.get $__receiver$138
    i32.load offset=0
    i32.const 38
    i32.add
    call_indirect (type $t_sig_4)
    i32.const 0
    i32.lt_s
    if
    local.get $this
    i32.load offset=48
    local.set $result
    end
        br $loop_while_29
      )
    )
    local.get $result
    call $Optional_of
    return
  )
  (func $Stream_max (export "Stream_max") (param $this i32) (param $comparator i32) (result i32)
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
    (local $__allocation$71 i32)
    (local $__receiver$140 i32)
    (local $__receiver$141 i32)
    (local $__receiver$142 i32)
    (local $__receiver$143 i32)
    (local $__receiver$144 i32)
    (local $__receiver$145 i32)
    (local $__caught_exc i32)
    local.get $comparator
    i32.const 0
    i32.eq
    if
    i32.const 8
    call $__javaw_object_alloc
    local.set $__allocation$71
    local.get $__allocation$71
    i32.const 1584
    i32.store offset=0
    local.get $__allocation$71
    call $NullPointerException__init
    local.get $__allocation$71
    throw $Exception
    end
    local.get $this
    call $__require_nonnull
    call $Stream_start
    local.get $this
    call $__require_nonnull
    call $Stream_advance
    i32.eqz
    if
    call $Optional_empty
    return
    end
    local.get $this
    i32.load offset=48
    local.set $result
    (block $break_while_30
      (loop $loop_while_30
    local.get $this
    call $__require_nonnull
    call $Stream_advance
        i32.eqz
        br_if $break_while_30
    local.get $comparator
    call $__require_nonnull
    local.tee $__receiver$144
    local.get $this
    i32.load offset=48
    local.get $result
    local.get $__receiver$144
    i32.load offset=0
    i32.const 38
    i32.add
    call_indirect (type $t_sig_4)
    i32.const 0
    i32.gt_s
    if
    local.get $this
    i32.load offset=48
    local.set $result
    end
        br $loop_while_30
      )
    )
    local.get $result
    call $Optional_of
    return
  )
  (func $Stream_toArray (export "Stream_toArray") (param $this i32) (result i32)
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
    (local $__allocation$72 i32)
    (local $__receiver$146 i32)
    (local $__receiver$147 i32)
    (local $__receiver$148 i32)
    (local $__receiver$149 i32)
    (local $__caught_exc i32)
    local.get $this
    call $__require_nonnull
    call $Stream_start
    i32.const 16
    call $__javaw_object_alloc
    local.set $__allocation$72
    local.get $__allocation$72
    i32.const 470
    i32.store offset=0
    local.get $__allocation$72
    call $ArrayList__init
    local.get $__allocation$72
    local.set $result
    (block $break_while_31
      (loop $loop_while_31
    local.get $this
    call $__require_nonnull
    call $Stream_advance
        i32.eqz
        br_if $break_while_31
    local.get $result
    call $__require_nonnull
    local.tee $__receiver$148
    local.get $this
    i32.load offset=48
    local.get $__receiver$148
    i32.load offset=0
    i32.const 22
    i32.add
    call_indirect (type $t_sig_3)
    drop
        br $loop_while_31
      )
    )
    local.get $result
    call $__require_nonnull
    local.tee $__receiver$149
    local.get $__receiver$149
    i32.load offset=0
    i32.const 41
    i32.add
    call_indirect (type $t_sig_2)
    return
  )
  (func $Stream_collect (export "Stream_collect") (param $this i32) (param $collector i32) (result i32)
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
    (local $__allocation$73 i32)
    (local $__receiver$150 i32)
    (local $__receiver$151 i32)
    (local $__caught_exc i32)
    local.get $collector
    i32.const 0
    i32.eq
    if
    i32.const 8
    call $__javaw_object_alloc
    local.set $__allocation$73
    local.get $__allocation$73
    i32.const 1584
    i32.store offset=0
    local.get $__allocation$73
    call $NullPointerException__init
    local.get $__allocation$73
    throw $Exception
    end
    local.get $this
    call $__require_nonnull
    call $Stream_available
    local.get $collector
    call $__require_nonnull
    local.tee $__receiver$151
    local.get $this
    local.get $__receiver$151
    i32.load offset=0
    i32.const 39
    i32.add
    call_indirect (type $t_sig_3)
    return
  )
  (func $Stream_toList (export "Stream_toList") (param $this i32) (result i32)
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
    (local $__allocation$74 i32)
    (local $__receiver$152 i32)
    (local $__caught_exc i32)
    i32.const 16
    call $__javaw_object_alloc
    local.set $__allocation$74
    local.get $__allocation$74
    i32.const 873
    i32.store offset=0
    local.get $__allocation$74
    local.get $this
    call $__require_nonnull
    local.tee $__receiver$152
    local.get $__receiver$152
    i32.load offset=0
    i32.const 59
    i32.add
    call_indirect (type $t_sig_2)
    call $__JavawReadOnlyList__init
    local.get $__allocation$74
    return
  )
  (func $Stream_isParallel (export "Stream_isParallel") (param $this i32) (result i32)
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
    i32.const 0
    return
  )
  (func $Stream_sequential (export "Stream_sequential") (param $this i32) (result i32)
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
    (local $__receiver$153 i32)
    (local $__caught_exc i32)
    local.get $this
    call $__require_nonnull
    call $Stream_available
    local.get $this
    return
  )
  (func $Stream_parallel (export "Stream_parallel") (param $this i32) (result i32)
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
    (local $__allocation$75 i32)
    (local $__caught_exc i32)
    i32.const 8
    call $__javaw_object_alloc
    local.set $__allocation$75
    local.get $__allocation$75
    i32.const 1748
    i32.store offset=0
    local.get $__allocation$75
    call $UnsupportedOperationException__init
    local.get $__allocation$75
    throw $Exception
    i32.const 0
  )
  (func $Stream_close (export "Stream_close") (param $this i32)
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
    (local $__receiver$154 i32)
    (local $__receiver$155 i32)
    (local $__caught_exc i32)
    local.get $this
    i32.const 1
    i32.store offset=56
    local.get $this
    i32.load offset=32
    i32.const 0
    i32.ne
    if
    local.get $this
    i32.load offset=32
    call $__require_nonnull
    local.tee $__receiver$154
    local.get $__receiver$154
    i32.load offset=0
    i32.const 3
    i32.add
    call_indirect (type $t_sig_5)
    end
    local.get $this
    i32.load offset=12
    i32.const 0
    i32.ne
    if
    local.get $this
    i32.load offset=12
    call $__require_nonnull
    local.tee $__receiver$155
    local.get $__receiver$155
    i32.load offset=0
    i32.const 3
    i32.add
    call_indirect (type $t_sig_5)
    end
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
    (local $__allocation$76 i32)
    (local $__caught_exc i32)
    i32.const 8
    call $__javaw_object_alloc
    local.set $__allocation$76
    local.get $__allocation$76
    i32.const 1543
    i32.store offset=0
    local.get $__allocation$76
    call $IllegalStateException__init
    local.get $__allocation$76
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
    (local $__allocation$77 i32)
    (local $__caught_exc i32)
    i32.const 8
    call $__javaw_object_alloc
    local.set $__allocation$77
    local.get $__allocation$77
    i32.const 1543
    i32.store offset=0
    local.get $__allocation$77
    call $IllegalStateException__init
    local.get $__allocation$77
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
    (local $__allocation$78 i32)
    (local $__caught_exc i32)
    i32.const 8
    call $__javaw_object_alloc
    local.set $__allocation$78
    local.get $__allocation$78
    i32.const 1543
    i32.store offset=0
    local.get $__allocation$78
    call $IllegalStateException__init
    local.get $__allocation$78
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
    (local $__allocation$79 i32)
    (local $__caught_exc i32)
    i32.const 8
    call $__javaw_object_alloc
    local.set $__allocation$79
    local.get $__allocation$79
    i32.const 1543
    i32.store offset=0
    local.get $__allocation$79
    call $IllegalStateException__init
    local.get $__allocation$79
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
    (local $__allocation$80 i32)
    (local $__allocation$81 i32)
    (local $__caught_exc i32)
    local.get $value
    if
    global.get $__trueBoolean
    i32.const 0
    i32.eq
    if
    i32.const 16
    call $__javaw_object_alloc
    local.set $__allocation$80
    local.get $__allocation$80
    i32.const 1104
    i32.store offset=0
    local.get $__allocation$80
    i32.const 1
    call $Boolean__init
    local.get $__allocation$80
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
    call $__javaw_object_alloc
    local.set $__allocation$81
    local.get $__allocation$81
    i32.const 1104
    i32.store offset=0
    local.get $__allocation$81
    i32.const 0
    call $Boolean__init
    local.get $__allocation$81
    global.set $__falseBoolean
    end
    global.get $__falseBoolean
    return
  )
  (func $Boolean_valueOf__53_74_72_69_6e_67_ (export "Boolean_valueOf__53_74_72_69_6e_67_") (param $text i32) (result i32)
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
    (local $__receiver$156 i32)
    (local $__receiver$157 i32)
    (local $__caught_exc i32)
    local.get $text
    call $__str_parse_bool
    call $Boolean_valueOf__62_6f_6f_6c_65_61_6e_
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
      i32.const 1104
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
    (local $__allocation$82 i32)
    (local $__allocation$83 i32)
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
    call $__javaw_object_alloc
    local.set $__allocation$82
    local.get $__allocation$82
    i32.const 1145
    i32.store offset=0
    local.get $__allocation$82
    local.get $value
    call $Byte__init
    local.get $__allocation$82
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
    call $__javaw_object_alloc
    local.set $__allocation$83
    local.get $__allocation$83
    i32.const 1145
    i32.store offset=0
    local.get $__allocation$83
    local.get $value
    call $Byte__init
    local.get $__allocation$83
    return
  )
  (func $Byte_valueOf__53_74_72_69_6e_67_ (export "Byte_valueOf__53_74_72_69_6e_67_") (param $text i32) (result i32)
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
    (local $__receiver$158 i32)
    (local $__receiver$159 i32)
    (local $__caught_exc i32)
    local.get $text
    call $__str_parse_i32
    i32.extend8_s
    call $Byte_valueOf__62_79_74_65_
    return
  )
  (func $Byte_valueOf__53_74_72_69_6e_67___69_6e_74_ (export "Byte_valueOf__53_74_72_69_6e_67___69_6e_74_") (param $text i32) (param $radix i32) (result i32)
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
    (local $__receiver$160 i32)
    (local $__receiver$161 i32)
    (local $__caught_exc i32)
    local.get $text
    local.get $radix
    call $__str_parse_i32_radix
    i32.extend8_s
    call $Byte_valueOf__62_79_74_65_
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
      i32.const 1145
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
  (func $valueOf (export "valueOf") (param $value i32) (result i32)
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
    (local $__allocation$84 i32)
    (local $__allocation$85 i32)
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
    call $__javaw_object_alloc
    local.set $__allocation$84
    local.get $__allocation$84
    i32.const 1191
    i32.store offset=0
    local.get $__allocation$84
    local.get $value
    call $Character__init
    local.get $__allocation$84
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
    call $__javaw_object_alloc
    local.set $__allocation$85
    local.get $__allocation$85
    i32.const 1191
    i32.store offset=0
    local.get $__allocation$85
    local.get $value
    call $Character__init
    local.get $__allocation$85
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
      i32.const 1191
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
    (local $__allocation$86 i32)
    (local $__caught_exc i32)
    i32.const 16
    call $__javaw_object_alloc
    local.set $__allocation$86
    local.get $__allocation$86
    i32.const 1232
    i32.store offset=0
    local.get $__allocation$86
    local.get $value
    call $Double__init
    local.get $__allocation$86
    return
  )
  (func $Double_valueOf__53_74_72_69_6e_67_ (export "Double_valueOf__53_74_72_69_6e_67_") (param $text i32) (result i32)
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
    (local $__receiver$162 i32)
    (local $__receiver$163 i32)
    (local $__caught_exc i32)
    local.get $text
    call $__str_parse_f64
    call $Double_valueOf__64_6f_75_62_6c_65_
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
    (local $__receiver$164 i32)
    (local $__receiver$165 i32)
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
      i32.const 1232
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
    (local $__receiver$166 i32)
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
    (local $__allocation$87 i32)
    (local $__caught_exc i32)
    i32.const 16
    call $__javaw_object_alloc
    local.set $__allocation$87
    local.get $__allocation$87
    i32.const 1278
    i32.store offset=0
    local.get $__allocation$87
    local.get $value
    call $Float__init
    local.get $__allocation$87
    return
  )
  (func $Float_valueOf__53_74_72_69_6e_67_ (export "Float_valueOf__53_74_72_69_6e_67_") (param $text i32) (result i32)
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
    (local $__receiver$167 i32)
    (local $__receiver$168 i32)
    (local $__caught_exc i32)
    local.get $text
    call $__str_parse_f64
    f32.demote_f64
    call $Float_valueOf__66_6c_6f_61_74_
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
    (local $__receiver$169 i32)
    (local $__receiver$170 i32)
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
      i32.const 1278
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
    (local $__receiver$171 i32)
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
    (local $__allocation$88 i32)
    (local $__allocation$89 i32)
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
    call $__javaw_object_alloc
    local.set $__allocation$88
    local.get $__allocation$88
    i32.const 1324
    i32.store offset=0
    local.get $__allocation$88
    local.get $value
    call $Integer__init
    local.get $__allocation$88
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
    call $__javaw_object_alloc
    local.set $__allocation$89
    local.get $__allocation$89
    i32.const 1324
    i32.store offset=0
    local.get $__allocation$89
    local.get $value
    call $Integer__init
    local.get $__allocation$89
    return
  )
  (func $Integer_valueOf__53_74_72_69_6e_67_ (export "Integer_valueOf__53_74_72_69_6e_67_") (param $text i32) (result i32)
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
    (local $__receiver$172 i32)
    (local $__receiver$173 i32)
    (local $__caught_exc i32)
    local.get $text
    call $__str_parse_i32
    call $Integer_valueOf__69_6e_74_
    return
  )
  (func $Integer_valueOf__53_74_72_69_6e_67___69_6e_74_ (export "Integer_valueOf__53_74_72_69_6e_67___69_6e_74_") (param $text i32) (param $radix i32) (result i32)
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
    (local $__receiver$174 i32)
    (local $__receiver$175 i32)
    (local $__caught_exc i32)
    local.get $text
    local.get $radix
    call $__str_parse_i32_radix
    call $Integer_valueOf__69_6e_74_
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
      i32.const 1324
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
    (local $__allocation$90 i32)
    (local $__allocation$91 i32)
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
    call $__javaw_object_alloc
    local.set $__allocation$90
    local.get $__allocation$90
    i32.const 1370
    i32.store offset=0
    local.get $__allocation$90
    local.get $value
    call $Long__init
    local.get $__allocation$90
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
    call $__javaw_object_alloc
    local.set $__allocation$91
    local.get $__allocation$91
    i32.const 1370
    i32.store offset=0
    local.get $__allocation$91
    local.get $value
    call $Long__init
    local.get $__allocation$91
    return
  )
  (func $Long_valueOf__53_74_72_69_6e_67_ (export "Long_valueOf__53_74_72_69_6e_67_") (param $text i32) (result i32)
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
    (local $__receiver$176 i32)
    (local $__receiver$177 i32)
    (local $__caught_exc i32)
    local.get $text
    call $__str_parse_i64
    call $Long_valueOf__6c_6f_6e_67_
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
      i32.const 1370
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
    (local $__allocation$92 i32)
    (local $__allocation$93 i32)
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
    call $__javaw_object_alloc
    local.set $__allocation$92
    local.get $__allocation$92
    i32.const 1416
    i32.store offset=0
    local.get $__allocation$92
    local.get $value
    call $Short__init
    local.get $__allocation$92
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
    call $__javaw_object_alloc
    local.set $__allocation$93
    local.get $__allocation$93
    i32.const 1416
    i32.store offset=0
    local.get $__allocation$93
    local.get $value
    call $Short__init
    local.get $__allocation$93
    return
  )
  (func $Short_valueOf__53_74_72_69_6e_67_ (export "Short_valueOf__53_74_72_69_6e_67_") (param $text i32) (result i32)
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
    (local $__receiver$178 i32)
    (local $__receiver$179 i32)
    (local $__caught_exc i32)
    local.get $text
    call $__str_parse_i32
    i32.extend16_s
    call $Short_valueOf__73_68_6f_72_74_
    return
  )
  (func $Short_valueOf__53_74_72_69_6e_67___69_6e_74_ (export "Short_valueOf__53_74_72_69_6e_67___69_6e_74_") (param $text i32) (param $radix i32) (result i32)
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
    (local $__receiver$180 i32)
    (local $__receiver$181 i32)
    (local $__caught_exc i32)
    local.get $text
    local.get $radix
    call $__str_parse_i32_radix
    i32.extend16_s
    call $Short_valueOf__73_68_6f_72_74_
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
      i32.const 1416
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
  ;; Class/Interface: __JavawLambda0
  (func $__JavawLambda0_run (export "__JavawLambda0_run") (param $this i32) (param $args i32)
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
    (local $__receiver$182 i32)
    (local $__receiver$183 i32)
    (local $__receiver$184 i32)
    (local $__caught_exc i32)
    local.get $this
    i32.load offset=4
    call $__require_nonnull
    i32.const 4152
    i32.const 4167
    i32.const 25
    call $SpringBootApp_addProduct
    local.get $this
    i32.load offset=4
    call $__require_nonnull
    i32.const 4172
    i32.const 4184
    i32.const 18
    call $SpringBootApp_addProduct
    local.get $this
    i32.load offset=4
    call $__require_nonnull
    i32.const 4190
    i32.const 4206
    i32.const 50
    call $SpringBootApp_addProduct
  )
  (func $__JavawLambda0__init (export "__JavawLambda0__init") (param $this i32) (param $__owner i32)
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
    local.get $__owner
    i32.store offset=4
  )
(global $__javaw_objects (mut i32) (i32.const 0))
(func $__javaw_object_alloc (param $size i32) (result i32)
  (local $object i32) (local $entry i32)
  local.get $size call $__heap_alloc local.set $object
  i32.const 8 call $__heap_alloc local.set $entry
  local.get $entry local.get $object i32.store
  local.get $entry global.get $__javaw_objects i32.store offset=4
  local.get $entry global.set $__javaw_objects
  local.get $object)
(func $__javaw_is_object (param $object i32) (result i32)
  (local $entry i32)
  global.get $__javaw_objects local.set $entry
  block $done loop $scan
    local.get $entry i32.eqz br_if $done
    local.get $entry i32.load local.get $object i32.eq
    if i32.const 1 return end
    local.get $entry i32.load offset=4 local.set $entry br $scan
  end end i32.const 0)
(func $__javaw_same (param $a i32) (param $b i32) (result i32)
  local.get $a local.get $b i32.eq if i32.const 1 return end
  local.get $a i32.eqz local.get $b i32.eqz i32.or if i32.const 0 return end
  local.get $a call $__javaw_is_object
  if (result i32)
    local.get $a local.get $b local.get $a i32.load i32.const 1 i32.add
    call_indirect (param i32 i32) (result i32)
  else
    local.get $b call $__javaw_is_object if i32.const 0 return end
    local.get $a local.get $b call $__str_equals
  end)
(func $__javaw_hash (param $value i32) (result i32)
  (local $hash i32) (local $index i32) (local $char i32)
  local.get $value i32.eqz if i32.const 0 return end
  local.get $value call $__javaw_is_object
  if
    local.get $value local.get $value i32.load i32.const 2 i32.add
    call_indirect (param i32) (result i32) return
  end
  block $done loop $chars
    local.get $value local.get $index i32.add i32.load8_u local.tee $char i32.eqz br_if $done
    local.get $hash i32.const 31 i32.mul local.get $char i32.add local.set $hash
    local.get $index i32.const 1 i32.add local.set $index br $chars
  end end local.get $hash)
  (func $__require_nonnull (param $value i32) (result i32) (local $e i32)
    local.get $value
    i32.eqz
    if
    i32.const 8
    call $__heap_alloc
    local.tee $e
    i32.const 1584
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
    i32.const 1789
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
  ;; Default toString() for Optional
  (func $Optional_toString (export "Optional_toString") (param $this i32) (result i32)
    local.get $this
    call $__str_from_i32
  )
  ;; Default equals() for Optional
  (func $Optional_equals (export "Optional_equals") (param $this i32) (param $other i32) (result i32)
    local.get $this
    local.get $other
    i32.eq
  )
  ;; Default hashCode() for Optional
  (func $Optional_hashCode (export "Optional_hashCode") (param $this i32) (result i32)
    local.get $this
  )
  ;; Default run() for Optional
  (func $Optional_run (export "Optional_run") (param $this i32)
  )
  ;; Default getClass() for Optional
  (func $Optional_getClass (export "Optional_getClass") (param $this i32) (result i32)
    global.get $__class_Optional
  )
  ;; Default toString() for ArrayList
  (func $ArrayList_toString (export "ArrayList_toString") (param $this i32) (result i32)
    local.get $this
    call $__str_from_i32
  )
  ;; Default equals() for ArrayList
  (func $ArrayList_equals (export "ArrayList_equals") (param $this i32) (param $other i32) (result i32)
    local.get $this
    local.get $other
    i32.eq
  )
  ;; Default hashCode() for ArrayList
  (func $ArrayList_hashCode (export "ArrayList_hashCode") (param $this i32) (result i32)
    local.get $this
  )
  ;; Default run() for ArrayList
  (func $ArrayList_run (export "ArrayList_run") (param $this i32)
  )
  ;; Default getClass() for ArrayList
  (func $ArrayList_getClass (export "ArrayList_getClass") (param $this i32) (result i32)
    global.get $__class_ArrayList
  )
  ;; Default toString() for __JavawListIterator
  (func $__JavawListIterator_toString (export "__JavawListIterator_toString") (param $this i32) (result i32)
    local.get $this
    call $__str_from_i32
  )
  ;; Default equals() for __JavawListIterator
  (func $__JavawListIterator_equals (export "__JavawListIterator_equals") (param $this i32) (param $other i32) (result i32)
    local.get $this
    local.get $other
    i32.eq
  )
  ;; Default hashCode() for __JavawListIterator
  (func $__JavawListIterator_hashCode (export "__JavawListIterator_hashCode") (param $this i32) (result i32)
    local.get $this
  )
  ;; Default run() for __JavawListIterator
  (func $__JavawListIterator_run (export "__JavawListIterator_run") (param $this i32)
  )
  ;; Default getClass() for __JavawListIterator
  (func $__JavawListIterator_getClass (export "__JavawListIterator_getClass") (param $this i32) (result i32)
    global.get $__class___JavawListIterator
  )
  ;; Default toString() for __JavawMapEntry
  (func $__JavawMapEntry_toString (export "__JavawMapEntry_toString") (param $this i32) (result i32)
    local.get $this
    call $__str_from_i32
  )
  ;; Default equals() for __JavawMapEntry
  (func $__JavawMapEntry_equals (export "__JavawMapEntry_equals") (param $this i32) (param $other i32) (result i32)
    local.get $this
    local.get $other
    i32.eq
  )
  ;; Default hashCode() for __JavawMapEntry
  (func $__JavawMapEntry_hashCode (export "__JavawMapEntry_hashCode") (param $this i32) (result i32)
    local.get $this
  )
  ;; Default run() for __JavawMapEntry
  (func $__JavawMapEntry_run (export "__JavawMapEntry_run") (param $this i32)
  )
  ;; Default getClass() for __JavawMapEntry
  (func $__JavawMapEntry_getClass (export "__JavawMapEntry_getClass") (param $this i32) (result i32)
    global.get $__class___JavawMapEntry
  )
  ;; Default toString() for HashMap
  (func $HashMap_toString (export "HashMap_toString") (param $this i32) (result i32)
    local.get $this
    call $__str_from_i32
  )
  ;; Default equals() for HashMap
  (func $HashMap_equals (export "HashMap_equals") (param $this i32) (param $other i32) (result i32)
    local.get $this
    local.get $other
    i32.eq
  )
  ;; Default hashCode() for HashMap
  (func $HashMap_hashCode (export "HashMap_hashCode") (param $this i32) (result i32)
    local.get $this
  )
  ;; Default run() for HashMap
  (func $HashMap_run (export "HashMap_run") (param $this i32)
  )
  ;; Default getClass() for HashMap
  (func $HashMap_getClass (export "HashMap_getClass") (param $this i32) (result i32)
    global.get $__class_HashMap
  )
  ;; Default toString() for __JavawKeyIterator
  (func $__JavawKeyIterator_toString (export "__JavawKeyIterator_toString") (param $this i32) (result i32)
    local.get $this
    call $__str_from_i32
  )
  ;; Default equals() for __JavawKeyIterator
  (func $__JavawKeyIterator_equals (export "__JavawKeyIterator_equals") (param $this i32) (param $other i32) (result i32)
    local.get $this
    local.get $other
    i32.eq
  )
  ;; Default hashCode() for __JavawKeyIterator
  (func $__JavawKeyIterator_hashCode (export "__JavawKeyIterator_hashCode") (param $this i32) (result i32)
    local.get $this
  )
  ;; Default run() for __JavawKeyIterator
  (func $__JavawKeyIterator_run (export "__JavawKeyIterator_run") (param $this i32)
  )
  ;; Default getClass() for __JavawKeyIterator
  (func $__JavawKeyIterator_getClass (export "__JavawKeyIterator_getClass") (param $this i32) (result i32)
    global.get $__class___JavawKeyIterator
  )
  ;; Default toString() for __JavawKeySet
  (func $__JavawKeySet_toString (export "__JavawKeySet_toString") (param $this i32) (result i32)
    local.get $this
    call $__str_from_i32
  )
  ;; Default equals() for __JavawKeySet
  (func $__JavawKeySet_equals (export "__JavawKeySet_equals") (param $this i32) (param $other i32) (result i32)
    local.get $this
    local.get $other
    i32.eq
  )
  ;; Default hashCode() for __JavawKeySet
  (func $__JavawKeySet_hashCode (export "__JavawKeySet_hashCode") (param $this i32) (result i32)
    local.get $this
  )
  ;; Default run() for __JavawKeySet
  (func $__JavawKeySet_run (export "__JavawKeySet_run") (param $this i32)
  )
  ;; Default getClass() for __JavawKeySet
  (func $__JavawKeySet_getClass (export "__JavawKeySet_getClass") (param $this i32) (result i32)
    global.get $__class___JavawKeySet
  )
  ;; Default toString() for HashSet
  (func $HashSet_toString (export "HashSet_toString") (param $this i32) (result i32)
    local.get $this
    call $__str_from_i32
  )
  ;; Default equals() for HashSet
  (func $HashSet_equals (export "HashSet_equals") (param $this i32) (param $other i32) (result i32)
    local.get $this
    local.get $other
    i32.eq
  )
  ;; Default hashCode() for HashSet
  (func $HashSet_hashCode (export "HashSet_hashCode") (param $this i32) (result i32)
    local.get $this
  )
  ;; Default run() for HashSet
  (func $HashSet_run (export "HashSet_run") (param $this i32)
  )
  ;; Default getClass() for HashSet
  (func $HashSet_getClass (export "HashSet_getClass") (param $this i32) (result i32)
    global.get $__class_HashSet
  )
  ;; Default toString() for __JavawArrayIterator
  (func $__JavawArrayIterator_toString (export "__JavawArrayIterator_toString") (param $this i32) (result i32)
    local.get $this
    call $__str_from_i32
  )
  ;; Default equals() for __JavawArrayIterator
  (func $__JavawArrayIterator_equals (export "__JavawArrayIterator_equals") (param $this i32) (param $other i32) (result i32)
    local.get $this
    local.get $other
    i32.eq
  )
  ;; Default hashCode() for __JavawArrayIterator
  (func $__JavawArrayIterator_hashCode (export "__JavawArrayIterator_hashCode") (param $this i32) (result i32)
    local.get $this
  )
  ;; Default run() for __JavawArrayIterator
  (func $__JavawArrayIterator_run (export "__JavawArrayIterator_run") (param $this i32)
  )
  ;; Default getClass() for __JavawArrayIterator
  (func $__JavawArrayIterator_getClass (export "__JavawArrayIterator_getClass") (param $this i32) (result i32)
    global.get $__class___JavawArrayIterator
  )
  ;; Default toString() for __JavawGenerateIterator
  (func $__JavawGenerateIterator_toString (export "__JavawGenerateIterator_toString") (param $this i32) (result i32)
    local.get $this
    call $__str_from_i32
  )
  ;; Default equals() for __JavawGenerateIterator
  (func $__JavawGenerateIterator_equals (export "__JavawGenerateIterator_equals") (param $this i32) (param $other i32) (result i32)
    local.get $this
    local.get $other
    i32.eq
  )
  ;; Default hashCode() for __JavawGenerateIterator
  (func $__JavawGenerateIterator_hashCode (export "__JavawGenerateIterator_hashCode") (param $this i32) (result i32)
    local.get $this
  )
  ;; Default run() for __JavawGenerateIterator
  (func $__JavawGenerateIterator_run (export "__JavawGenerateIterator_run") (param $this i32)
  )
  ;; Default getClass() for __JavawGenerateIterator
  (func $__JavawGenerateIterator_getClass (export "__JavawGenerateIterator_getClass") (param $this i32) (result i32)
    global.get $__class___JavawGenerateIterator
  )
  ;; Default toString() for __JavawIterateIterator
  (func $__JavawIterateIterator_toString (export "__JavawIterateIterator_toString") (param $this i32) (result i32)
    local.get $this
    call $__str_from_i32
  )
  ;; Default equals() for __JavawIterateIterator
  (func $__JavawIterateIterator_equals (export "__JavawIterateIterator_equals") (param $this i32) (param $other i32) (result i32)
    local.get $this
    local.get $other
    i32.eq
  )
  ;; Default hashCode() for __JavawIterateIterator
  (func $__JavawIterateIterator_hashCode (export "__JavawIterateIterator_hashCode") (param $this i32) (result i32)
    local.get $this
  )
  ;; Default run() for __JavawIterateIterator
  (func $__JavawIterateIterator_run (export "__JavawIterateIterator_run") (param $this i32)
  )
  ;; Default getClass() for __JavawIterateIterator
  (func $__JavawIterateIterator_getClass (export "__JavawIterateIterator_getClass") (param $this i32) (result i32)
    global.get $__class___JavawIterateIterator
  )
  ;; Default toString() for __JavawListCollector
  (func $__JavawListCollector_toString (export "__JavawListCollector_toString") (param $this i32) (result i32)
    local.get $this
    call $__str_from_i32
  )
  ;; Default equals() for __JavawListCollector
  (func $__JavawListCollector_equals (export "__JavawListCollector_equals") (param $this i32) (param $other i32) (result i32)
    local.get $this
    local.get $other
    i32.eq
  )
  ;; Default hashCode() for __JavawListCollector
  (func $__JavawListCollector_hashCode (export "__JavawListCollector_hashCode") (param $this i32) (result i32)
    local.get $this
  )
  ;; Default run() for __JavawListCollector
  (func $__JavawListCollector_run (export "__JavawListCollector_run") (param $this i32)
  )
  ;; Default getClass() for __JavawListCollector
  (func $__JavawListCollector_getClass (export "__JavawListCollector_getClass") (param $this i32) (result i32)
    global.get $__class___JavawListCollector
  )
  ;; Default toString() for __JavawSetCollector
  (func $__JavawSetCollector_toString (export "__JavawSetCollector_toString") (param $this i32) (result i32)
    local.get $this
    call $__str_from_i32
  )
  ;; Default equals() for __JavawSetCollector
  (func $__JavawSetCollector_equals (export "__JavawSetCollector_equals") (param $this i32) (param $other i32) (result i32)
    local.get $this
    local.get $other
    i32.eq
  )
  ;; Default hashCode() for __JavawSetCollector
  (func $__JavawSetCollector_hashCode (export "__JavawSetCollector_hashCode") (param $this i32) (result i32)
    local.get $this
  )
  ;; Default run() for __JavawSetCollector
  (func $__JavawSetCollector_run (export "__JavawSetCollector_run") (param $this i32)
  )
  ;; Default getClass() for __JavawSetCollector
  (func $__JavawSetCollector_getClass (export "__JavawSetCollector_getClass") (param $this i32) (result i32)
    global.get $__class___JavawSetCollector
  )
  ;; Default toString() for Stream
  (func $Stream_toString (export "Stream_toString") (param $this i32) (result i32)
    local.get $this
    call $__str_from_i32
  )
  ;; Default equals() for Stream
  (func $Stream_equals (export "Stream_equals") (param $this i32) (param $other i32) (result i32)
    local.get $this
    local.get $other
    i32.eq
  )
  ;; Default hashCode() for Stream
  (func $Stream_hashCode (export "Stream_hashCode") (param $this i32) (result i32)
    local.get $this
  )
  ;; Default run() for Stream
  (func $Stream_run (export "Stream_run") (param $this i32)
  )
  ;; Default getClass() for Stream
  (func $Stream_getClass (export "Stream_getClass") (param $this i32) (result i32)
    global.get $__class_Stream
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
  ;; Default toString() for __JavawLambda0
  (func $__JavawLambda0_toString (export "__JavawLambda0_toString") (param $this i32) (result i32)
    local.get $this
    call $__str_from_i32
  )
  ;; Default equals() for __JavawLambda0
  (func $__JavawLambda0_equals (export "__JavawLambda0_equals") (param $this i32) (param $other i32) (result i32)
    local.get $this
    local.get $other
    i32.eq
  )
  ;; Default hashCode() for __JavawLambda0
  (func $__JavawLambda0_hashCode (export "__JavawLambda0_hashCode") (param $this i32) (result i32)
    local.get $this
  )
  ;; Default getClass() for __JavawLambda0
  (func $__JavawLambda0_getClass (export "__JavawLambda0_getClass") (param $this i32) (result i32)
    global.get $__class___JavawLambda0
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
      global.get $__class_ApplicationRunner
      return
    end
    local.get $name
    i32.const 4340
    call $__str_equals
    if
      global.get $__class___JavawSpringContext
      return
    end
    local.get $name
    i32.const 4361
    call $__str_equals
    if
      global.get $__class_Supplier
      return
    end
    local.get $name
    i32.const 4370
    call $__str_equals
    if
      global.get $__class_Function
      return
    end
    local.get $name
    i32.const 4379
    call $__str_equals
    if
      global.get $__class_Predicate
      return
    end
    local.get $name
    i32.const 4389
    call $__str_equals
    if
      global.get $__class_Consumer
      return
    end
    local.get $name
    i32.const 4398
    call $__str_equals
    if
      global.get $__class_UnaryOperator
      return
    end
    local.get $name
    i32.const 4412
    call $__str_equals
    if
      global.get $__class_BinaryOperator
      return
    end
    local.get $name
    i32.const 4427
    call $__str_equals
    if
      global.get $__class_Optional
      return
    end
    local.get $name
    i32.const 4436
    call $__str_equals
    if
      global.get $__class_Iterable
      return
    end
    local.get $name
    i32.const 4445
    call $__str_equals
    if
      global.get $__class_Iterator
      return
    end
    local.get $name
    i32.const 4454
    call $__str_equals
    if
      global.get $__class_Collection
      return
    end
    local.get $name
    i32.const 4465
    call $__str_equals
    if
      global.get $__class_List
      return
    end
    local.get $name
    i32.const 4470
    call $__str_equals
    if
      global.get $__class_Set
      return
    end
    local.get $name
    i32.const 4474
    call $__str_equals
    if
      global.get $__class_Map
      return
    end
    local.get $name
    i32.const 4478
    call $__str_equals
    if
      global.get $__class___JavawObjects
      return
    end
    local.get $name
    i32.const 4493
    call $__str_equals
    if
      global.get $__class_ArrayList
      return
    end
    local.get $name
    i32.const 4503
    call $__str_equals
    if
      global.get $__class___JavawListIterator
      return
    end
    local.get $name
    i32.const 4523
    call $__str_equals
    if
      global.get $__class___JavawMapEntry
      return
    end
    local.get $name
    i32.const 4539
    call $__str_equals
    if
      global.get $__class_HashMap
      return
    end
    local.get $name
    i32.const 4547
    call $__str_equals
    if
      global.get $__class___JavawKeyIterator
      return
    end
    local.get $name
    i32.const 4566
    call $__str_equals
    if
      global.get $__class___JavawKeySet
      return
    end
    local.get $name
    i32.const 4580
    call $__str_equals
    if
      global.get $__class_HashSet
      return
    end
    local.get $name
    i32.const 4588
    call $__str_equals
    if
      global.get $__class_Comparator
      return
    end
    local.get $name
    i32.const 4599
    call $__str_equals
    if
      global.get $__class___JavawArrayIterator
      return
    end
    local.get $name
    i32.const 4620
    call $__str_equals
    if
      global.get $__class___JavawGenerateIterator
      return
    end
    local.get $name
    i32.const 4644
    call $__str_equals
    if
      global.get $__class___JavawIterateIterator
      return
    end
    local.get $name
    i32.const 4667
    call $__str_equals
    if
      global.get $__class___JavawReadOnlyList
      return
    end
    local.get $name
    i32.const 4687
    call $__str_equals
    if
      global.get $__class_Collector
      return
    end
    local.get $name
    i32.const 4697
    call $__str_equals
    if
      global.get $__class___JavawListCollector
      return
    end
    local.get $name
    i32.const 4718
    call $__str_equals
    if
      global.get $__class___JavawSetCollector
      return
    end
    local.get $name
    i32.const 4738
    call $__str_equals
    if
      global.get $__class_Collectors
      return
    end
    local.get $name
    i32.const 4749
    call $__str_equals
    if
      global.get $__class_Stream
      return
    end
    local.get $name
    i32.const 4756
    call $__str_equals
    if
      global.get $__class_Number
      return
    end
    local.get $name
    i32.const 4763
    call $__str_equals
    if
      global.get $__class_Boolean
      return
    end
    local.get $name
    i32.const 4771
    call $__str_equals
    if
      global.get $__class_Byte
      return
    end
    local.get $name
    i32.const 4776
    call $__str_equals
    if
      global.get $__class_Character
      return
    end
    local.get $name
    i32.const 4786
    call $__str_equals
    if
      global.get $__class_Double
      return
    end
    local.get $name
    i32.const 4793
    call $__str_equals
    if
      global.get $__class_Float
      return
    end
    local.get $name
    i32.const 4799
    call $__str_equals
    if
      global.get $__class_Integer
      return
    end
    local.get $name
    i32.const 4807
    call $__str_equals
    if
      global.get $__class_Long
      return
    end
    local.get $name
    i32.const 4812
    call $__str_equals
    if
      global.get $__class_Short
      return
    end
    local.get $name
    i32.const 4818
    call $__str_equals
    if
      global.get $__class___JavawLambda0
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
      global.get $__class_NoSuchElementException
      return
    end
    local.get $name
    i32.const 0
    call $__str_equals
    if
      global.get $__class_IndexOutOfBoundsException
      return
    end
    local.get $name
    i32.const 0
    call $__str_equals
    if
      global.get $__class_ConcurrentModificationException
      return
    end
    local.get $name
    i32.const 0
    call $__str_equals
    if
      global.get $__class_UnsupportedOperationException
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
    i32.const 4891
    call $__str_equals
    if
      global.get $__class_Runnable
      return
    end
    local.get $name
    i32.const 4884
    call $__str_equals
    if
      global.get $__class_Thread
      return
    end
    local.get $name
    i32.const 4878
    call $__str_equals
    if
      global.get $__class_Class
      return
    end
    local.get $name
    i32.const 4905
    call $__str_equals
    if
      global.get $__class_int
      return
    end
    local.get $name
    i32.const 4909
    call $__str_equals
    if
      global.get $__class_long
      return
    end
    local.get $name
    i32.const 4914
    call $__str_equals
    if
      global.get $__class_double
      return
    end
    local.get $name
    i32.const 4921
    call $__str_equals
    if
      global.get $__class_float
      return
    end
    local.get $name
    i32.const 4927
    call $__str_equals
    if
      global.get $__class_boolean
      return
    end
    local.get $name
    i32.const 4935
    call $__str_equals
    if
      global.get $__class_char
      return
    end
    local.get $name
    i32.const 4940
    call $__str_equals
    if
      global.get $__class_byte
      return
    end
    local.get $name
    i32.const 4945
    call $__str_equals
    if
      global.get $__class_short
      return
    end
    local.get $name
    i32.const 4951
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
    i32.const 46
    i32.eq
    if
      global.get $__class_SpringBootApp
      return
    end
    local.get $vtableBase
    i32.const 88
    i32.eq
    if
      global.get $__class_ProductController
      return
    end
    local.get $vtableBase
    i32.const 133
    i32.eq
    if
      global.get $__class_ProductRepository
      return
    end
    local.get $vtableBase
    i32.const 177
    i32.eq
    if
      global.get $__class___JavawRow_ProductRepository
      return
    end
    local.get $vtableBase
    i32.const 340
    i32.eq
    if
      global.get $__class_ResponseStatusException
      return
    end
    local.get $vtableBase
    i32.const 0
    i32.eq
    if
      global.get $__class_ApplicationRunner
      return
    end
    local.get $vtableBase
    i32.const 381
    i32.eq
    if
      global.get $__class___JavawSpringContext
      return
    end
    local.get $vtableBase
    i32.const 0
    i32.eq
    if
      global.get $__class_Supplier
      return
    end
    local.get $vtableBase
    i32.const 0
    i32.eq
    if
      global.get $__class_Function
      return
    end
    local.get $vtableBase
    i32.const 0
    i32.eq
    if
      global.get $__class_Predicate
      return
    end
    local.get $vtableBase
    i32.const 0
    i32.eq
    if
      global.get $__class_Consumer
      return
    end
    local.get $vtableBase
    i32.const 0
    i32.eq
    if
      global.get $__class_UnaryOperator
      return
    end
    local.get $vtableBase
    i32.const 0
    i32.eq
    if
      global.get $__class_BinaryOperator
      return
    end
    local.get $vtableBase
    i32.const 421
    i32.eq
    if
      global.get $__class_Optional
      return
    end
    local.get $vtableBase
    i32.const 0
    i32.eq
    if
      global.get $__class_Iterable
      return
    end
    local.get $vtableBase
    i32.const 0
    i32.eq
    if
      global.get $__class_Iterator
      return
    end
    local.get $vtableBase
    i32.const 0
    i32.eq
    if
      global.get $__class_Collection
      return
    end
    local.get $vtableBase
    i32.const 0
    i32.eq
    if
      global.get $__class_List
      return
    end
    local.get $vtableBase
    i32.const 0
    i32.eq
    if
      global.get $__class_Set
      return
    end
    local.get $vtableBase
    i32.const 0
    i32.eq
    if
      global.get $__class_Map
      return
    end
    local.get $vtableBase
    i32.const 470
    i32.eq
    if
      global.get $__class_ArrayList
      return
    end
    local.get $vtableBase
    i32.const 512
    i32.eq
    if
      global.get $__class___JavawListIterator
      return
    end
    local.get $vtableBase
    i32.const 552
    i32.eq
    if
      global.get $__class___JavawMapEntry
      return
    end
    local.get $vtableBase
    i32.const 592
    i32.eq
    if
      global.get $__class_HashMap
      return
    end
    local.get $vtableBase
    i32.const 633
    i32.eq
    if
      global.get $__class___JavawKeyIterator
      return
    end
    local.get $vtableBase
    i32.const 673
    i32.eq
    if
      global.get $__class___JavawKeySet
      return
    end
    local.get $vtableBase
    i32.const 713
    i32.eq
    if
      global.get $__class_HashSet
      return
    end
    local.get $vtableBase
    i32.const 0
    i32.eq
    if
      global.get $__class_Comparator
      return
    end
    local.get $vtableBase
    i32.const 753
    i32.eq
    if
      global.get $__class___JavawArrayIterator
      return
    end
    local.get $vtableBase
    i32.const 793
    i32.eq
    if
      global.get $__class___JavawGenerateIterator
      return
    end
    local.get $vtableBase
    i32.const 833
    i32.eq
    if
      global.get $__class___JavawIterateIterator
      return
    end
    local.get $vtableBase
    i32.const 873
    i32.eq
    if
      global.get $__class___JavawReadOnlyList
      return
    end
    local.get $vtableBase
    i32.const 0
    i32.eq
    if
      global.get $__class_Collector
      return
    end
    local.get $vtableBase
    i32.const 915
    i32.eq
    if
      global.get $__class___JavawListCollector
      return
    end
    local.get $vtableBase
    i32.const 955
    i32.eq
    if
      global.get $__class___JavawSetCollector
      return
    end
    local.get $vtableBase
    i32.const 995
    i32.eq
    if
      global.get $__class_Stream
      return
    end
    local.get $vtableBase
    i32.const 1060
    i32.eq
    if
      global.get $__class_Number
      return
    end
    local.get $vtableBase
    i32.const 1104
    i32.eq
    if
      global.get $__class_Boolean
      return
    end
    local.get $vtableBase
    i32.const 1145
    i32.eq
    if
      global.get $__class_Byte
      return
    end
    local.get $vtableBase
    i32.const 1191
    i32.eq
    if
      global.get $__class_Character
      return
    end
    local.get $vtableBase
    i32.const 1232
    i32.eq
    if
      global.get $__class_Double
      return
    end
    local.get $vtableBase
    i32.const 1278
    i32.eq
    if
      global.get $__class_Float
      return
    end
    local.get $vtableBase
    i32.const 1324
    i32.eq
    if
      global.get $__class_Integer
      return
    end
    local.get $vtableBase
    i32.const 1370
    i32.eq
    if
      global.get $__class_Long
      return
    end
    local.get $vtableBase
    i32.const 1416
    i32.eq
    if
      global.get $__class_Short
      return
    end
    local.get $vtableBase
    i32.const 1462
    i32.eq
    if
      global.get $__class___JavawLambda0
      return
    end
    local.get $vtableBase
    i32.const 217
    i32.eq
    if
      global.get $__class_Throwable
      return
    end
    local.get $vtableBase
    i32.const 258
    i32.eq
    if
      global.get $__class_Exception
      return
    end
    local.get $vtableBase
    i32.const 299
    i32.eq
    if
      global.get $__class_RuntimeException
      return
    end
    local.get $vtableBase
    i32.const 1502
    i32.eq
    if
      global.get $__class_IllegalArgumentException
      return
    end
    local.get $vtableBase
    i32.const 1543
    i32.eq
    if
      global.get $__class_IllegalStateException
      return
    end
    local.get $vtableBase
    i32.const 1584
    i32.eq
    if
      global.get $__class_NullPointerException
      return
    end
    local.get $vtableBase
    i32.const 1625
    i32.eq
    if
      global.get $__class_NoSuchElementException
      return
    end
    local.get $vtableBase
    i32.const 1666
    i32.eq
    if
      global.get $__class_IndexOutOfBoundsException
      return
    end
    local.get $vtableBase
    i32.const 1707
    i32.eq
    if
      global.get $__class_ConcurrentModificationException
      return
    end
    local.get $vtableBase
    i32.const 1748
    i32.eq
    if
      global.get $__class_UnsupportedOperationException
      return
    end
    local.get $vtableBase
    i32.const 1789
    i32.eq
    if
      global.get $__class_ArithmeticException
      return
    end
    local.get $vtableBase
    i32.const 1830
    i32.eq
    if
      global.get $__class_WASIFile
      return
    end
    local.get $vtableBase
    i32.const 0
    i32.eq
    if
      global.get $__class_CharSequence
      return
    end
    local.get $vtableBase
    i32.const 0
    i32.eq
    if
      global.get $__class_AutoCloseable
      return
    end
    local.get $vtableBase
    i32.const 0
    i32.eq
    if
      global.get $__class_Comparable
      return
    end
    local.get $vtableBase
    i32.const 0
    i32.eq
    if
      global.get $__class_Runnable
      return
    end
    local.get $vtableBase
    i32.const 1870
    i32.eq
    if
      global.get $__class_Thread
      return
    end
    local.get $vtableBase
    i32.const 1910
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
      call_indirect (type $t_sig_5)
    else
      local.get $this
      local.get $this
      i32.load offset=0
      i32.const 8
      i32.add
      call_indirect (type $t_sig_5)
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
      call_indirect (type $t_sig_5)
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
    i32.const 1870
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
    i32.const 1910
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
    i32.const 1910
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
    i32.const 1910
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
    i32.const 1910
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
    i32.const 1910
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
    i32.const 1910
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
    i32.const 1910
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
    i32.const 1
    i32.store offset=16
    local.get $inst
    i32.const 0
    i32.store offset=20
    local.get $inst
    i32.const 0
    i32.store offset=24
    local.get $inst
    global.set $__class_ApplicationRunner
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 4340
    i32.store offset=4
    local.get $inst
    i32.const 4340
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
    global.set $__class___JavawSpringContext
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 4361
    i32.store offset=4
    local.get $inst
    i32.const 4361
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
    global.set $__class_Supplier
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 4370
    i32.store offset=4
    local.get $inst
    i32.const 4370
    i32.store offset=8
    local.get $inst
    i32.const 10
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
    global.set $__class_Function
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 4379
    i32.store offset=4
    local.get $inst
    i32.const 4379
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
    global.set $__class_Predicate
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 4389
    i32.store offset=4
    local.get $inst
    i32.const 4389
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
    global.set $__class_Consumer
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 4398
    i32.store offset=4
    local.get $inst
    i32.const 4398
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
    global.set $__class_UnaryOperator
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 4412
    i32.store offset=4
    local.get $inst
    i32.const 4412
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
    global.set $__class_BinaryOperator
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 4427
    i32.store offset=4
    local.get $inst
    i32.const 4427
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
    global.set $__class_Optional
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 4436
    i32.store offset=4
    local.get $inst
    i32.const 4436
    i32.store offset=8
    local.get $inst
    i32.const 16
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
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 4445
    i32.store offset=4
    local.get $inst
    i32.const 4445
    i32.store offset=8
    local.get $inst
    i32.const 17
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
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 4454
    i32.store offset=4
    local.get $inst
    i32.const 4454
    i32.store offset=8
    local.get $inst
    i32.const 18
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
    global.set $__class_Collection
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 4465
    i32.store offset=4
    local.get $inst
    i32.const 4465
    i32.store offset=8
    local.get $inst
    i32.const 19
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
    global.set $__class_List
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 4470
    i32.store offset=4
    local.get $inst
    i32.const 4470
    i32.store offset=8
    local.get $inst
    i32.const 20
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
    global.set $__class_Set
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 4474
    i32.store offset=4
    local.get $inst
    i32.const 4474
    i32.store offset=8
    local.get $inst
    i32.const 21
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
    global.set $__class_Map
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 4478
    i32.store offset=4
    local.get $inst
    i32.const 4478
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
    global.set $__class___JavawObjects
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 4493
    i32.store offset=4
    local.get $inst
    i32.const 4493
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
    global.set $__class_ArrayList
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 4503
    i32.store offset=4
    local.get $inst
    i32.const 4503
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
    global.set $__class___JavawListIterator
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 4523
    i32.store offset=4
    local.get $inst
    i32.const 4523
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
    global.set $__class___JavawMapEntry
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 4539
    i32.store offset=4
    local.get $inst
    i32.const 4539
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
    global.set $__class_HashMap
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 4547
    i32.store offset=4
    local.get $inst
    i32.const 4547
    i32.store offset=8
    local.get $inst
    i32.const 27
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
    global.set $__class___JavawKeyIterator
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 4566
    i32.store offset=4
    local.get $inst
    i32.const 4566
    i32.store offset=8
    local.get $inst
    i32.const 28
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
    global.set $__class___JavawKeySet
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 4580
    i32.store offset=4
    local.get $inst
    i32.const 4580
    i32.store offset=8
    local.get $inst
    i32.const 29
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
    global.set $__class_HashSet
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 4588
    i32.store offset=4
    local.get $inst
    i32.const 4588
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
    global.set $__class_Comparator
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 4599
    i32.store offset=4
    local.get $inst
    i32.const 4599
    i32.store offset=8
    local.get $inst
    i32.const 31
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
    global.set $__class___JavawArrayIterator
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 4620
    i32.store offset=4
    local.get $inst
    i32.const 4620
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
    global.set $__class___JavawGenerateIterator
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 4644
    i32.store offset=4
    local.get $inst
    i32.const 4644
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
    global.set $__class___JavawIterateIterator
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 4667
    i32.store offset=4
    local.get $inst
    i32.const 4667
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
    global.set $__class___JavawReadOnlyList
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 4687
    i32.store offset=4
    local.get $inst
    i32.const 4687
    i32.store offset=8
    local.get $inst
    i32.const 35
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
    global.set $__class_Collector
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 4697
    i32.store offset=4
    local.get $inst
    i32.const 4697
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
    global.set $__class___JavawListCollector
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 4718
    i32.store offset=4
    local.get $inst
    i32.const 4718
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
    global.set $__class___JavawSetCollector
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 4738
    i32.store offset=4
    local.get $inst
    i32.const 4738
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
    global.set $__class_Collectors
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 4749
    i32.store offset=4
    local.get $inst
    i32.const 4749
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
    global.set $__class_Stream
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 4756
    i32.store offset=4
    local.get $inst
    i32.const 4756
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
    global.set $__class_Number
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 4763
    i32.store offset=4
    local.get $inst
    i32.const 4763
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
    global.set $__class_Boolean
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 4771
    i32.store offset=4
    local.get $inst
    i32.const 4771
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
    global.set $__class_Byte
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 4776
    i32.store offset=4
    local.get $inst
    i32.const 4776
    i32.store offset=8
    local.get $inst
    i32.const 43
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
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 4786
    i32.store offset=4
    local.get $inst
    i32.const 4786
    i32.store offset=8
    local.get $inst
    i32.const 44
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
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 4793
    i32.store offset=4
    local.get $inst
    i32.const 4793
    i32.store offset=8
    local.get $inst
    i32.const 45
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
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 4799
    i32.store offset=4
    local.get $inst
    i32.const 4799
    i32.store offset=8
    local.get $inst
    i32.const 46
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
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 4807
    i32.store offset=4
    local.get $inst
    i32.const 4807
    i32.store offset=8
    local.get $inst
    i32.const 47
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
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 4812
    i32.store offset=4
    local.get $inst
    i32.const 4812
    i32.store offset=8
    local.get $inst
    i32.const 48
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
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 4818
    i32.store offset=4
    local.get $inst
    i32.const 4818
    i32.store offset=8
    local.get $inst
    i32.const 49
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
    global.set $__class___JavawLambda0
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 0
    i32.store offset=4
    local.get $inst
    i32.const 0
    i32.store offset=8
    local.get $inst
    i32.const 50
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
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 0
    i32.store offset=4
    local.get $inst
    i32.const 0
    i32.store offset=8
    local.get $inst
    i32.const 51
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
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 0
    i32.store offset=4
    local.get $inst
    i32.const 0
    i32.store offset=8
    local.get $inst
    i32.const 52
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
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 0
    i32.store offset=4
    local.get $inst
    i32.const 0
    i32.store offset=8
    local.get $inst
    i32.const 53
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
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 0
    i32.store offset=4
    local.get $inst
    i32.const 0
    i32.store offset=8
    local.get $inst
    i32.const 54
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
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 0
    i32.store offset=4
    local.get $inst
    i32.const 0
    i32.store offset=8
    local.get $inst
    i32.const 55
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
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 0
    i32.store offset=4
    local.get $inst
    i32.const 0
    i32.store offset=8
    local.get $inst
    i32.const 56
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
    global.set $__class_NoSuchElementException
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 0
    i32.store offset=4
    local.get $inst
    i32.const 0
    i32.store offset=8
    local.get $inst
    i32.const 57
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
    global.set $__class_IndexOutOfBoundsException
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 0
    i32.store offset=4
    local.get $inst
    i32.const 0
    i32.store offset=8
    local.get $inst
    i32.const 58
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
    global.set $__class_ConcurrentModificationException
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 0
    i32.store offset=4
    local.get $inst
    i32.const 0
    i32.store offset=8
    local.get $inst
    i32.const 59
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
    global.set $__class_UnsupportedOperationException
    i32.const 32
    call $__heap_alloc
    local.set $inst
    local.get $inst
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 0
    i32.store offset=4
    local.get $inst
    i32.const 0
    i32.store offset=8
    local.get $inst
    i32.const 60
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
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 0
    i32.store offset=4
    local.get $inst
    i32.const 0
    i32.store offset=8
    local.get $inst
    i32.const 61
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
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 0
    i32.store offset=4
    local.get $inst
    i32.const 0
    i32.store offset=8
    local.get $inst
    i32.const 62
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
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 0
    i32.store offset=4
    local.get $inst
    i32.const 0
    i32.store offset=8
    local.get $inst
    i32.const 63
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
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 0
    i32.store offset=4
    local.get $inst
    i32.const 0
    i32.store offset=8
    local.get $inst
    i32.const 64
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
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 0
    i32.store offset=4
    local.get $inst
    i32.const 0
    i32.store offset=8
    local.get $inst
    i32.const 65
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
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 4891
    i32.store offset=4
    local.get $inst
    i32.const 4891
    i32.store offset=8
    local.get $inst
    i32.const 66
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
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 4884
    i32.store offset=4
    local.get $inst
    i32.const 4884
    i32.store offset=8
    local.get $inst
    i32.const 67
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
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 4878
    i32.store offset=4
    local.get $inst
    i32.const 4878
    i32.store offset=8
    local.get $inst
    i32.const 68
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
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 4905
    i32.store offset=4
    local.get $inst
    i32.const 4905
    i32.store offset=8
    local.get $inst
    i32.const 69
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
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 4909
    i32.store offset=4
    local.get $inst
    i32.const 4909
    i32.store offset=8
    local.get $inst
    i32.const 70
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
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 4914
    i32.store offset=4
    local.get $inst
    i32.const 4914
    i32.store offset=8
    local.get $inst
    i32.const 71
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
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 4921
    i32.store offset=4
    local.get $inst
    i32.const 4921
    i32.store offset=8
    local.get $inst
    i32.const 72
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
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 4927
    i32.store offset=4
    local.get $inst
    i32.const 4927
    i32.store offset=8
    local.get $inst
    i32.const 73
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
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 4935
    i32.store offset=4
    local.get $inst
    i32.const 4935
    i32.store offset=8
    local.get $inst
    i32.const 74
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
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 4940
    i32.store offset=4
    local.get $inst
    i32.const 4940
    i32.store offset=8
    local.get $inst
    i32.const 75
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
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 4945
    i32.store offset=4
    local.get $inst
    i32.const 4945
    i32.store offset=8
    local.get $inst
    i32.const 76
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
    i32.const 1910
    i32.store offset=0
    local.get $inst
    i32.const 4951
    i32.store offset=4
    local.get $inst
    i32.const 4951
    i32.store offset=8
    local.get $inst
    i32.const 77
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
    global.get $__class_Collection
    global.get $__class_Iterable
    i32.store offset=24
    global.get $__class_List
    global.get $__class_Collection
    i32.store offset=24
    global.get $__class_Set
    global.get $__class_Collection
    i32.store offset=24
    global.get $__class___JavawReadOnlyList
    global.get $__class_ArrayList
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
    global.get $__class_NoSuchElementException
    global.get $__class_RuntimeException
    i32.store offset=24
    global.get $__class_IndexOutOfBoundsException
    global.get $__class_RuntimeException
    i32.store offset=24
    global.get $__class_ConcurrentModificationException
    global.get $__class_RuntimeException
    i32.store offset=24
    global.get $__class_UnsupportedOperationException
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
