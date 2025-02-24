package es.uvigo.esei.daa.entities;

import static java.util.Objects.requireNonNull;

/**
 * An entity that represents a pet.
 * 
 * @author Rubén Cambre Abalo
 */

public class Pet {
    private int id;
    private String name;
    private Person owner;
    private Type type;    //type of pet: a dog, a cat, a rabbit...

    // Constructor needed for the JSON conversion
    Pet() {}

    /**
     * Constructs a new instance of {@link Pet}.
     *
     * @param id identifier of the pet.
     * @param name name of the pet.
     * @param owner object person that represents the owner of the pet.
     * @param type object type that represents the type of pet.
     */
    public Pet(int id, String name, Person owner, Type type) {
        this.id = id;
        this.name = name;
        this.owner = owner;
        this.type = type;
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

    public Person getOwner() {
        return owner;
    }

    public void setOwner(Person owner) {
        this.owner = requireNonNull(owner, "All pets must have an owner");
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = requireNonNull(type, "Type can't be null");
    }
}
