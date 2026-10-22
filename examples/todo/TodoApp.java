package com.example.javaw.examples.todo;

import com.example.javaw.dom.DOM;
import com.example.javaw.dom.Element;
import com.example.javaw.util.StringBuilder;

public class TodoApp {
    public static int nextId = 1;
    public static int todoCount = 0;
    public static int completedCount = 0;

    public static void main(String[] args) {
        start();
    }

    public static void start() {
        Element root = DOM.getElementById("todo-app");
        if (root == null) {
            root = DOM.getBody();
        }

        // Build Title
        Element title = DOM.createElement("h2");
        title.setInnerText("Java WebAssembly Todo Application");
        title.setStyle("color", "#38bdf8");
        title.setStyle("margin-bottom", "16px");
        root.appendChild(title);

        // Build Input Row
        Element inputGroup = DOM.createElement("div");
        inputGroup.setAttribute("class", "input-group");
        inputGroup.setStyle("display", "flex");
        inputGroup.setStyle("gap", "8px");
        inputGroup.setStyle("margin-bottom", "16px");

        Element input = DOM.createElement("input");
        input.setAttribute("id", "todo-input");
        input.setAttribute("type", "text");
        input.setAttribute("placeholder", "What needs to be done?");
        input.setStyle("flex", "1");
        input.setStyle("padding", "8px 12px");
        input.setStyle("border-radius", "6px");
        input.setStyle("border", "1px solid #475569");
        input.setStyle("background", "#1e293b");
        input.setStyle("color", "#f8fafc");
        inputGroup.appendChild(input);

        Element addBtn = DOM.createElement("button");
        addBtn.setAttribute("id", "todo-add-btn");
        addBtn.setInnerText("Add Task");
        addBtn.setStyle("padding", "8px 16px");
        addBtn.setStyle("background", "#0ea5e9");
        addBtn.setStyle("color", "white");
        addBtn.setStyle("border", "none");
        addBtn.setStyle("border-radius", "6px");
        addBtn.setStyle("cursor", "pointer");
        addBtn.addEventListener("click", "addTodo");
        inputGroup.appendChild(addBtn);

        root.appendChild(inputGroup);

        // Todo List Container
        Element list = DOM.createElement("ul");
        list.setAttribute("id", "todo-list");
        list.setStyle("list-style", "none");
        list.setStyle("padding", "0");
        list.setStyle("margin", "0 0 16px 0");
        root.appendChild(list);

        // Status Summary
        Element status = DOM.createElement("div");
        status.setAttribute("id", "todo-status");
        status.setStyle("font-size", "14px");
        status.setStyle("color", "#94a3b8");
        status.setInnerText("0 items remaining (0 completed)");
        root.appendChild(status);

        // Seed with initial sample tasks
        createTodoItem("Explore Java-to-WebAssembly compiler");
        createTodoItem("Build rich interactive browser interfaces");
    }

    public static void addTodo() {
        Element input = DOM.getElementById("todo-input");
        if (input == null) return;
        String text = input.getValue();
        if (text == null || text.isEmpty()) return;

        createTodoItem(text);
        input.setValue("");
    }

    public static void createTodoItem(String text) {
        Element list = DOM.getElementById("todo-list");
        if (list == null) return;

        int currentId = nextId;
        nextId = nextId + 1;
        todoCount = todoCount + 1;

        Element item = DOM.createElement("li");
        item.setAttribute("id", "todo-item-" + currentId);
        item.setAttribute("class", "todo-item");
        item.setStyle("display", "flex");
        item.setStyle("align-items", "center");
        item.setStyle("justify-content", "space-between");
        item.setStyle("padding", "10px 14px");
        item.setStyle("margin-bottom", "8px");
        item.setStyle("background", "#1e293b");
        item.setStyle("border", "1px solid #334155");
        item.setStyle("border-radius", "6px");

        Element textSpan = DOM.createElement("span");
        textSpan.setAttribute("id", "todo-text-" + currentId);
        textSpan.setInnerText(text);
        textSpan.setStyle("flex", "1");
        textSpan.setStyle("color", "#f1f5f9");
        item.appendChild(textSpan);

        Element btnGroup = DOM.createElement("div");
        btnGroup.setStyle("display", "flex");
        btnGroup.setStyle("gap", "6px");

        // Toggle Done button
        Element doneBtn = DOM.createElement("button");
        doneBtn.setInnerText("Done");
        doneBtn.setStyle("padding", "4px 8px");
        doneBtn.setStyle("font-size", "12px");
        doneBtn.setStyle("background", "#10b981");
        doneBtn.setStyle("color", "white");
        doneBtn.setStyle("border", "none");
        doneBtn.setStyle("border-radius", "4px");
        doneBtn.setStyle("cursor", "pointer");
        doneBtn.addEventListener("click", "toggleTodoDone");
        btnGroup.appendChild(doneBtn);

        // Remove button
        Element delBtn = DOM.createElement("button");
        delBtn.setInnerText("Delete");
        delBtn.setStyle("padding", "4px 8px");
        delBtn.setStyle("font-size", "12px");
        delBtn.setStyle("background", "#ef4444");
        delBtn.setStyle("color", "white");
        delBtn.setStyle("border", "none");
        delBtn.setStyle("border-radius", "4px");
        delBtn.setStyle("cursor", "pointer");
        delBtn.addEventListener("click", "deleteTodo");
        btnGroup.appendChild(delBtn);

        item.appendChild(btnGroup);
        list.appendChild(item);

        updateStatusText();
    }

    public static void toggleTodoDone() {
        completedCount = completedCount + 1;
        updateStatusText();
    }

    public static void deleteTodo() {
        if (todoCount > 0) {
            todoCount = todoCount - 1;
        }
        Element list = DOM.getElementById("todo-list");
        Element firstItem = DOM.querySelector(".todo-item");
        if (list != null && firstItem != null) {
            list.removeChild(firstItem);
        }
        updateStatusText();
    }

    public static void updateStatusText() {
        Element status = DOM.getElementById("todo-status");
        if (status == null) return;

        StringBuilder sb = new StringBuilder();
        int active = todoCount - completedCount;
        if (active < 0) active = 0;
        sb.append(active);
        sb.append(" items active (");
        sb.append(completedCount);
        sb.append(" completed, ");
        sb.append(todoCount);
        sb.append(" total)");

        status.setInnerText(sb.toString());
    }
}
