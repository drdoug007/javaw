package one.dastec.javaw.bytecode;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import java.util.*;

/**
 * Module-owned, reclaiming heap for bundled-runtime builds: a free-list allocator over growable imported memory and
 * a precise, non-moving mark-sweep collector that hosts invoke only at stack-free safepoints ({@code __gc_collect}).
 *
 * <p>Blocks have an 8-byte header before an 8-byte-aligned payload. Word 0 holds the payload capacity with flags in
 * its low bits (1 used, 2 marked, 4 scanned). Word 1 holds the host retain count of a used block, or the next block
 * of a free list. Free lists for payloads up to {@link #SMALL} bytes are exact per 8-byte size; larger free blocks
 * share one first-fit list. Every allocation is a tagged object or array, so tracing follows only reference fields
 * and reference-array elements.
 */
final class BytecodeHeap {
    static final int SMALL = 256;
    /** List heads: index 0 for large blocks, then one per small size, at {@code base}; padded to keep payloads 8-aligned. */
    private static final int TABLE = ((SMALL / 8 + 1) * 4 + 7) & ~7;
    private static final int FIRST_REFERENCE_ARRAY = -100;

    private BytecodeHeap() {}

    /**
     * Heap runtime for a final, pruned module text. Roots are host retains, reference statics and runtime caches still
     * present in {@code wat}, and the static emergency exception objects. {@code instantiated} bounds the receiver
     * classes; any other object tag traps during collection rather than being traced incompletely.
     */
    static String runtimeWat(Map<String, ClassNode> linked, BytecodeDispatch dispatch, Set<String> instantiated, String wat) {
        BytecodeObjectLayouts layouts = BytecodeObjectLayouts.of(linked);
        int base = (BytecodeTraces.stackBase(linked) + BytecodeTraces.CAPACITY * 8 + 7) & ~7;
        StringBuilder out = new StringBuilder();
        out.append("(global $__gc_table i32 (i32.const ").append(base).append("))\n")
                .append("(global $__gc_start i32 (i32.const ").append(base + TABLE).append("))\n")
                .append("(global $__gc_ptr (mut i32) (i32.const 0))\n")
                .append("(global $__gc_stack (mut i32) (i32.const 0))\n")
                .append("(global $__gc_top (mut i32) (i32.const 0))\n")
                .append("(global $__gc_overflow (mut i32) (i32.const 0))\n")
                .append("(global $__gc_low (mut i32) (i32.const -1))\n")
                // Debug: when non-zero, fill every allocation with 0x7f before its caller zeroes it, like a dirty host heap.
                .append("(global $__gc_scribble (export \"__gc_scribble\") (mut i32) (i32.const 0))\n")
                // Debug: allocation requests so far, and requests still to fail (negative fails every request).
                .append("(global $__gc_allocations (export \"__gc_allocations\") (mut i32) (i32.const 0))\n")
                .append("(global $__gc_fail (export \"__gc_fail_allocations\") (mut i32) (i32.const 0))\n")
                // Diagnostic: mark-stack pushes dropped (and recovered by rescanning) during the last collection.
                .append("(global $__gc_overflows (export \"__gc_mark_overflows\") (mut i32) (i32.const 0))\n");
        out.append("""
            (func $__gc_init
              global.get $__gc_start global.set $__gc_ptr
              global.get $__gc_table i32.const 0 i32.const %d memory.fill)
            (func $__gc_reserve (param $end i32) (result i32)
              local.get $end i64.extend_i32_u memory.size i64.extend_i32_u i64.const 16 i64.shl i64.le_u if i32.const 1 return end
              local.get $end i32.const 1 i32.sub i32.const 16 i32.shr_u i32.const 1 i32.add memory.size i32.sub
              memory.grow i32.const -1 i32.ne)
            (func $__gc_free_block (param $block i32) (param $size i32) (local $list i32)
              global.get $__gc_table local.set $list
              local.get $size i32.const %d i32.le_u if global.get $__gc_table local.get $size i32.const 1 i32.shr_u i32.add local.set $list end
              local.get $block local.get $size i32.store
              local.get $block local.get $list i32.load i32.store offset=4
              local.get $list local.get $block i32.store)
            (func $__heap_alloc (param $size i32) (result i32)
              (local $need i32) (local $block i32) (local $prev i32) (local $capacity i32) (local $end i32) (local $fit i32)
              global.get $__gc_ptr i32.eqz if call $__gc_init end
              global.get $__gc_allocations i32.const 1 i32.add global.set $__gc_allocations
              global.get $__gc_fail if
                global.get $__gc_fail i32.const 0 i32.gt_s if global.get $__gc_fail i32.const 1 i32.sub global.set $__gc_fail end
                i32.const 0 return
              end
              local.get $size i32.const 2147483632 i32.gt_u if i32.const 0 return end
              local.get $size i32.const 7 i32.add i32.const -8 i32.and local.tee $need i32.eqz if i32.const 8 local.set $need end
              block $take
                local.get $need i32.const %d i32.le_u if
                  global.get $__gc_table local.get $need i32.const 1 i32.shr_u i32.add local.tee $prev i32.load local.tee $block
                  if local.get $prev local.get $block i32.load offset=4 i32.store br $take end
                  ;; Otherwise split the smallest larger small block; coalescing leaves sizes no request asks for exactly.
                  local.get $need local.set $fit
                  block $small_none loop $small
                    local.get $fit i32.const 8 i32.add local.tee $fit i32.const %d i32.gt_u br_if $small_none
                    global.get $__gc_table local.get $fit i32.const 1 i32.shr_u i32.add local.tee $prev i32.load local.tee $block i32.eqz br_if $small
                    local.get $prev local.get $block i32.load offset=4 i32.store
                    local.get $fit local.get $need i32.sub i32.const 16 i32.ge_u if
                      local.get $block i32.const 8 i32.add local.get $need i32.add
                      local.get $fit local.get $need i32.sub i32.const 8 i32.sub call $__gc_free_block
                      local.get $block local.get $need i32.store
                    end
                    br $take
                  end end
                end
                global.get $__gc_table local.set $prev
                block $none loop $search
                  local.get $prev i32.load local.tee $block i32.eqz br_if $none
                  local.get $block i32.load local.tee $capacity local.get $need i32.ge_u if
                    local.get $prev local.get $block i32.load offset=4 i32.store
                    local.get $capacity local.get $need i32.sub i32.const 16 i32.ge_u if
                      local.get $block i32.const 8 i32.add local.get $need i32.add
                      local.get $capacity local.get $need i32.sub i32.const 8 i32.sub call $__gc_free_block
                      local.get $block local.get $need i32.store
                    end
                    br $take
                  end
                  local.get $block i32.const 4 i32.add local.set $prev br $search
                end end
                global.get $__gc_ptr local.tee $block i32.const 8 i32.add local.get $need i32.add local.tee $end
                local.get $block i32.lt_u if i32.const 0 return end
                local.get $end call $__gc_reserve i32.eqz if i32.const 0 return end
                local.get $block local.get $need i32.store
                local.get $end global.set $__gc_ptr
              end
              local.get $block local.get $block i32.load i32.const 1 i32.or i32.store
              local.get $block i32.const 0 i32.store offset=4
              global.get $__gc_scribble if local.get $block i32.const 8 i32.add i32.const 127 local.get $block i32.load i32.const -8 i32.and memory.fill end
              local.get $block i32.const 8 i32.add)
            (func $__gc_block (param $ptr i32) (result i32) (local $block i32)
              local.get $ptr global.get $__gc_start i32.const 8 i32.add i32.lt_u
              local.get $ptr global.get $__gc_ptr i32.ge_u i32.or if i32.const 0 return end
              local.get $ptr i32.const 8 i32.sub local.tee $block i32.load i32.const 1 i32.and i32.eqz if unreachable end
              local.get $block)
            (func $__gc_mark (param $ptr i32) (local $block i32) (local $word i32)
              local.get $ptr call $__gc_block local.tee $block i32.eqz if return end
              local.get $block i32.load local.tee $word i32.const 2 i32.and if return end
              local.get $block local.get $word i32.const 2 i32.or i32.store
              global.get $__gc_top i32.const -4 i32.ge_u
              if (result i32) i32.const 1 else global.get $__gc_top i32.const 4 i32.add call $__gc_reserve i32.eqz end
              if
                ;; Leave the block marked but unscanned; recovery rescans from the lowest dropped block.
                i32.const 1 global.set $__gc_overflow
                global.get $__gc_overflows i32.const 1 i32.add global.set $__gc_overflows
                local.get $block global.get $__gc_low i32.lt_u if local.get $block global.set $__gc_low end
                return
              end
              global.get $__gc_top local.get $block i32.store
              global.get $__gc_top i32.const 4 i32.add global.set $__gc_top)
            (func $__gc_drain (local $block i32)
              loop $next
                global.get $__gc_top global.get $__gc_stack i32.gt_u if
                  global.get $__gc_top i32.const 4 i32.sub global.set $__gc_top
                  global.get $__gc_top i32.load local.tee $block local.get $block i32.load i32.const 4 i32.or i32.store
                  local.get $block i32.const 8 i32.add call $__gc_trace
                  br $next
                end
              end)
            (func $__gc_retain (export "__gc_retain") (param $ptr i32) (result i32) (local $block i32)
              local.get $ptr call $__gc_block local.tee $block if
                local.get $block i32.load offset=4 i32.const 2147483647 i32.eq if unreachable end
                local.get $block local.get $block i32.load offset=4 i32.const 1 i32.add i32.store offset=4
              end local.get $ptr)
            (func $__gc_release (export "__gc_release") (param $ptr i32) (local $block i32)
              local.get $ptr call $__gc_block local.tee $block if
                local.get $block i32.load offset=4 i32.eqz if unreachable end
                local.get $block local.get $block i32.load offset=4 i32.const 1 i32.sub i32.store offset=4
              end)
            (func $__gc_live_bytes (export "__gc_live_bytes") (result i32) (local $block i32) (local $total i32)
              global.get $__gc_ptr i32.eqz if i32.const 0 return end
              global.get $__gc_start local.set $block
              block $done loop $scan
                local.get $block global.get $__gc_ptr i32.ge_u br_if $done
                local.get $block i32.load i32.const 1 i32.and if local.get $total local.get $block i32.load i32.const -8 i32.and i32.add local.set $total end
                local.get $block i32.const 8 i32.add local.get $block i32.load i32.const -8 i32.and i32.add local.set $block br $scan
              end end local.get $total)
            (func $__gc_heap_bytes (export "__gc_heap_bytes") (result i32)
              global.get $__gc_ptr i32.eqz if i32.const 0 return end
              global.get $__gc_ptr global.get $__gc_start i32.sub)
            """.formatted(TABLE, SMALL, SMALL, SMALL));
        traceWat(out, linked, dispatch, layouts, instantiated, wat);
        collectWat(out, linked, layouts, instantiated, wat);
        return out.toString();
    }

