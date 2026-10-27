package one.dastec.javaw.bytecode.cfg;

import java.util.*;

public class Relooper {
    private int shapeIdCounter = 0;

    public abstract static class Shape {
        protected final int id;
        protected Shape next;

        public Shape(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public Shape getNext() {
            return next;
        }

        public void setNext(Shape next) {
            this.next = next;
        }
    }

    public static class SimpleShape extends Shape {
        private final BasicBlock block;

        public SimpleShape(int id, BasicBlock block) {
            super(id);
            this.block = block;
        }

        public BasicBlock getBlock() {
            return block;
        }
    }

    public static class LoopShape extends Shape {
        private final BasicBlock header;
        private Shape inner;

        public LoopShape(int id, BasicBlock header, Shape inner) {
            super(id);
            this.header = header;
            this.inner = inner;
        }

        public BasicBlock getHeader() {
            return header;
        }

        public Shape getInner() {
            return inner;
        }

        public void setInner(Shape inner) {
            this.inner = inner;
        }
    }

    public static class MultipleShape extends Shape {
        private final Map<BasicBlock, Shape> handlers = new LinkedHashMap<>();

        public MultipleShape(int id) {
            super(id);
        }

        public Map<BasicBlock, Shape> getHandlers() {
            return handlers;
        }

        public void addHandler(BasicBlock entry, Shape handler) {
            handlers.put(entry, handler);
        }
    }

    private final Set<BasicBlock> handledLoopHeaders = new HashSet<>();

    public Shape reloop(ControlFlowGraph cfg) {
        if (cfg.getBlocks().isEmpty()) {
            return null;
        }
        handledLoopHeaders.clear();
        Set<BasicBlock> allBlocks = new LinkedHashSet<>(cfg.getBlocks());
        return process(allBlocks, cfg.getEntryBlock());
    }

    private Shape process(Set<BasicBlock> blocks, BasicBlock entry) {
        if (blocks.isEmpty() || entry == null || !blocks.contains(entry)) {
            return null;
        }

        if (blocks.size() == 1) {
            return new SimpleShape(shapeIdCounter++, entry);
        }

        // Check if entry has loop back-edges from within blocks
        boolean isLoop = !handledLoopHeaders.contains(entry) && (entry.isLoopHeader() || hasBackEdgeTo(blocks, entry));
        if (isLoop) {
            handledLoopHeaders.add(entry);
            // Find natural loop body (all blocks in `blocks` that can reach `entry` without leaving `blocks`)
            Set<BasicBlock> loopBody = findLoopBody(blocks, entry);
            Set<BasicBlock> loopExits = new LinkedHashSet<>(blocks);
            loopExits.removeAll(loopBody);

            Shape inner = process(loopBody, entry);
            LoopShape loopShape = new LoopShape(shapeIdCounter++, entry, inner);

            if (!loopExits.isEmpty()) {
                BasicBlock exitEntry = findFirstEntry(loopExits, loopBody);
                if (exitEntry != null) {
                    loopShape.setNext(process(loopExits, exitEntry));
                }
            }
            return loopShape;
        }

        // SimpleShape: entry is not a loop header (or already handled)
        SimpleShape simple = new SimpleShape(shapeIdCounter++, entry);
        Set<BasicBlock> remaining = new LinkedHashSet<>(blocks);
        remaining.remove(entry);

        if (!remaining.isEmpty()) {
            // Check entries into remaining
            List<BasicBlock> entries = findEntriesFrom(entry, remaining);
            if (entries.size() == 1) {
                simple.setNext(process(remaining, entries.get(0)));
            } else if (entries.size() > 1) {
                // Multiple branch entries from `entry`
                MultipleShape multi = new MultipleShape(shapeIdCounter++);
                for (BasicBlock e : entries) {
                    Set<BasicBlock> reachable = getReachableSubgraph(remaining, e);
                    multi.addHandler(e, process(reachable, e));
                }
                simple.setNext(multi);
            }
        }

        return simple;
    }

    private boolean hasBackEdgeTo(Set<BasicBlock> blocks, BasicBlock target) {
        for (BasicBlock b : blocks) {
            if (b.getSuccessors().contains(target) && b.getId() >= target.getId()) {
                return true;
            }
        }
        return false;
    }

    private Set<BasicBlock> findLoopBody(Set<BasicBlock> blocks, BasicBlock header) {
        Set<BasicBlock> body = new LinkedHashSet<>();
        body.add(header);

        Queue<BasicBlock> queue = new ArrayDeque<>();
        for (BasicBlock pred : header.getPredecessors()) {
            if (blocks.contains(pred) && !pred.equals(header)) {
                body.add(pred);
                queue.add(pred);
            }
        }

        while (!queue.isEmpty()) {
            BasicBlock curr = queue.poll();
            for (BasicBlock pred : curr.getPredecessors()) {
                if (blocks.contains(pred) && body.add(pred)) {
                    queue.add(pred);
                }
            }
        }

        // Add any internal forward paths strictly within the loop body
        boolean changed = true;
        while (changed) {
            changed = false;
            for (BasicBlock b : new ArrayList<>(body)) {
                for (BasicBlock succ : b.getSuccessors()) {
                    if (blocks.contains(succ) && !body.contains(succ)) {
                        // Check if all successors of succ lead back to loop header
                        if (canReach(blocks, succ, header)) {
                            body.add(succ);
                            changed = true;
                        }
                    }
                }
            }
        }

        return body;
    }

    private boolean canReach(Set<BasicBlock> allowed, BasicBlock from, BasicBlock to) {
        Set<BasicBlock> visited = new HashSet<>();
        Queue<BasicBlock> q = new ArrayDeque<>();
        q.add(from);
        visited.add(from);

        while (!q.isEmpty()) {
            BasicBlock c = q.poll();
            if (c.equals(to)) return true;
            for (BasicBlock s : c.getSuccessors()) {
                if (allowed.contains(s) && visited.add(s)) {
                    q.add(s);
                }
            }
        }
        return false;
    }

    private BasicBlock findFirstEntry(Set<BasicBlock> targetSet, Set<BasicBlock> fromSet) {
        for (BasicBlock from : fromSet) {
            for (BasicBlock succ : from.getSuccessors()) {
                if (targetSet.contains(succ)) {
                    return succ;
                }
            }
        }
        return targetSet.iterator().next();
    }

    private List<BasicBlock> findEntriesFrom(BasicBlock source, Set<BasicBlock> targetSet) {
        List<BasicBlock> list = new ArrayList<>();
        for (BasicBlock succ : source.getSuccessors()) {
            if (targetSet.contains(succ) && !list.contains(succ)) {
                list.add(succ);
            }
        }
        return list;
    }

    private Set<BasicBlock> getReachableSubgraph(Set<BasicBlock> pool, BasicBlock start) {
        Set<BasicBlock> reachable = new LinkedHashSet<>();
        Queue<BasicBlock> q = new ArrayDeque<>();
        reachable.add(start);
        q.add(start);

        while (!q.isEmpty()) {
            BasicBlock curr = q.poll();
            for (BasicBlock s : curr.getSuccessors()) {
                if (pool.contains(s) && reachable.add(s)) {
                    q.add(s);
                }
            }
        }
        return reachable;
    }
}
