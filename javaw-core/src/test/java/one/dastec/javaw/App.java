package one.dastec.javaw;

import one.dastec.javaw.dom.DOM;
import one.dastec.javaw.dom.Element;

public class App {
    public static int counter = 0;

    public void start() {
        Element app = DOM.getElementById("app");

        Element heading = DOM.createElement("h1");
        heading.setAttribute("class", "app-title");
        heading.setInnerText("Java to WebAssembly DOM App");
        heading.setStyle("color", "blue");
        app.appendChild(heading);

        Element status = DOM.createElement("p");
        status.setAttribute("id", "status-text");
        status.setInnerText("Count: 0");
        status.addClass("status-badge");
        app.appendChild(status);

        Element btn = DOM.createElement("button");
        btn.setAttribute("id", "btn-increment");
        btn.setInnerText("Increment Counter");
        btn.addEventListener("click", "increment");
        app.appendChild(btn);

        Element input = DOM.createElement("input");
        input.setAttribute("id", "user-input");
        input.setAttribute("type", "text");
        input.setValue("Initial text");
        app.appendChild(input);

        Element checkbox = DOM.createElement("input");
        checkbox.setAttribute("id", "user-check");
        checkbox.setAttribute("type", "checkbox");
        checkbox.setChecked(true);
        app.appendChild(checkbox);

        Element container = DOM.createElement("div");
        container.setAttribute("id", "item-container");
        for (int i = 0; i < 3; i++) {
            Element item = DOM.createElement("span");
            item.addClass("item-tag");
            item.setInnerText("Item " + i);
            container.appendChild(item);
        }
        app.appendChild(container);
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
        status.toggleClass("active");
    }

    public String readStatusText() {
        Element status = DOM.querySelector("#status-text");
        return status.getInnerText();
    }

    public String readInputValue() {
        Element input = DOM.getElementById("user-input");
        return input.getValue();
    }

    public boolean readCheckboxState() {
        Element check = DOM.getElementById("user-check");
        return check.isChecked();
    }

    public int countItemTags() {
        Element[] items = DOM.querySelectorAll(".item-tag");
        return items.length;
    }

    public void removeFirstItem() {
        Element container = DOM.getElementById("item-container");
        Element firstItem = DOM.querySelector(".item-tag");
        container.removeChild(firstItem);
    }
}
