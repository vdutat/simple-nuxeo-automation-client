package org.nuxeo.vdutat;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.nuxeo.ecm.automation.client.Constants;
import org.nuxeo.ecm.automation.client.OperationRequest;
import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.client.adapters.DocumentService;
import org.nuxeo.ecm.automation.client.jaxrs.impl.HttpAutomationClient;
import org.nuxeo.ecm.automation.client.jaxrs.spi.auth.PortalSSOAuthInterceptor;
import org.nuxeo.ecm.automation.client.model.Document;
import org.nuxeo.ecm.automation.client.model.Documents;
import org.nuxeo.ecm.automation.client.model.PaginableDocuments;
import org.nuxeo.ecm.automation.client.model.PathRef;

/**
 * @author vdutat
 * 
 * mvn clean compile exec:java -Dexec.mainClass="org.nuxeo.vdutat.MyAutomationClient"
 */
public class MyAutomationClient {

	private static boolean usePortalSSO = false;

    public static void main(String[] args) throws Exception {
		HttpAutomationClient client = new HttpAutomationClient(
				"http://localhost:8080/nuxeo/site/automation" // From Nuxeo doc
//				"http://localhost:8080/nuxeo/site/api/v1/automation"
				);
		Session session;
		if (usePortalSSO) {
		    usePortalSSOAuthentication(client);
            session = client.getSession();
		} else {
		    session = client.getSession("Administrator", "Administrator");
		}
		    
//		query(session, "SELECT * FROM File where ecm:path STARTSWITH '/default-domain/workspaces/SUPNXP-13087'");
//		getDocumentHistory(session, "/default-domain/workspaces/ws1/file 1");
//		testSUPNXP13087(session);
//		testSUPNXP13087_eachDoc(session);
//		testSUPNXP13087_callQueryChainWithStringParam(session);
//      testSUPNXP13130(session);
//		testUndeleteDocument(session, "/default-domain/workspaces/ws1/file 1");
//		restoreVersion(session, "/default-domain/workspaces/ws1/file 1", "1.0");
//		testNXP(session, "/default-domain/workspaces/ws1/doc3");
		// SUPNXP-14547
//        testSUPNXP14547(session, "Document.Query", "/default-domain/workspaces/tmp");
//        query(session, "SELECT * FROM Document where ecm:path STARTSWITH '/default-domain/workspaces/tmp'");
//        testSUPNXP15586(session, "/default-domain/workspaces/SUPNXP-15586");
        testSUPNXP16421_updateMultiValuedProperty(session, "/default-domain/workspaces/SUPNXP-16421/File 001");
		
		client.shutdown();
	}

	private static void testNXP(Session session, String pathOrId) throws Exception {
	    printVersions(session, pathOrId);
	    getDocumentHistory(session, pathOrId);
    }

    private static void printVersions(Session session, String pathOrId) throws Exception {
        Document doc = (Document) session.newRequest("Document.Fetch")
                .set("value", pathOrId)
                .execute();
        Documents versions = (Documents) session.newRequest("Document.GetVersions")
                .setInput(doc)
                .setHeader(Constants.HEADER_NX_SCHEMAS, "*")
                .execute();
        if (!versions.isEmpty()) {
            for (Document version : versions) {
                System.out.println(version + ": version " + version.getVersionLabel() + " (" + version.getProperties().getString("dc:description") + ")");
            }
        } else {
            System.err.println("No version for document " + doc);
        }
    }

    private static void restoreVersion(Session session, String pathOrId, String versionLabel) throws Exception {
        Document doc = (Document) session.newRequest("Document.Fetch")
                .set("value", pathOrId)
                .execute();
        System.out.println("Restoring version " + versionLabel + " of document " + doc.getPath() + " (" + doc.getProperties().getString("dc:description") + ")");
        Documents versions = (Documents) session.newRequest("Document.GetVersions")
                .setInput(doc)
                .setHeader(Constants.HEADER_NX_SCHEMAS, "*")
                .execute();
        if (!versions.isEmpty() && !versionLabel.equals(doc.getVersionLabel())) {
            boolean versionFound = false;
            for (Document version : versions) {
                if (versionLabel.equals(version.getVersionLabel())) {
                    versionFound = true;
                    Document restoredDocument = (Document) session.newRequest("Document.RestoreVersion")
                            .setInput(version)
                            .execute();
                    System.out.println(doc + " restored to version " + restoredDocument.getVersionLabel() + " (" + restoredDocument.getProperties().getString("dc:description") + ")");
                    break;
                }
            }
            if (!versionFound) {
                System.err.println("No version " + versionLabel + " found for document " + doc);
            }
        } else {
            System.err.println("No version for document " + doc);
        }
    }

