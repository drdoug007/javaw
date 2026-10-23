package com.example.javaw;
import org.junit.jupiter.api.Test;
class ManagedHeapTest {
    @Test void tracesRootsCyclesArraysAndHostOwnershipAndReusesMemory() throws Exception {
        byte[] wasm=JavacToWasm.compile("App.java", """
            class Link {
                public Link next; public int[] values;
                public Link(int value) { values=new int[]{value,8,9}; }
            }
            class App {
                public static Link root;
                public static Supplier<int[]> captured;
                public static void establish() { root=new Link(42); root.next=root; int[] data=new int[]{77}; captured=()->data; }
                public static int check() { return root.next.values[0]+captured.get()[0]; }
                public static void clear() { root=null; captured=null; }
                public static Link orphan(int value) { Link link=new Link(value); link.next=link; return link; }
                public static int read(Link link) { return link.next.values[0]; }
                public static void churn(int value) { Link a=orphan(value); String text="temporary-"+value; int[] data=new int[256]; data[0]=a.values[0]+text.length(); }
                public static int builder() { StringBuilder text=new StringBuilder(); for(int i=0;i<3000;i++) text.append('x'); text.insert(1,"hello"); return text.toString().length(); }
                public static void fail() { Link a=orphan(3); throw new IllegalStateException(); }
            }
            """,false).getWasm();
        OverloadResolutionTest.execute(wasm,"""
            e.__gc_collect(); const baseline=e.__gc_live_bytes();
            e.establish(); e.__gc_collect(); assert.equal(e.check(),119);
            const live=e.__gc_live_bytes(); assert.ok(live>baseline);
            const host=e.orphan(91); e.__gc_retain(host); e.__gc_retain(host);
            e.__gc_collect(); assert.equal(e.read(host),91);
            e.__gc_release(host); e.__gc_collect(); assert.equal(e.read(host),91);
            e.__gc_release(host); e.clear(); assert.ok(e.__gc_collect()>0);
            assert.equal(e.__gc_live_bytes(),baseline);
            const raw=e.__heap_alloc(64); e.__gc_retain(raw+4);
            new DataView(e.memory.buffer).setInt32(raw+4,123,true);
            e.__gc_collect(); assert.equal(new DataView(e.memory.buffer).getInt32(raw+4,true),123);
            e.__gc_release(raw+4); e.__gc_collect();
            const reused=e.__heap_alloc(64); assert.equal(reused,raw);
            assert.equal(new DataView(e.memory.buffer).getInt32(reused+4,true),0);
            e.__gc_collect(); assert.equal(e.builder(),3005); e.__gc_collect();
            assert.throws(()=>e.fail()); e.__gc_collect();
            assert.equal(e.__gc_live_bytes(),baseline);
            for(let i=0;i<30;i++) { e.churn(i); e.__gc_collect(); }
            const plateau=e.__gc_heap_bytes(), pages=e.memory.buffer.byteLength;
            for(let i=0;i<500;i++) { e.churn(i); e.__gc_collect(); }
            assert.equal(e.__gc_live_bytes(),baseline);
            assert.equal(e.__gc_heap_bytes(),plateau);
            assert.equal(e.memory.buffer.byteLength,pages);
            assert.throws(()=>e.__heap_alloc(-1));
            assert.throws(()=>e.__heap_alloc(0x7fffffff));
            """);
    }
}
