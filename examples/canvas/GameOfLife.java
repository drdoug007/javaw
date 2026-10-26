package dastec.one.javaw.examples.canvas;

import dastec.one.javaw.dom.CanvasRenderingContext2D;
import dastec.one.javaw.dom.DOM;
import dastec.one.javaw.dom.Element;

public class GameOfLife {
    public static final int COLS = 40;
    public static final int ROWS = 30;
    public static final int CELL_SIZE = 12;

    public static int[] grid = new int[COLS * ROWS];
    public static int[] nextGrid = new int[COLS * ROWS];
    public static int generation = 0;
    public static boolean isRunning = false;

    public static void main(String[] args) {
        initGame();
    }

    public static void initGame() {
        grid = new int[COLS * ROWS];
        nextGrid = new int[COLS * ROWS];
        generation = 0;

        Element container = DOM.getElementById("game-container");
        if (container == null) {
            container = DOM.getBody();
        }

        Element title = DOM.createElement("h2");
        title.setInnerText("Conway's Game of Life (HTML5 Canvas 2D)");
        title.setStyle("color", "#34d399");
        container.appendChild(title);

        Element canvas = DOM.createElement("canvas");
        canvas.setAttribute("id", "life-canvas");
        canvas.setAttribute("width", "480");
        canvas.setAttribute("height", "360");
        canvas.setStyle("background", "#020617");
        canvas.setStyle("border", "1px solid #1e293b");
        canvas.setStyle("border-radius", "8px");
        canvas.setStyle("display", "block");
        canvas.setStyle("margin-bottom", "12px");
        container.appendChild(canvas);

        Element controls = DOM.createElement("div");
        controls.setStyle("display", "flex");
        controls.setStyle("gap", "10px");
        controls.setStyle("align-items", "center");

        Element stepBtn = DOM.createElement("button");
        stepBtn.setInnerText("Next Step");
        stepBtn.setStyle("padding", "6px 14px");
        stepBtn.setStyle("background", "#10b981");
        stepBtn.setStyle("color", "white");
        stepBtn.setStyle("border", "none");
        stepBtn.setStyle("border-radius", "4px");
        stepBtn.setStyle("cursor", "pointer");
        stepBtn.addEventListener("click", "step");
        controls.appendChild(stepBtn);

        Element randomBtn = DOM.createElement("button");
        randomBtn.setInnerText("Randomize");
        randomBtn.setStyle("padding", "6px 14px");
        randomBtn.setStyle("background", "#6366f1");
        randomBtn.setStyle("color", "white");
        randomBtn.setStyle("border", "none");
        randomBtn.setStyle("border-radius", "4px");
        randomBtn.setStyle("cursor", "pointer");
        randomBtn.addEventListener("click", "randomize");
        controls.appendChild(randomBtn);

        Element clearBtn = DOM.createElement("button");
        clearBtn.setInnerText("Clear");
        clearBtn.setStyle("padding", "6px 14px");
        clearBtn.setStyle("background", "#ef4444");
        clearBtn.setStyle("color", "white");
        clearBtn.setStyle("border", "none");
        clearBtn.setStyle("border-radius", "4px");
        clearBtn.setStyle("cursor", "pointer");
        clearBtn.addEventListener("click", "clearGrid");
        controls.appendChild(clearBtn);

        Element info = DOM.createElement("span");
        info.setAttribute("id", "gen-info");
        info.setStyle("color", "#94a3b8");
        info.setStyle("font-size", "14px");
        info.setInnerText("Generation: 0");
        controls.appendChild(info);

        container.appendChild(controls);

        // Seed with glider and oscillator
        seedGlider(2, 2);
        seedBlinker(15, 10);
        seedGlider(20, 15);

        render();
    }

    public static void seedGlider(int x, int y) {
        setCell(x + 1, y, 1);
        setCell(x + 2, y + 1, 1);
        setCell(x, y + 2, 1);
        setCell(x + 1, y + 2, 1);
        setCell(x + 2, y + 2, 1);
    }

    public static void seedBlinker(int x, int y) {
        setCell(x, y, 1);
        setCell(x + 1, y, 1);
        setCell(x + 2, y, 1);
    }

    public static int getCell(int x, int y) {
        if (x < 0 || x >= COLS || y < 0 || y >= ROWS) return 0;
        return grid[y * COLS + x];
    }

    public static void setCell(int x, int y, int val) {
        if (x >= 0 && x < COLS && y >= 0 && y < ROWS) {
            grid[y * COLS + x] = val;
        }
    }

    public static int countNeighbors(int x, int y) {
        int count = 0;
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) continue;
                count += getCell(x + dx, y + dy);
            }
        }
        return count;
    }

    public static void step() {
        for (int y = 0; y < ROWS; y++) {
            for (int x = 0; x < COLS; x++) {
                int neighbors = countNeighbors(x, y);
                int current = getCell(x, y);
                if (current == 1) {
                    if (neighbors == 2 || neighbors == 3) {
                        nextGrid[y * COLS + x] = 1;
                    } else {
                        nextGrid[y * COLS + x] = 0;
                    }
                } else {
                    if (neighbors == 3) {
                        nextGrid[y * COLS + x] = 1;
                    } else {
                        nextGrid[y * COLS + x] = 0;
                    }
                }
            }
        }

        // Copy nextGrid back to grid
        for (int i = 0; i < COLS * ROWS; i++) {
            grid[i] = nextGrid[i];
        }

        generation = generation + 1;
        updateInfo();
        render();
    }

    public static void randomize() {
        for (int i = 0; i < COLS * ROWS; i++) {
            // Simple linear congruential pseudo-random generator
            int r = (i * 1103515245 + 12345) & 0x7fffffff;
            grid[i] = (r % 5 == 0) ? 1 : 0;
        }
        generation = 0;
        updateInfo();
        render();
    }

    public static void clearGrid() {
        for (int i = 0; i < COLS * ROWS; i++) {
            grid[i] = 0;
        }
        generation = 0;
        updateInfo();
        render();
    }

    public static void updateInfo() {
        Element info = DOM.getElementById("gen-info");
        if (info != null) {
            info.setInnerText("Generation: " + generation);
        }
    }

    public static void render() {
        CanvasRenderingContext2D ctx = DOM.getCanvasContext2D("life-canvas");
        if (ctx == null) return;

        // Clear canvas
        ctx.setFillStyle("#020617");
        ctx.fillRect(0, 0, COLS * CELL_SIZE, ROWS * CELL_SIZE);

        // Draw active cells
        ctx.setFillStyle("#10b981");
        for (int y = 0; y < ROWS; y++) {
            for (int x = 0; x < COLS; x++) {
                if (grid[y * COLS + x] == 1) {
                    ctx.fillRect(x * CELL_SIZE + 1, y * CELL_SIZE + 1, CELL_SIZE - 2, CELL_SIZE - 2);
                }
            }
        }
    }
}
