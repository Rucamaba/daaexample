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
import es.uvigo.esei.daa.entities.Type;

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
     * Returns a list of pets owned by a specific person.
     *
     * @param ownerId identifier of the owner person
     * @return a list with all the pets owned by the specified person
     * @throws DAOException if an error happens while retrieving the pets
     */
    public List<Pet> listByOwner(int ownerId) throws DAOException {
        try (final Connection conn = this.getConnection()) {
            final String query = "SELECT * FROM pets WHERE owner_id = ?";
            
            try (final PreparedStatement statement = conn.prepareStatement(query)) {
                statement.setInt(1, ownerId);
                
                try (final ResultSet result = statement.executeQuery()) {
                    final List<Pet> pets = new LinkedList<>();
                    
                    while (result.next()) {
                        pets.add(rowToEntity(result));
                    }
                    
                    return pets;
                }
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Error listing pets by owner", e);
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
    public Pet add(String name, Person owner, Type type)
    throws DAOException, IllegalArgumentException {
        if (name == null || owner == null || type == null) {
            throw new IllegalArgumentException("name, owner, and type can't be null");
        }
        
        try (Connection conn = this.getConnection()) {
            final String query = "INSERT INTO pets (name, owner_id, type_id) VALUES (?, ?, ?)";
            
            try (PreparedStatement statement = conn.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, name);
                statement.setInt(2, owner.getId());
                statement.setInt(3, type.getId());
                
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
            final String query = "UPDATE pets SET name=?, owner_id=?, type_id=? WHERE id=?";
            
            try (PreparedStatement statement = conn.prepareStatement(query)) {
                statement.setString(1, pet.getName());
                statement.setInt(2, pet.getOwner().getId());
                statement.setInt(3, pet.getType().getId());
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
        try {
            Person owner = new PeopleDAO().get(result.getInt("owner_id"));
            Type type = new TypeDAO().get(result.getInt("type_id"));
            
            return new Pet(
                result.getInt("id"),
                result.getString("name"),
                owner,
                type
            );
        } catch (DAOException e) {
            LOG.log(Level.SEVERE, "Error getting pet relationships", e);
            throw new SQLException("Error getting pet relationships", e);
        }
    }
}
