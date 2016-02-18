package uk.dsxt.voting.common.networking;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang.StringUtils;
import uk.dsxt.voting.common.datamodel.RequestType;
import uk.dsxt.voting.common.datamodel.walletapi.*;
import uk.dsxt.voting.common.utils.HttpHelper;
import uk.dsxt.voting.common.utils.PropertiesHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Log4j2
public class BaseWalletManager implements WalletManager {

    private final File workingDir;
    private final String jarPath;
    private final String nxtPropertiesPath;
    private final ObjectMapper mapper;
    private final String mainAddress;
    private final String passwordForRegister;
    private final String port;
    private final HttpHelper httpHelper;

    private String passphrase;
    private String accountId;
    private String selfAccount;

    private Process nxtProcess;
    private boolean isForgeNow = false;

    public BaseWalletManager(Properties properties, String[] args) {
        workingDir = new File(System.getProperty("user.dir"));
        log.info("Working directory (user.dir): {}", workingDir.getAbsolutePath());

        jarPath = properties.getProperty("nxt.jar.path");

        passwordForRegister = properties.getProperty("nxt.register.password");
        httpHelper = new HttpHelper(5000, 5000);

        mapper = new ObjectMapper();
        mapper.setNodeFactory(JsonNodeFactory.withExactBigDecimals(true));
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        nxtPropertiesPath = args != null && args.length > 0 ? args[0] : properties.getProperty("nxt.properties.path");
        Properties nxtProperties = PropertiesHelper.loadPropertiesFromPath(nxtPropertiesPath);
        if (args != null && args.length > 0) {
            mainAddress = args[1];
            passphrase = args[2];
            port = nxtProperties.getProperty("nxt.apiServerPort");
        } else {
            mainAddress = properties.getProperty("nxt.main.address");
            passphrase = properties.getProperty("nxt.account.passphrase");
            port = properties.getProperty("nxt.apiServerPort");
            nxtProperties.setProperty("nxt.peerServerPort", properties.getProperty("nxt.peerServerPort"));
            nxtProperties.setProperty("nxt.apiServerPort", port);
            nxtProperties.setProperty("nxt.dbDir", properties.getProperty("nxt.dbDir"));
            nxtProperties.setProperty("nxt.testDbDir", properties.getProperty("nxt.dbDir"));
            nxtProperties.setProperty("nxt.defaultPeers", properties.getProperty("nxt.defaultPeers"));
            nxtProperties.setProperty("nxt.defaultTestnetPeers", properties.getProperty("nxt.defaultTestnetPeers"));
            nxtProperties.setProperty("nxt.isOffline", properties.getProperty("nxt.isOffline"));
            nxtProperties.setProperty("nxt.isTestnet", properties.getProperty("nxt.isTestnet"));
            nxtProperties.setProperty("nxt.timeMultiplier", properties.getProperty("nxt.timeMultiplier"));
            try (FileOutputStream fos = new FileOutputStream(nxtPropertiesPath)) {
                nxtProperties.store(fos, "");
            } catch (Exception e) {
                String errorMessage = String.format("Can't save wallet. Error: %s", e.getMessage());
                log.error(errorMessage, e);
                throw new RuntimeException(errorMessage);
            }
        }
    }

    private <T> T sendApiRequest(WalletRequestType type, String secretPhrase, Consumer<Map<String, String>> argumentsBuilder, Class<T> tClass) {
        if (type != WalletRequestType.GET_ACCOUNT_ID && type != WalletRequestType.GET_BLOCK)
            waitInitialize();
        return sendApiRequest(type, keyToValue -> {
            keyToValue.put("secretPhrase", secretPhrase);
            argumentsBuilder.accept(keyToValue);
        }, tClass);
    }

