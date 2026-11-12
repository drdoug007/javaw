package one.dastec.javaw.bytecode;

import one.dastec.javaw.Wat2Wasm;
import one.dastec.javaw.bytecode.BytecodeCompiler.NativeImport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.ToolProvider;
import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The examples/nebula-run game: its unchanged sources, compiled by javac and by javaw-bytecode with the example's
 * canvas/HUD/audio native imports. The autopilot simulation must hash the same draw commands on the JVM and in Wasm,
 * and the browser entry points must run against stub imports with the heap collected between frames.
 */
class BytecodeNebulaRunTest {
    @TempDir Path directory;

    private static final Path SOURCES = Path.of("..", "examples", "nebula-run", "src", "nebula");
    private static final String SCREEN = "nebula.CanvasScreen.";
    private static final List<String> BINDINGS = List.of(
            "selectLayer(I)V=canvas:layer", "fill(I)V=canvas:clear", "fillRect(IIIII)V=canvas:rect",
            "fillCircle(IIII)V=canvas:circle", "fillDiamond(IIII)V=canvas:diamond", "showHud(IIII)V=hud:update",
            "playTone(III)V=audio:tone");

    @Test void gameMatchesTheJvmCommandForCommand() throws Exception {
        Path classes = Files.createDirectories(directory.resolve("classes"));
        List<String> arguments = new ArrayList<>(List.of("-g", "--release", "17", "-d", classes.toString()));
        try (var files = Files.list(SOURCES)) { files.filter(file -> file.toString().endsWith(".java")).sorted().forEach(file -> arguments.add(file.toString())); }
        assertTrue(arguments.size() > 10, "example sources not found at " + SOURCES.toAbsolutePath());
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null, arguments.toArray(String[]::new)));

        String jvm;
        try (URLClassLoader loader = new URLClassLoader(new URL[]{classes.toUri().toURL()}, null)) {
            jvm = (String) loader.loadClass("nebula.Simulation").getMethod("run", int.class, int.class).invoke(null, 2026, 6000);
        }
        assertTrue(jvm.contains("games=") && !jvm.contains("commands=0 "), jvm);

        List<NativeImport> imports = BINDINGS.stream().map(binding -> NativeImport.parse(SCREEN + binding)).toList();
        String wat = BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar(classes)), List.of(), Set.of(),
                BytecodeCompiler.Memory.managed(), imports);
        Path input = directory.resolve("nebula.wat"), output = directory.resolve("nebula.wasm"), script = directory.resolve("run.cjs");
        Files.writeString(input, wat);
        WasmTestSupport.assemble(input, output, Wat2Wasm.EXCEPTIONS);
        Files.writeString(script, """
            const fs=require('node:fs'),assert=require('node:assert/strict');
            const compiled=new WebAssembly.Module(fs.readFileSync(process.argv[2]));
            assert.deepEqual(WebAssembly.Module.imports(compiled).map(i=>i.module+'.'+i.name),
                ['env.memory','canvas.layer','canvas.clear','canvas.rect','canvas.circle','canvas.diamond','hud.update','audio.tone']);
            const calls={},count=name=>()=>{calls[name]=(calls[name]||0)+1;};
            const imports={canvas:{layer:count('layer'),clear:count('clear'),rect:count('rect'),circle:count('circle'),diamond:count('diamond')},
                hud:{update:count('hud')},audio:{tone:count('tone')}};
            function instance(){
                const memory=new WebAssembly.Memory({initial:2});
                const e=new WebAssembly.Instance(compiled,{env:{memory},...imports}).exports;
                e.__gc_scribble.value=1;
                function text(pointer){const view=new DataView(memory.buffer);
                    const chars=view.getInt32(pointer+4,true),length=view.getInt32(chars+4,true);
                    let result='';for(let i=0;i<length;i++)result+=String.fromCharCode(view.getUint16(chars+8+2*i,true));return result;}
                return {e,memory,text};
            }
            {const {e,text}=instance();assert.equal(text(e['nebula.Simulation.run'](2026,6000)),%s);}
            {const {e,memory}=instance();e['nebula.Nebula.start'](7);let peak=0;
             for(let frame=0;frame<1200;frame++){
                if(frame%%45===0)e['nebula.Nebula.steer'](frame/45%%4);
                if(frame===600||frame===640)e['nebula.Nebula.press']();
                e['nebula.Nebula.update']();e['nebula.Nebula.render']();e.__gc_collect();peak=Math.max(peak,e.__gc_live_bytes());}
             for(const name of ['layer','clear','rect','circle','diamond','hud','tone'])assert.ok(calls[name]>0,name);
             assert.ok(peak<64*1024,'live heap '+peak);assert.equal(memory.buffer.byteLength,2*65536);}
            """.formatted(js(jvm)));
        WasmTestSupport.node(script, output);
    }

    /** Every game class except the JVM-only entry point, which is not in the source directory. */
    private static byte[] jar(Path classes) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (JarOutputStream jar = new JarOutputStream(bytes); var files = Files.walk(classes)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".class")).sorted().toList()) {
                jar.putNextEntry(new JarEntry(classes.relativize(file).toString().replace(File.separatorChar, '/')));
                jar.write(Files.readAllBytes(file));
                jar.closeEntry();
            }
        }
        return bytes.toByteArray();
    }

    private static String js(String value) {
        StringBuilder out = new StringBuilder("\"");
        for (char c : value.toCharArray()) out.append(String.format("\\u%04x", (int) c));
        return out.append('"').toString();
    }
}
