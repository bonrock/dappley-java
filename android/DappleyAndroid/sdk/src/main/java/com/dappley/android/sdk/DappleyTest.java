package com.dappley.android.sdk;

import android.content.Context;

import com.dappley.android.sdk.chain.BlockChainManager;
import com.dappley.android.sdk.chain.TransactionManager;
import com.dappley.android.sdk.config.Configuration;
import com.dappley.android.sdk.crypto.AesCipher;
import com.dappley.android.sdk.crypto.Bip39;
import com.dappley.android.sdk.crypto.EcCipher;
import com.dappley.android.sdk.crypto.KeyPairTool;
import com.dappley.android.sdk.db.BlockChainDb;
import com.dappley.android.sdk.db.BlockDb;
import com.dappley.android.sdk.db.BlockIndexDb;
import com.dappley.android.sdk.db.TransactionDb;
import com.dappley.android.sdk.db.UtxoDb;
import com.dappley.android.sdk.db.UtxoIndexDb;
import com.dappley.android.sdk.net.ProtocalProvider;
import com.dappley.android.sdk.po.Transaction;
import com.dappley.android.sdk.po.Utxo;
import com.dappley.android.sdk.po.UtxoIndex;
import com.dappley.android.sdk.protobuf.BlockProto;
import com.dappley.android.sdk.protobuf.RpcProto;
import com.dappley.android.sdk.protobuf.RpcServiceGrpc;
import com.dappley.android.sdk.task.LocalBlockSchedule;
import com.dappley.android.sdk.util.AddressUtil;
import com.dappley.android.sdk.util.Base64;
import com.dappley.android.sdk.util.HashUtil;
import com.dappley.android.sdk.util.HexUtil;
import com.google.protobuf.ByteString;
import com.tencent.mmkv.MMKV;

