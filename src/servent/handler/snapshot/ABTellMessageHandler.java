package servent.handler.snapshot;

import app.AppConfig;
import app.CausalBroadcastShared;
import app.snapshot_bitcake.SnapshotCollector;
import servent.handler.MessageHandler;
import servent.handler.TransactionHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.util.MessageUtil;

public class ABTellMessageHandler implements MessageHandler {

    private Message clientMessage;
    private SnapshotCollector snapshotCollector;

    public ABTellMessageHandler(Message clientMessage, SnapshotCollector snapshotCollector) {
        this.clientMessage = clientMessage;
        this.snapshotCollector = snapshotCollector;
    }

    @Override
    public void run() {

        if (clientMessage.getMessageType() == MessageType.AB_TELL_AMOUNT){
            boolean didPut = TransactionHandler.receivedBroadcasts.add(clientMessage);
//            if(AppConfig.myServentInfo.getId() == 3){
//                AppConfig.timestampedErrorPrint(TransactionHandler.receivedBroadcasts.toString() + " lista " + didPut + " poruka " +
//                        clientMessage);
//            }
            if (didPut){
                if (AppConfig.myServentInfo.getId() == clientMessage.getSnapshotInitiatorId()){
                    snapshotCollector.addABSnapshotInfo(clientMessage.getOriginalSenderInfo().getId(), clientMessage.getSnapshotResult());
                }else {
                    for (Integer neighbor:AppConfig.myServentInfo.getNeighbors()){
                        clientMessage = clientMessage.changeReceiver(neighbor).makeMeASender();
                        MessageUtil.sendMessage(clientMessage);
                    }
                }


            }else {
                AppConfig.timestampedStandardPrint("No rebroadcast, seen this message");
            }
        }



    }
}