    private <T> T sendApiRequest(WalletRequestType type, Consumer<Map<String, String>> argumentsBuilder, Class<T> tClass) {
        try {
            if (type != WalletRequestType.GET_ACCOUNT_ID && type != WalletRequestType.GET_BLOCK)
                waitInitialize();
            Map<String, String> arguments = new LinkedHashMap<>();
            arguments.put("requestType", type.toString());
            argumentsBuilder.accept(arguments);
            StringBuilder url = new StringBuilder();
            url.append("http://localhost:");
            url.append(port);
            url.append("/nxt?");
            for (Map.Entry<String, String> keyToValue : arguments.entrySet()) {
                url.append(keyToValue.getKey());
                url.append("=");
                url.append(URLEncoder.encode(keyToValue.getValue(), StandardCharsets.UTF_8.toString()));
                url.append("&");
            }
            if (url.lastIndexOf("&") == url.length() - 1)
                url.replace(url.length() - 1, url.length(), "");
            String response = httpHelper.request(url.toString(), RequestType.POST);
            T result = null;
            try {
                result = mapper.readValue(response, tClass);
            } catch (IOException e) {
                log.error("Can't parse response: {}. Error message: {}", response, e.getMessage());
            }
            return result;
        } catch (Exception e) {
            log.error("Method {} failed. Error message {}", type, e.getMessage());
            return null;
        }
    }