    /** Emergency OutOfMemoryError/StackOverflowError objects: static data, traced as roots, never heap blocks. */
    private static List<BytecodeExceptions.Implicit> emergencies(Map<String, ClassNode> linked, String wat) {
        List<BytecodeExceptions.Implicit> result = new ArrayList<>();
        for (var kind : List.of(BytecodeExceptions.Implicit.OUT_OF_MEMORY, BytecodeExceptions.Implicit.STACK_OVERFLOW))
            if (wat.contains("(data (i32.const " + BytecodeTraces.emergencyAddress(kind, linked) + ") ")) result.add(kind);
        return result;
    }

    private static void traceWat(StringBuilder out, Map<String, ClassNode> linked, BytecodeDispatch dispatch,
                                 BytecodeObjectLayouts layouts, Set<String> instantiated, String wat) {
        Set<String> classes = new TreeSet<>(instantiated);
        for (var kind : emergencies(linked, wat)) classes.add(kind.className);
        // Class ids are dense from 1; tag 0 is a block that was never tagged and holds no references.
        Map<Integer, String> labels = new TreeMap<>();
        List<String> arms = new ArrayList<>();
        int maximum = 0;
        for (String name : classes) {
            if (!layouts.contains(name)) continue;
            int id = dispatch.classId(name);
            maximum = Math.max(maximum, id);
            if (layouts.referenceOffsets(name).isEmpty()) labels.put(id, "$done");
            else {
                labels.put(id, "$c" + id);
                arms.add(name);
            }
        }
        out.append("(func $__gc_trace (param $object i32) (local $tag i32) (local $cursor i32) (local $end i32)\n")
                .append("local.get $object i32.load local.tee $tag i32.const 0 i32.lt_s if\n")
                .append("local.get $tag i32.const ").append(FIRST_REFERENCE_ARRAY).append(" i32.gt_s if return end\n")
                .append("local.get $object i32.const 8 i32.add local.tee $cursor local.get $object i32.load offset=4 i32.const 2 i32.shl i32.add local.set $end\n")
                .append("block $elements_done loop $elements local.get $cursor local.get $end i32.ge_u br_if $elements_done\n")
                .append("local.get $cursor i32.load call $__gc_mark local.get $cursor i32.const 4 i32.add local.set $cursor br $elements end end return end\n")
                .append("block $done block $bad\n");
        for (int i = arms.size() - 1; i >= 0; i--) out.append("block $c").append(dispatch.classId(arms.get(i))).append('\n');
        out.append("local.get $tag br_table $done");
        for (int id = 1; id <= maximum; id++) out.append(' ').append(labels.getOrDefault(id, "$bad"));
        out.append(" $bad\n");
        for (String name : arms) {
            out.append("end\n");
            for (int offset : layouts.referenceOffsets(name))
                out.append("local.get $object i32.load offset=").append(offset).append(" call $__gc_mark\n");
            out.append("br $done\n");
        }
        // An object of a class the module cannot instantiate means reachability was unsound; do not free its graph.
        out.append("end unreachable end)\n");
    }