    private static void testUndeleteDocument(Session session, String pathOrId) throws Exception {
        Document doc = (Document) session.newRequest("Document.Fetch")
                .setHeader(Constants.HEADER_NX_SCHEMAS, "*")
                .set("value", pathOrId)
                .execute();
        System.out.println(doc + " - " + doc.getState());
        if (!"deleted".equals(doc.getState())) {
            doc = (Document) session.newRequest("Document.SetLifeCycle").setInput(doc)
//                    .setHeader(Constants.HEADER_NX_SCHEMAS, "*")
                    .set("value", "delete")
                    .execute();
        } else if ("deleted".equals(doc.getState())) {
            doc = (Document) session.newRequest("Document.SetLifeCycle").setInput(doc)
//                    .setHeader(Constants.HEADER_NX_SCHEMAS, "*")
                    .set("value", "undelete")
                    .execute();
        }
    }

    private static void query(Session session, String nxql) throws Exception {
		Documents docs = (Documents) session.newRequest("Document.Query")
				.setHeader(Constants.HEADER_NX_SCHEMAS, "*")
				.set("query",nxql)
				.execute(); // 1000 documents max. !!!!!!!!!!!!!!!!!
		int index = 0;
		if (!docs.isEmpty()) {
			System.out.println(docs);
			for (Document doc : docs) {
                System.out.println("index:" + ++index);
				System.out.println("title:" + doc.getTitle());
				System.out.println("state:" + doc.getState());
				System.out.println("version:" + doc.getVersionLabel());
				System.out.println("Last modification:" + doc.getLastModified());
				System.out.println("Creator:" + doc.getString("dc:creator"));
			}
		}
	}

	private static void getDocumentHistory(Session session, String pathOrId) throws Exception {
		Document doc = (Document) session.newRequest("Document.Fetch")
				.setHeader(Constants.HEADER_NX_SCHEMAS, "*")
				.set("value", pathOrId)
				.execute();
		Object result = session.newRequest("Audit.PageProvider")
				.setHeader(Constants.REQUEST_ACCEPT_HEADER, MediaType.APPLICATION_JSON)
				.set("query", "FROM LogEntry log WHERE log.docUUID='" + doc.getId() + "'")
				.set("pageSize", 500)
				.set("currentPageIndex", 0)
				.execute();
		System.out.println("result:" + result);
		JsonNode node = (JsonNode) result;
		
		int count = node.get("currentPageSize").getValueAsInt();
		JsonNode entries = node.get("entries");
		for (int i = 0; i < count; i++) {
			System.out.println("event:" + entries.get(i).get("category").getValueAsText() + "/" + entries.get(i).get("eventId").getValueAsText());
			System.out.println(" event date:" + entries.get(i).get("eventDate").getValueAsText());
			System.out.println(" event comment:" + entries.get(i).get("comment").getValueAsText());
			System.out.println(" lifecycle:" + entries.get(i).get("docLifeCycle").getValueAsText());
			System.out.println(" user:" + entries.get(i).get("principalName").getValueAsText());
		}
	}
	
	protected static void testSUPNXP13087(Session session) throws Exception {
		Documents docs = (Documents) session.newRequest("Document.Query")
				.set("query","SELECT * FROM File where ecm:path STARTSWITH '/default-domain/workspaces/SUPNXP-13087'")
				.execute();
		if (!docs.isEmpty()) {
			System.out.println("Documents to delete:" + docs);
			session.newRequest("Document.Delete").setInput(docs).execute();
			System.out.println("List deleted: " + docs);
		} else {
			System.out.println("No documents found");
		}
	}
	
	protected static void testSUPNXP13087_eachDoc(Session session) throws Exception {
		Documents docs = (Documents) session.newRequest("Document.Query")
				.set("query", "SELECT * FROM File WHERE ecm:path STARTSWITH '/default-domain/workspaces/SUPNXP-13087'")
				.execute();
		if (!docs.isEmpty()) {
            OperationRequest newRequest = session.newRequest("Document.Delete");
  			for (Document doc : docs) {
  				newRequest.setInput(doc).execute();
  				System.out.println("Deleted: " + doc);
  			}
		} else {
			System.out.println("No documents found");
		}
	}

