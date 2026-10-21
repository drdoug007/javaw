package com.example.javaw;

import com.example.javaw.dom.DOM;
import com.example.javaw.dom.Element;

public class App {
    public static int counter = 0;

    public void start() {
        Element app = DOM.getElementById("app");

        Element heading = DOM.createElement("h1");
        heading.setAttribute("class", "app-title");
        heading.setInnerText("Java to WebAssembly DOM App");
        app.appendChild(heading);

        Element status = DOM.createElement("p");
        status.setAttribute("id", "status-text");
        status.setInnerText("Count: 0");
        app.appendChild(status);

        Element btn = DOM.createElement("button");
        btn.setAttribute("id", "btn-increment");
        btn.setInnerText("Increment Counter");
        btn.addEventListener("click", "increment");
        app.appendChild(btn);
    }

    public void increment() {
        counter = counter + 1;
        Element status = DOM.getElementById("status-text");
        if (counter == 1) {
            status.setInnerText("Count: 1 click");
        } else if (counter == 2) {
            status.setInnerText("Count: 2 clicks");
        } else {
            status.setInnerText("Count: multiple clicks");
        }
    }
}
