package es.uvigo.esei.daa.rest;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import es.uvigo.esei.daa.dao.PetDAO;
import es.uvigo.esei.daa.entities.Pet;
import es.uvigo.esei.daa.entities.Person;
import es.uvigo.esei.daa.dao.PeopleDAO;
import es.uvigo.esei.daa.dao.DAOException;

@Path("/people/{id}/pets")
@Produces(MediaType.APPLICATION_JSON)
public class PetResource {
    private final PetDAO dao = new PetDAO();
    private final PeopleDAO peopleDao = new PeopleDAO();
    private final static Logger LOG = Logger.getLogger(PetResource.class.getName());

    @GET
    public List<Pet> list(@PathParam("id") int ownerId) {
        try {
            return dao.listByOwner(ownerId);
        } catch (DAOException e) {
            LOG.log(Level.SEVERE, "Error listing pets for owner: " + ownerId, e);
            throw new WebApplicationException("Error listing pets", e);
        }
    }

    @GET
    @Path("/{id}")
    public Pet get(@PathParam("id") int id) {
        try {
            return dao.get(id);
        } catch (IllegalArgumentException e) {
            LOG.log(Level.FINE, "Invalid pet id in get method", e);
            throw new WebApplicationException("Invalid pet id", Response.Status.BAD_REQUEST);
        } catch (DAOException e) {
            LOG.log(Level.SEVERE, "Error getting a pet", e);
            throw new WebApplicationException("Error getting a pet", e);
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response add(Pet pet) {
        try {
            Person owner = peopleDao.get(pet.getOwner().getId());
            Pet newPet = dao.add(pet.getName(), owner, pet.getType());
            return Response.ok(newPet).build();
        } catch (IllegalArgumentException e) {
            LOG.log(Level.FINE, "Invalid data in add method", e);
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (DAOException e) {
            LOG.log(Level.SEVERE, "Error adding a pet", e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response modify(@PathParam("id") int id, Pet pet) {
        try {
            Pet existingPet = dao.get(id);
            existingPet.setName(pet.getName());
            existingPet.setType(pet.getType());
            existingPet.setOwner(peopleDao.get(pet.getOwner().getId()));
            dao.modify(existingPet);
            return Response.ok(existingPet).build();
        } catch (IllegalArgumentException e) {
            LOG.log(Level.FINE, "Invalid data in modify method", e);
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (DAOException e) {
            LOG.log(Level.SEVERE, "Error modifying a pet", e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") int id) {
        try {
            dao.delete(id);
            return Response.ok(id).build();
        } catch (IllegalArgumentException e) {
            LOG.log(Level.FINE, "Invalid pet id in delete method", e);
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (DAOException e) {
            LOG.log(Level.SEVERE, "Error deleting a pet", e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }
}