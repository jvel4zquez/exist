package org.exist.numbering.test;

import junit.framework.TestCase;
import org.exist.util.Configuration;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.storage.StorageAddress;
import org.exist.storage.txn.TransactionManager;
import org.exist.storage.txn.Txn;
import org.exist.security.*;
import org.exist.security.xacml.AccessContext;
import org.exist.collections.Collection;
import org.exist.collections.IndexInfo;
import org.exist.xquery.XQuery;
import org.exist.xquery.value.Sequence;
import org.exist.dom.NodeProxy;
import org.exist.dom.StoredNode;
import org.w3c.dom.Node;
import org.w3c.dom.Attr;
import org.w3c.dom.Text;
/*
 *  eXist Open Source Native XML Database
 *  Copyright (C) 2001-06 The eXist Project
 *  http://exist-db.org
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 *  $Id$
 */

public class DLNStorageTest extends TestCase {

    private static String TEST_COLLECTION = DBBroker.ROOT_COLLECTION + "/test";

    private static String TEST_XML =
            "<test>\n" +
                    "    <para>My first paragraph.</para>\n" +
                    "    <!-- A comment -->\n" +
                    "    <para>This one contains a <a href=\"#\">link</a>.</para>\n" +
                    "    <?echo \"A processing instruction\"?>\n" +
                    "    <para>Another <b>paragraph</b>.</para>\n" +
                    "</test>";

    public void testNodeStorage() throws Exception {
        BrokerPool pool = BrokerPool.getInstance();
        DBBroker broker = null;
        try {
            broker = pool.get(org.exist.security.SecurityManager.SYSTEM_USER);
            XQuery xquery = broker.getXQueryService();
            assertNotNull(xquery);
            // test element ids
            Sequence seq = xquery.execute("doc('/db/test/test_string.xml')/test/para",
                    null, AccessContext.TEST);
            assertEquals(3, seq.getLength());
            NodeProxy comment = (NodeProxy) seq.itemAt(0);
            assertEquals(comment.getNodeId().toString(), "1.1");
            comment = (NodeProxy) seq.itemAt(1);
            assertEquals(comment.getNodeId().toString(), "1.3");
            comment = (NodeProxy) seq.itemAt(2);
            assertEquals(comment.getNodeId().toString(), "1.5");

            seq = xquery.execute("doc('/db/test/test_string.xml')/test//a",
                    null, AccessContext.TEST);
            assertEquals(1, seq.getLength());
            NodeProxy a = (NodeProxy) seq.itemAt(0);
            assertEquals("1.3.2", a.getNodeId().toString());

            // test attribute id
            seq = xquery.execute("doc('/db/test/test_string.xml')/test//a/@href",
                    null, AccessContext.TEST);
            assertEquals(1, seq.getLength());
            NodeProxy href = (NodeProxy) seq.itemAt(0);
            System.out.println(StorageAddress.toString(href.getInternalAddress()));
            assertEquals("1.3.2.1", href.getNodeId().toString());
            // test Attr deserialization
            Attr attr = (Attr) href.getNode();
            System.out.println(StorageAddress.toString(((StoredNode)attr).getInternalAddress()));
            // test Attr fields
            assertEquals(attr.getNodeName(), "href");
            assertEquals(attr.getName(), "href");
            assertEquals(attr.getValue(), "#");
             // test DOMFile.getNodeValue()
            assertEquals(href.getStringValue(), "#");

            // test text node
            seq = xquery.execute("doc('/db/test/test_string.xml')/test//b/text()",
                    null, AccessContext.TEST);
            assertEquals(1, seq.getLength());
            NodeProxy text = (NodeProxy) seq.itemAt(0);
            assertEquals("1.5.2.1", text.getNodeId().toString());
            // test DOMFile.getNodeValue()
            assertEquals(text.getStringValue(), "paragraph");
            // test Text deserialization
            Text node = (Text) text.getNode();
            assertEquals(node.getNodeValue(), "paragraph");
            assertEquals(node.getData(), "paragraph");
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            pool.release(broker);
        }
    }

    protected void setUp() throws Exception {
        String home, file = "conf.xml";
        home = System.getProperty("exist.home");
        if (home == null)
            home = System.getProperty("user.dir");
        try {
            Configuration config = new Configuration(file, home);
            BrokerPool.configure(1, 5, config);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        BrokerPool pool = BrokerPool.getInstance();
        DBBroker broker = null;
        try {
            broker = pool.get(org.exist.security.SecurityManager.SYSTEM_USER);
            TransactionManager transact = pool.getTransactionManager();

            Txn transaction = transact.beginTransaction();
            System.out.println("Transaction started ...");

            Collection test = broker.getOrCreateCollection(transaction, TEST_COLLECTION);
            broker.saveCollection(transaction, test);

            IndexInfo info = test.validateXMLResource(transaction, broker, "test_string.xml", TEST_XML);
            assertNotNull(info);

            test.store(transaction, broker, info, TEST_XML, false);

            transact.commit(transaction);
            System.out.println("Transaction commited ...");
        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            pool.release(broker);
        }
    }

    protected void tearDown() {
        BrokerPool.stopAll(false);
    }
}