	protected static void testSUPNXP13087_callQueryChainWithStringParam(Session session) throws Exception {
		Documents docs = (Documents) session.newRequest("Document.Query")
				.set("query", "SELECT * FROM File WHERE ecm:path STARTSWITH '/default-domain/workspaces/SUPNXP-13087'")
				.execute();
		if (!docs.isEmpty()) {
			List<String> ids = new ArrayList<String>();
			StringBuilder sb = new StringBuilder();
			for (Document doc : docs) {
				ids.add("'" + doc.getId() + "'");
			}
			String idsString = StringUtils.join(ids, ',');
			System.out.println("*" + idsString);
			docs = (Documents) session.newRequest("SUPNXP-13087")
					.set("idsString", idsString)
					.execute();
			if (!docs.isEmpty()) {
				System.out.println("**" + docs);
			}
		} else {
			System.out.println("No document 'File' found");
		}
	}
    
    protected static void testSUPNXP13130(Session session) throws Exception {
        Documents docs = (Documents) session.newRequest("Document.Query")
                .set("query", "SELECT * FROM File WHERE ecm:path STARTSWITH '/default-domain/workspaces/SUPNXP-13087'")
                .execute();
        if (!docs.isEmpty()) {
            OperationRequest request = session.newRequest("Context.RunOperationOnList");
            request.set("id", "SUPNXP-13130_foreach");
            request.set("list", "list");
            request.setContextProperty("list", docs);
            request.set("isolate", "true");
            request.set("item", "item");
            request.execute();
        }

    }
    
    protected static void testSUPNXP14547(Session session, String opName, String pathOrId) throws Exception {
        Document doc = (Document) session.newRequest("Document.Fetch")
                .setHeader(Constants.HEADER_NX_SCHEMAS, "*")
                .set("value", pathOrId)
                .execute();
        final int pageSize = 100;
//        String opName = "Document.Query";
//        String opName = "Document.PageProvider";
        if ("Document.Query".equals(opName)) {
            OperationRequest request = session.newRequest(opName)
                    .setHeader(Constants.HEADER_NX_SCHEMAS, "*")
                    .set("query", "SELECT * FROM Document WHERE ecm:parentId = '" + doc.getId() + "'")
                    .set("pageSize", pageSize);
            PaginableDocuments docs;
            int pageIndex = 0;
            do {
                docs = (PaginableDocuments) request.set("currentPageIndex", pageIndex).execute();
                System.out.println("Page " + (docs.getCurrentPageIndex() + 1) + "/" + docs.getNumberOfPages());
                for (Document elem : docs) {
                    System.out.println("- " + elem.getId() + " - " + elem.getString("dc:title"));
                }
                pageIndex++;
            } while ((docs.getCurrentPageIndex()+1) < docs.getNumberOfPages());
        } else {
            // Document.PageProvider
            OperationRequest request = session.newRequest("Document.PageProvider")
                    .setHeader(Constants.HEADER_NX_SCHEMAS, "*")
                    .set("maxResults", "-1")
                    .set("query", "SELECT * FROM Document WHERE ecm:parentId = '" + doc.getId() + "'")
                    .set("pageSize", pageSize);
            PaginableDocuments docs;
            int pageIndex = 0;
            do {
            docs = (PaginableDocuments) request.set("currentPageIndex", pageIndex).execute();
                System.out.println("Page " + (docs.getCurrentPageIndex() + 1) + "/" + docs.getNumberOfPages());
                for (Document elem : docs) {
                    System.out.println("- " + elem.getId() + " - " + elem.getString("dc:title"));
                }
                pageIndex++;
            } while ((docs.getCurrentPageIndex()+1) < docs.getNumberOfPages());
        }
    }
    
    protected static void testSUPNXP15586(Session session, String pathOrId) throws Exception {
        Documents docs = (Documents) session.newRequest("Document.Query")
                .set("query", "SELECT * FROM File WHERE ecm:path STARTSWITH '" + pathOrId + "'")
                .execute();
        if (!docs.isEmpty()) {
            Documents updatedDocs = (Documents) session.newRequest("Document.Update")
                .setHeader(Constants.HEADER_NX_SCHEMAS, "*")
                .setInput(docs).set("save", "true")
                .set("properties", "dc:description=updated to test SUPNXP-15586")
                .execute();
            assert docs.size() == updatedDocs.size();
        }
    }

    private static void testSUPNXP16421_updateMultiValuedProperty(Session session, String pathOrId) throws Exception {
        DocumentService documentService = session.getAdapter(DocumentService.class);
        // Fetch the document with all its properties (all schemas)
        Document doc = documentService.getDocument(new PathRef(pathOrId), "*");
        // Modify multi-valued property: add 3 elements: "arts", "business" and "c,d"
        doc.set("dc:subjects", "arts,business,c\\,d");
        // Update document in repository
        documentService.update(doc);
    }
    
    private static void usePortalSSOAuthentication(HttpAutomationClient client) {
        client.setRequestInterceptor(new PortalSSOAuthInterceptor("nuxeo5secretkey", "Administrator"));
    }
}
