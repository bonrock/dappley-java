package com.dappley.java.sdk;

import com.dappley.java.core.chain.TransactionManager;
import com.dappley.java.core.chain.WalletManager;
import com.dappley.java.core.net.DataProvider;
import com.dappley.java.core.net.RemoteDataProvider;
import com.dappley.java.core.net.TransactionSender;
import com.dappley.java.core.po.Transaction;
import com.dappley.java.core.po.Utxo;
import com.dappley.java.core.po.Wallet;
import com.dappley.java.core.util.AddressUtil;
import com.dappley.java.core.util.ObjectUtils;
import com.dappley.java.sdk.chain.UtxoManager;
import com.dappley.java.sdk.config.Configuration;
import com.dappley.java.sdk.net.LocalDataProvider;
import com.dappley.java.sdk.util.Asserts;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;
import java.util.List;

/**
 * Dappley Java Sdk client.
 * <p>
 * Provides different method to synchronize block information from neighbor chain nodes.
 * </p>
 * <p>
 * DataMode.LOCAL_STORAGE: use local storage with database.
 * DataMode.REMOTE_ONLINE: use real data from neighbor chain node. This may cause info delay while the node's data is not accurate.
 * </p>
 */
@Slf4j
public class Dappley {
    private static DataProvider dataProvider;
    private static TransactionSender transactionSender;

    /**
     * Initialize client
     * @param dataMode choose a data reader mode
     */
    public static void init(DataMode dataMode) {
        String serverIp = Configuration.getInstance().getServerIp();
        int serverPort = Configuration.getInstance().getServerPort();
        if (dataMode == DataMode.LOCAL_STORAGE) {
            throw new IllegalArgumentException("only remote online mode is supported now.");
        }
        try {
            if (dataMode == DataMode.REMOTE_ONLINE) {
                dataProvider = new RemoteDataProvider(RemoteDataProvider.RemoteProtocalType.RPC, serverIp, serverPort);
            }
            transactionSender = new TransactionSender(serverIp, serverPort);
        } catch (Exception e) {
            log.error("init: ", e);
        }
    }

    /**
     * Create a new wallet address.
     * <p>Contains mnemonic and private key in wallet.</p>
     * @return Wallet
     */
    public static Wallet createWallet() {
        Wallet wallet = WalletManager.createWallet();
        return wallet;
    }

    /**
     * Import wallet info from mnemonic words.
     * <p>Mnemonic are made up of 12 english words within blank as seperator.</p>
     * @param mnemonic words
     * @return Wallet
     */
    public static Wallet importWalletFromMnemonic(String mnemonic) {
        Wallet wallet = WalletManager.importWalletFromMnemonic(mnemonic);
        return wallet;
    }

    /**
     * Import wallet info from privateKey.
     * @param privateKey
     * @return Wallet
     */
    public static Wallet importWalletFromPrivateKey(String privateKey) {
        Wallet wallet = WalletManager.importWalletFromPrivateKey(privateKey);
        return wallet;
    }

    /**
     * Returns balance under current address.
     * @param address wallet address
     * @return BigInteger balance amount
     */
    public static BigInteger getWalletBalance(String address) {
        Asserts.init(dataProvider);
        return dataProvider.getBalance(address);
    }

    /**
     * Returns all balances in wallet list.
     * @param wallets wallet list
     * @return List<Wallet> wallet list with balance inside
     */
    public static List<Wallet> getWalletBalances(List<Wallet> wallets) {
        Asserts.init(dataProvider);
        BigInteger balance;
        for (Wallet wallet : wallets) {
            balance = dataProvider.getBalance(wallet.getAddress());
            wallet.setBalance(balance);
        }
        return wallets;
    }

    /**
     * Encrypt wallet data with AES.
     * @param wallet   data
     * @param password
     * @return Wallet encrypted wallet data
     */
    public static Wallet encryptWallet(Wallet wallet, String password) {
        return WalletManager.encryptWallet(wallet, password);
    }

    /**
     * Decrypt wallet data.
     * @param wallet   encrypted wallet data
     * @param password
     * @return Wallet wallet data
     */
    public static Wallet decryptWallet(Wallet wallet, String password) {
        return WalletManager.decryptWallet(wallet, password);
    }

    /**
     * Add a new wallet address into local storage.
     * @param address wallet address
     */
    public static void addAddress(String address) {
        Asserts.init(dataProvider);
        if (dataProvider instanceof LocalDataProvider) {
            // TODO add address to database
        }
    }

    /**
     * Remove a wallet address in local storage.
     * @param address wallet address
     */
    public static void removeAddress(String address) {
        Asserts.init(dataProvider);
        if (dataProvider instanceof LocalDataProvider) {
            // TODO remove address from database
        }
    }

    /**
     * Convert publicKey hash to wallet address
     * @param pubKeyHash publicKey hash
     * @return String wallet address
     */
    public static String publicKeyToAddress(byte[] pubKeyHash) {
        return AddressUtil.getAddressFromPubKeyHash(pubKeyHash);
    }

