package com.dappley.java.core.net;

import com.dappley.java.core.po.BlockChainInfo;
import com.dappley.java.core.po.ContractQueryResult;
import com.dappley.java.core.po.SendTxResult;
import com.dappley.java.core.protobuf.*;
import com.dappley.java.core.util.Asserts;
import com.google.protobuf.ByteString;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Implementation of ProtocalProvider on RPC protocal.
 */
@Slf4j
public class RpcProtocalProvider implements ProtocalProvider {

    private ManagedChannel channel;
    private RpcServiceGrpc.RpcServiceBlockingStub rpcServiceBlockingStub;
    private String serverIp;
    private int serverPort;

    @Override
    public void init(String serverIp, int serverPort) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        initChannel();
    }

    /**
     * Initialize rpc channel
     */
    private void initChannel() {
        RpcChannelBuilder channelBuilder = new RpcChannelBuilder().newChannel(serverIp, serverPort);
        channel = channelBuilder.build();
    }

    /**
     * Returns the singleton of RpcServiceBlockingStub
     * @return RpcServiceBlockingStub
     */
    private RpcServiceGrpc.RpcServiceBlockingStub getRpcServiceBlockingStub() {
        if (channel.getState(true) == ConnectivityState.IDLE
                || channel.getState(true) == ConnectivityState.SHUTDOWN
                || channel.getState(true) == ConnectivityState.TRANSIENT_FAILURE) {
            initChannel();
            rpcServiceBlockingStub = RpcServiceGrpc.newBlockingStub(channel);
        }
        if (rpcServiceBlockingStub == null) {
            rpcServiceBlockingStub = RpcServiceGrpc.newBlockingStub(channel);
        }
        return rpcServiceBlockingStub;
    }

    @Override
    public void close() {
        if (channel == null) {
            return;
        }
        channel.shutdown();
    }

    @Override
    public String getVersion() {
        Asserts.clientInit(channel);
        Asserts.channelOpen(channel);

        RpcProto.GetVersionRequest request = RpcProto.GetVersionRequest.newBuilder()
                .build();
        RpcProto.GetVersionResponse response = getRpcServiceBlockingStub().rpcGetVersion(request);
        String message = "[protocal version:" + response.getProtoVersion() + "] [server version: " + response.getServerVersion() + "]";
        log.debug("getVersion: " + message);
        return message;
    }

    @Override
    public long getBalance(String address) {
        Asserts.clientInit(channel);
        Asserts.channelOpen(channel);

        RpcProto.GetBalanceRequest request = RpcProto.GetBalanceRequest.newBuilder()
                .setAddress(address)
                .build();
        RpcProto.GetBalanceResponse response = getRpcServiceBlockingStub().rpcGetBalance(request);
        log.debug("getBalance: " + response.getAmount());
        return response.getAmount();
    }

    @Override
    public BlockChainInfo getBlockchainInfo() {
        Asserts.clientInit(channel);
        Asserts.channelOpen(channel);
        RpcProto.GetBlockchainInfoRequest request = RpcProto.GetBlockchainInfoRequest.newBuilder()
                .build();
        RpcProto.GetBlockchainInfoResponse response = getRpcServiceBlockingStub().rpcGetBlockchainInfo(request);
        BlockChainInfo blockChainInfo = new BlockChainInfo();
        blockChainInfo.setTailBlockHash(response.getTailBlockHash());
        blockChainInfo.setBlockHeight(response.getBlockHeight());
        blockChainInfo.setProducers(response.getProducersList());
        log.debug("getBlockchainInfo: " + response.toString());
        return blockChainInfo;
    }

    @Override
    public List<UtxoProto.Utxo> getUtxo(String address) {
        Asserts.clientInit(channel);
        Asserts.channelOpen(channel);
        RpcProto.GetUTXORequest request = RpcProto.GetUTXORequest.newBuilder()
                .setAddress(address)
                .build();
        RpcProto.GetUTXOResponse response = getRpcServiceBlockingStub().rpcGetUTXO(request);
        List<UtxoProto.Utxo> utxos = response.getUtxosList();
        return utxos;
    }

    @Override
    public List<BlockProto.Block> getBlocks(List<ByteString> startHashs, int count) {
        Asserts.clientInit(channel);
        Asserts.channelOpen(channel);
        RpcProto.GetBlocksRequest request = RpcProto.GetBlocksRequest.newBuilder()
                .addAllStartBlockHashes(startHashs)
                .setMaxCount(count)
                .build();
        RpcProto.GetBlocksResponse response = getRpcServiceBlockingStub().rpcGetBlocks(request);
        log.debug("getBlocks blockCount" + response.getBlocksCount());
        return response.getBlocksList();
    }

    @Override
    public BlockProto.Block getBlockByHash(ByteString byteHash) {
        Asserts.clientInit(channel);
        Asserts.channelOpen(channel);
        RpcProto.GetBlockByHashRequest request = RpcProto.GetBlockByHashRequest.newBuilder()
                .setHash(byteHash)
                .build();
        RpcProto.GetBlockByHashResponse response = getRpcServiceBlockingStub().rpcGetBlockByHash(request);
        BlockProto.Block block = response.getBlock();
        return block;
    }

    @Override
    public BlockProto.Block getBlockByHeight(long height) {
        Asserts.clientInit(channel);
        Asserts.channelOpen(channel);
        RpcProto.GetBlockByHeightRequest request = RpcProto.GetBlockByHeightRequest.newBuilder()
                .setHeight(height)
                .build();
        RpcProto.GetBlockByHeightResponse response = getRpcServiceBlockingStub().rpcGetBlockByHeight(request);
        BlockProto.Block block = response.getBlock();
        return block;
    }

    @Override
    public SendTxResult sendTransaction(TransactionProto.Transaction transaction) {
        Asserts.clientInit(channel);
        Asserts.channelOpen(channel);
        RpcProto.SendTransactionRequest request = RpcProto.SendTransactionRequest.newBuilder()
                .setTransaction(transaction)
                .build();
        SendTxResult sendTxResult = new SendTxResult();
        sendTxResult.setSuccess(false);
        try {
            RpcProto.SendTransactionResponse response = getRpcServiceBlockingStub().rpcSendTransaction(request);
            sendTxResult.setSuccess(true);
            sendTxResult.setGeneratedContractAddress(response.getGeneratedContractAddress());
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return sendTxResult;
    }

    @Override
    public ByteString estimateGas(TransactionProto.Transaction transaction) {
        Asserts.clientInit(channel);
        Asserts.channelOpen(channel);
        RpcProto.EstimateGasRequest request = RpcProto.EstimateGasRequest.newBuilder()
                .setTransaction(transaction)
                .build();
        RpcProto.EstimateGasResponse response = getRpcServiceBlockingStub().rpcEstimateGas(request);
        ByteString gasCount = response.getGasCount();
        return gasCount;
    }

    @Override
    public ByteString getGasPrice() {
        Asserts.clientInit(channel);
        Asserts.channelOpen(channel);
        RpcProto.GasPriceRequest request = RpcProto.GasPriceRequest.newBuilder()
                .build();
        RpcProto.GasPriceResponse response = getRpcServiceBlockingStub().rpcGasPrice(request);
        ByteString gasPrice = response.getGasPrice();
        return gasPrice;
    }

    @Override
    public ContractQueryResult contractQuery(String contractAddress, String key, String value) {
        Asserts.clientInit(channel);
        Asserts.channelOpen(channel);
        RpcProto.ContractQueryRequest.Builder builder = RpcProto.ContractQueryRequest.newBuilder();
        builder.setContractAddr(contractAddress);
        if (key != null) {
            builder.setKey(key);
        }
        if (value != null) {
            builder.setValue(value);
        }
        RpcProto.ContractQueryRequest request = builder.build();
        RpcProto.ContractQueryResponse response = getRpcServiceBlockingStub().rpcContractQuery(request);
        ContractQueryResult result = new ContractQueryResult();
        result.setResultKey(response.getKey());
        result.setResultValue(response.getValue());
        return result;
    }
}