    private static void collectWat(StringBuilder out, Map<String, ClassNode> linked, BytecodeObjectLayouts layouts,
                                   Set<String> instantiated, String wat) {
        Set<String> statics = new HashSet<>();
        for (ClassNode type : linked.values())
            for (FieldNode field : type.fields)
                if ((field.access & Opcodes.ACC_STATIC) != 0 && (field.desc.startsWith("L") || field.desc.startsWith("[")))
                    statics.add(TypeHelper.getGlobalFieldName(type.name, field.name, field.desc));
        Set<String> roots = new TreeSet<>();
        var globals = java.util.regex.Pattern.compile("\\(global (\\$[^\\s()]+) \\(mut i32\\)").matcher(wat);
        while (globals.find()) {
            String name = globals.group(1);
            if (statics.contains(name) || name.endsWith("_cached") && (name.startsWith("$__text") || name.startsWith("$__mirror"))
                    || name.startsWith("$method_") && name.endsWith("_failure")) roots.add(name);
        }
        out.append("""
            (func $__gc_collect (export "__gc_collect") (result i32)
              (local $block i32) (local $word i32) (local $size i32) (local $run i32) (local $reclaimed i32)
              global.get $__gc_ptr i32.eqz if call $__gc_init end
              global.get $__gc_ptr global.set $__gc_stack
              global.get $__gc_ptr global.set $__gc_top
              i32.const 0 global.set $__gc_overflow
              i32.const 0 global.set $__gc_overflows
              i32.const -1 global.set $__gc_low
              global.get $__gc_start local.set $block
              block $retained_done loop $retained
                local.get $block global.get $__gc_ptr i32.ge_u br_if $retained_done
                local.get $block i32.load local.tee $word i32.const 1 i32.and if
                  local.get $block i32.load offset=4 if local.get $block i32.const 8 i32.add call $__gc_mark end
                end
                local.get $block i32.const 8 i32.add local.get $word i32.const -8 i32.and i32.add local.set $block br $retained
              end end
            """);
        for (String root : roots) out.append("global.get ").append(root).append(" call $__gc_mark\n");
        for (var kind : emergencies(linked, wat))
            out.append("i32.const ").append(BytecodeTraces.emergencyAddress(kind, linked)).append(" call $__gc_trace\n");
        out.append("""
              call $__gc_drain
              ;; A full mark stack leaves marked, unscanned blocks; rescan until none remain. A block dropped below the
              ;; scan position rewinds the scan to it, so long downward-linked graphs do not need one pass per node.
              block $recovered loop $recover
                global.get $__gc_overflow i32.eqz br_if $recovered
                i32.const 0 global.set $__gc_overflow
                i32.const -1 global.set $__gc_low
                global.get $__gc_start local.set $block
                block $rescan_done loop $rescan
                  local.get $block global.get $__gc_ptr i32.ge_u br_if $rescan_done
                  local.get $block i32.load local.tee $word i32.const 7 i32.and i32.const 3 i32.eq if
                    local.get $block local.get $word i32.const 4 i32.or i32.store
                    local.get $block i32.const 8 i32.add call $__gc_trace
                    call $__gc_drain
                    global.get $__gc_low local.get $block i32.lt_u if
                      global.get $__gc_low local.set $block i32.const -1 global.set $__gc_low br $rescan
                    end
                  end
                  local.get $block i32.const 8 i32.add local.get $word i32.const -8 i32.and i32.add local.set $block br $rescan
                end end
                br $recover
              end end
              ;; Sweep: rebuild every free list, coalescing adjacent free blocks; a trailing free run returns to the bump pointer.
              global.get $__gc_table i32.const 0 i32.const %d memory.fill
              global.get $__gc_start local.set $block
              block $sweep_done loop $sweep
                local.get $block global.get $__gc_ptr i32.ge_u br_if $sweep_done
                local.get $block i32.load local.tee $word i32.const -8 i32.and local.set $size
                local.get $word i32.const 3 i32.and i32.const 3 i32.eq if
                  local.get $block local.get $size i32.const 1 i32.or i32.store
                  local.get $run if local.get $run local.get $block local.get $run i32.sub i32.const 8 i32.sub call $__gc_free_block i32.const 0 local.set $run end
                else
                  local.get $word i32.const 1 i32.and if local.get $reclaimed local.get $size i32.add local.set $reclaimed end
                  local.get $run i32.eqz if local.get $block local.set $run end
                end
                local.get $block i32.const 8 i32.add local.get $size i32.add local.set $block br $sweep
              end end
              local.get $run if local.get $run global.set $__gc_ptr end
              local.get $reclaimed)
            """.formatted(TABLE));
    }
}