    /**
     * Valid the address is legal
     * @param address wallet or contract address
     * @return boolean is legal
     */
    public static boolean validateAddress(String address) {
        return AddressUtil.validateAddress(address);
    }

    /**
     * Valid the contract address is legal
     * @param address contract address
     * @return boolean is legal
     */
    public static boolean validateContractAddress(String address) {
        return AddressUtil.validateContractAddress(address);
    }

    /**
     * Returns paged utxo list.
     * @param address   wallet address
     * @param pageIndex current page index
     * @param pageSize
     * @return List<Utxo>
     */
    public static List<Utxo> getUtxos(String address, int pageIndex, int pageSize) {
        Asserts.init(dataProvider);
        if (pageIndex <= 0 || pageSize <= 0) {
            return null;
        }
        List<Utxo> utxos = dataProvider.getUtxos(address);
        if (ObjectUtils.isEmpty(utxos)) {
            return null;
        }
        int pageNo = (pageIndex - 1) * pageSize;
        if (utxos.size() <= pageNo) {
            return null;
        }
        int toIndex = pageNo + pageSize;
        if (toIndex > utxos.size()) {
            toIndex = utxos.size();
        }
        List<Utxo> subList = utxos.subList(pageNo, toIndex);
        return subList;
    }

    /**
     * Send a new transaction to blockchain online.
     * @param fromAddress from user's address
     * @param toAddress   to user's address
     * @param amount      transferred amount
     * @param privateKey  from user's privateKey
     * @return boolean is transaction committed successful
     */
    public static boolean sendTransaction(String fromAddress, String toAddress, BigInteger amount, BigInteger privateKey) {
        Asserts.init(dataProvider);
        if (!AddressUtil.validateUserAddress(fromAddress)) {
            throw new IllegalArgumentException("fromAddress is illegal !");
        }
        if (!AddressUtil.validateUserAddress(toAddress)) {
            throw new IllegalArgumentException("toAddress is illegal !");
        }
        return sendTransaction(fromAddress, toAddress, amount, privateKey, null);
    }

    /**
     * Send a new transaction to blockchain online.
     * @param fromAddress     from user's address
     * @param contractAddress contract's address
     * @param fee             contract transaction fee
     * @param privateKey      from user's privateKey
     * @param contract        contract content
     * @return boolean is transaction committed successful
     */
    public static boolean sendTransactionWithContract(String fromAddress, String contractAddress, BigInteger fee, BigInteger privateKey, String contract) {
        Asserts.init(dataProvider);
        if (!AddressUtil.validateUserAddress(fromAddress)) {
            throw new IllegalArgumentException("fromAddress is illegal !");
        }
        if (!AddressUtil.validateContractAddress(contractAddress)) {
            throw new IllegalArgumentException("contractAddress is illegal !");
        }
        if (contract == null) {
            throw new NullPointerException("contract cannot be null !");
        }
        return sendTransaction(fromAddress, contractAddress, fee, privateKey, contract);
    }

    /**
     * Send a new transaction to blockchain online.
     * @param fromAddress from user's address
     * @param toAddress   to address
     * @param amount      transferred amount
     * @param privateKey  from user's privateKey
     * @param contract    contract content
     * @return boolean is transaction committed successful
     */
    private static boolean sendTransaction(String fromAddress, String toAddress, BigInteger amount, BigInteger privateKey, String contract) {
        if (ObjectUtils.isEmpty(fromAddress) || ObjectUtils.isEmpty(toAddress)) {
            return false;
        }
        List<Utxo> allUtxo = dataProvider.getUtxos(fromAddress);
        if (ObjectUtils.isEmpty(allUtxo)) {
            return false;
        }
        List<Utxo> utxos = UtxoManager.getSuitableUtxos(allUtxo, amount);
        if (ObjectUtils.isEmpty(utxos)) {
            return false;
        }
        Transaction transaction = TransactionManager.newTransaction(utxos, toAddress, amount, privateKey, contract);
        int errorCode = transactionSender.sendTransaction(transaction);
        if (errorCode == 0) {
            // success
            return true;
        }
        return false;
    }

    /**
     * Create a new contract address
     * @return String contract address
     */
    public static String createContractAddress() {
        return AddressUtil.createContractAddress();
    }


    /**
     * Data storage mode
     * <p>We provides two type of data synchronize mode.
     * <code>LOCAL_STORAGE</code> will use local disk space to save blockchain datas.
     * <code>REMOTE_ONLINE</code> will use in-time data from neighbor nodes which is not always be creditable.
     * </p>
     */
    public enum DataMode {
        /**
         * All datas will be stored at local disk.
         */
        LOCAL_STORAGE,
        /**
         * All datas will be get from remove node in real time.
         */
        REMOTE_ONLINE
    }
}