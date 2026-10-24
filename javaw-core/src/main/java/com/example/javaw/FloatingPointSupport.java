package com.example.javaw;

/** Host-independent Java floating remainder shared by the source and bytecode backends. */
public final class FloatingPointSupport {
    private FloatingPointSupport() {}

    /**
     * Reduce magnitudes by exact power-of-two multiples, avoiding quotient overflow and
     * cancellation from x - trunc(x/y)*y. At most the binary64 exponent range is traversed.
     * Binary32 operands promote exactly; their remainder is representable before demotion.
     */
    public static String remainderFunctionsWat() {
        return """
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
        """;
    }
}
