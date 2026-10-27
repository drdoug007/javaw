package one.dastec.javaw.examples.simd;

import one.dastec.javaw.dom.CanvasRenderingContext2D;
import one.dastec.javaw.dom.DOM;
import one.dastec.javaw.dom.Element;
import one.dastec.javaw.simd.Float32x4;

public class SimdParticles {
    // 4 particles processed in parallel per Float32x4 lane
    public static Float32x4 posX = Float32x4.of(50.0f, 120.0f, 200.0f, 320.0f);
    public static Float32x4 posY = Float32x4.of(60.0f, 180.0f, 90.0f, 220.0f);
    public static Float32x4 velX = Float32x4.of(2.5f, -3.0f, 1.8f, -2.2f);
    public static Float32x4 velY = Float32x4.of(1.8f, 2.2f, -2.5f, 3.0f);

    public static final float CANVAS_W = 400.0f;
    public static final float CANVAS_H = 300.0f;
    public static int tickCount = 0;

    public static void main(String[] args) {
        initSimulation();
    }

    public static void initSimulation() {
        posX = Float32x4.of(50.0f, 120.0f, 200.0f, 320.0f);
        posY = Float32x4.of(60.0f, 180.0f, 90.0f, 220.0f);
        velX = Float32x4.of(2.5f, -3.0f, 1.8f, -2.2f);
        velY = Float32x4.of(1.8f, 2.2f, -2.5f, 3.0f);
        tickCount = 0;

        Element root = DOM.getElementById("simd-container");
        if (root == null) {
            root = DOM.getBody();
        }

        Element title = DOM.createElement("h2");
        title.setInnerText("128-Bit SIMD Vectorized Particle Engine");
        title.setStyle("color", "#c084fc");
        root.appendChild(title);

        Element canvas = DOM.createElement("canvas");
        canvas.setAttribute("id", "simd-canvas");
        canvas.setAttribute("width", "400");
        canvas.setAttribute("height", "300");
        canvas.setStyle("background", "#18181b");
        canvas.setStyle("border", "1px solid #3f3f46");
        canvas.setStyle("border-radius", "8px");
        canvas.setStyle("margin-bottom", "12px");
        root.appendChild(canvas);

        Element controls = DOM.createElement("div");
        controls.setStyle("display", "flex");
        controls.setStyle("gap", "10px");
        controls.setStyle("align-items", "center");

        Element updateBtn = DOM.createElement("button");
        updateBtn.setInnerText("SIMD Update Tick");
        updateBtn.setStyle("padding", "6px 14px");
        updateBtn.setStyle("background", "#a855f7");
        updateBtn.setStyle("color", "white");
        updateBtn.setStyle("border", "none");
        updateBtn.setStyle("border-radius", "4px");
        updateBtn.setStyle("cursor", "pointer");
        updateBtn.addEventListener("click", "tick");
        controls.appendChild(updateBtn);

        Element status = DOM.createElement("span");
        status.setAttribute("id", "simd-status");
        status.setStyle("color", "#a1a1aa");
        status.setStyle("font-size", "14px");
        status.setInnerText("Processed 4 particles simultaneously via v128 SIMD");
        controls.appendChild(status);

        root.appendChild(controls);

        render();
    }

    public static void tick() {
        // Parallel position integration via SIMD addition
        posX = posX.add(velX);
        posY = posY.add(velY);

        // Boundary reflection checks
        float x0 = posX.getX();
        float x1 = posX.getY();
        float x2 = posX.getZ();
        float x3 = posX.getW();

        float y0 = posY.getX();
        float y1 = posY.getY();
        float y2 = posY.getZ();
        float y3 = posY.getW();

        float vx0 = velX.getX();
        float vx1 = velX.getY();
        float vx2 = velX.getZ();
        float vx3 = velX.getW();

        float vy0 = velY.getX();
        float vy1 = velY.getY();
        float vy2 = velY.getZ();
        float vy3 = velY.getW();

        if (x0 <= 10.0f || x0 >= CANVAS_W - 10.0f) vx0 = -vx0;
        if (x1 <= 10.0f || x1 >= CANVAS_W - 10.0f) vx1 = -vx1;
        if (x2 <= 10.0f || x2 >= CANVAS_W - 10.0f) vx2 = -vx2;
        if (x3 <= 10.0f || x3 >= CANVAS_W - 10.0f) vx3 = -vx3;

        if (y0 <= 10.0f || y0 >= CANVAS_H - 10.0f) vy0 = -vy0;
        if (y1 <= 10.0f || y1 >= CANVAS_H - 10.0f) vy1 = -vy1;
        if (y2 <= 10.0f || y2 >= CANVAS_H - 10.0f) vy2 = -vy2;
        if (y3 <= 10.0f || y3 >= CANVAS_H - 10.0f) vy3 = -vy3;

        velX = Float32x4.of(vx0, vx1, vx2, vx3);
        velY = Float32x4.of(vy0, vy1, vy2, vy3);

        tickCount = tickCount + 1;
        Element status = DOM.getElementById("simd-status");
        if (status != null) {
            status.setInnerText("Processed 4 particles simultaneously via v128 SIMD (Tick " + tickCount + ")");
        }
        render();
    }

    public static void render() {
        CanvasRenderingContext2D ctx = DOM.getCanvasContext2D("simd-canvas");
        if (ctx == null) return;

        // Clear background
        ctx.setFillStyle("#18181b");
        ctx.fillRect(0, 0, (int) CANVAS_W, (int) CANVAS_H);

        // Draw particle 0 (Cyan)
        ctx.setFillStyle("#06b6d4");
        ctx.beginPath();
        ctx.arc((int) posX.getX(), (int) posY.getX(), 8, 0.0, 6.283, false);
        ctx.fill();

        // Draw particle 1 (Emerald)
        ctx.setFillStyle("#10b981");
        ctx.beginPath();
        ctx.arc((int) posX.getY(), (int) posY.getY(), 8, 0.0, 6.283, false);
        ctx.fill();

        // Draw particle 2 (Violet)
        ctx.setFillStyle("#8b5cf6");
        ctx.beginPath();
        ctx.arc((int) posX.getZ(), (int) posY.getZ(), 8, 0.0, 6.283, false);
        ctx.fill();

        // Draw particle 3 (Amber)
        ctx.setFillStyle("#f59e0b");
        ctx.beginPath();
        ctx.arc((int) posX.getW(), (int) posY.getW(), 8, 0.0, 6.283, false);
        ctx.fill();
    }
}
