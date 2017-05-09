package tests

import base.TestBaseSetup
import org.openqa.selenium.By
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.testng.Assert
import org.testng.annotations.AfterClass
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeClass
import org.testng.annotations.Listeners
import org.testng.annotations.Test
import pageobject.AssignmentPage
import pageobject.LoginPage
import pageobject.MapPage
import pageobject.PreviewMapPage
import utils.Action
import utils.AppServerAPI
import utils.Const
import utils.Log

/**
 * Created by polushynd on 5/5/2017.
 * 
 */

@Listeners(utils.Listener.class)
class ReturnToAssignmentsViewTest extends TestBaseSetup{
    protected RemoteWebDriver driver;
    protected AssignmentPage assignPage;
    protected MapPage mapPage
    protected PreviewMapPage previewPage

    @BeforeClass
    void setUp() {
        driver = getDriver();
        LoginPage loginPage = new LoginPage(driver);
        assignPage = loginPage.signInUser(Const.USER)
    }

    // Test 1: Check out document, revert and confirm it
    @Test(priority = 1)
    public void test1() {
        AppServerAPI.flushCollection('/content/authoring/')

        //A topic X is assigned to the current user with context Map A.
        String mapID = AppServerAPI.createTopicOrMap("/system/templates/maps/map.ditamap", "Map A", "Writer", "Authoring:work", 'MAP')
        String topicID = AppServerAPI.createTopicAndAddItToContextMap("/system/templates/topics/topic.xml", "Topic X", "Writer", "Authoring:work", mapID, AppServerAPI.session)
        AppServerAPI.replaceDocumentContent("/content/authoring/${mapID}.ditamap", "</map>", "<topicref href=\"${topicID}.xml\"/></map>")

        assignPage.clickRefreshBtn()
        //Open Map View
        mapPage = assignPage.openMapViewClick(mapID)

        //The title and id of map A are displayed in the Map view.
        boolean title = mapPage.mapElement.getText().contains('Map A')
        boolean id = mapPage.mapId.getText().contains(mapID)
        Assert.assertTrue(title && id, "Error: title: ${title}, id: ${id}")
    }

    @Test(priority = 2)
    public void test2() {
        AppServerAPI.flushCollection('/content/authoring/')

        //A topic X is assigned to the current user with context Map A.
        //The map A contains a reference to map B.
        //The topic X is nested in the child map of map A.

        //Create Map A
        String mapID_A = AppServerAPI.createTopicOrMap("/system/templates/maps/map.ditamap", "Map A", "Writer", "Authoring:work", 'MAP')

        //Create Map B
        String mapID_B = AppServerAPI.createTopicOrMap("/system/templates/maps/map.ditamap", "Map B", "Writer", "Authoring:work", 'MAP')

        //Create Topic X which has context ref to Map A
        String topicID_X = AppServerAPI.createTopicAndAddItToContextMap("/system/templates/topics/topic.xml", "Topic X", "Writer", "Authoring:work", mapID_A, AppServerAPI.session)

        //Add ref to Map A about Map B
        AppServerAPI.replaceDocumentContent("/content/authoring/${mapID_A}.ditamap", "</map>", "<mapref format=\"ditamap\" href=\"${mapID_B}.ditamap\"/></map>")

        //Add ref to Map B about Topic X
        AppServerAPI.replaceDocumentContent("/content/authoring/${mapID_B}.ditamap", "</map>", "<topicref href=\"${topicID_X}.xml\"/></map>")

        assignPage.clickRefreshBtn()

        //Open Map View
        mapPage = assignPage.openMapViewClick(mapID_A)

        //The topic X is displayed (the reference to map itself is invisible for the SME).
        boolean topicXisDisplayed = Action.getSource(driver).contains("Topic X")
        boolean mapAisDisplayed = Action.getSource(driver).contains("Map A")
        boolean mapBisDisplayed = Action.getSource(driver).contains("Map B")

        Assert.assertTrue(topicXisDisplayed && mapAisDisplayed && !mapBisDisplayed, "Some element is not or dispalyed: topicXisDisplayed - ${topicXisDisplayed}, " +
                "mapAisDisplayed - ${mapAisDisplayed}, mapBisDisplayed - ${mapBisDisplayed} -- should be not displayed")
    }

