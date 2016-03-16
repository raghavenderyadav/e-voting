package uk.dsxt.voting.tests;

import com.jcraft.jsch.*;
import lombok.extern.log4j.Log4j2;
import uk.dsxt.voting.common.utils.PropertiesHelper;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Log4j2
public class RemoteTestsLauncher implements BaseTestsLauncher {

    private static final int BUFFER_SIZE = 4096;
    private static final String WORK_DIR = "/home/ubuntu/e-voting/";
    private JSch sshProvider = new JSch();
    private final int sshPort = 22;
    private final String user;
    private final String masterHost;
    private final int MASTER_PEER_PORT = 7873;
    private final String MASTER_PEER_ADDRESS;
    private final int MASTER_APP_PORT = 9000;
    private final String MAIN_ADDRESS;
    
    public static void main(String[] args) {
        try {
            log.info("Starting module {}...", MODULE_NAME);
            Properties properties = PropertiesHelper.loadProperties(MODULE_NAME);
            RemoteTestsLauncher instance = new RemoteTestsLauncher(properties);
            instance.run(properties);
        } catch (Exception e) {
            log.error("Module {} failed: ", MODULE_NAME, e.getMessage());
        }
    }
    
    RemoteTestsLauncher(Properties properties) throws Exception {
        user = properties.getProperty("vm.user");
        masterHost = properties.getProperty("vm.mainNode");
        sshProvider.addIdentity(properties.getProperty("vm.crtPath"));
        MASTER_PEER_ADDRESS = String.format("http://%s:%d", masterHost, MASTER_PEER_PORT); 
        MAIN_ADDRESS = properties.getProperty("master.address");
    }
    
