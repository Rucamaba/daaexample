package es.uvigo.esei.daa.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import es.uvigo.esei.daa.entities.Pet;
import es.uvigo.esei.daa.entities.Person;

/**
 * DAO class for the {@link Pet} entities.
 * 
 */
public class PetDAO extends DAO {
    private final static Logger LOG = Logger.getLogger(PetDAO.class.getName());

    /**
     * Returns a pet stored persisted in the system.
     * 
     * @param id identifier of the pet.
     * @return a pet with the provided identifier.
     * @throws DAOException if an error happens while retrieving the pet.
     * @throws IllegalArgumentException if the provided id does not correspond
     * with any persisted pet.
     */
    public Pet get(int id) throws DAOException, IllegalArgumentException {
        try (final Connection conn = this.getConnection()) {
            final String query = "SELECT * FROM pets WHERE id=?";
            
            try (final PreparedStatement statement = conn.prepareStatement(query)) {
                statement.setInt(1, id);
                
                try (final ResultSet result = statement.executeQuery()) {
                    if (result.next()) {
                        return rowToEntity(result);
                    } else {
                        throw new IllegalArgumentException("Invalid id");
                    }
                }
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Error getting a pet", e);
            throw new DAOException(e);
        }
    }

    /**
     * Returns a list with all the pets persisted in the system.
     * 
     * @return a list with all the pets persisted in the system.
     * @throws DAOException if an error happens while retrieving the pets.
     */
    public List<Pet> list() throws DAOException {
        try (final Connection conn = this.getConnection()) {
            final String query = "SELECT * FROM pets";
            
            try (final PreparedStatement statement = conn.prepareStatement(query)) {
                try (final ResultSet result = statement.executeQuery()) {
                    final List<Pet> pets = new LinkedList<>();
                    
                    while (result.next()) {
                        pets.add(rowToEntity(result));
                    }
                    
                    return pets;
                }
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Error listing pets", e);
            throw new DAOException(e);
        }
    }

    /**
     * Persists a new pet in the system. An identifier will be assigned
     * automatically to the new pet.
     * 
     * @param name name of the new pet. Can't be {@code null}.
     * @param owner owner of the new pet. Can't be {@code null}.
     * @param type type of the new pet. Can't be {@code null}.
     * @return a {@link Pet} entity representing the persisted pet.
     * @throws DAOException if an error happens while persisting the new pet.
     * @throws IllegalArgumentException if the name, owner, or type are {@code null}.
     */
    public Pet add(String name, Person owner, String type)
    throws DAOException, IllegalArgumentException {
        if (name == null || owner == null || type == null) {
            throw new IllegalArgumentException("name, owner, and type can't be null");
        }
        
        try (Connection conn = this.getConnection()) {
            final String query = "INSERT INTO pets (name, owner_id, type) VALUES (?, ?, ?)";
            
            try (PreparedStatement statement = conn.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, name);
                statement.setInt(2, owner.getId());
                statement.setString(3, type);
                
                if (statement.executeUpdate() == 1) {
                    try (ResultSet resultKeys = statement.getGeneratedKeys()) {
                        if (resultKeys.next()) {
                            return new Pet(resultKeys.getInt(1), name, owner, type);
                        } else {
                            LOG.log(Level.SEVERE, "Error retrieving inserted id");
                            throw new SQLException("Error retrieving inserted id");
                        }
                    }
                } else {
                    LOG.log(Level.SEVERE, "Error inserting value");
                    throw new SQLException("Error inserting value");
                }
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Error adding a pet", e);
            throw new DAOException(e);
        }
    }

    /**
     * Modifies a pet previously persisted in the system. The pet will be
     * retrieved by the provided id and its current name, owner, and type will be
     * replaced with the provided.
     * 
     * @param pet a {@link Pet} entity with the new data.
     * @throws DAOException if an error happens while modifying the pet.
     * @throws IllegalArgumentException if the pet is {@code null}.
     */
    public void modify(Pet pet)
    throws DAOException, IllegalArgumentException {
        if (pet == null) {
            throw new IllegalArgumentException("pet can't be null");
        }
        
        try (Connection conn = this.getConnection()) {
            final String query = "UPDATE pets SET name=?, owner_id=?, type=? WHERE id=?";
            
            try (PreparedStatement statement = conn.prepareStatement(query)) {
                statement.setString(1, pet.getName());
                statement.setInt(2, pet.getOwner().getId());
                statement.setString(3, pet.getType());
                statement.setInt(4, pet.getId());
                
                if (statement.executeUpdate() != 1) {
                    throw new IllegalArgumentException("Invalid id");
                }
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Error modifying a pet", e);
            throw new DAOException(e);
        }
    }

    /**
     * Removes a persisted pet from the system.
     * 
     * @param id identifier of the pet to be deleted.
     * @throws DAOException if an error happens while deleting the pet.
     * @throws IllegalArgumentException if the provided id does not correspond
     * with any persisted pet.
     */
    public void delete(int id)
    throws DAOException, IllegalArgumentException {
        try (final Connection conn = this.getConnection()) {
            final String query = "DELETE FROM pets WHERE id=?";
            
            try (final PreparedStatement statement = conn.prepareStatement(query)) {
                statement.setInt(1, id);
                
                if (statement.executeUpdate() != 1) {
                    throw new IllegalArgumentException("Invalid id");
                }
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Error deleting a pet", e);
            throw new DAOException(e);
        }
    }

    private Pet rowToEntity(ResultSet result) throws SQLException {
        // Assuming you have a method to get a Person by ID
        Person owner;
        try {
            owner = new PeopleDAO().get(result.getInt("owner_id"));
        } catch (DAOException e) {
            LOG.log(Level.SEVERE, "Error getting owner", e);
            throw new SQLException("Error getting owner", e);
        }
        return new Pet(
            result.getInt("id"),
            result.getString("name"),
            owner,
            result.getString("type")
        );
    }
}
