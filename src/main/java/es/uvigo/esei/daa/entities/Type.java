package es.uvigo.esei.daa.entities;

import static java.util.Objects.requireNonNull;

/**
 * An entity that represents a pet type.
 * 
 * @author Roo
 */
public class Type {
    private int id;
    private String name;    // name of the type (e.g., "dog", "cat", "rabbit")

    // Constructor needed for the JSON conversion
    Type() {}

    /**
     * Constructs a new instance of {@link Type}.
     *
     * @param id identifier of the type
     * @param name name of the type
     */
    public Type(int id, String name) {
        this.id = id;
        this.name = requireNonNull(name, "Name can't be null");
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = requireNonNull(name, "Name can't be null");
    }
}