    private void installNode(String pathToInstallScript, String ownerId, String privateKey, String mainNxtAddress, 
                             String accountPassphrase, boolean master, int peerPort, int apiPort, int appPort, 
                             String nxtMasterHost, String ownerHost, String webHost, String directory) throws Exception {
        Session session = getSession(masterHost);
        String pathToMasterConfig = WORK_DIR + "build/" + directory + "/client.properties";
        log.debug(makeCmd(session, String.format("cd %s; ./update.sh", WORK_DIR)));
        String resourceString = PropertiesHelper.getResourceString(pathToInstallScript);
        log.debug(makeCmd(session, resourceString.replace("$1", directory)));
        String backendConfig = makeCmd(session, String.format("cat %s", pathToMasterConfig));
        log.debug(String.format("Initial backend config: %s%n", backendConfig));
        Map<String, String> overrides = new LinkedHashMap<>();
        overrides.put("client.isMain", Boolean.toString(master));
        overrides.put("voting.files", "voting.xml");
        overrides.put("scheduled_messages.file_path", "messages.txt");
        overrides.put("participants_xml.file_path", "mi_participants.xml");
        overrides.put("credentials.filepath", "credentials.json");
        overrides.put("clients.filepath", "clients.json");
        overrides.put("client.webHost.port", Integer.toString(appPort));
        overrides.put("owner.id", ownerId);
        overrides.put("owner.private_key", privateKey);
        overrides.put("register.server.url", ownerHost);
        overrides.put("mock.wallet", "false");
        overrides.put("mock.registries", "true");
        overrides.put("nxt.jar.path", "../libs/nxt.jar");
        overrides.put("nxt.properties.path", "./conf/nxt-default.properties");
        overrides.put("nxt.peerServerPort", Integer.toString(peerPort));
        overrides.put("nxt.apiServerPort", Integer.toString(apiPort));
        overrides.put("nxt.dbDir", String.format("./%s", DB_FOLDER));
        overrides.put("nxt.testDbDir", String.format("./%s", DB_FOLDER));
        overrides.put("nxt.defaultPeers", nxtMasterHost);
        overrides.put("nxt.defaultTestnetPeers", nxtMasterHost);
        overrides.put("nxt.isOffline", "false");
        overrides.put("nxt.isTestnet", "true");
        overrides.put("nxt.main.address", mainNxtAddress);
        overrides.put("nxt.account.passphrase", accountPassphrase);
        Map<String, String> original = new LinkedHashMap<>();
        for (String keyToValueStr : backendConfig.split(String.format("%n"))) {
            String[] keyToValue = keyToValueStr.split("=");
            if (keyToValue.length == 2)
                original.put(keyToValue[0], keyToValue[1]);
        }
        for (Map.Entry<String, String> keyToValue : overrides.entrySet()) {
            original.put(keyToValue.getKey(), keyToValue.getValue());
        }
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, String> keyToValue : original.entrySet()) {
            result.append(keyToValue.getKey());
            result.append("=");
            result.append(keyToValue.getValue());
            result.append(String.format("%n"));
        }
        log.debug(String.format("Result backend config: %s%n", backendConfig));
        makeCmd(session, String.format("/bin/echo -e \"%s\" > %s", result.toString(), pathToMasterConfig));
        String pathToMasterFrontendConfig = WORK_DIR + "build/" + directory + "/gui-public/app/server-properties.js";
        String frontendConfig = makeCmd(session, String.format("cat %s", pathToMasterFrontendConfig));
        log.debug(String.format("Original frontend config: %s%n", frontendConfig));
        frontendConfig = frontendConfig.replaceAll("\"serverUrl\": \"*\",", String.format("\"serverUrl\": \"%s\"", webHost));
        frontendConfig = frontendConfig.replaceAll("\"serverPort\": *,", String.format("\"serverPort\": %s", appPort));
        frontendConfig = frontendConfig.replaceAll("\"pathToApi\": \"*\",", "\"pathToApi\": \"api\"");
        frontendConfig = frontendConfig.replaceAll("\"readPortFromUrl\": \"*\",", "\"readPortFromUrl\": true");
        log.debug(String.format("Result frontend config: %s%n", frontendConfig));
        makeCmd(session, String.format("/bin/echo -e \"%s\" > %s", frontendConfig, pathToMasterFrontendConfig));
    }
    
    private void run(Properties properties) throws Exception {
        if (Boolean.parseBoolean(properties.getProperty("vm.needInstall"))) {
            installNode(
                "ssh/createNode.sh",
                "00",
                "MIIBVAIBADANBgkqhkiG9w0BAQEFAASCAT4wggE6AgEAAkEAlNntpENmQzCyPx+M3D1RZypdxkfFF2+60CSDtqCSvsi/MLsPEu87CDYxuBmTtLY5zBP2HcNIvT9cB699nRNFAQIDAQABAkBAk4sViGgFHks2N2nU4oU+TJMCQoCu+joBstWxlVgUjDYGk/QHEMhx60kZ3L2Pw8k8uFZVCDXy0/uemuIp8vABAiEA7xlJWC8bCDYqVggQgK9yzAuL7P1T0+dUF080P8kR7nECIQCfX4epGtFSWJFOK+CGly/mLyhZrn6g0cu7jKCw5BgnkQIhAJFihNCURBGoLfIGEVLOXDVqR/kgyNou7VkHFjQ65SZhAiAf79fSpmId+0ua+6XxsqhRm0+dsR8FASWvfr3Q1NSWUQIgGfqAUV4I0nG8sIz3UE7rf+tzQaScDYOoCNu4amJjxEI=",
                MAIN_ADDRESS,
                properties.getProperty("master.passphrase"),
                true,
                MASTER_PEER_PORT,
                7872,
                MASTER_APP_PORT,
                MASTER_PEER_ADDRESS,
                String.format("http://%s:%d/voting-api", masterHost, MASTER_APP_PORT),
                String.format("http://%s:%d", masterHost, MASTER_APP_PORT),
                "master");
        }
        //TODO install other nodes
    }
    
    public String makeCmd(Session s, String cmd) throws Exception {
        ChannelExec exec = (ChannelExec)s.openChannel("exec");
        exec.setCommand(cmd);
        exec.connect();
        byte[] output = readData(exec, exec.getInputStream());
        byte[] error = readData(exec, exec.getErrStream());
        int exitStatus = exec.getExitStatus();
        byte[] resultStream = exitStatus == 0 ? output : error;
        String result = new String(resultStream, StandardCharsets.UTF_8);
        exec.disconnect();
        return result;
    }
    
    private byte[] readData(ChannelExec exec, InputStream stream) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        while (!exec.isEOF()) {
            while (stream.available() > 0) {
                byte[] tmp = new byte[BUFFER_SIZE];
                int i = stream.read(tmp, 0, BUFFER_SIZE);
                if (i < 0) {
                    break;
                }
                buffer.write(tmp, 0, i);
            }
            Thread.sleep(100);
        }
        return buffer.toByteArray();
    }
    
    public Session getSession(String host) throws Exception {
        Session session = sshProvider.getSession(user, host, sshPort);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();
        return session;
    }
}
