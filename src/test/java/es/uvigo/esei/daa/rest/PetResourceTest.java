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
import es.uvigo.esei.daa.entities.Pet;
import es.uvigo.esei.daa.entities.Person;
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
public class PetResourceTest extends JerseyTest {
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
    public void testListByOwner() throws IOException {
        final Response response = target("people/1/pets").request()
            .header("Authorization", "Basic " + userToken(adminLogin()))
            .get();
        
        assertThat(response, hasOkStatus());

        final List<Pet> pets = response.readEntity(new GenericType<List<Pet>>(){});
        
        // We should have at least one pet for owner 1 (Max)
        assertThat(pets.size() > 0, is(true));
        // The first pet should be Max
        assertThat(pets.get(0).getName(), is("Max"));
    }

    @Test
    public void testGet() throws IOException {
        final Response response = target("people/1/pets/1").request()
            .header("Authorization", "Basic " + userToken(adminLogin()))
            .get();
        
        assertThat(response, hasOkStatus());

        final Pet pet = response.readEntity(Pet.class);
        
        assertThat(pet.getId(), is(1));
        assertThat(pet.getName(), is("Max"));
        assertThat(pet.getType().getId(), is(1)); // dog
    }

    @Test
    public void testGetInvalidId() throws IOException {
        final Response response = target("people/1/pets/999").request()
            .header("Authorization", "Basic " + userToken(adminLogin()))
            .get();
        
        assertThat(response, hasBadRequestStatus());
    }

    @Test
    @ExpectedDatabase("/datasets/dataset.xml")
    public void testAdd() throws IOException {
        Pet newPet = new Pet(0, "Buddy", new Person(1, "Antón", "Álvarez"), new Type(1, "dog"));
        
        final Response response = target("people/1/pets").request(MediaType.APPLICATION_JSON_TYPE)
            .header("Authorization", "Basic " + userToken(adminLogin()))
            .post(javax.ws.rs.client.Entity.entity(newPet, MediaType.APPLICATION_JSON_TYPE));
        
        assertThat(response, hasOkStatus());

        final Pet pet = response.readEntity(Pet.class);
        
        assertThat(pet.getName(), is(newPet.getName()));
        assertThat(pet.getType().getId(), is(newPet.getType().getId()));
    }

    @Test
    @ExpectedDatabase("/datasets/dataset.xml")
    public void testModify() throws IOException {
        Pet modifiedPet = new Pet(1, "Max Jr", new Person(1, "Antón", "Álvarez"), new Type(2, "cat"));
        
        final Response response = target("people/1/pets/1").request(MediaType.APPLICATION_JSON_TYPE)
            .header("Authorization", "Basic " + userToken(adminLogin()))
            .put(javax.ws.rs.client.Entity.entity(modifiedPet, MediaType.APPLICATION_JSON_TYPE));
        
        assertThat(response, hasOkStatus());

        final Pet pet = response.readEntity(Pet.class);
        
        assertThat(pet.getName(), is(modifiedPet.getName()));
        assertThat(pet.getType().getId(), is(modifiedPet.getType().getId()));
    }

    @Test
    @ExpectedDatabase("/datasets/dataset.xml")
    public void testDelete() throws IOException {
        final Response response = target("people/1/pets/1").request()
            .header("Authorization", "Basic " + userToken(adminLogin()))
            .delete();
        
        assertThat(response, hasOkStatus());

        final Integer deletedId = response.readEntity(Integer.class);
        
        assertThat(deletedId, is(1));
    }
}