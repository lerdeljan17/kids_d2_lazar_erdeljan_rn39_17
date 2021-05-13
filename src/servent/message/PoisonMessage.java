package servent.message;

import app.AppConfig;

public class PoisonMessage extends BasicMessage {

	private static final long serialVersionUID = -5625132784318034900L;

	public PoisonMessage() {
		super(MessageType.POISON, AppConfig.myServentInfo, null);
	}
}
