package com.org.app.network.request;

import com.org.app.network.Config;
import com.org.app.network.networkException.EntityNotFound;
import com.org.app.network.networkException.StateAlreadySet;
import org.hyperledger.fabric.sdk.ChaincodeID;
import org.hyperledger.fabric.sdk.ChaincodeResponse.Status;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.TransactionProposalRequest;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Set extends A_BlockchainRequest {
	private static final byte[] EXPECTED_EVENT_DATA = "!".getBytes(UTF_8);
	private static final String EXPECTED_EVENT_NAME = "event";
	private String entity = null;
	private String value = null;
	public Collection<ProposalResponse> responses;

	public Set(String entity, String value) {

		/* Enroll Admin to Org1MSP and initialize channel */
		super();
		this.entity = entity;
		this.value = value;
	}

	@Override
	public void send() throws EntityNotFound, StateAlreadySet, Exception {

		TransactionProposalRequest request = fabClient.getInstance().newTransactionProposalRequest();
		ChaincodeID ccid = ChaincodeID.newBuilder().setName(Config.CHAINCODE_1_NAME).build();
		request.setChaincodeID(ccid);
		request.setFcn("set");
		String[] arguments = { entity, value };
		request.setArgs(arguments);
		request.setProposalWaitTime(1000);

		Map<String, byte[]> tm2 = new HashMap<>();
		tm2.put("HyperLedgerFabric", "TransactionProposalRequest:JavaSDK".getBytes(UTF_8));
		tm2.put("method", "TransactionProposalRequest".getBytes(UTF_8));
		tm2.put("result", ":)".getBytes(UTF_8));
		tm2.put(EXPECTED_EVENT_NAME, EXPECTED_EVENT_DATA);
		request.setTransientMap(tm2);
		Collection<ProposalResponse> responses = channelClient.sendTransactionProposal(request);
		for (ProposalResponse res : responses) {
			Status status = res.getStatus();

			String payload = new String(res.getChaincodeActionResponsePayload());
			// Raising exception if the entity isn't in the BC
			// or if the state of the entity is already set to the value
			if (payload.equals("NOT_FOUND")) {
				throw new EntityNotFound("");
			}
			else if (payload.equals("STATE_ALREADY_SET")) {
				throw new StateAlreadySet("");
			}


			transactionID = res.getTransactionID();
			Logger.getLogger(Add.class.getName()).log(Level.INFO,
					"\n\n\n#----------------------------> Invoked set on " + Config.CHAINCODE_1_NAME
							+ ". Status - " + status + "\n\n\n");
		}

	}
}