    @Test(priority = 3)
    public void test3() {

        AppServerAPI.flushCollection('/content/authoring/')

        //A topic X is assigned to the current user with context Map A.
        //The map A contains a structural topicref without a navtitle.
        //The topic X is nested in the structural topic ref of map A.
        //The user opens the map A.

        //Create Map A
        String mapID_A = AppServerAPI.createTopicOrMap("/system/templates/maps/map.ditamap", "Map A", "Writer", "Authoring:work", 'MAP')

        //Create Topic X which has context ref to Map A
        String topicID_X = AppServerAPI.createTopicAndAddItToContextMap("/system/templates/topics/topic.xml", "Topic X", "Writer", "Authoring:work", mapID_A, AppServerAPI.session)

        //Add ref to Map A about topic X
        AppServerAPI.replaceDocumentContent("/content/authoring/${mapID_A}.ditamap", "</map>", "<topicref><topicref href='${topicID_X}.xml'/></topicref></map>")

        assignPage.clickRefreshBtn()
        //The user opens the map A.
        mapPage = assignPage.openMapViewClick(mapID_A)
        boolean case1 = false

        try {
            WebDriverWait wait = new WebDriverWait(driver, 5);
            wait.until(ExpectedConditions.elementToBeClickable(mapPage.firstTopic))
            case1 = true
        }
        catch(e) {
            Log.error("Can not find the topic!")
            println e
        }

        Assert.assertTrue(case1, "The topic X is displayed (the structural topicref itself is invisible for the SME in this case).")
    }

    @Test(priority = 4)
    public void test4() {

        AppServerAPI.flushCollection('/content/authoring/')

        //A topic X is assigned to the current user with context Map A.
        //The map A contains a structural topicref with a navtitle.
        //The topic X is nested in the structural topic ref of map A.
        //The user opens the map A.

        //Create Map A
        String mapID_A = AppServerAPI.createTopicOrMap("/system/templates/maps/map.ditamap", "Map A", "Writer", "Authoring:work", 'MAP')
        //Create Topic X which has context ref to Map A
        String topicID_X = AppServerAPI.createTopicAndAddItToContextMap("/system/templates/topics/topic.xml", "Topic X", "Writer", "Authoring:work", mapID_A, AppServerAPI.session)

        //structural topic with navtitle attribute
        String str = "<topicref navtitle='Structural'><topicref href='${topicID_X}.xml'/></topicref>"

        AppServerAPI.replaceDocumentContent("/content/authoring/${mapID_A}.ditamap", "</map>", "${str}\n</map>")

        assignPage.clickRefreshBtn()
        //The user opens the map A.
        mapPage = assignPage.openMapViewClick(mapID_A)

        //The structural topic ref is displayed in this case.
        boolean case1 = mapPage.Structural.isDisplayed()
        mapPage.Structural.click()
        //The user can expand it to display topic X.
        boolean case2 = false
        try {
            WebDriverWait wait = new WebDriverWait(driver, 5);
            wait.until(ExpectedConditions.elementToBeClickable(mapPage.firstTopic))
            case2 = true
        }
        catch(e) {
            println e
        }

        Assert.assertTrue(case1 && case2, "The structural topic ref is not displayed in this case. or The user can expand it to display topic X. ${case1}, ${case2}")
    }

    @Test(priority = 5)
    public void test5() {
        AppServerAPI.flushCollection('/content/authoring/')
        //A topic X is assigned to the current user with context Map A.
        //The map A contains a topicgroup.
        //The topic X is nested in the topicgroup of map A.
        //The user opens the map A
        //Create Map A
        String mapID_A = AppServerAPI.createTopicOrMap("/system/templates/maps/map.ditamap", "Map A", "Writer", "Authoring:work", 'MAP')

        //Create Topic X which has context ref to Map A
        String topicID_X = AppServerAPI.createTopicAndAddItToContextMap("/system/templates/topics/topic.xml", "Topic X", "Writer", "Authoring:work", mapID_A, AppServerAPI.session)

        String content = "<topicgroup  collection-type=\"family\" toc=\"no\"><topicref><topicref href='${topicID_X}.xml'/></topicref></topicgroup>"

        //Add ref to Map A about topic X
        AppServerAPI.replaceDocumentContent("/content/authoring/${mapID_A}.ditamap", "</map>", "${content}</map>")

        assignPage.clickRefreshBtn()
        //The user opens the map A.
        mapPage = assignPage.openMapViewClick(mapID_A)
        Assert.assertTrue(!Action.getSource(driver).contains("topicgroup"), "The topic X is displayed (the topicgroup itself is invisible for the SME.")

    }

