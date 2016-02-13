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
    private final String port;
    private final HttpHelper httpHelper;

    private String passphrase;
    private String accountId;
    private String selfAccount;
    private Long firstBlockTime;

    public BaseWalletManager(Properties properties) {
        jarPath = properties.getProperty("nxt.jar.path");
        nxtPropertiesPath = properties.getProperty("nxt.properties.path");
        mainAddress = properties.getProperty("nxt.main.address");
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
        nxtProperties.setProperty("nxt.defaultPeers", properties.getProperty("nxt.defaultPeers"));
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
                url.append(keyToValue.getValue());
                url.append("&");
            }
            if (url.lastIndexOf("&") == url.length() - 1)
                url.replace(url.length() - 1, url.length(), "");
            String response = httpHelper.request(url.toString(), null, RequestType.POST);
            T result = null;
            try {
                result = mapper.readValue(response, tClass);
            } catch (IOException e) {
                log.error(String.format("Can't parse response: %s", response), e);
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
            Process start = processBuilder.start();
            inheritIO(start.getInputStream(), false);
            inheritIO(start.getErrorStream(), true);
        } catch (Exception e) {
            String errorMessage = String.format("Can't run wallet. Error: %s", e.getMessage());
            log.error(errorMessage, e);
            throw new RuntimeException(errorMessage);
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
            keyToValue.put("fee", "0");
            keyToValue.put("amount", Long.toString(money.multiply(new BigDecimal(Constants.ONE_NXT)).longValue()));
            keyToValue.put("deadline", "60");
        }, SendTransactionResponse.class);
    }

    @Override
    public String sendMessage(byte[] body) {
        SendTransactionResponse transaction = sendApiRequest(WalletRequestType.SEND_MESSAGE, passphrase, keyToValue -> {
            keyToValue.put("recipient", mainAddress);
            keyToValue.put("fee", "0");
            keyToValue.put("message", new String(body, StandardCharsets.UTF_8));
            keyToValue.put("deadline", "60");
        }, SendTransactionResponse.class);
        if (transaction != null)
            return transaction.getTransactionId();
        return null;
    }

    @Override
    public String getSelfAddress() {
        return selfAccount;
    }

    @Override
    public List<Message> getNewMessages(long timestamp) {
        List<WalletMessage> result = new ArrayList<>();
        for (int i = 0; ; i++) {
            List<WalletMessage> messages = getMessages(i * 10, (i + 1) * 10);
            if (messages == null)
                return null;
            boolean isFinished = false;
            for (WalletMessage message : messages) {
                if (message.getTransactionTimestamp() < timestamp) {
                    isFinished = true;
                    break;
                }
                result.add(message);
            }
            if (isFinished)
                break;
        }

        try {
            return result.stream().map(pm -> {
                String transactionId = pm.getTransaction();
                String readedMessage = getReadMessage(transactionId);
                if (readedMessage == null)
                    throw new RuntimeException(transactionId);
                return new Message(transactionId, readedMessage.getBytes(StandardCharsets.UTF_8));
            }).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("getNewMessages[{}] failed. Message: {}", timestamp, e.getMessage());
            return null;
        }
    }

    private List<WalletMessage> getMessages(int firstIndex, int lastIndex) {
        PrunableMessages result = sendApiRequest(WalletRequestType.GET_PRUNABLE_MESSAGES, keyToValue -> {
            keyToValue.put("account", selfAccount);
            keyToValue.put("firstIndex", Integer.toString(firstIndex));
            keyToValue.put("lastIndex", Integer.toString(lastIndex));
        }, PrunableMessages.class);
        if (result == null)
            return null;
        return Arrays.asList(result.getPrunableMessages());
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
        while (selfAccount == null || passphrase == null || firstBlockTime == null) {
            try {
                AccountResponse account = sendApiRequest(WalletRequestType.GET_ACCOUNT_ID, passphrase, keyToValue -> {}, AccountResponse.class);
                if (account != null) {
                    accountId = account.getAccountId();
                    selfAccount = account.getAddress();
                }
                BlockResponse block = getBlock(0);
                if (block != null)
                    firstBlockTime = block.getTimestamp();
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                return;
            } catch (Exception e) {
                log.error("waitInitialize. Error: {}", e.getMessage());
            }
        }
    }
}
