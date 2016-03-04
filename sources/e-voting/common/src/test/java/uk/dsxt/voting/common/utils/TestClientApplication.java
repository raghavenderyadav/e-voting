package uk.dsxt.voting.common.utils;


import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.ApplicationPath;

@ApplicationPath("")
public class TestClientApplication extends ResourceConfig {
    public TestClientApplication() throws Exception {
        this.registerInstances(new TestClientResource());
    }
}