    @Test(priority = 6)
    public void test6() {
        AppServerAPI.flushCollection('/content/authoring/')
        //A topic X is assigned to the current user with context Map A.
        // The map A contains a topicstub.
        // The user opens the map A

        //Create Map A
        String mapID_A = AppServerAPI.createTopicOrMap("/system/templates/maps/map.ditamap", "Map A", "Writer", "Authoring:work", 'MAP')
        //Create Topic X which has context ref to Map A
        String topicID_X = AppServerAPI.createTopicAndAddItToContextMap("/system/templates/topics/topic.xml", "Topic X", "Writer", "Authoring:work", mapID_A, AppServerAPI.session)
        AppServerAPI.replaceDocumentContent("/content/authoring/${mapID_A}.ditamap", "</map>", "<topicref navtitle=\"Topic Stub\" type=\"Technical Content/topic.xml\"/><topicref href=\"${topicID_X}.xml\"/></map>")

        assignPage.clickRefreshBtn()
        //The user opens the map A.
        mapPage = assignPage.openMapViewClick(mapID_A)

        //The topicstub is displayed.
        Assert.assertTrue(Action.getSource(driver).contains("Topic Stub"), "TopicStun is not displayed")
    }

    @Test(priority = 7)
    public void test7() {
        AppServerAPI.flushCollection('/content/authoring/')

        //A topic X is assigned to the current user with context Map A.
        //The map A contains a topichead without a navtitle.
        //The topic X is nested in the topichead of map A.
        //The user opens the map A.

        //Create Map A
        String mapID_A = AppServerAPI.createTopicOrMap("/system/templates/maps/map.ditamap", "Map A", "Writer", "Authoring:work", 'MAP')
        //Create Topic X which has context ref to Map A
        String topicID_X = AppServerAPI.createTopicAndAddItToContextMap("/system/templates/topics/topic.xml", "Topic X", "Writer", "Authoring:work", mapID_A, AppServerAPI.session)
        String content = "<topichead><topicref href=\"${topicID_X}.xml\"/></topichead>"
        AppServerAPI.replaceDocumentContent("/content/authoring/${mapID_A}.ditamap", "</map>", "$content</map>")

        assignPage.clickRefreshBtn()
        //The user opens the map A.
        mapPage = assignPage.openMapViewClick(mapID_A)
        boolean case0 = false
        try {
            WebDriverWait wait = new WebDriverWait(driver, 5);
            wait.until(ExpectedConditions.elementToBeClickable(mapPage.firstTopic))
            case0 = true
        }
        catch(e){
            Log.error("Can not find the topic!: $e")
        }

        boolean case1 = Action.getSource(driver).contains("topichead")

        Assert.assertTrue(case0 && !case1, "The topic X is displayed $case0 (the topichead itself is invisible for the SME in this case) $case1.")

    }

    @Test(priority = 8)
    public void test8() {
        AppServerAPI.flushCollection('/content/authoring/')

        //A topic X is assigned to the current user with context Map A.
        //The map A contains a topichead with navtitle.
        //The topic X is nested in the topichead of map A.
        //The user opens the map A.

        //Create Map A
        String mapID_A = AppServerAPI.createTopicOrMap("/system/templates/maps/map.ditamap", "Map A", "Writer", "Authoring:work", 'MAP')
        //Create Topic X which has context ref to Map A
        String topicID_X = AppServerAPI.createTopicAndAddItToContextMap("/system/templates/topics/topic.xml", "Topic X", "Writer", "Authoring:work", mapID_A, AppServerAPI.session)
        String content = "<topichead navtitle=\"Parameter Entity elements\"><topicref href=\"${topicID_X}.xml\"/></topichead>"
        AppServerAPI.replaceDocumentContent("/content/authoring/${mapID_A}.ditamap", "</map>", "$content</map>")

        assignPage.clickRefreshBtn()
        //The user opens the map A.
        mapPage = assignPage.openMapViewClick(mapID_A)
        boolean case0 = mapPage.elementTitle.getText().contains("Parameter Entity elements")
        boolean case1 = mapPage.Structural.isDisplayed()
        mapPage.Structural.click()
        boolean case2 = false
        try {
            WebDriverWait wait = new WebDriverWait(driver, 5);
            wait.until(ExpectedConditions.elementToBeClickable(mapPage.firstTopic))
            case2 = true
        }
        catch(e){
            println "Can not find the topic!: $e"
        }

        Assert.assertTrue(case0 && case1 && case2, "The topichead is displayed in this case. : $case0,  The user can expand it to display topic X. : $case1")
    }

