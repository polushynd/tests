import unittest
from selenium import webdriver
import requests, json
import xmlrunner
import unicodedata
import time

driver = webdriver.PhantomJS()
url = 'http://172.20.40.216:8090/webauthorx'
user = 'tester1'
cookie = None

def setUpModule():
    global cookie
    driver.get(url)
    elem = driver.find_element_by_id("username")
    elem.send_keys(user)
    elem = driver.find_element_by_css_selector('button[type="submit"]').click()
    all_cookies = driver.get_cookies()
    jSession = all_cookies[0].get('value')
    cookie = {'JSESSIONID' : jSession}

def tearDownModule():
    driver.close()

class Test(unittest.TestCase):


   def test_001_assignments(self):
        api_url = url+'/rest/1.0/assignments/tester1@ixiasoft/contributor/active'
        response = requests.get(api_url, cookies=cookie)
        result = json.loads(response.text)['result'][0]['assignedTo'][0].get('name')
        self.assertEqual(result, 'Dmytro Polushyn [dmytro.polushyn@ixiasoft.com]')

   def test_002_documentsSubList(self):
        api_url = url+'/rest/1.0/documents/metadata/subList'
        data = { 'data' : ['/content/authoring/yop1469453046440.xml']}
        response = requests.post(api_url, cookies=cookie, data=json.dumps(data))
        json.loads(response.text)['result'][0].get('path')
        self.assertEqual(json.loads(response.text)['result'][0].get('path') , '/content/authoring/yop1469453046440.xml')

   def test_003_usersSubList(self):
        api_url = url+'/rest/1.0/users/subList'
        data = { 'data' :  ['Dmytro Polushyn [dmytro.polushyn@ixiasoft.com]'] }
        response = requests.post(api_url, cookies=cookie, data=json.dumps(data))
        self.assertEqual(json.loads(response.text)['result'][0].get('name').get('first'), 'Dmytro')

   def test_004_xeditorContent(self):
        api_url = url+'/rest/1.0/documents/%7Ccontent%7Cauthoring%7Cyop1469453046440.xml/xeditorContent'
        response = requests.get(api_url, cookies=cookie)
        print response.text.encode('utf-8')
        result = response.text.encode('utf-8')
        self.assertIn('topic_with_text', result)

   def test_005_lockState(self):
        api_url = url+'/rest/1.0/documents/%7Ccontent%7Cauthoring%7Cxus1470248936339.xml/lockState'
        response = requests.post(api_url, cookies=cookie)
        self.assertEqual(json.loads(response.text)['result'][0].get('assignedTo')[0].get('state'), 'todo' )

   def test_006_UnlockState(self):
        api_url = url+'/rest/1.0/documents/%7Ccontent%7Cauthoring%7Cxus1470248936339.xml/lockState'
        response = requests.delete(api_url, cookies=cookie)
        self.assertEqual(json.loads(response.text)['result'][0].get('assignedTo')[0].get('state'), 'todo' )

   def test_007_lockState(self):
        api_url = url+'/rest/1.0/documents/%7Ccontent%7Cauthoring%7Cxus1470248936339.xml/lockState'
        response = requests.post(api_url, cookies=cookie)
        self.assertEqual(json.loads(response.text)['result'][0].get('assignedTo')[0].get('state'), 'todo' )


   def test_008_xeditorContent_Post(self):
        api_url = url+'/rest/1.0/documents/%7Ccontent%7Cauthoring%7Cxus1470248936339.xml/xeditorContent'
        data = { 'data' : {
        'documentPath' : '/content/authoring/xus1470248936339.xml',
        'documentHTML' : '<div xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" data-type="topic" data-attrname-xmllang="xmllang" data-attrvalue-xmllang="en-us" data-attrname-id="id" data-attrvalue-id="xus1470248936339"><div data-type="title" data-attrname-ixia_locid="ixia_locid" data-attrvalue-ixia_locid="1"><span data-type="text">topic5</span></div><div data-type="shortdesc" data-attrname-ixia_locid="ixia_locid" data-attrvalue-ixia_locid="2"><span data-type="text"></span></div><div data-type="body"><div data-type="p" data-attrname-ixia_locid="ixia_locid" data-attrvalue-ixia_locid="3"><span data-type="text">topic13</span></div></div></div>',
         'typeIdentifier' : ''
                        }
        }
        response = requests.post(api_url, cookies=cookie, data=json.dumps(data))
        self.assertEqual(json.loads(response.text).get('error').get('code'), 0)

   def test_009_newVersion(self):
        api_url = url+'/rest/1.0/documents/%7Ccontent%7Cauthoring%7Cxus1470248936339.xml/newVersion'
        response = requests.put(api_url, cookies=cookie)
        self.assertEqual(json.loads(response.text)['result'][0].get('documentPath'), '/content/authoring/xus1470248936339.xml')


if __name__ == "__main__":
        with open('unittest_results.xml', 'wb') as output:
			unittest.main(testRunner=xmlrunner.XMLTestRunner(output=output), failfast=True, buffer=False, catchbreak=False)