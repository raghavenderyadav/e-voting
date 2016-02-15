package uk.dsxt.voting.common.networking;

import org.junit.Ignore;
import org.junit.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

public class BaseWalletManagerTest {

    @Test
    @Ignore
    public void testNxtApi() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("nxt.jar.path", "../libs/nxt.jar");
        properties.setProperty("nxt.properties.path", "conf/nxt-default.properties");
        properties.setProperty("nxt.peerServerPort", "7871");
        properties.setProperty("nxt.apiServerPort", "7782");
        properties.setProperty("nxt.dbDir", "./nxt-db-1");
        properties.setProperty("nxt.testDbDir", "./nxt-db-1");
        properties.setProperty("nxt.defaultPeers", "93.187.190.144");
        properties.setProperty("nxt.defaultTestnetPeers", "93.187.190.144");
        properties.setProperty("nxt.main.address", "NXT-9PHW-CVXU-2TDY-H4878");
        properties.setProperty("nxt.account.passphrase", "master_password");
        properties.setProperty("nxt.isOffline", "true");
        properties.setProperty("nxt.isTestnet", "true");
        properties.setProperty("nxt.timeMultiplier", "1000");
        BaseWalletManager wm1 = new BaseWalletManager(properties);
        wm1.runWallet();
        Thread.sleep(1000);
        assertEquals(wm1.getBalance().compareTo(new BigDecimal(1000000L)), 0);
        Thread.sleep(10000);
        wm1.stopWallet();
        Thread.sleep(1000);
        properties.setProperty("nxt.isOffline", "false");
        properties.setProperty("nxt.isTestnet", "true");
        properties.setProperty("nxt.timeMultiplier", "10");
        wm1 = new BaseWalletManager(properties);
        wm1.runWallet();
        Thread.sleep(3000);
        properties.setProperty("nxt.peerServerPort", "9001");
        properties.setProperty("nxt.apiServerPort", "8882");
        properties.setProperty("nxt.dbDir", "./nxt-db-2");
        properties.setProperty("nxt.account.passphrase", "client_password");
        WalletManager wm2 = new BaseWalletManager(properties);
        wm2.runWallet();
        String selfAddress = wm2.getSelfAddress();
        assertNotNull(selfAddress);
        System.out.println(selfAddress);
        String messageId = wm2.sendMessage("test".getBytes(StandardCharsets.UTF_8));
        assertNotNull(messageId);
        System.out.println(messageId);
        while (true)
            Thread.sleep(1000);
    }
}
