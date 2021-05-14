package app.snapshot_bitcake;

import app.AppConfig;
import app.CausalBroadcastShared;
import app.ServentInfo;
import servent.message.Message;
import servent.message.TokenMessage;
import servent.message.snapshot.ABTellAmountMessage;
import servent.message.util.MessageUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ABitcakeManager implements BitcakeManager{

    private final AtomicInteger currentAmount = new AtomicInteger(1000);

    @Override
    public void takeSomeBitcakes(int amount) {
        currentAmount.getAndAdd(-amount);
    }

    @Override
    public void addSomeBitcakes(int amount) {
        currentAmount.getAndAdd(amount);
    }

    @Override
    public int getCurrentBitcakeAmount() {
        return currentAmount.get();
    }


    public void handleToken(Message clientMessage, SnapshotCollector snapshotCollector, int currentBitcakeAmount) {

        ABSnapshotResult abSnapshotResult = new ABSnapshotResult(AppConfig.myServentInfo.getId(),currentBitcakeAmount);
        if (AppConfig.myServentInfo.getId() == clientMessage.getOriginalSenderInfo().getId()){
            //dodam svoj rez
            snapshotCollector.addABSnapshotInfo(clientMessage.getOriginalSenderInfo().getId(),abSnapshotResult);
        }else {
            // TODO: 13.5.2021. broadcast tell message komsijama
            Message tellMessage = new ABTellAmountMessage(AppConfig.myServentInfo,null,abSnapshotResult,clientMessage.getOriginalSenderInfo().getId());
            for(Integer neighbor:AppConfig.myServentInfo.getNeighbors()) {

                tellMessage = tellMessage.changeReceiver(neighbor);
                MessageUtil.sendMessage(tellMessage);
        }
        }

    }



    public void doSnapshot() {

        // TODO: 13.5.2021. begin snap
        Map<Integer, Integer> myClock = new ConcurrentHashMap<>(CausalBroadcastShared.getVectorClock());


        Message token = new TokenMessage(AppConfig.myServentInfo,null, AppConfig.myServentInfo.getId(), myClock);

        for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
            token = token.changeReceiver(neighbor);
            MessageUtil.sendMessage(token);
        }
        token.changeReceiver(AppConfig.myServentInfo.getId());
        MessageUtil.sendMessage(token);
        CausalBroadcastShared.commitCausalMessage(token);

//        token.sendEffect();

    }

    }
