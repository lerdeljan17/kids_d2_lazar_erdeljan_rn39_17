package servent.handler;

import app.AppConfig;
import app.CausalBroadcastShared;
import app.ServentInfo;
import app.snapshot_bitcake.BitcakeManager;
import app.snapshot_bitcake.SnapshotType;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.TransactionMessage;
import servent.message.util.MessageUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TransactionHandler implements MessageHandler {

	private Message clientMessage;
	private BitcakeManager bitcakeManager;
	public static Set<Message> receivedBroadcasts = Collections.newSetFromMap(new ConcurrentHashMap<Message, Boolean>());

	public TransactionHandler(Message clientMessage, BitcakeManager bitcakeManager) {
		this.clientMessage = clientMessage;
		this.bitcakeManager = bitcakeManager;
	}

	@Override
	public void run() {


		//Sanity check.
		if (clientMessage.getMessageType() == MessageType.TRANSACTION) {




//			TransactionMessage transactionMessage = (TransactionMessage) clientMessage;
				if (clientMessage.getOriginalSenderInfo().getId() == AppConfig.myServentInfo.getId()) {
					//We are the sender :o someone bounced this back to us. /ignore
					AppConfig.timestampedStandardPrint("Got own message back. No rebroadcast.");

				} else {
					//Try to put in the set. Thread safe add ftw.
					boolean didPut = receivedBroadcasts.add(clientMessage);

					if (didPut) {
						//New message for us. Rebroadcast it.


						clientMessage.setBitcakeManager(bitcakeManager);
						CausalBroadcastShared.addPendingMessage(clientMessage);
						CausalBroadcastShared.checkPendingMessages();
						AppConfig.timestampedStandardPrint("Rebroadcasting... " + receivedBroadcasts.size());

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

	public static void handleTransaction(Message clientMessage){

//		AppConfig.timestampedErrorPrint("uso u if poceeetak");

		if (clientMessage.getMessageType() == MessageType.TRANSACTION) {

			BitcakeManager bitcakeManager = clientMessage.getBitcakeManager();

			String amountString = clientMessage.getMessageText();

			int amountNumber = 0;
			try {
				amountNumber = Integer.parseInt(amountString);
			} catch (NumberFormatException e) {
				AppConfig.timestampedErrorPrint("Couldn't parse amount: " + amountString);
				return;
			}

			bitcakeManager.addSomeBitcakes(amountNumber);

			// TODO: 14.5.2021. deo za ab kad se primi poruka
			if (AppConfig.SNAPSHOT_TYPE == SnapshotType.AB){
//				AppConfig.timestampedErrorPrint("uso u if kod ab");
				synchronized (CausalBroadcastShared.RLock){
					try {
						if (!CausalBroadcastShared.RECD.containsKey(clientMessage.getOriginalSenderInfo().getId())) {

							List<Integer> toAdd = new ArrayList<>();
							toAdd.add(Integer.parseInt(clientMessage.getMessageText()));
							CausalBroadcastShared.RECD.put(clientMessage.getOriginalSenderInfo().getId(), toAdd);
//							AppConfig.timestampedErrorPrint("uspee da doda "  +  toAdd + " " + clientMessage.getOriginalSenderInfo().getId());

						} else {
//							AppConfig.timestampedErrorPrint("dodajem u recd ");
							CausalBroadcastShared.RECD.get(clientMessage.getOriginalSenderInfo().getId()).add(Integer.parseInt(clientMessage.getMessageText()));
						}

					} catch (Exception e) {
						AppConfig.timestampedErrorPrint(e.getMessage() + Arrays.toString(e.getStackTrace()));
					}
				}
			}


				} else {
			AppConfig.timestampedErrorPrint("Transaction handler got: " + clientMessage);
		}

	}

}
