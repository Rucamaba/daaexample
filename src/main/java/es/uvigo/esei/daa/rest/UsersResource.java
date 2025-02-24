package es.uvigo.esei.daa.rest;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Base64;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import es.uvigo.esei.daa.dao.DAOException;
import es.uvigo.esei.daa.dao.UsersDAO;

@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
public class UsersResource {
    private final static Logger LOG = Logger.getLogger(UsersResource.class.getName());
    
    private final UsersDAO dao;
    
    private @Context SecurityContext security;
    
    public UsersResource() {
        this(new UsersDAO());
    }
    
    UsersResource(UsersDAO dao) {
        this(dao, null);
    }
    
    UsersResource(UsersDAO dao, SecurityContext security) {
        this.dao = dao;
        this.security = security;
    }
    
    @GET
    @Path("/{login}")
    public Response get(
        @PathParam("login") String login,
        @HeaderParam("Custom-Auth") String authHeader
    ) {
        try {
            if (authHeader != null && authHeader.startsWith("Basic ")) {
                String base64Credentials = authHeader.substring("Basic ".length());
                String credentials = new String(Base64.getDecoder().decode(base64Credentials));
                final String[] values = credentials.split(":", 2);
                
                if (values.length == 2) {
                    String username = values[0];
                    String password = values[1];
                    
                    // Validate credentials using the proper method
                    if (dao.checkLogin(username, password)) {
                        return Response.ok(dao.get(login)).build();
                    }
                }
            }
            
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity("Invalid credentials")
                .build();
                
        } catch (IllegalArgumentException iae) {
            LOG.log(Level.FINE, "Invalid user login in get method", iae);
            
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(iae.getMessage())
                .build();
        } catch (DAOException e) {
            LOG.log(Level.SEVERE, "Error getting an user", e);
            
            return Response.serverError()
                .entity(e.getMessage())
                .build();
        }
    }
    
    private String getLogin() {
        return this.security.getUserPrincipal().getName();
    }
    
    private boolean isAdmin() {
        return this.security.isUserInRole("ADMIN");
    }
}
