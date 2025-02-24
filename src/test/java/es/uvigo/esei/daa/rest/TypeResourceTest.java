package es.uvigo.esei.daa.rest;

import static es.uvigo.esei.daa.matchers.HasHttpStatus.hasOkStatus;
import static es.uvigo.esei.daa.matchers.HasHttpStatus.hasBadRequestStatus;
import static es.uvigo.esei.daa.dataset.UsersDataset.adminLogin;
import static es.uvigo.esei.daa.dataset.UsersDataset.userToken;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.util.List;

import javax.sql.DataSource;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;

import es.uvigo.esei.daa.DAAExampleTestApplication;
import es.uvigo.esei.daa.entities.Type;
import es.uvigo.esei.daa.listeners.ApplicationContextBinding;
import es.uvigo.esei.daa.listeners.ApplicationContextJndiBindingTestExecutionListener;
import es.uvigo.esei.daa.listeners.DbManagement;
import es.uvigo.esei.daa.listeners.DbManagementTestExecutionListener;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:contexts/mem-context.xml")
@TestExecutionListeners({
    DbUnitTestExecutionListener.class,
    DbManagementTestExecutionListener.class,
    ApplicationContextJndiBindingTestExecutionListener.class
})
@ApplicationContextBinding(
    jndiUrl = "java:/comp/env/jdbc/daaexample",
    type = DataSource.class
)
@DbManagement(
    create = "classpath:db/hsqldb.sql",
    drop = "classpath:db/hsqldb-drop.sql"
)
@DatabaseSetup("/datasets/dataset.xml")
@ExpectedDatabase("/datasets/dataset.xml")
public class TypeResourceTest extends JerseyTest {
    @Override
    protected Application configure() {
        return new DAAExampleTestApplication();
    }

    @Override
    protected void configureClient(ClientConfig config) {
        super.configureClient(config);
        
        // Enables JSON transformation in client
        config.register(JacksonJsonProvider.class);
        config.property("com.sun.jersey.api.json.POJOMappingFeature", Boolean.TRUE);
    }

    @Test
    public void testList() throws IOException {
        final Response response = target("types").request()
            .header("Authorization", "Basic " + userToken(adminLogin()))
            .get();
        
        assertThat(response, hasOkStatus());

        final List<Type> types = response.readEntity(new GenericType<List<Type>>(){});
        
        // Assert default types are present (dog, cat, etc.)
        assertThat(types.size() > 0, is(true));
    }

    @Test
    public void testGet() throws IOException {
        final Response response = target("types/1").request()
            .header("Authorization", "Basic " + userToken(adminLogin()))
            .get();
        
        assertThat(response, hasOkStatus());

        final Type type = response.readEntity(Type.class);
        
        assertThat(type.getId(), is(1));
    }

    @Test
    public void testGetInvalidId() throws IOException {
        final Response response = target("types/999").request()
            .header("Authorization", "Basic " + userToken(adminLogin()))
            .get();
        
        assertThat(response, hasBadRequestStatus());
    }

    @Test
    @ExpectedDatabase("/datasets/dataset.xml")
    public void testAdd() throws IOException {
        final Type newType = new Type(0, "hamster");
        
        final Response response = target("types").request(MediaType.APPLICATION_JSON_TYPE)
            .header("Authorization", "Basic " + userToken(adminLogin()))
            .post(javax.ws.rs.client.Entity.entity(newType, MediaType.APPLICATION_JSON_TYPE));
        
        assertThat(response, hasOkStatus());

        final Type type = response.readEntity(Type.class);
        
        assertThat(type.getName(), is(newType.getName()));
    }
}