package nebula;

/** JVM side of the comparison. Kept out of the Wasm build: the bundled runtime has no java.lang.System. */
public final class SimulationMain {
    public static void main(String[] args) {
        int seed = args.length > 0 ? Integer.parseInt(args[0]) : 2026, frames = args.length > 1 ? Integer.parseInt(args[1]) : 20000;
        System.out.println(Simulation.run(seed, frames));
    }
}