    @Test(priority = 9)
    public void test9() {

        AppServerAPI.flushCollection('/content/authoring/')

        //A topic X is assigned to the current user with context Map A.
        //The map A contains a reference to external link.
        //The user opens the map A.

        String mapID_A = AppServerAPI.createTopicOrMap("/system/templates/maps/map.ditamap", "Map A", "Writer", "Authoring:work", 'MAP')
        String topicID_X = AppServerAPI.createTopicAndAddItToContextMap("/system/templates/topics/topic.xml", "Topic X", "Writer", "Authoring:work", mapID_A, AppServerAPI.session)

        //Ext link example http://svn.ixiasoft.local/svn/webux/tags/wp_trunk_1/WebAuthorClient/WebAuthor_Client_Project/test/maps/externalLinkType.ditamap.xml
        String extLink = '''<topicref href='http://www.ixiasoft.com/navtitle-attr' scope='external' navtitle='IXIASOFT.com'/>
        <topicref href='http://wwww.ixiasoft.com/navtitle-element' scope='external'/>'''


        AppServerAPI.replaceDocumentContent("/content/authoring/${mapID_A}.ditamap", "</map>", "${extLink}\n<topicref href=\"${topicID_X}.xml\"/></map>")

        assignPage.clickRefreshBtn()

        //The user opens the map A.
        mapPage = assignPage.openMapViewClick(mapID_A)

        int counter = 0
        mapPage.externalLinks.each {
            if(it.getText().contains("IXIASOFT.com")){
                counter ++;
            }
            if(it.getText().contains("http://wwww.ixiasoft.com/navtitle-element")){
                counter ++;
            }
        }
        Assert.assertTrue( counter == 2, "The external link is not displayed")
    }

    @Test(priority = 10)
    public void test10() {
        AppServerAPI.flushCollection('/content/authoring/')
        //A topic X is assigned to the current user with context Map A.
        //The map A contains a topicset.
        //The user opens the map A.
        String mapID_A = AppServerAPI.createTopicOrMap("/system/templates/maps/map.ditamap", "Map A", "Writer", "Authoring:work", 'MAP')

        //Create Topic X which has context ref to Map A
        String topicID_X = AppServerAPI.createTopicAndAddItToContextMap("/system/templates/topics/topic.xml", "Topic X", "Writer", "Authoring:work", mapID_A, AppServerAPI.session)

        String content = "<topicset id=\"sqlbasics\"><topicref><topicref href='${topicID_X}.xml'/></topicref></topicset>"

        //Add ref to Map A about topic X
        AppServerAPI.replaceDocumentContent("/content/authoring/${mapID_A}.ditamap", "</map>", "${content}</map>")

        assignPage.clickRefreshBtn()
        //The user opens the map A.
        mapPage = assignPage.openMapViewClick(mapID_A)
        Assert.assertTrue(!Action.getSource(driver).contains("topicset"), "The topic X is displayed (the topicgroup itself is invisible for the SME.")

    }

    @Test(priority = 11)
    public void test11() {

        AppServerAPI.flushCollection('/content/authoring/')

        //A topic X is assigned to the current user with context Map A.
        //The user opens the map A.
        //Create Map A
        String mapID_A = AppServerAPI.createTopicOrMap("/system/templates/maps/map.ditamap", "Map A", "Writer", "Authoring:work", 'MAP')
        //Create Topic X which has context ref to Map A
        String topicID_X = AppServerAPI.createTopicAndAddItToContextMap("/system/templates/topics/topic.xml", "Topic X", "Writer", "Authoring:work", mapID_A, AppServerAPI.session)
        AppServerAPI.replaceDocumentContent("/content/authoring/${mapID_A}.ditamap", "</map>", "<topicref href=\"${topicID_X}.xml\"/></map>")

        assignPage.clickRefreshBtn()
        //The user opens the map A.
        mapPage = assignPage.openMapViewClick(mapID_A)
        boolean titleIsDisplayed = mapPage.firstTopic.getText().contains("Topic X")

        Assert.assertTrue( titleIsDisplayed, "Can not find The title of the rederenced topic X")
    }

    @AfterMethod
    void closeAllWindows(){
        def refreshArrow = driver.findElements(By.id("read-view-panel"))
        if(refreshArrow.size() > 0 && refreshArrow[0].isDisplayed()){
            refreshArrow[0].click()
        }
        def backToAssignView = driver.findElements(By.cssSelector("span.map-view-switch > button"))
        if(backToAssignView.size() > 0 && backToAssignView[0].isDisplayed() ){
            backToAssignView[0].click()
        }
    }

    @AfterClass
    void tearDown(){
        driver.quit()
    }
}