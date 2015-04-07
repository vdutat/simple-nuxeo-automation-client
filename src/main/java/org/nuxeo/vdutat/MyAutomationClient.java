package org.nuxeo.vdutat;

import org.nuxeo.ecm.automation.client.jaxrs.impl.HttpAutomationClient;
import org.nuxeo.ecm.automation.client.model.Documents;
import org.nuxeo.ecm.automation.client.OperationRequest;
import org.nuxeo.ecm.automation.client.Session;

/**
 * @author vdutat
 * 
 * mvn clean compile exec:java -Dexec.mainClass="org.nuxeo.vdutat.MyAutomationClient"
 */
public class MyAutomationClient {

	public static void main(String[] args) throws Exception {
		HttpAutomationClient client = new HttpAutomationClient(
				"http://localhost:8080/nuxeo/site/automation");
		Session session = client.getSession("Administrator", "Administrator");
		Documents docs = (Documents) session.newRequest("Document.Query")
				.set("query","SELECT * FROM File where ecm:path STARTSWITH '/default-domain/workspaces/SUPNXP-13087'")
				.execute();
		if (!docs.isEmpty()) {
			System.out.println(docs);
		}
//		testSUPNXP13087(session);
//		testSUPNXP13087_callChain(session);
		client.shutdown();
	}
	
	protected static void testSUPNXP13087(Session session) throws Exception {
		Documents docs = (Documents) session.newRequest("Document.Query")
				.set("query","SELECT * FROM File where ecm:path STARTSWITH '/default-domain/workspaces/SUPNXP-13087'")
				.execute();
		if (!docs.isEmpty()) {
			System.out.println(docs);
			session.newRequest("Document.Delete").setInput(docs).execute();
		}
	}
	
	protected static void testSUPNXP13087_callChain(Session session) throws Exception {
		Documents docs = (Documents) session.newRequest("Document.Query")
				.set("query","SELECT * FROM File WHERE ecm:path STARTSWITH '/default-domain/workspaces/SUPNXP-13087'")
				.execute();
		if (!docs.isEmpty()) {
			OperationRequest newRequest = session.newRequest("Document.Delete");
			newRequest.setInput(docs).execute();
			System.out.println("List deleted: " + docs);
//			for (Document doc : docs) {
//				newRequest.setInput(doc).execute();
//				System.out.println("Deleted: " + doc);
//			}
		} else {
			System.out.println("No documents found");
		}
	}

}
