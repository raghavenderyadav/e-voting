package uk.dsxt.voting.common.networking;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import lombok.extern.log4j.Log4j2;
import nxt.Constants;
import nxt.Nxt;
import uk.dsxt.voting.common.datamodel.RequestType;
import uk.dsxt.voting.common.datamodel.walletapi.*;
import uk.dsxt.voting.common.utils.HttpHelper;

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
    private Long firstBlockTime;

    private Process nxtProcess;
    private boolean isForgeNow = false;

    public BaseWalletManager(Properties properties) {
        jarPath = properties.getProperty("nxt.jar.path");
        nxtPropertiesPath = properties.getProperty("nxt.properties.path");
        mainAddress = properties.getProperty("nxt.main.address");
        passwordForRegister = properties.getProperty("nxt.register.password");
        passphrase = properties.getProperty("nxt.account.passphrase");
        httpHelper = new HttpHelper(5000, 5000);

        mapper = new ObjectMapper();
        mapper.setNodeFactory(JsonNodeFactory.withExactBigDecimals(true));
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        port = properties.getProperty("nxt.apiServerPort");
        Properties nxtProperties = new Properties();
        Nxt.loadProperties(nxtProperties, nxtPropertiesPath, true);
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
                log.error("Can't parse response: {}", response, e);
            }
            return result;
        } catch (Exception e) {
            log.error("Method {} failed.", type, e);
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
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command(cmd);
            nxtProcess = processBuilder.start();
            inheritIO(nxtProcess.getInputStream(), false);
            inheritIO(nxtProcess.getErrorStream(), true);
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    stopWallet();
                }
            });
        } catch (Exception e) {
            String errorMessage = String.format("Can't run wallet. Error: %s", e.getMessage());
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
            keyToValue.put("amountNQT", Long.toString(money.multiply(new BigDecimal(Constants.ONE_NXT)).longValue()));
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
        List<Transaction> result = new ArrayList<>();
            List<Transaction> messages = getMessages();
            if (messages == null)
                return null;
            for (Transaction message : messages) {
                if (message.getTimestamp() < timestamp) {
                    break;
                }
                if (message.getAttachment().isMessageIsText())
                    result.add(message);
            }

        try {
            return result.stream().map(pm -> {
                String transactionId = pm.getTransaction();
                String readedMessage = pm.getAttachment().getMessage();
                if (readedMessage == null)
                    throw new RuntimeException(transactionId);
                return new Message(transactionId, readedMessage.getBytes(StandardCharsets.UTF_8));
            }).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("getNewMessages[{}] failed. Message: {}", timestamp, e.getMessage());
            return null;
        }
    }

    private List<Transaction> getMessages() {
        //TODO get confirmed messages too
        UnconfirmedTransactionsResponse result = sendApiRequest(WalletRequestType.GET_UNCONFIRMED_TRANSACTIONS, keyToValue -> {
            keyToValue.put("account", selfAccount);
        }, UnconfirmedTransactionsResponse.class);
        if (result == null)
            return null;
        return Arrays.asList(result.getUnconfirmedTransactions());
    }

    private String getReadMessage(String transactionId) {
        ReadMessage result = sendApiRequest(WalletRequestType.READ_MESSAGE, passphrase,
                keyToValue -> keyToValue.put("transaction", transactionId), ReadMessage.class);
        if (result == null)
            return null;
        return result.getMessage();
    }

    private BlockResponse getBlock(int height) {
        return sendApiRequest(WalletRequestType.GET_BLOCK, passphrase, keyToValue -> keyToValue.put("height", Integer.toString(height)), BlockResponse.class);
    }

    private boolean startForging() {
        StartForgingResponse response = sendApiRequest(WalletRequestType.START_FORGING, passphrase, keyToValue -> {}, StartForgingResponse.class);
        return response != null;
    }

    private void inheritIO(final InputStream src, final boolean isError) {
        new Thread(() -> {
            Scanner sc = new Scanner(src);
            while (sc.hasNextLine()) {
                if (isError) {
                    log.error(sc.nextLine());
                } else {
                    log.info(sc.nextLine());
                }
            }
        }).start();
    }

    private void waitInitialize() {
        if (selfAccount != null && passphrase != null & firstBlockTime != null)
            return;
        log.info("Start wallet initialization...");
        while (selfAccount == null || passphrase == null || firstBlockTime == null) {
            try {
                if (accountId == null || selfAccount == null) {
                    AccountResponse account = sendApiRequest(WalletRequestType.GET_ACCOUNT_ID, passphrase, keyToValue -> {}, AccountResponse.class);
                    if (account != null) {
                        accountId = account.getAccountId();
                        selfAccount = account.getAddress();
                    }
                }
                //TODO remove it:
                if (firstBlockTime == null) {
                    BlockResponse block = getBlock(0);
                    if (block != null)
                        firstBlockTime = block.getTimestamp();
                }
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                return;
            } catch (Exception e) {
                log.error("waitInitialize. Error: {}", e.getMessage());
            }
        }
        log.info("Wallet initialization finished. selfAccount={} firstBlockTime={}", selfAccount, firstBlockTime);
        while (!isForgeNow) {
            isForgeNow = startForging();
            sleep(1000);
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw  new RuntimeException(e);
        }
    }
}
