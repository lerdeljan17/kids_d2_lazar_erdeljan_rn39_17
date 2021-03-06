package cli.command;

import app.AppConfig;
import app.CausalBroadcastShared;
import app.ServentInfo;
import app.snapshot_bitcake.BitcakeManager;
import servent.handler.TransactionHandler;
import servent.message.BasicMessage;
import servent.message.Message;
import servent.message.TransactionMessage;
import servent.message.util.MessageUtil;

import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TransactionBurstCommand implements CLICommand {

	private static final int TRANSACTION_COUNT = 5;
	private static final int BURST_WORKERS = 10;
	private static final int MAX_TRANSFER_AMOUNT = 10;
	
	//Chandy-Lamport
//	private static final int TRANSACTION_COUNT = 3;
//	private static final int BURST_WORKERS = 5;
//	private static final int MAX_TRANSFER_AMOUNT = 10;
	
	private BitcakeManager bitcakeManager;
	
	public TransactionBurstCommand(BitcakeManager bitcakeManager) {
		this.bitcakeManager = bitcakeManager;
	}
	
	private class TransactionBurstWorker implements Runnable {
		
		@Override
		public void run() {

			for (int i = 0; i < TRANSACTION_COUNT; i++) {
				Map<Integer,Integer> myClock = new ConcurrentHashMap<>(CausalBroadcastShared.getVectorClock());

				Message transactionMessage = new TransactionMessage(
						AppConfig.myServentInfo, null, 0, bitcakeManager,myClock);

				for (int neighbor : AppConfig.myServentInfo.getNeighbors()) {
					ServentInfo neighborInfo = AppConfig.getInfoById(neighbor);
					
					int amount = 1 + (int)(Math.random() * MAX_TRANSFER_AMOUNT);
					
					/*
					 * The message itself will reduce our bitcake count as it is being sent.
					 * The sending might be delayed, so we want to make sure we do the
					 * reducing at the right time, not earlier.
					 */

					transactionMessage.setMessageText(String.valueOf(amount));
					transactionMessage = transactionMessage.changeReceiver(neighbor);
					
					MessageUtil.sendMessage(transactionMessage);
				}

//				transactionMessage = transactionMessage.changeReceiver(AppConfig.myServentInfo.getId());
//				CausalBroadcastShared.commitCausalMessage(transactionMessage);
//				CausalBroadcastShared.addPendingMessage(transactionMessage);
//				CausalBroadcastShared.checkPendingMessages();
//				TransactionHandler.receivedBroadcasts.add(transactionMessage);
//				CausalBroadcastShared.incrementClock(transactionMessage.getOriginalSenderInfo().getId());
				// TODO: 14.5.2021. Mozda ne ovde, prebaceno u delayedMessSend
//				transactionMessage.sendEffect();
				CausalBroadcastShared.incrementClock(AppConfig.myServentInfo.getId());
			}
		}
	}
	
	@Override
	public String commandName() {
		return "transaction_burst";
	}

	@Override
	public void execute(String args) {
		for (int i = 0; i < BURST_WORKERS; i++) {
			Thread t = new Thread(new TransactionBurstWorker());
			
			t.start();
		}
	}

	
}