    @Override
    public void runWallet() {
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add("java");
            cmd.add("-jar");
            cmd.add(jarPath);
            cmd.add(nxtPropertiesPath);

            log.debug("Starting nxt wallet process: {}", StringUtils.join(cmd, " "));

            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.directory(workingDir);
            processBuilder.command(cmd);
            nxtProcess = processBuilder.start();
            inheritIO(nxtProcess.getInputStream());
            inheritIO(nxtProcess.getErrorStream());
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    stopWallet();
                }
            });
            waitInitialize();
        } catch (Exception e) {
            String errorMessage = String.format("Couldn't run wallet. Error: %s", e.getMessage());
            log.error(errorMessage, e);
            throw new RuntimeException(errorMessage);
        }
    }

    @Override
    public void stopWallet() {
        try {
            if (nxtProcess.isAlive())
                nxtProcess.destroyForcibly();
        } catch (Exception e) {
            log.error("stopWallet method failed", e);
        }
    }

    @Override
    public BigDecimal getBalance() {
        BalanceResponse balance = sendApiRequest(WalletRequestType.GET_BALANCE, keyToValue -> keyToValue.put("account", accountId), BalanceResponse.class);
        if (balance != null)
            return balance.getBalance();
        return null;
    }

    @Override
    public void sendMoneyToAddressBalance(BigDecimal money, String address) {
        sendApiRequest(WalletRequestType.SEND_MONEY, passphrase, keyToValue -> {
            keyToValue.put("recipient", address);
            keyToValue.put("feeNQT", "0");
            keyToValue.put("amountNQT", Long.toString(money.multiply(new BigDecimal(BaseWalletResponse.ONE_NXT)).longValue()));
            keyToValue.put("deadline", "60");
        }, SendTransactionResponse.class);
    }

    @Override
    public String sendMessage(byte[] body) {
        return sendMessage(mainAddress, body);
    }

    public String sendMessage(String recipient, byte[] body) {
        return sendMessage(recipient, body, passphrase);
    }

    public String sendMessage(String recipient, byte[] body, String senderPassword) {
        SendTransactionResponse transaction = sendApiRequest(WalletRequestType.SEND_MESSAGE, senderPassword, keyToValue -> {
            keyToValue.put("recipient", recipient);
            keyToValue.put("feeNQT", "0");
            keyToValue.put("message", new String(body, StandardCharsets.UTF_8));
            keyToValue.put("deadline", "60");
        }, SendTransactionResponse.class);
        if (transaction != null)
            return transaction.getTransactionId();
        return null;
    }

    @Override
    public String getSelfAddress() {
        waitInitialize();
        return selfAccount;
    }

    @Override
    public List<Message> getNewMessages(long timestamp) {
        Set<String> resultIds = new HashSet<>();
        List<Message> result = new ArrayList<>();
        List<Message> confirmedMessages = getConfirmedMessages(timestamp);
        List<Message> unconfirmedMessages = getUnconfirmedMessages(timestamp);
        if (confirmedMessages != null) {
            resultIds.addAll(confirmedMessages.stream().map(Message::getId).collect(Collectors.toList()));
            confirmedMessages.stream().forEach(m -> result.add(m));
        }
        if (unconfirmedMessages != null) {
            unconfirmedMessages.stream().filter(m -> resultIds.contains(m.getId())).forEach(m -> result.add(m));
        }
        return result;
    }

    private List<Message> getConfirmedMessages(long timestamp) {
        TransactionsResponse result = sendApiRequest(WalletRequestType.GET_BLOCKCHAIN_TRANSACTIONS, keyToValue -> {
            keyToValue.put("account", selfAccount);
            keyToValue.put("timestamp", Long.toString(timestamp / 1000));
            keyToValue.put("withMessage", "true");
        }, TransactionsResponse.class);
        if (result == null)
            return null;
        try {
            return Arrays.asList(result.getTransactions()).stream().
                    map(t -> new Message(t.getTransaction(), t.getAttachment().getMessage().getBytes(StandardCharsets.UTF_8))).
                    collect(Collectors.toList());
        } catch (Exception e) {
            log.error("getConfirmedMessages[{}] failed. Message: {}", timestamp, e.getMessage());
            return null;
        }
    }

    private List<Message> getUnconfirmedMessages(long timestamp) {
        long secondsTimestamp = timestamp / 1000;
        UnconfirmedTransactionsResponse result = sendApiRequest(WalletRequestType.GET_UNCONFIRMED_TRANSACTIONS,
                keyToValue -> keyToValue.put("account", selfAccount), UnconfirmedTransactionsResponse.class);
        if (result == null)
            return null;
        try {
            return Arrays.asList(result.getUnconfirmedTransactions()).stream().
                    filter(t -> t.getAttachment().isMessageIsText() && t.getTimestamp() >= secondsTimestamp).
                    map(t -> new Message(t.getTransaction(), t.getAttachment().getMessage().getBytes(StandardCharsets.UTF_8))).
                    collect(Collectors.toList());
        } catch (Exception e) {
            log.error("getUnconfirmedMessages[{}] failed. Message: {}", timestamp, e.getMessage());
            return null;
        }
    }

    private boolean startForging() {
        StartForgingResponse response = sendApiRequest(WalletRequestType.START_FORGING, passphrase, keyToValue -> {
        }, StartForgingResponse.class);
        return response != null;
    }

    private void inheritIO(final InputStream src) {
        new Thread(() -> {
            Scanner sc = new Scanner(src);
            while (sc.hasNextLine()) {
                String logStr = sc.nextLine();
                if (logStr.toUpperCase().contains("ERROR")) {
                    log.error(logStr);
                } else if (logStr.toUpperCase().contains("WARNING")) {
                    log.warn(logStr);
                } else {
                    log.info(logStr);
                }
            }
        }).start();
    }

    private void waitInitialize() {
        if (selfAccount != null && passphrase != null)
            return;
        log.info("Start wallet initialization...");
        while ((selfAccount == null || passphrase == null) && !Thread.currentThread().isInterrupted()) {
            try {
                if (accountId == null || selfAccount == null) {
                    AccountResponse account = sendApiRequest(WalletRequestType.GET_ACCOUNT_ID, passphrase, keyToValue -> {
                    }, AccountResponse.class);
                    if (account != null) {
                        accountId = account.getAccountId();
                        selfAccount = account.getAddress();
                    }
                }
                sleep(100);
            } catch (Exception e) {
                log.error("waitInitialize. Error: {}", e.getMessage());
            }
        }
        log.info("Wallet initialization finished. selfAccount={}", selfAccount);
        while (!isForgeNow && !Thread.currentThread().isInterrupted()) {
            isForgeNow = startForging();
            sleep(100);
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
