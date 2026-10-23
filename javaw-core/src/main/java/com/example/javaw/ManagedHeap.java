package com.example.javaw;

import java.util.LinkedHashSet;
import java.util.regex.Pattern;

/** Non-moving conservative tracing at explicit, stack-free host safepoints. */
final class ManagedHeap {
    static void emitAllocator(StringBuilder out) {
        // Header: capacity, flags (used/marked/scanned/object), host retain count, payload type tag (0 raw/string, 1 untyped array, class id + 2 typed array).
        out.append("""
          (func $__heap_alloc (export "__heap_alloc") (param $size i32) (result i32)
            (local $block i32) (local $capacity i32) (local $need i32) (local $end i32) (local $split i32)
            local.get $size i32.const 2147483616 i32.gt_u if unreachable end
            local.get $size i32.const 7 i32.add i32.const -8 i32.and local.set $need
            local.get $need i32.eqz if i32.const 8 local.set $need end
            global.get $__heap_base local.set $block
            block $found loop $search
              local.get $block global.get $__heap_ptr i32.ge_u br_if $found
              local.get $block i32.load local.set $capacity
              local.get $block i32.load offset=4 i32.const 1 i32.and i32.eqz
              local.get $capacity local.get $need i32.ge_u i32.and
              if
                local.get $capacity local.get $need i32.sub i32.const 24 i32.ge_u
                if
                  local.get $block i32.const 16 i32.add local.get $need i32.add local.set $split
                  local.get $split local.get $capacity local.get $need i32.sub i32.const 16 i32.sub i32.store
                  local.get $split i32.const 0 i32.store offset=4
                  local.get $split i32.const 0 i32.store offset=8
                  local.get $split i32.const 0 i32.store offset=12
                  local.get $block local.get $need i32.store
                end
                br $found
              end
              local.get $block i32.const 16 i32.add local.get $capacity i32.add local.set $block br $search
            end end
            local.get $block global.get $__heap_ptr i32.eq
            if
              local.get $block i32.const 16 i32.add local.get $need i32.add local.tee $end
              local.get $block i32.lt_u if unreachable end
              local.get $end i64.extend_i32_u memory.size i64.extend_i32_u i64.const 16 i64.shl i64.gt_u
              if
                local.get $end i32.const 1 i32.sub i32.const 16 i32.shr_u i32.const 1 i32.add
                memory.size i32.sub memory.grow i32.const -1 i32.eq if unreachable end
              end
              local.get $block local.get $need i32.store
              local.get $end global.set $__heap_ptr
            end
            local.get $block i32.const 1 i32.store offset=4
            local.get $block i32.const 0 i32.store offset=8
            local.get $block i32.const 0 i32.store offset=12
            local.get $block i32.const 16 i32.add i32.const 0 local.get $block i32.load memory.fill
            local.get $block i32.const 16 i32.add)
          (func $__gc_find (param $ptr i32) (result i32)
            (local $block i32) (local $end i32)
            local.get $ptr global.get $__heap_base i32.lt_u
            local.get $ptr global.get $__heap_ptr i32.ge_u i32.or if i32.const 0 return end
            global.get $__heap_base local.set $block
            block $done loop $scan
              local.get $block global.get $__heap_ptr i32.ge_u br_if $done
              local.get $block i32.const 16 i32.add local.get $block i32.load i32.add local.set $end
              local.get $ptr local.get $block i32.const 16 i32.add i32.ge_u
              local.get $ptr local.get $end i32.lt_u i32.and
              if
                local.get $block i32.load offset=4 i32.const 1 i32.and
                if local.get $block return end
                i32.const 0 return
              end
              local.get $end local.set $block br $scan
            end end i32.const 0)
          (func $__gc_mark (param $ptr i32)
            (local $block i32)
            local.get $ptr call $__gc_find local.tee $block
            if local.get $block local.get $block i32.load offset=4 i32.const 2 i32.or i32.store offset=4 end)
          (func $__gc_retain (export "__gc_retain") (param $ptr i32) (result i32)
            (local $block i32)
            local.get $ptr call $__gc_find local.tee $block
            if
              local.get $block i32.load offset=8 i32.const 2147483647 i32.eq if unreachable end
              local.get $block local.get $block i32.load offset=8 i32.const 1 i32.add i32.store offset=8
            end local.get $ptr)
          (func $__gc_release (export "__gc_release") (param $ptr i32)
            (local $block i32)
            local.get $ptr call $__gc_find local.tee $block
            if
              local.get $block i32.load offset=8 i32.eqz if unreachable end
              local.get $block local.get $block i32.load offset=8 i32.const 1 i32.sub i32.store offset=8
            end)
          (func $__gc_live_bytes (export "__gc_live_bytes") (result i32)
            (local $block i32) (local $total i32)
            global.get $__heap_base local.set $block
            block $done loop $scan
              local.get $block global.get $__heap_ptr i32.ge_u br_if $done
              local.get $block i32.load offset=4 i32.const 1 i32.and
              if local.get $total local.get $block i32.load i32.add local.set $total end
              local.get $block i32.const 16 i32.add local.get $block i32.load i32.add local.set $block br $scan
            end end local.get $total)
          (func $__gc_heap_bytes (export "__gc_heap_bytes") (result i32)
            global.get $__heap_ptr global.get $__heap_base i32.sub)
            """);
    }