import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.spongycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.spongycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.web3j.crypto.ECKeyPair;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class DappleyTest {
    public static void main(String[] args) throws Exception {
        KeyPair keyPair = KeyPairTool.newKeyPair();

        ECKeyPair ecKeyPair = KeyPairTool.castToEcKeyPair(keyPair);
        System.out.println(ecKeyPair.getPrivateKey().toString(16));
        String address = AddressUtil.createAddress(ecKeyPair);
        System.out.println(address);

//        testRpcConnect(null);

        String addr = "1FZqATrZWdXWi9tsGHZzHzgwJRnpwQoCGi";
        System.out.println(HexUtil.toHex(HashUtil.getPubKeyHash(addr)));
    }

    public static void testAes() {
        String encoded = AesCipher.encryptToHex("I am the one.", "aesKey");
        System.out.println(encoded);
        String decoded = AesCipher.decryptFromHex(encoded, "aesKey");
        System.out.println(decoded);
        System.out.println(new String(AesCipher.decrypt(AesCipher.encrypt("I am the one.".getBytes(), "aesKey"), "aesKey")));
    }

    public static void testWord() {
        Bip39.generateMnemonic();
    }

    public static void testDB(Context context) {
        MMKV.initialize(context);
        BlockDb blockDB = new BlockDb(context);
        BlockProto.BlockHeader blockHeader = BlockProto.BlockHeader.newBuilder().setHash(ByteString.copyFromUtf8("testblock")).build();
        BlockProto.Block block = BlockProto.Block.newBuilder().setHeader(blockHeader).build();
//        boolean isSuccess = blockDB.saveBytes(block);
//        boolean isSuccess = blockDB.save(block);
        System.out.println(block);

    }

    public static void testRecovery() {
        BigInteger privateKey = new BigInteger("bb23d2ff19f5b16955e8a24dca34dd520980fe3bddca2b3e1b56663f0ec1aa7e", 16);
        BigInteger publicKey = new BigInteger("5767188359795479736405662394620174832271978020098784807457600728164221489565479030464547807529865979388276872414794355820379785119919302677391625913455408", 16);
        ECKeyPair ecKeyPair = ECKeyPair.create(privateKey);
        System.out.println(ecKeyPair.getPublicKey());
        BCECPublicKey prk = KeyPairTool.fromPubInteger(publicKey);
        BCECPrivateKey prvk = KeyPairTool.fromPrivInteger(privateKey);
        byte[] publicKeyBytes = prk.getQ().getEncoded(false);
        BigInteger publicKeyValue =
                new BigInteger(1, Arrays.copyOfRange(publicKeyBytes, 1, publicKeyBytes.length));
        System.out.println(publicKeyValue);
        System.out.println(prvk);
    }

    public static String testEncrypt() {
        KeyPair keyPair = KeyPairTool.newKeyPair();
//        ECPublicKey publicKey = (ECPublicKey) keyPair.getPublic();
//        ECPrivateKey privateKey = (ECPrivateKey) keyPair.getPrivate();
        try {
//            System.out.println(new BigInteger(keyPair.getPrivate().getEncoded()));
            PublicKey publicKey = KeyPairTool.getPublicKey(Base64.encode(keyPair.getPublic().getEncoded()));
            PrivateKey privateKey = KeyPairTool.getPrivateKey(Base64.encode(keyPair.getPrivate().getEncoded()));
            String test = "this is data";
            byte[] encoded = new byte[0];
            encoded = EcCipher.encrypt(test.getBytes("UTF-8"), publicKey);
            String result = "original:" + test;
            byte[] decoded = EcCipher.decrypt(encoded, privateKey);
            result += "\ndecoded:" + new String(decoded, "UTF-8");
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static void testRpcConnect(Context context) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(Configuration.getInstance(context).getServerIp(), Configuration.getInstance(context).getServerPort())
                .usePlaintext()
                .build();
        RpcServiceGrpc.RpcServiceBlockingStub stub = RpcServiceGrpc.newBlockingStub(channel);
        RpcProto.GetBalanceRequest request = RpcProto.GetBalanceRequest.newBuilder()
                .setAddress("1BpXBb3uunLa9PL8MmkMtKNd3jzb5DHFkG")
                .build();
        RpcProto.GetBalanceResponse response = stub.rpcGetBalance(request);
        String message = response.getMessage();
        System.out.println(message);
    }

    public static void testSchedule(Context context) {
        MMKV.initialize(context);

        BlockChainManager.initGenesisBlock(context);

        String address = "1BpXBb3uunLa9PL8MmkMtKNd3jzb5DHFkG";
        BlockChainManager.addWalletAddress(context, address);
        // receiver
        address = "1FZqATrZWdXWi9tsGHZzHzgwJRnpwQoCGi";
        BlockChainManager.addWalletAddress(context, address);

        LocalBlockSchedule.start(context);
    }

    public static void testLocal(Context context) {
        String address = "1BpXBb3uunLa9PL8MmkMtKNd3jzb5DHFkG";
//        String address = "1FZqATrZWdXWi9tsGHZzHzgwJRnpwQoCGi";
        MMKV.initialize(context);

//        BlockChainManager.initGenesisBlock(context);
        UtxoIndexDb utxoIndexDb = new UtxoIndexDb(context);
        UtxoDb utxoDb = new UtxoDb(context);
        Set<UtxoIndex> utxoIndexSet = utxoIndexDb.get(address);

        System.out.println("utxoIndexSet: " + (utxoIndexSet == null ? 0 : utxoIndexSet.size()));

        if (utxoIndexSet != null) {
            int i = 0;
            for (UtxoIndex utxoIndex : utxoIndexSet) {
                if (i > 10) {
                    break;
                }
                Utxo utxo = utxoDb.get(utxoIndex.toString());
                System.out.println(utxo.toString());
                i++;
            }
        }
    }

    public static void clearAddress(Context context){
        MMKV.initialize(context);
        BlockChainDb blockChainDb = new BlockChainDb(context);
        blockChainDb.saveWalletAddressSet(new HashSet<String>());
    }

    public static void clearAll(Context context) {
        MMKV.initialize(context);
        UtxoIndexDb utxoIndexDb = new UtxoIndexDb(context);
        BlockDb blockDb = new BlockDb(context);
        BlockIndexDb blockIndexDb = new BlockIndexDb(context);
        TransactionDb transactionDb = new TransactionDb(context);
        UtxoDb utxoDb = new UtxoDb(context);

        utxoIndexDb.clearAll();
        utxoDb.clearAll();
        transactionDb.clearAll();
        blockIndexDb.clearAll();
        blockDb.clearAll();

        // add genesis block
        BlockChainManager.initGenesisBlock(context);
    }
}
