/*
 *  eXist Open Source Native XML Database
 *  Copyright (C) 2001-04 The eXist Project
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
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *  
 *  $Id$
 */
package org.exist.xquery.test;

import java.io.File;

import org.custommonkey.xmlunit.XMLTestCase;
import org.exist.storage.DBBroker;
import org.exist.xmldb.DatabaseInstanceManager;
import org.exist.xmldb.IndexQueryService;
import org.exist.xmldb.XQueryService;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Database;
import org.xmldb.api.base.ResourceSet;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.CollectionManagementService;
import org.xmldb.api.modules.XMLResource;

public class FtQueryTest extends XMLTestCase {
 
	private final static String TEST_XML =
		"<test-doc>" +
			"<test-elem id=\"1\" attribute1=\"test some text\"/>" +
			"<test-elem id=\"2\" attribute2=\"test some text\"/>" +
			"<test-elem id=\"3\" attribute1=\"test some text\"/>" +
			"<test-elem id=\"4\" attribute3=\"test some text\"/>" +
		"</test-doc>";
	
    private final static String FILES[] = { "hamlet.xml", "macbeth.xml", "r_and_j.xml" };
    
    static File existDir;
    static {
      String existHome = System.getProperty("exist.home");
      existDir = existHome==null ? new File(".") : new File(existHome);
    }
    private final static File SHAKES_DIR = new File(existDir,"samples" + File.separator + "shakespeare");
    private final static File MODS_DIR = new File(existDir,"samples" + File.separator + "mods");

	private static final String TEST_COLLECTION_NAME = "testft";
    private static final String TEST_COLLECTION_PATH = DBBroker.ROOT_COLLECTION + "/" + TEST_COLLECTION_NAME;
    
    private Database database;
    private Collection testCollection;
    
    public void testFtOperators() {
    	try {
	    	System.out.println("----- testFtOperators -----");
	        XQueryService service = (XQueryService)
	            testCollection.getService("XQueryService", "1.0");
	        ResourceSet result = service.query("//SPEECH[LINE &= 'love']");
	        assertEquals(160, result.getSize());
	        result = service.query("//SPEECH[LINE &= 'thou']");
	        assertEquals(290, result.getSize());
	        result = service.query("//SPEECH[LINE &= 'thou']");
	        assertEquals(290, result.getSize());
	        result = service.query("//SPEECH[LINE &= 'fenny snake']/LINE[1]");
	        assertEquals(1, result.getSize());
	        assertXMLEqual(result.getResource(0).getContent().toString(), "<LINE>Fillet of a fenny snake,</LINE>");
	        result = service.query("//SPEECH[LINE &= 'god*']");
	        assertEquals(79, result.getSize());
	        result = service.query("//SPEECH[LINE &= 'god in heaven']");
	        assertEquals(2, result.getSize());
	        result = service.query("//SPEECH[SPEAKER &= 'Nurse']");
	        assertEquals(90, result.getSize());
	        result = service.query("declare namespace mods='http://www.loc.gov/mods/v3'; //mods:titleInfo[mods:title &= 'self*']");
	        assertEquals(2, result.getSize());
	        result = service.query("declare namespace mods='http://www.loc.gov/mods/v3'; //mods:titleInfo[mods:title &= 'self employed']");
	        assertEquals(1, result.getSize());
	        result = service.query("declare namespace mods='http://www.loc.gov/mods/v3'; //mods:titleInfo[match-all(mods:title, '.*ploy.*')]");
	        assertEquals(3, result.getSize());

            result = service.query("declare namespace mods='http://www.loc.gov/mods/v3'; " +
                    "//mods:titleInfo/mods:title[. &= 'alternative']");
            assertEquals(0, result.getSize());

            result = service.query("declare namespace mods='http://www.loc.gov/mods/v3'; " +
                    "//mods:titleInfo/mods:title[attribute() &= 'alternative']");
            assertEquals(9, result.getSize());

            result = service.query("declare namespace mods='http://www.loc.gov/mods/v3'; " +
                    "//mods:titleInfo/mods:title[@type &= 'alternative']");
            assertEquals(9, result.getSize());
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
		}
	}
    
    public void testFtScan() {
    	try {
	    	System.out.println("----- testFtScan -----");
	        String queryBody =
	            "declare namespace f=\'http://exist-db.org/xquery/test\';\n" + 
	            "declare namespace mods='http://www.loc.gov/mods/v3';\n" + 
	            "import module namespace t=\'http://exist-db.org/xquery/text\';\n" + 
	            "\n" + 
	            "declare function f:term-callback($term as xs:string, $data as xs:int+)\n" + 
	            "as element()+ {\n" + 
	            "    <item>\n" + 
	            "        <term>{$term}</term>\n" + 
	            "        <frequency>{$data[1]}</frequency>\n" + 
	            "    </item>\n" + 
	            "};\n" + 
	            "\n";
	        
	        XQueryService service = (XQueryService)
	            testCollection.getService("XQueryService", "1.0");
	        String query = queryBody + "t:index-terms(collection('" + TEST_COLLECTION_PATH + "'), \'is\', util:function(\'f:term-callback\', 2), 1000)";
	        ResourceSet result = service.query(query);
	        assertEquals(7, result.getSize());
	        
	        query = queryBody + "t:index-terms(collection('"  + TEST_COLLECTION_PATH + "')//LINE, \'is\', util:function(\'f:term-callback\', 2), 1000)";
	        result = service.query(query);
	        assertEquals(6, result.getSize());
	        
	        query = queryBody + "t:index-terms(collection('" + TEST_COLLECTION_PATH + "')//mods:title, \'s\', util:function(\'f:term-callback\', 2), 1000)";
	        result = service.query(query);
	        assertEquals(20, result.getSize());
		} catch (XMLDBException e) {
			fail(e.getMessage());
		}	        
    }
    