    static void emitCollector(StringBuilder out) {
        // Globals are emitted by several compiler/runtime components. Gather their final symbols.
        var globals=new LinkedHashSet<String>();
        var matcher=Pattern.compile("\\(global (\\$[^\\s()]+) (?:\\(mut i32\\)|i32) ").matcher(out);
        while(matcher.find()) if(!matcher.group(1).equals("$__heap_ptr") && !matcher.group(1).equals("$__heap_base")) globals.add(matcher.group(1));
        out.append("""
          (func $__gc_collect (export "__gc_collect") (result i32)
            (local $block i32) (local $flags i32) (local $cursor i32) (local $end i32)
            (local $again i32) (local $reclaimed i32) (local $next i32)
            global.get $__heap_base local.set $block
            block $reset_done loop $reset
              local.get $block global.get $__heap_ptr i32.ge_u br_if $reset_done
              local.get $block local.get $block i32.load offset=4 i32.const -7 i32.and i32.store offset=4
              local.get $block i32.load offset=8
              if local.get $block i32.const 16 i32.add call $__gc_mark end
              local.get $block i32.const 16 i32.add local.get $block i32.load i32.add local.set $block br $reset
            end end
            """);
        for(String global:globals) out.append("    global.get ").append(global).append(" call $__gc_mark\n");
        out.append("""
            loop $trace
              i32.const 0 local.set $again
              global.get $__heap_base local.set $block
              block $trace_done loop $scan
                local.get $block global.get $__heap_ptr i32.ge_u br_if $trace_done
                local.get $block i32.load offset=4 local.tee $flags i32.const 7 i32.and i32.const 3 i32.eq
                if
                  i32.const 1 local.set $again
                  local.get $block local.get $flags i32.const 4 i32.or i32.store offset=4
                  local.get $block i32.const 16 i32.add local.set $cursor
                  local.get $cursor local.get $block i32.load i32.add local.set $end
                  block $words_done loop $words
                    local.get $cursor local.get $end i32.ge_u br_if $words_done
                    local.get $cursor i32.load call $__gc_mark
                    local.get $cursor i32.const 4 i32.add local.set $cursor br $words
                  end end
                end
                local.get $block i32.const 16 i32.add local.get $block i32.load i32.add local.set $block br $scan
              end end
              local.get $again br_if $trace
            end
            global.get $__heap_base local.set $block
            block $sweep_done loop $sweep
              local.get $block global.get $__heap_ptr i32.ge_u br_if $sweep_done
              local.get $block i32.load offset=4 local.tee $flags i32.const 3 i32.and i32.const 1 i32.eq
              if
                local.get $reclaimed local.get $block i32.load i32.add local.set $reclaimed
                local.get $block i32.const 0 i32.store offset=4
                local.get $block i32.const 0 i32.store offset=8
                local.get $block i32.const 16 i32.add i32.const 0 local.get $block i32.load memory.fill
              end
              local.get $block i32.const 16 i32.add local.get $block i32.load i32.add local.set $block br $sweep
            end end
            global.get $__heap_base local.set $block
            block $merge_done loop $merge
              local.get $block global.get $__heap_ptr i32.ge_u br_if $merge_done
              local.get $block i32.const 16 i32.add local.get $block i32.load i32.add local.set $next
              local.get $next global.get $__heap_ptr i32.lt_u
              if
                local.get $block i32.load offset=4 i32.const 1 i32.and i32.eqz
                local.get $next i32.load offset=4 i32.const 1 i32.and i32.eqz i32.and
                if
                  local.get $block local.get $block i32.load i32.const 16 i32.add local.get $next i32.load i32.add i32.store
                  br $merge
                end
              end
              local.get $next local.set $block br $merge
            end end local.get $reclaimed)
            """);
    }
}
