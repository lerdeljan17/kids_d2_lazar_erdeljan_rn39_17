package servent.handler.snapshot;

import app.AppConfig;
import app.CausalBroadcastShared;
import app.ServentInfo;
import app.snapshot_bitcake.BitcakeManager;
import app.snapshot_bitcake.SnapshotCollector;
import servent.handler.MessageHandler;
import servent.handler.TransactionHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.TransactionMessage;
import servent.message.util.MessageUtil;

public class ABTokenHandler implements MessageHandler {


    private Message clientMessage;
//    private BitcakeManager bitcakeManager;
    private SnapshotCollector snapshotCollector;

    public ABTokenHandler(Message clientMessage, SnapshotCollector snapshotCollector) {
        this.clientMessage = clientMessage;
        this.snapshotCollector = snapshotCollector;
    }

    @Override
    public void run() {

        //Sanity check.
        if (clientMessage.getMessageType() == MessageType.AB_TOKEN) {
            ServentInfo senderInfo = clientMessage.getOriginalSenderInfo();
            ServentInfo lastSenderInfo = clientMessage.getRoute().size() == 0 ?
                    clientMessage.getOriginalSenderInfo() :
                    clientMessage.getRoute().get(clientMessage.getRoute().size()-1);

            /*
             * The standard read message already prints out that we got a msg.
             * However, we also want to see who sent this to us directly, besides from
             * seeing the original owner - if we are not in a clique, this might
             * not be the same node.
             */
            String text = String.format("Got %s from %s broadcast by %s",
                    clientMessage.getMessageText(), lastSenderInfo, senderInfo);

            AppConfig.timestampedStandardPrint(text);

            if (senderInfo.getId() == AppConfig.myServentInfo.getId()) {
                //We are the sender :o someone bounced this back to us. /ignore
                AppConfig.timestampedStandardPrint("Got own message back. No rebroadcast.");
            } else {
                //Try to put in the set. Thread safe add ftw.
                boolean didPut = TransactionHandler.receivedBroadcasts.add(clientMessage);

                if (didPut) {
                    //New message for us. Rebroadcast it.

                    CausalBroadcastShared.addPendingMessage(clientMessage);
                    CausalBroadcastShared.checkPendingMessages();

                    AppConfig.timestampedStandardPrint("Rebroadcasting... ");

                    for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
                        //Same message, different receiver, and add us to the route table.
                        MessageUtil.sendMessage(clientMessage.changeReceiver(neighbor).makeMeASender());
                    }

                } else {
                    //We already got this from somewhere else. /ignore
                    AppConfig.timestampedStandardPrint("Already had this. No rebroadcast.");
                }
            }



        }


    }
}
