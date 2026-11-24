import java.util.ArrayList;
import java.util.List;
import one.dastec.javaw.components.ComponentEvent;
import one.dastec.javaw.components.Css;
import one.dastec.javaw.components.CustomElement;
import one.dastec.javaw.components.Property;
import one.dastec.javaw.components.State;
import one.dastec.javaw.components.Template;
import one.dastec.javaw.components.WebComponent;

/** One item. A class rather than a record: records passed as Object are not supported yet. */
final class Todo {
    private final int id;
    private final String text;
    private final boolean done;
    Todo(int id, String text, boolean done) { this.id = id; this.text = text; this.done = done; }
    int id() { return id; }
    String text() { return text; }
    boolean done() { return done; }
}

/** The todo list of the design document's authoring model, within the implemented subset. */
@CustomElement("todo-list")
public class TodoList extends WebComponent {
    static final Css STYLES = css("""
        :host { display: block; font: 14px system-ui; }
        li.done { text-decoration: line-through; opacity: .6; }
        """);

    /** Fired when the number of open items changes. */
    public record RemainingChanged(int remaining) implements ComponentEvent {}

    /** The heading text. */
    @Property(reflect = true) private String heading = "Todos";
    @State private List<Todo> items = new ArrayList<Todo>();
    @State private String draft = "";
    private int nextId = 1;

    @Override protected Template render() {
        return html("""
            <h2>${heading}</h2>
            <input .value=${draft} @input=${e -> draft = e.target().getValue()}>
            <button ?disabled=${draft.isBlank()} @click=${this::add}>Add</button>
            <ul>${repeat(items, Todo::id, this::row)}</ul>
            <p>${remaining()} left</p>
            """);
    }

    private Template row(Todo t) {
        return html("""
            <li class=${t.done() ? "done" : ""} @click=${() -> toggle(t)}>${t.text()}</li>
            """);
    }

    private void add() {
        List<Todo> next = new ArrayList<Todo>(items);
        next.add(new Todo(nextId++, draft, false));
        items = next;
        draft = "";
        dispatch(new RemainingChanged(remaining()));
    }

    private void toggle(Todo t) {
        List<Todo> next = new ArrayList<Todo>();
        for (Todo x : items) next.add(x == t ? new Todo(x.id(), x.text(), !x.done()) : x);
        items = next;
        dispatch(new RemainingChanged(remaining()));
    }

    private int remaining() {
        int n = 0;
        for (Todo t : items) if (!t.done()) n++;
        return n;
    }
}
