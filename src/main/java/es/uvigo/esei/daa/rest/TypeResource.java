package es.uvigo.esei.daa.rest;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import es.uvigo.esei.daa.dao.TypeDAO;
import es.uvigo.esei.daa.entities.Type;
import es.uvigo.esei.daa.dao.DAOException;

@Path("/types")
@Produces(MediaType.APPLICATION_JSON)
public class TypeResource {
    private final TypeDAO dao = new TypeDAO();
    private final static Logger LOG = Logger.getLogger(TypeResource.class.getName());

    @GET
    @RolesAllowed({"ADMIN", "USER"})
    public List<Type> list() {
        try {
            return dao.list();
        } catch (DAOException e) {
            LOG.log(Level.SEVERE, "Error listing types", e);
            throw new WebApplicationException("Error listing types", e);
        }
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "USER"})
    public Type get(@PathParam("id") int id) {
        try {
            return dao.get(id);
        } catch (IllegalArgumentException e) {
            LOG.log(Level.FINE, "Invalid type id in get method", e);
            throw new WebApplicationException("Invalid type id", Response.Status.BAD_REQUEST);
        } catch (DAOException e) {
            LOG.log(Level.SEVERE, "Error getting a type", e);
            throw new WebApplicationException("Error getting a type", e);
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("ADMIN")
    public Response add(Type type) {
        try {
            Type newType = dao.add(type.getName());
            return Response.ok(newType).build();
        } catch (IllegalArgumentException e) {
            LOG.log(Level.FINE, "Invalid data in add method", e);
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (DAOException e) {
            LOG.log(Level.SEVERE, "Error adding a type", e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }
}