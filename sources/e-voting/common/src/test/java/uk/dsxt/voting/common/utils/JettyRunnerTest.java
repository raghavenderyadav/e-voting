package uk.dsxt.voting.common.utils;

import org.eclipse.jetty.server.Server;
import org.junit.Ignore;
import org.junit.Test;
import uk.dsxt.voting.common.utils.web.JettyRunner;

import java.util.Properties;

public class JettyRunnerTest {

    @Test
    @Ignore
    public void webappTest() throws Exception {
        Properties testProperties = new Properties();
        testProperties.setProperty("web.port", "8888");
        testProperties.setProperty("jetty.maxThreads", "100");
        testProperties.setProperty("jetty.minThreads", "10");
        testProperties.setProperty("jetty.idleTimeout", "5000");
        testProperties.setProperty("jetty.maxQueueSize", "1000");
        Server run = JettyRunner.run(new TestClientApplication(), testProperties, 8888,
                "../gui-public/app", "/{1}(api|holderAPI){1}/{1}.*");
        run.join();
    }

}
