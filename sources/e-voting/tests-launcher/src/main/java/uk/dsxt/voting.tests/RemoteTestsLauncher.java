package uk.dsxt.voting.tests;

import com.jcraft.jsch.*;
import lombok.extern.log4j.Log4j2;
import uk.dsxt.voting.common.utils.PropertiesHelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
    
    public static void main(String[] args) {
        try {
            log.info("Starting module {}...", MODULE_NAME);
            Properties properties = PropertiesHelper.loadProperties(MODULE_NAME);
            RemoteTestsLauncher instance = new RemoteTestsLauncher(properties);
            instance.run();
        } catch (Exception e) {
            log.error("Module {} failed: ", MODULE_NAME, e.getMessage());
        }
    }
    
    RemoteTestsLauncher(Properties properties) throws Exception {
        user = properties.getProperty("vm.user");
        masterHost = properties.getProperty("vm.mainNode");
        sshProvider.addIdentity(properties.getProperty("vm.crtPath"));
        
    }
    
    private void installNode(String pathToInstallScript, String ownerId, String privateKey, String mainNxtAddress, 
                             String accountPassphrase, boolean master, int peerPort, int apiPort, int appPort, 
                             String nxtMasterHost, String ownerHost) throws Exception {
        Session session = getSession(masterHost);
        String pathToMasterConfig = WORK_DIR + "build/master/client.properties";
        log.debug(makeCmd(session, String.format("cd %s; ./update.sh", WORK_DIR)));
        //TODO read script
        log.debug(makeCmd(session, pathToInstallScript));
        String s = makeCmd(session, String.format("cat %s", pathToMasterConfig));
        Map<String, String> overrides = new LinkedHashMap<>();
        overrides.put("client.isMain", Boolean.toString(master));
        overrides.put("voting.files", "voting.xml");
        overrides.put("scheduled_messages.file_path", "messages.txt");
        overrides.put("participants_xml.file_path", "mi_participants.xml");
        overrides.put("credentials.filepath", "credentials.json");
        overrides.put("clients.filepath", "clients.json");
        overrides.put("client.web.port", Integer.toString(appPort));
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
        for (String keyToValueStr : s.split(String.format("%n"))) {
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
        makeCmd(session, String.format("/bin/echo -e \"%s\" > %s", result.toString(), pathToMasterConfig));
        //TODO update frontend config
    }
    
    private void run() throws Exception {
    }
    
    public String makeCmd(Session s, String cmd) throws Exception {
        ChannelExec exec = (ChannelExec)s.openChannel("exec");
        exec.setCommand(cmd);
        exec.connect();
        int exitStatus = exec.getExitStatus();
        InputStream resultStream = exitStatus == 0 ? exec.getInputStream() : exec.getErrStream();
        String result = new String(readData(resultStream), StandardCharsets.UTF_8);
        exec.disconnect();
        return result;
    }

    private byte[] readData(InputStream stream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        while (stream.available() > 0) {
            byte[] tmp = new byte[BUFFER_SIZE];
            int i = stream.read(tmp, 0, BUFFER_SIZE);
            if (i < 0) {
                break;
            }
            buffer.write(tmp, 0, i);
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
