package uk.dsxt.voting.common.utils;

import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("/api")
public class TestClientResource extends ResourceConfig {
    @GET
    @Path("/test")
    @Produces("application/json")
    public String test() {
        return "{ \"msg\": \"api request was success\"}";
    }
}