    public void testFtUpdate() {
    	try {
	    	System.out.println("----- testFtUpdate -----");
	        XQueryService service = (XQueryService)
	            testCollection.getService("XQueryService", "1.0");
	        service.query(
	                "update insert <SPEAKER>First Witch</SPEAKER> preceding //SPEECH[LINE &= 'fenny snake']/SPEAKER"
	        );
	        ResourceSet result = service.query("//SPEECH[LINE &= 'fenny snake']/SPEAKER");
	        assertEquals(2, result.getSize());
	        result = service.query("//SPEECH[LINE &= 'fenny snake' and SPEAKER &= 'first']");
	        assertEquals(1, result.getSize());
	        
	        service.query(
	                "update delete //SPEECH[LINE &= 'fenny snake']/SPEAKER[2]"
	        );
	        result = service.query("//SPEECH[LINE &= 'fenny snake' and SPEAKER &= 'first']");
	        assertEquals(1, result.getSize());
		} catch (XMLDBException e) {
			fail(e.getMessage());
		}	        
    }

    public void testFtConfiguration() {
    	System.out.println("----- testFtConfiguration -----");
    	try {
    		// check attributes="false"
    		String config =
    			"<collection xmlns=\"http://exist-db.org/collection-config/1.0\">" +
		    	"    <index>" +
		    	"        <fulltext default=\"all\" attributes=\"false\" alphanum=\"true\">" +
		    	"					<include path=\"//test-elem/@attribute1\"/>" +
		    	"				</fulltext>" +
		    	"    </index>" +
		    	"</collection>";
    		IndexQueryService idxConf = (IndexQueryService)
				testCollection.getService("IndexQueryService", "1.0");
    		idxConf.configureCollection(config);
    		
    		XMLResource doc =
                (XMLResource) testCollection.createResource(
                        "test-attributes.xml", "XMLResource");
            doc.setContent(TEST_XML);
            testCollection.storeResource(doc);
            
            XQueryService service = (XQueryService)
            	testCollection.getService("XQueryService", "1.0");
            String query = "//test-elem[@* &= 'some text']";
            ResourceSet result = service.query(query);
            assertEquals(2, result.getSize());
            
            // check attributes="true"
            config =
    			"<collection xmlns=\"http://exist-db.org/collection-config/1.0\">" +
		    	"    <index>" +
		    	"        <fulltext default=\"all\" attributes=\"true\" alphanum=\"true\">" +
		    	"					<exclude path=\"//test-elem/@attribute2\"/>" +
		    	"				</fulltext>" +
		    	"    </index>" +
		    	"</collection>";
            idxConf.configureCollection(config);
            idxConf.reindexCollection();
            
            result = service.query(query);
            assertEquals(3, result.getSize());
    	} catch(Exception e) {
    		fail(e.getMessage());
    	}
    }
    
    protected void setUp() {
        try {
            // initialize driver
            Class cl = Class.forName("org.exist.xmldb.DatabaseImpl");
            database = (Database) cl.newInstance();
            database.setProperty("create-database", "true");
            DatabaseManager.registerDatabase(database);
            
            Collection root =
                DatabaseManager.getCollection("xmldb:exist://" + DBBroker.ROOT_COLLECTION, "admin", null);
            CollectionManagementService service =
                (CollectionManagementService) root.getService(
                    "CollectionManagementService",
                    "1.0");
            testCollection = service.createCollection(TEST_COLLECTION_NAME);
            assertNotNull(testCollection);

            for (int i = 0; i < FILES.length; i++) {
                XMLResource doc =
                    (XMLResource) testCollection.createResource(
                            FILES[i], "XMLResource" );
                doc.setContent(new File(SHAKES_DIR, FILES[i]));
                testCollection.storeResource(doc);                
                assertNotNull(testCollection.getResource(FILES[i]));
            }
            
            File modsFiles[] = MODS_DIR.listFiles();
            for (int i = 0; i < modsFiles.length; i++) {
                if (modsFiles[i].isFile()) {
                    XMLResource doc =
                        (XMLResource) testCollection.createResource(
                        		modsFiles[i].getName(), "XMLResource" );
                    doc.setContent(modsFiles[i]);
                    testCollection.storeResource(doc);
                    assertNotNull(testCollection.getResource(modsFiles[i].getName()));
                }
            }
        } catch (ClassNotFoundException e) {
        } catch (InstantiationException e) {
        } catch (IllegalAccessException e) {
        } catch (XMLDBException e) {
            e.printStackTrace();
        }
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() {
    	try {
	        Collection root =
	            DatabaseManager.getCollection("xmldb:exist://" + DBBroker.ROOT_COLLECTION, "admin", null);
	        CollectionManagementService service =
	            (CollectionManagementService) root.getService(
	                "CollectionManagementService",
	                "1.0");
	        service.removeCollection(TEST_COLLECTION_NAME);
	        
	        DatabaseManager.deregisterDatabase(database);
	        DatabaseInstanceManager dim =
	            (DatabaseInstanceManager) testCollection.getService(
	                "DatabaseInstanceManager", "1.0");
	        dim.shutdown();
            database = null;
            testCollection = null;
	        System.out.println("tearDown PASSED");
		} catch (XMLDBException e) {
			fail(e.getMessage());
		}
    }
}
