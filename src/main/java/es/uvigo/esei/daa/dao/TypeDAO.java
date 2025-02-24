package es.uvigo.esei.daa.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import es.uvigo.esei.daa.entities.Type;

/**
 * DAO class for the {@link Type} entities.
 * 
 */
public class TypeDAO extends DAO {
    private final static Logger LOG = Logger.getLogger(TypeDAO.class.getName());

    /**
     * Returns a type stored persisted in the system.
     * 
     * @param id identifier of the type.
     * @return a type with the provided identifier.
     * @throws DAOException if an error happens while retrieving the type.
     * @throws IllegalArgumentException if the provided id does not correspond
     * with any persisted type.
     */
    public Type get(int id) throws DAOException, IllegalArgumentException {
        try (final Connection conn = this.getConnection()) {
            final String query = "SELECT * FROM types WHERE id=?";
            
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
            LOG.log(Level.SEVERE, "Error getting a type", e);
            throw new DAOException(e);
        }
    }

    /**
     * Returns a list with all the types persisted in the system.
     * 
     * @return a list with all the types persisted in the system.
     * @throws DAOException if an error happens while retrieving the types.
     */
    public List<Type> list() throws DAOException {
        try (final Connection conn = this.getConnection()) {
            final String query = "SELECT * FROM types";
            
            try (final PreparedStatement statement = conn.prepareStatement(query)) {
                try (final ResultSet result = statement.executeQuery()) {
                    final List<Type> types = new LinkedList<>();
                    
                    while (result.next()) {
                        types.add(rowToEntity(result));
                    }
                    
                    return types;
                }
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Error listing types", e);
            throw new DAOException(e);
        }
    }

    /**
     * Persists a new type in the system. An identifier will be assigned
     * automatically to the new type.
     * 
     * @param name name of the new type. Can't be {@code null}.
     * @return a {@link Type} entity representing the persisted type.
     * @throws DAOException if an error happens while persisting the new type.
     * @throws IllegalArgumentException if the name is {@code null}.
     */
    public Type add(String name) throws DAOException, IllegalArgumentException {
        if (name == null) {
            throw new IllegalArgumentException("name can't be null");
        }
        
        try (Connection conn = this.getConnection()) {
            final String query = "INSERT INTO types (name) VALUES (?)";
            
            try (PreparedStatement statement = conn.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, name);
                
                if (statement.executeUpdate() == 1) {
                    try (ResultSet resultKeys = statement.getGeneratedKeys()) {
                        if (resultKeys.next()) {
                            return new Type(resultKeys.getInt(1), name);
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
            LOG.log(Level.SEVERE, "Error adding a type", e);
            throw new DAOException(e);
        }
    }

    private Type rowToEntity(ResultSet result) throws SQLException {
        return new Type(
            result.getInt("id"),
            result.getString("name")
        );
    }
}