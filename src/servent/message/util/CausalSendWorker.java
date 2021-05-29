package servent.message.util;

import app.AppConfig;
import app.Cancellable;
import app.CausalBroadcastShared;
import app.snapshot_bitcake.ABitcakeManager;
import app.snapshot_bitcake.BitcakeManager;
import app.snapshot_bitcake.SnapshotCollector;
import servent.handler.TransactionHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.PoisonMessage;

import java.util.Arrays;

public class CausalSendWorker implements Runnable, Cancellable {

    BitcakeManager bitcakeManager;
    SnapshotCollector snapshotCollector;

    public CausalSendWorker(SnapshotCollector snapshotCollector) {
        this.snapshotCollector = snapshotCollector;
        this.bitcakeManager = snapshotCollector.getBitcakeManager();
    }

    @Override
    public void stop() {
        try {
//           CausalBroadcastShared.commitCausalMessage(new PoisonMessage());
            CausalBroadcastShared.commitedCausalMessageList.add(new PoisonMessage());
        } catch (Exception e) {
            AppConfig.timestampedErrorPrint(e.getMessage() + Arrays.toString(e.getStackTrace()));
        }
    }

    @Override
    public void run() {

        while (true){
            try {

                Message message = CausalBroadcastShared.commitedCausalMessageList.take();


                if (message.getMessageType() == MessageType.POISON)
                    break;


                if (message.getMessageType() == MessageType.TRANSACTION){
//                    AppConfig.timestampedErrorPrint("uso u if");
//                    if (message.getReceiverInfo().getId() == AppConfig.myServentInfo.getId()){
//                        AppConfig.timestampedErrorPrint("isti su");
//                        continue;
//                    }
                    if (message.getReceiverInfo().getId() == AppConfig.myServentInfo.getId()){
//                    AppConfig.timestampedErrorPrint(" trnas mess iz csw " + message.getReceiverInfo() + " my: " + AppConfig.myServentInfo);
                        TransactionHandler.handleTransaction(message);
                    }
                }

                if (message.getMessageType() == MessageType.AB_TOKEN){
                    ABitcakeManager abManager = (ABitcakeManager) bitcakeManager;
                    abManager.handleToken(message,snapshotCollector,abManager.getCurrentBitcakeAmount());
                }


            }catch (Exception e){
                AppConfig.timestampedErrorPrint(e.getMessage() + Arrays.toString(e.getStackTrace()));
            }
        }

    }
}
