package one.dastec.javaw;
/** Object identity registry distinguishes source objects from the runtime's untagged strings. */
final class CollectionRuntime {
    static void emit(StringBuilder out) {
        out.append("""
          (func $__javaw_object_alloc (param $size i32) (result i32)
            (local $object i32)
            local.get $size call $__heap_alloc local.tee $object
            i32.const 12 i32.sub i32.const 9 i32.store
            local.get $object)
          (func $__javaw_is_object (param $object i32) (result i32)
            (local $block i32)
            local.get $object call $__gc_find local.tee $block i32.eqz if i32.const 0 return end
            local.get $object local.get $block i32.const 16 i32.add i32.eq
            local.get $block i32.load offset=4 i32.const 8 i32.and i32.const 0 i32.ne i32.and)
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
          (func $__javaw_compare (param $a i32) (param $b i32) (result i32)
            local.get $a call $__require_nonnull drop
            local.get $b call $__require_nonnull drop
            local.get $a call $__javaw_is_object
            if (result i32)
              local.get $a local.get $b local.get $a i32.load i32.const 4 i32.add
              call_indirect (param i32 i32) (result i32)
            else local.get $a local.get $b call $__str_compareTo end)
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
            """);
    }
}
