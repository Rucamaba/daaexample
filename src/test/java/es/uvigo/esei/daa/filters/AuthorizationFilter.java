package es.uvigo.esei.daa.filters;

import java.io.IOException;
import java.security.Principal;
import java.util.Base64;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;

import es.uvigo.esei.daa.dao.DAOException;
import es.uvigo.esei.daa.dao.UsersDAO;
import es.uvigo.esei.daa.entities.User;

/**
 * This performs the Basic HTTP authentication following (almost) the same
 * rules as the defined in the web.xml file.
 * 
 * @author Miguel Reboiro Jato
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthorizationFilter implements ContainerRequestFilter {
	private final UsersDAO dao;
	
	public AuthorizationFilter() {
	this.dao = new UsersDAO();
	}
	
	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
	// Get the authentication passed in HTTP headers parameters
	final String auth = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);

		if (auth == null || !auth.startsWith("Basic ")) {
		requestContext.abortWith(createResponse());
		} else {
		final byte[] decodedToken = Base64.getDecoder()
		.decode(auth.substring("Basic ".length()));
		
		final String userColonPass = new String(decodedToken);
			final String[] userPass = userColonPass.split(":", 2);
			
			if (userPass.length == 2) {
				try {
					if (this.dao.checkLogin(userPass[0], userPass[1])) {
						final User user = this.dao.get(userPass[0]);
						
						// Allow GET requests for all authenticated users
						if (requestContext.getMethod().equals("GET")) {
						    requestContext.setSecurityContext(new UserSecurityContext(user));
						} else if (!user.getRole().equals("ADMIN")) {
						    // Only admins can modify (POST/PUT/DELETE)
						    requestContext.abortWith(createResponse());
						} else {
						    requestContext.setSecurityContext(new UserSecurityContext(user));
						}
					} else {
						requestContext.abortWith(createResponse());
					}
				} catch (DAOException e) {
					requestContext.abortWith(createResponse());
				}
			} else {
				requestContext.abortWith(createResponse());
			}
		}
	}
	
	private static Response createResponse() {
	    return Response.status(Status.UNAUTHORIZED)
	        .header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"DAA Example API\"")
	        .entity("Page requires login.")
	        .build();
	}
	
	private static final class UserSecurityContext implements SecurityContext {
		private final User user;

		private UserSecurityContext(User user) {
			this.user = user;
		}

		@Override
		public boolean isUserInRole(String role) {
			return user.getRole().equals(role);
		}

		@Override
		public boolean isSecure() {
			return false;
		}

		@Override
		public Principal getUserPrincipal() {
			return new Principal() {
				@Override
				public String getName() {
					return user.getLogin();
				}
			};
		}

		@Override
		public String getAuthenticationScheme() {
			return SecurityContext.BASIC_AUTH;
		}
	}
}
